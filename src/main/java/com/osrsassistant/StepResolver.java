package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resolves a checklist step into a single MicroAction — the immediate next thing the player should do.
 * Runs every game tick for the active step, purely client-side logic.
 *
 * The resolver handles universal flows (inventory → bank → coins → GE) that work for any item.
 * Crafting knowledge and material breakdowns are the backend AI's job — it should generate
 * granular checklist steps rather than expecting the resolver to know recipes.
 */
@Slf4j
public class StepResolver
{
	private final Client client;
	private final ItemManager itemManager;
	private final ObjectTracker objectTracker;

	private static final int BANK_PROXIMITY = 10;
	private static final int GE_REGION_ID = 12598;

	private static final String[] BANK_OBJECTS = {"Bank booth", "Bank chest", "Grand Exchange booth", "Bank chest box"};
	private static final String[] BANK_NPCS = {"Banker"};

	public StepResolver(Client client, ItemManager itemManager, ObjectTracker objectTracker)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.objectTracker = objectTracker;
	}

	/**
	 * Resolve the active step into a single MicroAction based on current game state.
	 * Returns null if the step is already complete.
	 */
	public MicroAction resolve(ChecklistStep step, PlayerContext ctx)
	{
		if (step == null || step.isCompleted())
		{
			return null;
		}

		StepCondition cond = step.getCondition();
		if (cond == null)
		{
			return resolveNoCondition(step);
		}

		switch (cond.getType())
		{
			case ITEM:
				return resolveItem(step, cond, ctx);
			case CHAT:
				return resolveChat(step, cond);
			case SKILL:
				return resolveSkill(cond);
			case QUEST:
				return resolveQuest(cond);
			case LOCATION:
				return resolveLocation(step, cond);
			default:
				return new MicroAction(step.getDescription(), MicroAction.ActionType.INFO);
		}
	}

	private MicroAction resolveNoCondition(ChecklistStep step)
	{
		// Try to find an interactable from the description in the scene
		MicroAction sceneAction = resolveFromDescription(step);
		if (sceneAction != null)
		{
			return sceneAction;
		}

		// Fall back to goto coordinates or name-based lookup
		MicroAction navAction = resolveNavigation(step);
		if (navAction != null)
		{
			return navAction;
		}

		log.debug("No navigation resolved for step: {}", step.getDescription());
		return new MicroAction(step.getDescription(), MicroAction.ActionType.INFO);
	}

	/**
	 * item:Name:Qty — Universal resolution for any item:
	 * 1. Have it in inventory? → done
	 * 2. Have it in bank? → withdraw flow
	 * 3. Don't have it → coins check → GE
	 */
	private MicroAction resolveItem(ChecklistStep step, StepCondition cond, PlayerContext ctx)
	{
		String itemName = cond.getParam1();
		int requiredQty = parseQty(cond.getParam2());

		// 1. Already have enough in inventory?
		int inventoryCount = countInInventory(itemName);
		if (inventoryCount >= requiredQty)
		{
			return null; // Step should auto-complete
		}

		int stillNeed = requiredQty - inventoryCount;

		// 2. Have it in bank? Withdraw.
		int bankCount = countInBank(itemName, ctx);
		if (bankCount > 0)
		{
			int toWithdraw = Math.min(bankCount, stillNeed);
			return resolveBankWithdraw(itemName, toWithdraw);
		}

		// 3. Don't have it anywhere — need to acquire. Check coins first.
		return resolveAcquire(itemName, stillNeed, ctx);
	}

	/**
	 * Player needs to acquire an item they don't have.
	 * Check coins before sending to GE.
	 */
	private MicroAction resolveAcquire(String itemName, int qty, PlayerContext ctx)
	{
		int coinsInInventory = countInInventory("Coins");
		int coinsInBank = countInBank("Coins", ctx);

		// No coins in inventory but have some in bank — withdraw coins first
		if (coinsInInventory == 0 && coinsInBank > 0)
		{
			return resolveBankWithdraw("Coins", 1);
		}

		// No coins anywhere
		if (coinsInInventory == 0 && coinsInBank == 0)
		{
			return new MicroAction("Need coins to buy " + itemName + formatQty(qty),
				MicroAction.ActionType.INFO);
		}

		// Have coins — go to GE
		return resolveGeBuy(itemName, qty);
	}

	/**
	 * Guide player to withdraw items from bank.
	 */
	private MicroAction resolveBankWithdraw(String itemName, int qty)
	{
		// Bank already open?
		if (isBankOpen())
		{
			return new MicroAction("Withdraw " + itemName + formatQty(qty),
				MicroAction.ActionType.INFO, null, null,
				Collections.singletonList(itemName), Collections.emptyList());
		}

		// Find nearest bank (object or NPC)
		BankResult bank = findNearestBankWithName();
		if (bank != null)
		{
			if (bank.dist > BANK_PROXIMITY)
			{
				return new MicroAction("Go to nearest bank (" + bank.dist + " tiles)",
					MicroAction.ActionType.NAVIGATE, bank.pos, null);
			}
			else
			{
				// Highlight both bank objects and NPCs
				List<String> highlightObjs = new java.util.ArrayList<>(Arrays.asList(BANK_OBJECTS));
				Collections.addAll(highlightObjs, BANK_NPCS);
				return new MicroAction("Open bank -> Withdraw " + itemName + formatQty(qty),
					MicroAction.ActionType.INTERACT, null, bank.name,
					Collections.singletonList(itemName), highlightObjs);
			}
		}

		return new MicroAction("Go to a bank to withdraw " + itemName + formatQty(qty),
			MicroAction.ActionType.INFO);
	}

	/**
	 * Guide player to buy items from GE.
	 */
	private MicroAction resolveGeBuy(String itemName, int qty)
	{
		if (isAtGe())
		{
			return new MicroAction("Buy " + itemName + formatQty(qty) + " from GE",
				MicroAction.ActionType.INFO);
		}

		WorldPoint gePoint = new WorldPoint(3165, 3487, 0);
		int dist = getDistanceTo(gePoint);
		return new MicroAction("Go to Grand Exchange (" + dist + " tiles)",
			MicroAction.ActionType.NAVIGATE, gePoint, "Grand Exchange");
	}

	/**
	 * chat:keyword — Player needs to trigger a game message.
	 */
	private MicroAction resolveChat(ChecklistStep step, StepCondition cond)
	{
		// First, try to find the interactable from the description in the scene.
		MicroAction sceneAction = resolveFromDescription(step);
		if (sceneAction != null)
		{
			return sceneAction;
		}

		// Try goto name as an object in the scene
		if (step.getGotoName() != null)
		{
			WorldPoint objPos = objectTracker.findNearest(step.getGotoName());
			if (objPos != null)
			{
				int objDist = getDistanceTo(objPos);
				if (objDist <= 5)
				{
					return new MicroAction("Interact with " + step.getGotoName(),
						MicroAction.ActionType.INTERACT, null, step.getGotoName());
				}
				else
				{
					return new MicroAction("Go to " + step.getGotoName() + " (" + objDist + " tiles)",
						MicroAction.ActionType.NAVIGATE, objPos, step.getGotoName());
				}
			}
		}

		// Nothing in scene — fall back to goto coordinates or name-based lookup
		MicroAction navAction = resolveNavigation(step);
		if (navAction != null)
		{
			return navAction;
		}

		log.debug("No navigation resolved for chat step: {} (goto={})",
			step.getDescription(), step.getGotoName());
		return new MicroAction(step.getDescription(), MicroAction.ActionType.INFO);
	}

	/**
	 * skill:Name:Level — Long-term goal, just show progress.
	 */
	private MicroAction resolveSkill(StepCondition cond)
	{
		String skillName = cond.getParam1();
		int requiredLevel = parseQty(cond.getParam2());

		int currentLevel = 1;
		for (Skill skill : Skill.values())
		{
			if (skill.getName().equalsIgnoreCase(skillName))
			{
				currentLevel = client.getRealSkillLevel(skill);
				break;
			}
		}

		if (currentLevel >= requiredLevel)
		{
			return null;
		}

		return new MicroAction("Train " + skillName + " to " + requiredLevel + " (currently " + currentLevel + ")",
			MicroAction.ActionType.INFO);
	}

	/**
	 * quest:Name — Just show the quest name.
	 */
	private MicroAction resolveQuest(StepCondition cond)
	{
		return new MicroAction("Complete " + cond.getParam1(),
			MicroAction.ActionType.INFO);
	}

	/**
	 * loc:Region — Navigate to location.
	 */
	private MicroAction resolveLocation(ChecklistStep step, StepCondition cond)
	{
		// Try goto coordinates first, then name-based fallback
		MicroAction navAction = resolveNavigation(step);
		if (navAction != null)
		{
			return navAction;
		}

		// Try the condition param as a location name
		WorldPoint resolved = lookupLocation(cond.getParam1());
		if (resolved != null)
		{
			int dist = getDistanceTo(resolved);
			if (dist > 5)
			{
				return new MicroAction("Go to " + cond.getParam1() + " (" + dist + " tiles)",
					MicroAction.ActionType.NAVIGATE, resolved, null);
			}
		}

		log.warn("Failed to resolve location step: {} (param={})", step.getDescription(), cond.getParam1());
		return new MicroAction("Go to " + cond.getParam1(), MicroAction.ActionType.INFO);
	}

	// ---- Navigation fallback ----

	/**
	 * Try to navigate using goto coordinates, or fall back to name-based location lookup.
	 * Returns null if no navigation could be resolved.
	 */
	private MicroAction resolveNavigation(ChecklistStep step)
	{
		// 1. Has explicit goto coordinates
		if (step.hasGoto())
		{
			WorldPoint target = new WorldPoint(step.getGotoX(), step.getGotoY(), 0);
			int dist = getDistanceTo(target);
			if (dist > 5)
			{
				String name = step.getGotoName() != null ? step.getGotoName() : "destination";
				return new MicroAction("Go to " + name + " (" + dist + " tiles)",
					MicroAction.ActionType.NAVIGATE, target, null);
			}
			return null; // Already there
		}

		// 2. Has goto name but no coordinates — try client-side location lookup
		if (step.getGotoName() != null)
		{
			WorldPoint resolved = lookupLocation(step.getGotoName());
			if (resolved != null)
			{
				int dist = getDistanceTo(resolved);
				if (dist > 5)
				{
					log.info("Resolved goto '{}' via client-side lookup -> ({}, {})",
						step.getGotoName(), resolved.getX(), resolved.getY());
					return new MicroAction("Go to " + step.getGotoName() + " (" + dist + " tiles)",
						MicroAction.ActionType.NAVIGATE, resolved, step.getGotoName());
				}
				return null; // Already there
			}
			else
			{
				log.warn("Failed to resolve goto '{}' — no coordinates found", step.getGotoName());
			}
		}

		// 3. No goto at all — try to extract a location name from the description
		String desc = step.getDescription();
		if (desc != null)
		{
			WorldPoint resolved = lookupLocation(desc);
			if (resolved != null)
			{
				int dist = getDistanceTo(resolved);
				if (dist > 5)
				{
					log.info("Resolved description '{}' via location lookup -> ({}, {})",
						desc, resolved.getX(), resolved.getY());
					return new MicroAction("Go to destination (" + dist + " tiles)",
						MicroAction.ActionType.NAVIGATE, resolved, null);
				}
			}
		}

		return null;
	}

	/**
	 * Look up a location name in the client-side database.
	 * Supports exact and partial matching (case-insensitive).
	 */
	public WorldPoint lookupLocation(String name)
	{
		return LocationDatabase.getWorldPoint(name);
	}

	// ---- Scene search ----

	/**
	 * Try to find an interactable object/NPC from the step description in the loaded scene.
	 * Returns NAVIGATE if found far, INTERACT if found close, null if not in scene.
	 */
	private MicroAction resolveFromDescription(ChecklistStep step)
	{
		String target = TextUtils.extractTarget(step.getDescription());
		if (target == null) return null;

		WorldPoint objPos = objectTracker.findNearest(target);
		if (objPos == null) return null;

		int dist = getDistanceTo(objPos);
		if (dist <= 5)
		{
			return new MicroAction("Interact with " + target,
				MicroAction.ActionType.INTERACT, null, target);
		}
		else
		{
			return new MicroAction("Go to " + target + " (" + dist + " tiles)",
				MicroAction.ActionType.NAVIGATE, objPos, target);
		}
	}

	// ---- Utility methods ----

	private int countInInventory(String itemName)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null) return 0;

		int count = 0;
		for (Item item : inventory.getItems())
		{
			if (item.getId() == -1 || item.getId() == 6512) continue;
			String name = itemManager.getItemComposition(item.getId()).getName();
			if (name.equalsIgnoreCase(itemName))
			{
				count += item.getQuantity();
			}
		}
		return count;
	}

	private int countInBank(String itemName, PlayerContext ctx)
	{
		if (ctx == null || ctx.getBank() == null) return 0;

		int count = 0;
		for (ItemInfo bankItem : ctx.getBank())
		{
			if (bankItem.getName().equalsIgnoreCase(itemName))
			{
				count += bankItem.getQuantity();
			}
		}
		return count;
	}

	private boolean isBankOpen()
	{
		Widget bankWidget = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		return bankWidget != null && !bankWidget.isHidden();
	}

	private static class BankResult
	{
		final WorldPoint pos;
		final String name;
		final int dist;

		BankResult(WorldPoint pos, String name, int dist)
		{
			this.pos = pos;
			this.name = name;
			this.dist = dist;
		}
	}

	private BankResult findNearestBankWithName()
	{
		WorldPoint nearest = null;
		String nearestName = null;
		int nearestDist = Integer.MAX_VALUE;

		// Search bank objects
		for (String bankName : BANK_OBJECTS)
		{
			WorldPoint pos = objectTracker.findNearestObject(bankName);
			if (pos != null)
			{
				int dist = getDistanceTo(pos);
				if (dist < nearestDist)
				{
					nearestDist = dist;
					nearest = pos;
					nearestName = bankName;
				}
			}
		}

		// Search banker NPCs (GE bankers, etc.)
		for (String npcName : BANK_NPCS)
		{
			WorldPoint pos = objectTracker.findNearestNpc(npcName);
			if (pos != null)
			{
				int dist = getDistanceTo(pos);
				if (dist < nearestDist)
				{
					nearestDist = dist;
					nearest = pos;
					nearestName = npcName;
				}
			}
		}

		if (nearest != null)
		{
			return new BankResult(nearest, nearestName, nearestDist);
		}

		// No bank in loaded scene — fall back to nearest known bank location
		return findNearestKnownBank();
	}

	private static final String[] KNOWN_BANK_KEYS = {
		"lumbridge bank", "varrock west bank", "varrock east bank",
		"falador west bank", "falador east bank", "edgeville bank",
		"draynor bank", "al kharid bank", "catherby bank",
		"seers' bank", "ardougne bank", "canifis bank", "hosidius bank"
	};

	private BankResult findNearestKnownBank()
	{
		if (client.getLocalPlayer() == null) return null;

		WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		WorldPoint nearest = null;
		String nearestName = null;
		int nearestDist = Integer.MAX_VALUE;

		for (String key : KNOWN_BANK_KEYS)
		{
			int[] coords = LocationDatabase.getCoordinates(key);
			if (coords == null) continue;

			WorldPoint bankPos = new WorldPoint(coords[0], coords[1], 0);
			int dist = playerPos.distanceTo(bankPos);
			if (dist < nearestDist)
			{
				nearestDist = dist;
				nearest = bankPos;
				nearestName = key.substring(0, 1).toUpperCase() + key.substring(1);
			}
		}

		return nearest != null ? new BankResult(nearest, nearestName, nearestDist) : null;
	}

	private boolean isAtGe()
	{
		if (client.getLocalPlayer() == null) return false;
		return client.getLocalPlayer().getWorldLocation().getRegionID() == GE_REGION_ID;
	}

	private int getDistanceTo(WorldPoint target)
	{
		if (client.getLocalPlayer() == null) return Integer.MAX_VALUE;
		return client.getLocalPlayer().getWorldLocation().distanceTo(target);
	}

	private int parseQty(String s)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch (Exception e)
		{
			return 1;
		}
	}

	private String formatQty(int qty)
	{
		return qty > 1 ? " x" + qty : "";
	}

}
