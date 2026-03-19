package com.osrsassistant;

import lombok.Data;

@Data
public class ChecklistStep
{
	private final String description;
	private final StepCondition condition;
	private boolean completed = false;

	// Optional navigation
	private String gotoName;
	private int gotoX = -1;
	private int gotoY = -1;

	public ChecklistStep(String description, StepCondition condition)
	{
		this.description = description;
		this.condition = condition;
	}

	public boolean hasGoto()
	{
		return gotoX >= 0 && gotoY >= 0;
	}
}
