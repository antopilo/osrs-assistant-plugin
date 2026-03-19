package com.osrsassistant;

import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * A single micro-instruction resolved from a checklist step.
 * Represents the immediate next thing the player should do.
 */
public class MicroAction
{
	public enum ActionType
	{
		NAVIGATE,  // Go somewhere
		INTERACT,  // Click on something
		INFO       // Informational hint (withdraw, buy, train, etc.)
	}

	private final String instruction;
	private final ActionType type;
	private final WorldPoint navTarget;
	private final String objectName;
	private final List<String> highlightItems;
	private final List<String> highlightObjects;

	public MicroAction(String instruction, ActionType type)
	{
		this(instruction, type, null, null, Collections.emptyList(), Collections.emptyList());
	}

	public MicroAction(String instruction, ActionType type, WorldPoint navTarget, String objectName)
	{
		this(instruction, type, navTarget, objectName, Collections.emptyList(), Collections.emptyList());
	}

	public MicroAction(String instruction, ActionType type, WorldPoint navTarget, String objectName,
					   List<String> highlightItems, List<String> highlightObjects)
	{
		this.instruction = instruction;
		this.type = type;
		this.navTarget = navTarget;
		this.objectName = objectName;
		this.highlightItems = highlightItems != null ? highlightItems : Collections.emptyList();
		this.highlightObjects = highlightObjects != null ? highlightObjects : Collections.emptyList();
	}

	public String getInstruction()
	{
		return instruction;
	}

	public ActionType getType()
	{
		return type;
	}

	public WorldPoint getNavTarget()
	{
		return navTarget;
	}

	public String getObjectName()
	{
		return objectName;
	}

	public List<String> getHighlightItems()
	{
		return highlightItems;
	}

	public List<String> getHighlightObjects()
	{
		return highlightObjects;
	}
}
