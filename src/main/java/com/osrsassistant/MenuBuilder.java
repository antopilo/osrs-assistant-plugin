package com.osrsassistant;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.util.Text;

import java.util.List;

public class MenuBuilder
{
	private final Client client;
	private final OsrsAssistantPanel panel;
	private final NavigationManager navigationManager;
	private final ChecklistManager checklistManager;
	private final Runnable cancelNavigation;
	private final Runnable onChecklistChange;

	public MenuBuilder(Client client, OsrsAssistantPanel panel, NavigationManager navigationManager,
					   ChecklistManager checklistManager, Runnable cancelNavigation, Runnable onChecklistChange)
	{
		this.client = client;
		this.panel = panel;
		this.navigationManager = navigationManager;
		this.checklistManager = checklistManager;
		this.cancelNavigation = cancelNavigation;
		this.onChecklistChange = onChecklistChange;
	}

	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuAction type = MenuAction.of(event.getType());
		String option = event.getOption();
		String target = event.getTarget();

		// Add "Ask Assistant" when we see an Examine entry for any entity type
		if (type == MenuAction.EXAMINE_NPC || type == MenuAction.EXAMINE_ITEM
			|| type == MenuAction.EXAMINE_ITEM_GROUND || type == MenuAction.EXAMINE_OBJECT)
		{
			String name = Text.removeTags(target);
			String category = type == MenuAction.EXAMINE_NPC ? "NPC"
				: type == MenuAction.EXAMINE_OBJECT ? "object"
				: "item";

			client.getMenu().createMenuEntry(-1)
				.setOption("Ask Assistant")
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					String question;
					switch (category)
					{
						case "NPC":
							question = "Tell me about the NPC: " + name;
							break;
						case "item":
							question = "Tell me about the item: " + name;
							break;
						default:
							question = "Tell me about: " + name;
							break;
					}
					panel.askAssistant(question);
				});
		}

		// Inventory/bank items and quest list — CC_OP with "Examine" or quest-related options
		if ((type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY)
			&& option != null && target != null && !target.isEmpty())
		{
			String name = Text.removeTags(target);

			if (option.equals("Examine"))
			{
				client.getMenu().createMenuEntry(-1)
					.setOption("Ask Assistant")
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.onClick(e -> panel.askAssistant("Tell me about the item: " + name));
			}
			else if (option.equals("Read journal:") || option.equals("Read Journal:"))
			{
				client.getMenu().createMenuEntry(-1)
					.setOption("Ask Assistant")
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.onClick(e -> panel.askAssistant("Tell me about the quest: " + name + ". What are the requirements, rewards, and a brief walkthrough?"));
			}
		}

		// Quest list uses WIDGET_TYPE_1 for clicking quest names
		if (type == MenuAction.WIDGET_TYPE_1
			&& target != null && !target.isEmpty())
		{
			String name = Text.removeTags(target);

			client.getMenu().createMenuEntry(-1)
				.setOption("Ask Assistant")
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e -> panel.askAssistant("Tell me about the quest: " + name + ". What are the requirements, rewards, and a brief walkthrough?"));
		}

		// Skills tab right-click: detect "View" or skill-related options on skill widgets
		if (option != null && target != null)
		{
			String skillName = extractSkillFromTarget(target);
			if (skillName != null && (option.equals("View") || option.equals("Guide")))
			{
				int playerLevel = 0;
				for (Skill s : Skill.values())
				{
					if (s.getName().equalsIgnoreCase(skillName))
					{
						playerLevel = client.getRealSkillLevel(s);
						break;
					}
				}

				final int level = playerLevel;
				final String skill = skillName;
				client.getMenu().createMenuEntry(-1)
					.setOption("Ask Assistant")
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.onClick(e -> panel.askAssistant(
						"How should I train " + skill + "? I'm currently level " + level
						+ ". What are the best methods, what do I unlock next, and what should I aim for?"));
			}
		}

		// Show "Cancel Navigation" option when navigating (on Walk here entries)
		if (navigationManager.isNavigating() && option != null && option.equals("Walk here"))
		{
			String destName = navigationManager.getDestinationName();
			String label = destName != null ? "Cancel: " + destName : "Cancel Navigation";
			client.getMenu().createMenuEntry(-1)
				.setOption(label)
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> cancelNavigation.run());
		}

		// Show checklist options when active
		if (checklistManager.hasChecklist() && option != null && option.equals("Walk here"))
		{
			List<Checklist> checklists = checklistManager.getActiveChecklists();
			for (int i = 0; i < checklists.size(); i++)
			{
				Checklist cl = checklists.get(i);
				final int idx = i;
				String title = cl.getTitle();
				if (title.length() > 18) title = title.substring(0, 16) + "..";
				final String clTitle = title;

				boolean isTracked = (idx == checklistManager.getTrackedIndex());
				String trackColor = isTracked ? "<col=00ff64>" : "<col=00c8ff>";
				String targetTag = trackColor + clTitle + "</col>";

				// Track / focus this checklist for navigation
				if (!isTracked)
				{
					client.getMenu().createMenuEntry(-1)
						.setOption("Track")
						.setTarget(targetTag)
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							checklistManager.setTrackedIndex(idx);
							onChecklistChange.run();
						});
				}

				// Skip current step
				ChecklistStep activeStep = cl.getActiveStep();
				if (activeStep != null && !activeStep.isCompleted())
				{
					String stepDesc = activeStep.getDescription();
					if (stepDesc.length() > 22) stepDesc = stepDesc.substring(0, 20) + "..";
					client.getMenu().createMenuEntry(-1)
						.setOption("Skip: " + stepDesc)
						.setTarget(targetTag)
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							activeStep.setCompleted(true);
							onChecklistChange.run();
						});
				}

				// Go back
				client.getMenu().createMenuEntry(-1)
					.setOption("Go Back")
					.setTarget(targetTag)
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						checklistManager.undoLastStep(cl);
						onChecklistChange.run();
					});

				// Close
				client.getMenu().createMenuEntry(-1)
					.setOption("Close")
					.setTarget(targetTag)
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						checklistManager.removeChecklist(idx);
						if (!checklistManager.hasChecklist())
						{
							cancelNavigation.run();
						}
						else
						{
							onChecklistChange.run();
						}
					});
			}
		}
	}

	private String extractSkillFromTarget(String target)
	{
		String clean = Text.removeTags(target).trim();
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL) continue;
			if (clean.equalsIgnoreCase(skill.getName()))
			{
				return skill.getName();
			}
		}
		return null;
	}
}
