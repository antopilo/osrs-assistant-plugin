package com.osrsassistant;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "OSRS Assistant"
)
public class OsrsAssistantPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OsrsAssistantConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private net.runelite.client.eventbus.EventBus eventBus;

	@Inject
	private net.runelite.client.plugins.PluginManager pluginManager;

	private NavigationButton navigationButton;
	private ChecklistOverlay checklistOverlay;
	private HighlightOverlay highlightOverlay;
	private WidgetHighlightOverlay widgetHighlightOverlay;
	private NavigationManager navigationManager;
	private ChecklistManager checklistManager;
	private ObjectTracker objectTracker;
	private StepResolver stepResolver;
	private OsrsAssistantPanel panel;
	private MenuBuilder menuBuilder;
	private PlayerContextCollector contextCollector;
	private volatile PlayerContext cachedContext;
	private int tickCounter = 0;

	// Track skill levels to detect level-ups
	private final java.util.Map<Skill, Integer> lastKnownLevels = new java.util.HashMap<>();
	private boolean levelsInitialized = false;

	// Track region to detect area changes
	private int lastRegionId = -1;

	// When true, manual GO TO navigation takes priority over checklist auto-nav
	private volatile boolean manualNavigation = false;

	@Override
	protected void startUp()
	{
		contextCollector = new PlayerContextCollector(client, itemManager);
		cachedContext = new PlayerContext();

		checklistOverlay = new ChecklistOverlay(this);
		highlightOverlay = new HighlightOverlay(client, this, itemManager);
		widgetHighlightOverlay = new WidgetHighlightOverlay(client, this, itemManager);
		navigationManager = new NavigationManager(client, eventBus, pluginManager);
		checklistManager = new ChecklistManager(client, itemManager);
		objectTracker = new ObjectTracker(client);
		stepResolver = new StepResolver(client, itemManager, objectTracker);
		checklistManager.setStepResolver(stepResolver);
		overlayManager.add(checklistOverlay);
		overlayManager.add(highlightOverlay);
		overlayManager.add(widgetHighlightOverlay);
		checklistOverlay.registerMouse(mouseManager);

		panel = new OsrsAssistantPanel(this, config, spriteManager, itemManager);
		menuBuilder = new MenuBuilder(client, panel, navigationManager, checklistManager,
			() -> cancelNavigation(), () -> navigateToCurrentChecklistStep());

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navigationButton = NavigationButton.builder()
			.tooltip("OSRS Assistant")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navigationButton);
		log.debug("OSRS Assistant started!");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		checklistOverlay.unregisterMouse(mouseManager);
		overlayManager.remove(checklistOverlay);
		overlayManager.remove(highlightOverlay);
		overlayManager.remove(widgetHighlightOverlay);
		navigationManager.cancel();
		checklistManager.clear();
		log.debug("OSRS Assistant stopped!");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("osrsassistant".equals(event.getGroup()))
		{
			String key = event.getKey();
			if ("llmProvider".equals(key) || "apiKey".equals(key) || "llmModelId".equals(key) || "backendUrl".equals(key) || "customPrompt".equals(key))
			{
				panel.refreshService();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		tickCounter++;
		if (tickCounter % 5 == 0)
		{
			cachedContext = contextCollector.collect();
		}

		// Preload sprite icons on the client thread so the panel can use them on the EDT
		panel.preloadIcons();

		// Update pathfinding navigation
		navigationManager.update();

		// Update checklist conditions (always runs, even during manual nav)
		if (checklistManager.hasChecklist())
		{
			boolean stepCompleted = checklistManager.onGameTick();
			checklistManager.resolveMicroAction(cachedContext);

			// Only auto-navigate if no manual GO TO is active
			if (!manualNavigation)
			{
				if (stepCompleted)
				{
					navigateToCurrentChecklistStep();
				}
				else
				{
					applyMicroActionNavigation();
				}
			}
			else if (!navigationManager.isNavigating())
			{
				// Manual nav finished (arrived or path ended) — resume checklist nav
				manualNavigation = false;
				navigateToCurrentChecklistStep();
			}
		}

		// Process pending item icon preloads
		if (!pendingItemIcons.isEmpty())
		{
			for (Integer itemId : new java.util.ArrayList<>(pendingItemIcons))
			{
				panel.preloadItemIcon(itemId);
				pendingItemIcons.remove(itemId);
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		int newLevel = event.getLevel();

		if (!levelsInitialized)
		{
			// First load — just record current levels, don't trigger prompts
			lastKnownLevels.put(skill, newLevel);
			if (lastKnownLevels.size() >= 23)
			{
				levelsInitialized = true;
			}
			return;
		}

		Integer oldLevel = lastKnownLevels.get(skill);
		lastKnownLevels.put(skill, newLevel);

		if (oldLevel != null && newLevel > oldLevel)
		{
			// Level up detected!
			String question = "I just leveled " + skill.getName() + " from " + oldLevel + " to " + newLevel
				+ ". What new training methods, gear, quests, or content does this unlock?";
			panel.askAssistant(question);
		}

		// Route XP changes to checklist for xp: condition checking
		if (checklistManager.hasChecklist())
		{
			int currentXp = event.getXp();
			boolean stepCompleted = checklistManager.onXpChanged(skill, currentXp);
			if (stepCompleted)
			{
				navigateToCurrentChecklistStep();
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Route to checklist for item condition checking
		if (checklistManager.hasChecklist())
		{
			boolean stepCompleted = checklistManager.onGameTick(); // re-check item conditions
			if (stepCompleted)
			{
				navigateToCurrentChecklistStep();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// Route to checklist for chat condition checking
		// GAMEMESSAGE: quest/system messages. SPAM: repetitive action messages (bury, cook, craft, etc.)
		if ((event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			&& checklistManager.hasChecklist())
		{
			String msg = Text.removeTags(event.getMessage());
			boolean stepCompleted = checklistManager.onChatMessage(msg);
			if (stepCompleted)
			{
				navigateToCurrentChecklistStep();
			}
		}

		// Detect slayer task assignment
		if (event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			String msg = Text.removeTags(event.getMessage());

			// Slayer task messages: "You're assigned to kill X; only Y more to go." or "Your new task is to kill X."
			if (msg.contains("assigned to kill") || msg.contains("new task is to kill"))
			{
				String monster = extractSlayerMonster(msg);
				if (monster != null)
				{
					panel.askAssistant("I just got a Slayer task to kill " + monster
						+ ". Where should I kill them, what gear/prayers should I use, and should I cannon?");
				}
			}

			// Death detection: "Oh dear, you are dead!"
			if (msg.contains("Oh dear, you are dead"))
			{
				PlayerContext ctx = getPlayerContext();
				String location = ctx != null ? "region " + ctx.getRegionId() : "unknown location";
				panel.askAssistant("I just died at " + location
					+ ". What might have killed me and how can I avoid dying here next time?");
			}
		}
	}

	private String extractSlayerMonster(String message)
	{
		// "You're assigned to kill gargoyles; only 142 more to go."
		java.util.regex.Matcher m1 = java.util.regex.Pattern.compile(
			"(?:assigned to kill|task is to kill)\\s+([\\w\\s]+?)(?:;|\\.|$)").matcher(message);
		if (m1.find())
		{
			return m1.group(1).trim();
		}
		return null;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		menuBuilder.onMenuEntryAdded(event);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!event.getCommand().equalsIgnoreCase("ask"))
		{
			return;
		}

		String[] args = event.getArguments();
		if (args.length == 0)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: ::ask <question>", "");
			return;
		}

		String question = String.join(" ", args);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=82b4ff>Old Wise Man is thinking...</col>", "");

		PlayerContext playerContext = getPlayerContext();
		new Thread(() ->
		{
			String reply = panel.getAssistantService().sendMessage(question, "chat-command", playerContext);
			if (reply == null)
			{
				return;
			}

			// Strip markdown and widget tags for game chat display
			String chatReply = reply
				.replaceAll("\\{skill:([^}:]+):(\\d+)\\}", "$2 $1")
				.replaceAll("\\{item:([^}:]+)(?::\\d+)?\\}", "$1")
				.replaceAll("\\{quest:([^}]+)\\}", "$1")
				.replaceAll("\\{goto:(?:object:|npc:)?([^}:]+)(?::\\d+:\\d+)?\\}", "$1")
				.replaceAll("\\{loc:([^}:]+)(?::\\d+:\\d+)?\\}", "$1")
				.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
				.replaceAll("\\*(.+?)\\*", "$1")
				.replaceAll("`([^`]+)`", "$1")
				.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
				.replaceAll("#{1,4}\\s", "");

			// Must add chat messages on the client thread
			clientThread.invokeLater(() ->
			{
				String[] lines = chatReply.split("\n");
				for (String line : lines)
				{
					String trimmed = line.trim();
					if (!trimmed.isEmpty())
					{
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
							"<col=64c864>[Old Wise Man]</col> " + trimmed, "");
					}
				}
			});
		}).start();
	}

	public PlayerContext getPlayerContext()
	{
		return cachedContext;
	}

	private final java.util.Set<Integer> pendingItemIcons = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

	public void requestItemIconPreload(int itemId)
	{
		pendingItemIcons.add(itemId);
	}

	public void navigateTo(int worldX, int worldY)
	{
		navigateTo(worldX, worldY, null);
	}

	public void navigateTo(int worldX, int worldY, String name)
	{
		String label = name != null ? name : "destination";
		createGotoChecklist(label, worldX, worldY);
	}

	public boolean isNavigating()
	{
		return navigationManager != null && navigationManager.isNavigating();
	}

	public String getNavigationName()
	{
		return navigationManager != null ? navigationManager.getDestinationName() : null;
	}

	public int getNavigationDistance()
	{
		return navigationManager != null ? navigationManager.getDistanceToDestination() : -1;
	}

	public OsrsAssistantPanel getPanel()
	{
		return panel;
	}

	public ChecklistManager getChecklistManager()
	{
		return checklistManager;
	}

	public void setChecklist(Checklist checklist)
	{
		checklistManager.setChecklist(checklist);
		navigateToCurrentChecklistStep();
	}

	public void addChecklist(Checklist checklist)
	{
		checklistManager.addChecklist(checklist);
		navigateToCurrentChecklistStep();
	}

	public void clearChecklist()
	{
		checklistManager.clear();
	}

	public void navigateAfterChecklistChange()
	{
		navigateToCurrentChecklistStep();
	}

	private void navigateToCurrentChecklistStep()
	{
		Checklist tracked = checklistManager.getTrackedChecklist();
		if (tracked == null || tracked.isComplete())
		{
			cancelNavigation();
			return;
		}

		List<WorldPoint> destinations = new java.util.ArrayList<>();
		List<String> names = new java.util.ArrayList<>();

		for (ChecklistGroup group : tracked.getGroups())
		{
			if (group.isCompleted()) continue;

			boolean groupHasStepGoto = false;
			for (ChecklistStep step : group.getSubsteps())
			{
				if (step.isCompleted()) continue;
				if (step.hasGoto())
				{
					destinations.add(new WorldPoint(step.getGotoX(), step.getGotoY(), 0));
					names.add(step.getGotoName());
					groupHasStepGoto = true;
				}
			}

			if (!groupHasStepGoto && group.hasGoto())
			{
				destinations.add(new WorldPoint(group.getGotoX(), group.getGotoY(), 0));
				names.add(group.getGotoName());
			}
		}

		if (!destinations.isEmpty())
		{
			if (destinations.size() > 1)
			{
				navigationManager.navigateSequential(destinations, names);
			}
			else
			{
				navigationManager.navigateTo(destinations.get(0), names.get(0));
			}
			return;
		}

		tryNavigateToStepTarget();
	}

	private void tryNavigateToStepTarget()
	{
		Checklist tracked = checklistManager.getTrackedChecklist();
		if (tracked == null) { cancelNavigation(); return; }

		ChecklistStep active = tracked.getActiveStep();
		if (active == null || active.isCompleted()) { cancelNavigation(); return; }

		String desc = active.getDescription();
		if (desc == null || desc.isEmpty()) { cancelNavigation(); return; }

		String searchName = active.getGotoName();
		if (searchName == null)
		{
			searchName = TextUtils.extractTarget(desc);
		}
		if (searchName == null) { cancelNavigation(); return; }

		final String target = searchName;
		clientThread.invokeLater(() ->
		{
			WorldPoint pos = objectTracker.findNearest(target);
			if (pos != null)
			{
				navigationManager.navigateTo(pos, target);
			}
		});
	}


	/**
	 * Micro-action is the source of truth for navigation.
	 * NAVIGATE → set nav target (auto-updates as player moves)
	 * INTERACT with objectName → find object in scene, navigate to it
	 * INFO or null → cancel navigation (player is where they need to be)
	 */
	private void applyMicroActionNavigation()
	{
		MicroAction action = checklistManager.getCurrentMicroAction();
		if (action == null)
		{
			return;
		}

		if (action.getType() == MicroAction.ActionType.NAVIGATE && action.getNavTarget() != null)
		{
			// Update navigation target — recalculated every tick so this tracks dynamically
			WorldPoint current = navigationManager.getDestination();
			if (current == null || !current.equals(action.getNavTarget()))
			{
				navigationManager.navigateTo(action.getNavTarget(), action.getObjectName());
			}
		}
		else if (action.getType() == MicroAction.ActionType.INTERACT && action.getObjectName() != null)
		{
			// Find the object in the scene and navigate to it
			WorldPoint objPos = objectTracker.findNearest(action.getObjectName());
			if (objPos != null)
			{
				WorldPoint current = navigationManager.getDestination();
				if (current == null || !current.equals(objPos))
				{
					navigationManager.navigateTo(objPos, action.getObjectName());
				}
			}
			else if (navigationManager.isNavigating())
			{
				// Object not in scene — stop navigating
				navigationManager.cancel();
			}
		}
		else
		{
			// INFO action or no nav needed — cancel any active navigation
			if (navigationManager.isNavigating())
			{
				navigationManager.cancel();
			}
		}
	}

	public MicroAction getCurrentMicroAction()
	{
		return checklistManager != null ? checklistManager.getCurrentMicroAction() : null;
	}

	public void navigateToObject(String objectName)
	{
		createGotoChecklistByName(objectName);
	}

	public void navigateToNpc(String npcName)
	{
		createGotoChecklistByName(npcName);
	}

	public void navigateToByName(String name)
	{
		createGotoChecklistByName(name);
	}

	public void navigateToNearest(String name)
	{
		createGotoChecklistByName(name);
	}

	/**
	 * Create a simple 1-step "Go to X" checklist with coordinates, track it, and start navigating.
	 */
	private void createGotoChecklist(String name, int worldX, int worldY)
	{
		Checklist cl = new Checklist("Go to " + name);
		ChecklistGroup group = new ChecklistGroup(name);
		group.setGotoX(worldX);
		group.setGotoY(worldY);
		group.setGotoName(name);
		ChecklistStep step = new ChecklistStep("Arrive at " + name, StepCondition.parse("loc:" + name));
		step.setGotoX(worldX);
		step.setGotoY(worldY);
		step.setGotoName(name);
		group.addStep(step);
		cl.addGroup(group);

		checklistManager.addAndTrack(cl);
		manualNavigation = false;
		navigateToCurrentChecklistStep();
	}

	/**
	 * Create a GO TO checklist by name — resolves coordinates from database, scene, or creates without coords.
	 */
	private void createGotoChecklistByName(String name)
	{
		int[] coords = LocationDatabase.getCoordinates(name);
		if (coords != null)
		{
			createGotoChecklist(name, coords[0], coords[1]);
			return;
		}

		// Try scene scan on client thread, fall back to creating without coords
		clientThread.invokeLater(() ->
		{
			WorldPoint pos = objectTracker.findNearest(name);
			if (pos != null)
			{
				createGotoChecklist(name, pos.getX(), pos.getY());
				return;
			}

			WorldPoint fallback = stepResolver.lookupLocation(name);
			if (fallback != null)
			{
				createGotoChecklist(name, fallback.getX(), fallback.getY());
				return;
			}

			// No coordinates found — still create the task without navigation.
			// The checklist will show and the player can navigate manually.
			log.info("No coordinates for '{}' — creating task without navigation", name);
			Checklist cl = new Checklist("Go to " + name);
			ChecklistGroup group = new ChecklistGroup(name);
			ChecklistStep step = new ChecklistStep("Arrive at " + name, StepCondition.parse("loc:" + name));
			step.setGotoName(name);
			group.addStep(step);
			cl.addGroup(group);
			checklistManager.addAndTrack(cl);
			manualNavigation = false;
		});
	}

	public void cancelNavigation()
	{
		manualNavigation = false;
		clientThread.invokeLater(() -> navigationManager.cancel());
	}

	public void showOnWorldMap(int worldX, int worldY)
	{
		clientThread.invokeLater(() ->
		{
			WorldPoint point = new WorldPoint(worldX, worldY, 0);
			client.getRenderOverview().setWorldMapPositionTarget(point);
		});
	}

	@Provides
	OsrsAssistantConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OsrsAssistantConfig.class);
	}
}
