package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;

/**
 * Highlights game objects, NPCs, and inventory/bank items relevant to the current checklist step.
 */
@Slf4j
public class HighlightOverlay extends Overlay
{
	private final Client client;
	private final OsrsAssistantPlugin plugin;
	private final ItemManager itemManager;

	private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 200, 100);
	private static final Color HIGHLIGHT_BORDER = new Color(0, 255, 200, 200);
	private static final Stroke HIGHLIGHT_STROKE = new BasicStroke(2f);

	public HighlightOverlay(Client client, OsrsAssistantPlugin plugin, ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ChecklistManager mgr = plugin.getChecklistManager();
		if (mgr == null || !mgr.hasChecklist())
		{
			return null;
		}

		// Highlight from micro-action if available
		MicroAction microAction = plugin.getCurrentMicroAction();
		if (microAction != null)
		{
			// Highlight world objects from micro-action
			for (String objName : microAction.getHighlightObjects())
			{
				highlightObjects(graphics, objName);
			}
			// Highlight the target object/NPC for INTERACT actions
			if (microAction.getType() == MicroAction.ActionType.INTERACT && microAction.getObjectName() != null)
			{
				highlightObjects(graphics, microAction.getObjectName());
				highlightNpcs(graphics, microAction.getObjectName());
			}
		}

		// Highlight world objects/NPCs for active steps in ALL checklists
		for (Checklist checklist : mgr.getActiveChecklists())
		{
			ChecklistStep activeStep = checklist.getActiveStep();
			if (activeStep == null || activeStep.isCompleted())
			{
				continue;
			}

			StepCondition cond = activeStep.getCondition();
			if (cond != null && cond.getType() == StepCondition.Type.CHAT)
			{
				if (activeStep.getGotoName() != null)
				{
					highlightObjects(graphics, activeStep.getGotoName());
					highlightNpcs(graphics, activeStep.getGotoName());
				}
				else
				{
					highlightObjectsFromDescription(graphics, activeStep.getDescription());
				}
			}

			// Highlight goto targets (objects/NPCs) or extract from description
			String gotoName = activeStep.getGotoName();
			if (gotoName != null)
			{
				highlightObjects(graphics, gotoName);
				highlightNpcs(graphics, gotoName);
			}
			else
			{
				highlightObjectsFromDescription(graphics, activeStep.getDescription());
				highlightNpcsFromDescription(graphics, activeStep.getDescription());
			}
		}

		return null;
	}

	private void highlightObjects(Graphics2D graphics, String name)
	{
		String searchLower = name.toLowerCase();
		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
		int plane = client.getPlane();

		if (tiles == null || tiles.length <= plane) return;

		for (int x = 0; x < tiles[plane].length; x++)
		{
			for (int y = 0; y < tiles[plane][x].length; y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null) continue;

				for (GameObject obj : tile.getGameObjects())
				{
					if (obj == null) continue;
					String objName = getObjectName(obj.getId());
					if (objName != null && objName.toLowerCase().contains(searchLower))
					{
						Shape hull = obj.getConvexHull();
						if (hull != null)
						{
							OverlayUtil.renderPolygon(graphics, hull, HIGHLIGHT_BORDER, HIGHLIGHT_COLOR, HIGHLIGHT_STROKE);
						}
					}
				}
			}
		}
	}

	private void highlightNpcs(Graphics2D graphics, String name)
	{
		String searchLower = name.toLowerCase();
		var npcs = client.getTopLevelWorldView().npcs();

		for (NPC npc : npcs)
		{
			if (npc == null || npc.getName() == null) continue;
			if (npc.getName().toLowerCase().contains(searchLower))
			{
				Shape hull = npc.getConvexHull();
				if (hull != null)
				{
					OverlayUtil.renderPolygon(graphics, hull, HIGHLIGHT_BORDER, HIGHLIGHT_COLOR, HIGHLIGHT_STROKE);
				}
			}
		}
	}

	private void highlightObjectsFromDescription(Graphics2D graphics, String description)
	{
		if (description == null) return;
		String descLower = description.toLowerCase();

		// Common interactable object keywords
		String[] keywords = {"birdhouse", "bird house", "altar", "furnace", "anvil", "bank booth",
			"bank chest", "tree", "rock", "patch", "compost", "chest", "door", "ladder",
			"staircase", "obelisk", "cooking range", "range", "spinning wheel", "loom",
			"well", "fountain", "ore", "fishing spot"};

		for (String keyword : keywords)
		{
			if (descLower.contains(keyword))
			{
				highlightObjects(graphics, keyword);
			}
		}
	}

	private void highlightNpcsFromDescription(Graphics2D graphics, String description)
	{
		if (description == null) return;

		// Look for "talk to X", "speak to X", "trade X" patterns
		String descLower = description.toLowerCase();
		String[] prefixes = {"talk to ", "speak to ", "trade "};

		for (String prefix : prefixes)
		{
			int idx = descLower.indexOf(prefix);
			if (idx >= 0)
			{
				String remainder = description.substring(idx + prefix.length()).trim();
				int end = remainder.length();
				for (int i = 0; i < remainder.length(); i++)
				{
					char c = remainder.charAt(i);
					if (c == ',' || c == '.' || c == '|' || c == '{' || c == '}')
					{
						end = i;
						break;
					}
				}
				String npcName = remainder.substring(0, end).trim();
				if (!npcName.isEmpty())
				{
					highlightNpcs(graphics, npcName);
				}
			}
		}
	}

	private String getObjectName(int objectId)
	{
		try
		{
			ObjectComposition comp = client.getObjectDefinition(objectId);
			if (comp == null) return null;
			if (comp.getImpostorIds() != null)
			{
				ObjectComposition impostor = comp.getImpostor();
				if (impostor != null) return impostor.getName();
			}
			String name = comp.getName();
			return (name != null && !name.equals("null")) ? name : null;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
