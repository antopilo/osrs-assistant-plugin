package com.osrsassistant;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Checklist
{
	private final String title;
	private final List<ChecklistGroup> groups;

	public Checklist(String title)
	{
		this.title = title;
		this.groups = new ArrayList<>();
	}

	public void addGroup(ChecklistGroup group)
	{
		groups.add(group);
	}

	/**
	 * Get the first uncompleted group.
	 */
	public ChecklistGroup getCurrentGroup()
	{
		for (ChecklistGroup group : groups)
		{
			if (!group.isCompleted())
			{
				return group;
			}
		}
		return null;
	}

	public int getCurrentGroupIndex()
	{
		for (int i = 0; i < groups.size(); i++)
		{
			if (!groups.get(i).isCompleted())
			{
				return i;
			}
		}
		return groups.size();
	}

	/**
	 * Get the active substep — the first uncompleted substep in the first uncompleted group.
	 */
	public ChecklistStep getActiveStep()
	{
		ChecklistGroup group = getCurrentGroup();
		return group != null ? group.getCurrentSubstep() : null;
	}

	public boolean isComplete()
	{
		return !groups.isEmpty() && groups.stream().allMatch(ChecklistGroup::isCompleted);
	}

	public int totalSteps()
	{
		return groups.stream().mapToInt(g -> g.getSubsteps().size()).sum();
	}

	public int completedSteps()
	{
		return groups.stream().mapToInt(ChecklistGroup::completedCount).sum();
	}
}
