package com.osrsassistant;

import net.runelite.api.MenuAction;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ChecklistOverlay extends Overlay implements MouseListener
{
	private final OsrsAssistantPlugin plugin;

	private static final int MIN_W = 180;
	private static final int MAX_W = 400;
	private static final int PAD = 10;
	private static final int RESIZE_HANDLE = 12;

	// State
	private int panelWidth = 230;
	private final List<ButtonRegion> buttons = new ArrayList<>();
	private final java.util.Set<Integer> collapsedChecklists = new java.util.HashSet<>();
	private Point overlayOrigin = new Point(0, 0);
	private Point mousePos = new Point(-1, -1);
	private Dimension lastSize = new Dimension(0, 0);
	private boolean resizing = false;
	private int resizeStartX = 0;
	private int resizeStartWidth = 0;

	private static class ButtonRegion
	{
		Rectangle bounds;
		Runnable action;
		String label;

		ButtonRegion(Rectangle bounds, String label, Runnable action)
		{
			this.bounds = bounds;
			this.label = label;
			this.action = action;
		}
	}

	public ChecklistOverlay(OsrsAssistantPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
	}

	public void registerMouse(MouseManager mouseManager)
	{
		mouseManager.registerMouseListener(this);
	}

	public void unregisterMouse(MouseManager mouseManager)
	{
		mouseManager.unregisterMouseListener(this);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ChecklistManager mgr = plugin.getChecklistManager();
		if (mgr == null || !mgr.hasChecklist())
		{
			return null;
		}

		List<Checklist> checklists = mgr.getActiveChecklists();
		buttons.clear();

		// Pre-render to calculate height
		int totalHeight = renderContent(null, checklists);

		// Actual render
		BufferedImage img = new BufferedImage(panelWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Background
		g.setColor(new Color(20, 20, 24, 240));
		g.fillRoundRect(0, 0, panelWidth, totalHeight, 10, 10);
		g.setColor(Theme.BORDER);
		g.drawRoundRect(0, 0, panelWidth - 1, totalHeight - 1, 10, 10);

		// Resize handle (bottom-right corner)
		int rhX = panelWidth - RESIZE_HANDLE - 2;
		int rhY = totalHeight - RESIZE_HANDLE - 2;
		g.setColor(Theme.TEXT_MUTED);
		for (int i = 0; i < 3; i++)
		{
			int offset = i * 4;
			g.drawLine(rhX + offset + 4, rhY + RESIZE_HANDLE, rhX + RESIZE_HANDLE, rhY + offset + 4);
		}

		buttons.clear();
		renderContent(g, checklists);

		g.dispose();

		// Store origin for hit-testing
		Rectangle bounds = getBounds();
		if (bounds != null)
		{
			overlayOrigin = bounds.getLocation();
		}

		graphics.drawImage(img, 0, 0, null);
		lastSize = new Dimension(panelWidth, totalHeight);
		return lastSize;
	}

	private int renderContent(Graphics2D g, List<Checklist> checklists)
	{
		int y = PAD;

		for (int ci = 0; ci < checklists.size(); ci++)
		{
			Checklist cl = checklists.get(ci);
			if (ci > 0)
			{
				y += 4;
				if (g != null)
				{
					g.setColor(Theme.BORDER);
					g.drawLine(PAD, y, panelWidth - PAD, y);
				}
				y += 4;
			}
			y = renderChecklist(g, cl, ci, y);
		}

		return y + PAD;
	}

	private int renderChecklist(Graphics2D g, Checklist cl, int clIndex, int y)
	{
		FontMetrics titleFm = getFm(g, Theme.SENDER);
		FontMetrics groupFm = getFm(g, Theme.SMALL_BOLD);
		FontMetrics stepFm = getFm(g, Theme.SMALL);
		FontMetrics hintFm = getFm(g, Theme.ITALIC.deriveFont(9f));

		int completed = cl.completedSteps();
		int total = cl.totalSteps();
		List<ChecklistGroup> groups = cl.getGroups();
		int currentGroupIdx = cl.getCurrentGroupIndex();

		boolean isTracked = (clIndex == plugin.getChecklistManager().getTrackedIndex());
		boolean isCollapsed = collapsedChecklists.contains(clIndex);
		final int idx = clIndex;

		// ---- Title row: [dot] Title           [fold] [X] ----
		if (g != null)
		{
			// Tracking dot
			int dotR = 4;
			int dotCY = y + titleFm.getHeight() / 2;
			g.setColor(isTracked ? Theme.SUCCESS : Theme.TEXT_MUTED);
			g.fillOval(PAD, dotCY - dotR, dotR * 2, dotR * 2);

			// Title text — clickable to track
			Rectangle titleBounds = new Rectangle(PAD, y, panelWidth - PAD * 2 - 36, titleFm.getHeight());
			boolean titleHover = titleBounds.contains(relMouse());
			g.setFont(Theme.SENDER);
			g.setColor(isTracked ? Theme.ACCENT : (titleHover ? Theme.ACCENT : Theme.TEXT_MUTED));
			g.drawString(truncate(cl.getTitle(), titleFm, panelWidth - PAD * 2 - 50), PAD + 12, y + titleFm.getAscent());
			buttons.add(new ButtonRegion(titleBounds, "track", () -> {
				plugin.getChecklistManager().setTrackedIndex(idx);
				plugin.navigateAfterChecklistChange();
			}));

			// Close [X]
			int closeX = panelWidth - PAD - 10;
			int closeY = y + 4;
			Rectangle closeBounds = new Rectangle(closeX - 4, closeY - 4, 16, 16);
			boolean closeHover = closeBounds.contains(relMouse());
			g.setColor(closeHover ? Theme.CANCEL : Theme.TEXT_MUTED);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(closeX, closeY, closeX + 8, closeY + 8);
			g.drawLine(closeX + 8, closeY, closeX, closeY + 8);
			g.setStroke(new BasicStroke(1f));
			buttons.add(new ButtonRegion(closeBounds, "close", () -> {
				plugin.getChecklistManager().removeChecklist(cl);
				if (!plugin.getChecklistManager().hasChecklist()) plugin.cancelNavigation();
				else plugin.navigateAfterChecklistChange();
			}));

			// Fold toggle
			int foldX = closeX - 18;
			int foldY = closeY + 1;
			Rectangle foldBounds = new Rectangle(foldX - 2, foldY - 4, 14, 14);
			boolean foldHover = foldBounds.contains(relMouse());
			g.setColor(foldHover ? Theme.TEXT : Theme.TEXT_MUTED);
			if (isCollapsed)
			{
				int[] tx = {foldX, foldX, foldX + 6};
				int[] ty = {foldY, foldY + 8, foldY + 4};
				g.fillPolygon(tx, ty, 3);
			}
			else
			{
				int[] tx = {foldX, foldX + 8, foldX + 4};
				int[] ty = {foldY + 1, foldY + 1, foldY + 6};
				g.fillPolygon(tx, ty, 3);
			}
			buttons.add(new ButtonRegion(foldBounds, "fold", () -> {
				if (collapsedChecklists.contains(idx)) collapsedChecklists.remove(idx);
				else collapsedChecklists.add(idx);
			}));
		}
		y += titleFm.getHeight() + 6;

		// ---- Progress bar ----
		int barW = panelWidth - PAD * 2;
		String progressText = completed + " of " + total;
		if (g != null)
		{
			g.setColor(Theme.SURFACE);
			g.fillRoundRect(PAD, y, barW, 6, 3, 3);
			if (total > 0)
			{
				int fw = Math.max((int)((double) completed / total * barW), 3);
				g.setColor(isTracked ? Theme.ACCENT : Theme.TEXT_MUTED);
				g.fillRoundRect(PAD, y, fw, 6, 3, 3);
			}
			g.setFont(Theme.SMALL);
			g.setColor(Theme.TEXT_DIM);
			FontMetrics sf = g.getFontMetrics();
			g.drawString(progressText, panelWidth - PAD - sf.stringWidth(progressText), y + 6 + sf.getAscent() + 2);
		}
		y += 18;

		if (isCollapsed) return y;

		// ---- Groups ----
		for (int i = 0; i < groups.size(); i++)
		{
			ChecklistGroup group = groups.get(i);
			boolean isCurrent = (i == currentGroupIdx && !group.isCompleted());
			boolean isDone = group.isCompleted();

			if (g != null)
			{
				// Group indicator + title
				int indCY = y + groupFm.getHeight() / 2;
				if (isDone)
				{
					drawCheckmark(g, PAD + 1, indCY, 8, Theme.SUCCESS);
					g.setColor(new Color(Theme.SUCCESS.getRed(), Theme.SUCCESS.getGreen(), Theme.SUCCESS.getBlue(), 120));
				}
				else if (isCurrent)
				{
					drawArrow(g, PAD + 1, indCY, 7, Theme.ACCENT);
					g.setColor(Theme.TEXT);
				}
				else
				{
					drawCircle(g, PAD + 3, indCY, 4, Theme.TEXT_MUTED);
					g.setColor(Theme.TEXT_MUTED);
				}
				g.setFont(Theme.SMALL_BOLD);

				String title = truncate(group.getTitle(), groupFm, panelWidth - PAD * 2 - 40);
				g.drawString(title, PAD + 14, y + groupFm.getAscent());

				// Progress count for current group
				if (isCurrent)
				{
					g.setFont(Theme.SMALL);
					g.setColor(Theme.TEXT_DIM);
					String ct = group.completedCount() + "/" + group.getSubsteps().size();
					g.drawString(ct, panelWidth - PAD - g.getFontMetrics().stringWidth(ct), y + groupFm.getAscent());
				}
			}
			y += groupFm.getHeight() + 4;

			// ---- Steps (only for current group) ----
			if (isCurrent)
			{
				int currentSubIdx = group.getCurrentSubstepIndex();
				int textMaxW = panelWidth - PAD * 2 - 30;

				for (int j = 0; j < group.getSubsteps().size(); j++)
				{
					ChecklistStep step = group.getSubsteps().get(j);
					boolean subDone = step.isCompleted();
					boolean subCurrent = (j == currentSubIdx);

					String[] lines = wrapText(step.getDescription(), stepFm, textMaxW);
					int stepH = lines.length * stepFm.getHeight() + 4;

					if (g != null)
					{
						// Highlight active step
						if (subCurrent && !subDone)
						{
							g.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 12));
							g.fillRoundRect(PAD + 8, y, panelWidth - PAD * 2 - 8, stepH, 4, 4);
						}

						// Step indicator
						int indCY = y + stepH / 2;
						if (subDone)
						{
							drawCheckmark(g, PAD + 14, indCY, 6, Theme.SUCCESS);
						}
						else if (subCurrent)
						{
							drawArrow(g, PAD + 14, indCY, 5, Theme.ACCENT);
						}
						else
						{
							drawCircle(g, PAD + 16, indCY, 3, Theme.TEXT_MUTED);
						}

						// Step text
						g.setFont(Theme.SMALL);
						g.setColor(subDone ? Theme.TEXT_MUTED : (subCurrent ? Theme.TEXT : Theme.TEXT_DIM));
						int textX = PAD + 26;
						int textY = y + stepFm.getAscent();
						for (String line : lines)
						{
							g.drawString(line, textX, textY);
							textY += stepFm.getHeight();
						}

						// Skip link on the right for current step
						if (subCurrent && !subDone)
						{
							g.setFont(Theme.SMALL);
							int skipW = stepFm.stringWidth("skip") + 4;
							int skipTextX = panelWidth - PAD - skipW;
							int skipTextY = y + stepFm.getAscent();
							Rectangle skipBounds = new Rectangle(skipTextX - 2, y, skipW + 4, stepH);
							boolean skipHover = skipBounds.contains(relMouse());
							g.setColor(skipHover ? Theme.WARNING : Theme.TEXT_MUTED);
							g.drawString("skip", skipTextX, skipTextY);
							buttons.add(new ButtonRegion(skipBounds, "skip", () -> {
								step.setCompleted(true);
								plugin.navigateAfterChecklistChange();
							}));
						}
					}
					y += stepH;
				}

				// ---- Navigation hint ----
				MicroAction microAction = plugin.getCurrentMicroAction();
				if (microAction != null)
				{
					String hintText = "-> " + microAction.getInstruction();
					String[] hintLines = wrapText(hintText, hintFm, panelWidth - PAD * 2 - 26);
					if (g != null)
					{
						g.setFont(Theme.ITALIC.deriveFont(9f));
						switch (microAction.getType())
						{
							case NAVIGATE: g.setColor(Theme.ACCENT); break;
							case INTERACT: g.setColor(Theme.WARNING); break;
							default: g.setColor(Theme.TEXT_DIM); break;
						}
						for (String line : hintLines)
						{
							g.drawString(line, PAD + 26, y + hintFm.getAscent());
							y += hintFm.getHeight();
						}
					}
					else
					{
						y += hintLines.length * hintFm.getHeight();
					}
					y += 4;
				}
				else if (plugin.isNavigating())
				{
					if (g != null)
					{
						g.setFont(Theme.ITALIC.deriveFont(9f));
						g.setColor(Theme.ACCENT);
						String navName = plugin.getNavigationName();
						int dist = plugin.getNavigationDistance();
						String navText = "-> " + (navName != null ? navName : "?");
						if (dist >= 0) navText += " (" + dist + " tiles)";
						g.drawString(navText, PAD + 26, y + hintFm.getAscent());
					}
					y += hintFm.getHeight() + 4;
				}

				// Undo link (small, inline)
				if (g != null)
				{
					g.setFont(Theme.SMALL);
					Rectangle undoBounds = new Rectangle(PAD + 26, y, stepFm.stringWidth("undo"), stepFm.getHeight());
					boolean undoHover = undoBounds.contains(relMouse());
					g.setColor(undoHover ? Theme.TEXT : Theme.TEXT_MUTED);
					g.drawString("undo", PAD + 26, y + stepFm.getAscent());
					buttons.add(new ButtonRegion(undoBounds, "undo", () -> {
						plugin.getChecklistManager().undoLastStep(cl);
						plugin.navigateAfterChecklistChange();
					}));
				}
				y += stepFm.getHeight() + 4;
			}
		}

		return y;
	}

	private Point relMouse()
	{
		return new Point(mousePos.x - overlayOrigin.x, mousePos.y - overlayOrigin.y);
	}

	private String truncate(String text, FontMetrics fm, int maxW)
	{
		if (fm.stringWidth(text) <= maxW) return text;
		while (text.length() > 3 && fm.stringWidth(text + "..") > maxW)
			text = text.substring(0, text.length() - 1);
		return text + "..";
	}

	private String[] wrapText(String text, FontMetrics fm, int maxW)
	{
		if (fm.stringWidth(text) <= maxW) return new String[]{text};
		List<String> lines = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : text.split(" "))
		{
			String test = cur.length() == 0 ? word : cur + " " + word;
			if (fm.stringWidth(test) > maxW && cur.length() > 0)
			{
				lines.add(cur.toString());
				cur = new StringBuilder(word);
			}
			else
			{
				cur = new StringBuilder(test);
			}
		}
		if (cur.length() > 0) lines.add(cur.toString());
		return lines.toArray(new String[0]);
	}

	private FontMetrics getFm(Graphics2D g, Font font)
	{
		if (g != null)
		{
			g.setFont(font);
			return g.getFontMetrics();
		}
		BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tg = tmp.createGraphics();
		tg.setFont(font);
		FontMetrics fm = tg.getFontMetrics();
		tg.dispose();
		return fm;
	}

	// -- Shape drawing helpers (no unicode dependency) --

	private void drawCheckmark(Graphics2D g, int x, int cy, int size, Color color)
	{
		g.setColor(color);
		g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		int half = size / 2;
		g.drawLine(x, cy, x + half, cy + half);
		g.drawLine(x + half, cy + half, x + size, cy - half);
		g.setStroke(new BasicStroke(1f));
	}

	private void drawArrow(Graphics2D g, int x, int cy, int size, Color color)
	{
		g.setColor(color);
		int half = size / 2;
		int[] ax = {x, x, x + size};
		int[] ay = {cy - half, cy + half, cy};
		g.fillPolygon(ax, ay, 3);
	}

	private void drawCircle(Graphics2D g, int cx, int cy, int radius, Color color)
	{
		g.setColor(color);
		g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
	}

	// -- Mouse handling for button clicks --

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		Point click = new Point(e.getX() - overlayOrigin.x, e.getY() - overlayOrigin.y);
		for (ButtonRegion btn : buttons)
		{
			if (btn.bounds.contains(click))
			{
				btn.action.run();
				e.consume();
				return e;
			}
		}
		return e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		Point rel = new Point(e.getX() - overlayOrigin.x, e.getY() - overlayOrigin.y);

		// Block ALL clicks inside the overlay from reaching the game
		if (rel.x >= 0 && rel.x < lastSize.width && rel.y >= 0 && rel.y < lastSize.height)
		{
			// Check resize handle first
			if (rel.x >= lastSize.width - RESIZE_HANDLE - 4 && rel.y >= lastSize.height - RESIZE_HANDLE - 4)
			{
				resizing = true;
				resizeStartX = e.getX();
				resizeStartWidth = panelWidth;
			}
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		if (resizing)
		{
			resizing = false;
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseExited(MouseEvent e) { return e; }

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		if (resizing)
		{
			int delta = e.getX() - resizeStartX;
			panelWidth = Math.max(MIN_W, Math.min(MAX_W, resizeStartWidth + delta));
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		mousePos = e.getPoint();
		return e;
	}
}
