package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ChecklistManager
{
	private final Client client;
	private final ItemManager itemManager;
	private final CopyOnWriteArrayList<Checklist> activeChecklists = new CopyOnWriteArrayList<>();

	private StepResolver stepResolver;
	private volatile MicroAction currentMicroAction;

	// Index of the checklist that drives navigation. -1 = none.
	private int trackedIndex = 0;

	public ChecklistManager(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	public int getTrackedIndex()
	{
		return trackedIndex;
	}

	public void setTrackedIndex(int index)
	{
		if (index >= -1 && index < activeChecklists.size())
		{
			trackedIndex = index;
		}
	}

	public Checklist getTrackedChecklist()
	{
		if (trackedIndex >= 0 && trackedIndex < activeChecklists.size())
		{
			return activeChecklists.get(trackedIndex);
		}
		return null;
	}

	public void setStepResolver(StepResolver resolver)
	{
		this.stepResolver = resolver;
	}

	public MicroAction getCurrentMicroAction()
	{
		return currentMicroAction;
	}

	public void addChecklist(Checklist checklist)
	{
		activeChecklists.add(checklist);
		log.info("Checklist added: {} ({} groups, {} total steps)",
			checklist.getTitle(), checklist.getGroups().size(), checklist.totalSteps());
	}

	/**
	 * Add a checklist and immediately track it (focus navigation on it).
	 */
	public void addAndTrack(Checklist checklist)
	{
		activeChecklists.add(checklist);
		trackedIndex = activeChecklists.size() - 1;
		log.info("Checklist added and tracked: {}", checklist.getTitle());
	}

	public void setChecklist(Checklist checklist)
	{
		activeChecklists.clear();
		addChecklist(checklist);
		trackedIndex = 0;
	}

	public void removeChecklist(int index)
	{
		if (index >= 0 && index < activeChecklists.size())
		{
			Checklist removed = activeChecklists.remove(index);
			log.info("Checklist removed: {}", removed.getTitle());

			// Adjust tracked index
			if (activeChecklists.isEmpty())
			{
				trackedIndex = -1;
			}
			else if (index == trackedIndex)
			{
				trackedIndex = Math.min(trackedIndex, activeChecklists.size() - 1);
			}
			else if (index < trackedIndex)
			{
				trackedIndex--;
			}
		}
	}

	public void removeChecklist(Checklist checklist)
	{
		int index = activeChecklists.indexOf(checklist);
		if (index >= 0)
		{
			removeChecklist(index);
		}
	}

	public void clear()
	{
		activeChecklists.clear();
		trackedIndex = -1;
		log.info("All checklists cleared");
	}

	public List<Checklist> getActiveChecklists()
	{
		return activeChecklists;
	}

	/**
	 * Returns the first checklist for backwards compatibility.
	 */
	public Checklist getActiveChecklist()
	{
		return activeChecklists.isEmpty() ? null : activeChecklists.get(0);
	}

	public boolean hasChecklist()
	{
		return !activeChecklists.isEmpty();
	}

	/**
	 * Called every game tick. Checks the active step in ALL checklists.
	 * Step completion is permanent — once a step is completed it stays
	 * completed even if the condition is no longer satisfied (e.g. items
	 * used on the next step). Use Undo for manual revert.
	 * Returns true if any step completed this tick.
	 */
	public boolean onGameTick()
	{
		boolean anyCompleted = false;

		for (Checklist checklist : activeChecklists)
		{
			ChecklistStep active = checklist.getActiveStep();
			if (active == null || active.isCompleted())
			{
				continue;
			}

			StepCondition cond = active.getCondition();
			if (cond != null && cond.check(client, itemManager))
			{
				active.setCompleted(true);
				log.info("Step completed: {}", active.getDescription());
				anyCompleted = true;
			}
		}

		// Remove completed checklists and adjust tracked index
		for (int i = activeChecklists.size() - 1; i >= 0; i--)
		{
			if (activeChecklists.get(i).isComplete())
			{
				activeChecklists.remove(i);
				if (i == trackedIndex)
				{
					// Tracked checklist completed — track next available or -1
					trackedIndex = activeChecklists.isEmpty() ? -1 : Math.min(trackedIndex, activeChecklists.size() - 1);
				}
				else if (i < trackedIndex)
				{
					trackedIndex--;
				}
			}
		}

		// Resolve micro-action for current active step
		resolveMicroAction();

		return anyCompleted;
	}

	/**
	 * Resolve the current micro-action from the tracked checklist's active step.
	 */
	private void resolveMicroAction()
	{
		resolveMicroAction(null);
	}

	/**
	 * Resolve micro-action with full player context.
	 * Only resolves for the tracked checklist.
	 */
	public void resolveMicroAction(PlayerContext ctx)
	{
		if (stepResolver == null)
		{
			currentMicroAction = null;
			return;
		}

		Checklist tracked = getTrackedChecklist();
		if (tracked != null)
		{
			ChecklistStep active = tracked.getActiveStep();
			if (active != null && !active.isCompleted())
			{
				currentMicroAction = stepResolver.resolve(active, ctx);
				return;
			}
		}
		currentMicroAction = null;
	}

	/**
	 * Called when a chat message arrives. Checks active step in ALL checklists.
	 */
	public boolean onChatMessage(String message)
	{
		boolean anyCompleted = false;
		for (Checklist checklist : activeChecklists)
		{
			ChecklistStep active = checklist.getActiveStep();
			if (active == null || active.isCompleted())
			{
				continue;
			}

			StepCondition cond = active.getCondition();
			if (cond == null) continue;

			cond.onChatMessage(message);
			if (cond.check(client, itemManager))
			{
				active.setCompleted(true);
				log.info("Step completed (chat): {}", active.getDescription());
				anyCompleted = true;
			}
		}
		return anyCompleted;
	}

	/**
	 * Called when XP changes. Routes to active step's xp: condition.
	 */
	public boolean onXpChanged(Skill skill, int currentXp)
	{
		boolean anyCompleted = false;
		for (Checklist checklist : activeChecklists)
		{
			ChecklistStep active = checklist.getActiveStep();
			if (active == null || active.isCompleted())
			{
				continue;
			}

			StepCondition cond = active.getCondition();
			if (cond == null) continue;

			cond.onXpChanged(skill, currentXp);
			if (cond.check(client, itemManager))
			{
				active.setCompleted(true);
				log.info("Step completed (xp): {}", active.getDescription());
				anyCompleted = true;
			}
		}
		return anyCompleted;
	}

	/**
	 * Undo the last completed step in the given checklist.
	 */
	public boolean undoLastStep(Checklist checklist)
	{
		if (checklist == null) return false;

		// Find the last completed step before the current active step
		ChecklistStep lastCompleted = null;
		for (ChecklistGroup group : checklist.getGroups())
		{
			for (ChecklistStep step : group.getSubsteps())
			{
				if (step.isCompleted())
				{
					lastCompleted = step;
				}
			}
		}

		if (lastCompleted != null)
		{
			lastCompleted.setCompleted(false);
			if (lastCompleted.getCondition() != null)
			{
				lastCompleted.getCondition().reset();
			}
			log.info("Step undone: {}", lastCompleted.getDescription());
			return true;
		}
		return false;
	}

	/**
	 * Undo the last completed step in the first checklist.
	 */
	public boolean undoLastStep()
	{
		return !activeChecklists.isEmpty() && undoLastStep(activeChecklists.get(0));
	}

	/**
	 * Gets the current navigation step from the first checklist with one.
	 */
	public ChecklistStep getCurrentNavigationStep()
	{
		for (Checklist checklist : activeChecklists)
		{
			ChecklistStep active = checklist.getActiveStep();
			if (active != null && active.hasGoto())
			{
				return active;
			}

			ChecklistGroup group = checklist.getCurrentGroup();
			if (group != null && group.hasGoto())
			{
				ChecklistStep groupNav = new ChecklistStep(group.getTitle(), null);
				groupNav.setGotoName(group.getGotoName());
				groupNav.setGotoX(group.getGotoX());
				groupNav.setGotoY(group.getGotoY());
				return groupNav;
			}
		}
		return null;
	}
}
