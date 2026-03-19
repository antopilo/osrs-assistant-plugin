package com.osrsassistant;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

/**
 * Highlights items in bank and inventory widgets.
 * Renders on ABOVE_WIDGETS layer so highlights appear on top of the bank/inventory interface.
 */
public class WidgetHighlightOverlay extends Overlay
{
	private final Client client;
	private final OsrsAssistantPlugin plugin;
	private final ItemManager itemManager;

	private static final Color ITEM_HIGHLIGHT = new Color(0, 255, 200, 80);
	private static final Color ITEM_BORDER = new Color(0, 255, 200, 200);
	private static final Stroke ITEM_STROKE = new BasicStroke(1.5f);

	public WidgetHighlightOverlay(Client client, OsrsAssistantPlugin plugin, ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
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

		// Highlight from micro-action
		MicroAction microAction = plugin.getCurrentMicroAction();
		if (microAction != null)
		{
			for (String itemName : microAction.getHighlightItems())
			{
				highlightInventoryItems(graphics, itemName);
				highlightBankItems(graphics, itemName);
			}
		}

		// Highlight items from active step conditions
		for (Checklist checklist : mgr.getActiveChecklists())
		{
			ChecklistStep activeStep = checklist.getActiveStep();
			if (activeStep == null || activeStep.isCompleted())
			{
				continue;
			}

			StepCondition cond = activeStep.getCondition();
			if (cond != null && cond.getType() == StepCondition.Type.ITEM)
			{
				highlightInventoryItems(graphics, cond.getParam1());
				highlightBankItems(graphics, cond.getParam1());
			}

			// All uncompleted item steps in current group
			ChecklistGroup group = checklist.getCurrentGroup();
			if (group != null)
			{
				for (ChecklistStep step : group.getSubsteps())
				{
					if (step.isCompleted()) continue;
					StepCondition stepCond = step.getCondition();
					if (stepCond != null && stepCond.getType() == StepCondition.Type.ITEM)
					{
						highlightInventoryItems(graphics, stepCond.getParam1());
						highlightBankItems(graphics, stepCond.getParam1());
					}
				}
			}
		}

		return null;
	}

	private void highlightInventoryItems(Graphics2D graphics, String itemName)
	{
		Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
		if (inventory == null || inventory.isHidden()) return;
		highlightItemsInWidget(graphics, inventory, itemName);
	}

	private void highlightBankItems(Graphics2D graphics, String itemName)
	{
		Widget bankContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		if (bankContainer == null || bankContainer.isHidden()) return;
		highlightItemsInWidget(graphics, bankContainer, itemName);
	}

	private void highlightItemsInWidget(Graphics2D graphics, Widget container, String itemName)
	{
		String searchLower = itemName.toLowerCase();
		Widget[] children = container.getDynamicChildren();
		if (children == null) return;

		for (Widget child : children)
		{
			if (child == null || child.getItemId() <= 0) continue;

			try
			{
				String name = itemManager.getItemComposition(child.getItemId()).getName();
				if (name != null && name.toLowerCase().contains(searchLower))
				{
					Rectangle bounds = child.getBounds();
					if (bounds != null && bounds.width > 0)
					{
						graphics.setColor(ITEM_HIGHLIGHT);
						graphics.fill(bounds);
						graphics.setColor(ITEM_BORDER);
						graphics.setStroke(ITEM_STROKE);
						graphics.draw(bounds);
					}
				}
			}
			catch (Exception e)
			{
				// ignore
			}
		}
	}
}
