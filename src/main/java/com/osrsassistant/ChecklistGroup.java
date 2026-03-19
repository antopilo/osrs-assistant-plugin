package com.osrsassistant;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChecklistGroup
{
	private final String title;
	private final List<ChecklistStep> substeps;

	// Optional group-level navigation (where to go when this group starts)
	private String gotoName;
	private int gotoX = -1;
	private int gotoY = -1;

	public ChecklistGroup(String title)
	{
		this.title = title;
		this.substeps = new ArrayList<>();
	}

	public void addStep(ChecklistStep step)
	{
		substeps.add(step);
	}

	public boolean isCompleted()
	{
		return !substeps.isEmpty() && substeps.stream().allMatch(ChecklistStep::isCompleted);
	}

	public ChecklistStep getCurrentSubstep()
	{
		for (ChecklistStep step : substeps)
		{
			if (!step.isCompleted())
			{
				return step;
			}
		}
		return null;
	}

	public int getCurrentSubstepIndex()
	{
		for (int i = 0; i < substeps.size(); i++)
		{
			if (!substeps.get(i).isCompleted())
			{
				return i;
			}
		}
		return substeps.size();
	}

	public int completedCount()
	{
		return (int) substeps.stream().filter(ChecklistStep::isCompleted).count();
	}

	public boolean hasGoto()
	{
		return gotoX >= 0 && gotoY >= 0;
	}
}
