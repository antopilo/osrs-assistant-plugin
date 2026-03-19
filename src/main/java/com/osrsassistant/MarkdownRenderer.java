package com.osrsassistant;

import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownRenderer
{
	private final OsrsAssistantPlugin plugin;
	private final ItemManager itemManager;
	private final Map<String, ImageIcon> skillIconCache;
	private final Map<Integer, ImageIcon> itemIconCache;

	// Patterns for inline markdown and widget tags
	// {loc:} is treated as an alias for {goto:} — both render as navigable GO TO pills
	static final Pattern INLINE_PATTERN = Pattern.compile(
		"\\{skill:([^}:]+):(\\d+\\+?)\\}" +                   // group 1,2: skill badge
		"|\\{item:([^}:]+):(\\d+)\\}" +                       // group 3,4: item with id
		"|\\{item:([^}]+)\\}" +                               // group 5: item without id
		"|\\{quest:([^}]+)\\}" +                              // group 6: quest badge
		"|\\{(?:goto|loc):object:([^}]+)\\}" +                // group 7: goto object by name
		"|\\{(?:goto|loc):npc:([^}]+)\\}" +                   // group 8: goto npc by name
		"|\\{(?:goto|loc):([^}:]+):(\\d+):(\\d+)\\}" +        // group 9,10,11: goto with coords
		"|\\{(?:goto|loc):([^}]+)\\}" +                       // group 12: goto without coords
		"|(`[^`]+`)" +                                        // group 13: inline code
		"|\\*\\*\\*(.+?)\\*\\*\\*" +                          // group 14: bold italic
		"|\\*\\*(.+?)\\*\\*" +                                // group 15: bold
		"|\\*(.+?)\\*" +                                      // group 16: italic
		"|\\[([^\\]]+)\\]\\(([^)]+)\\)" +                     // group 17,18: link text, url
		"|(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)"   // group 19: bare url
	);

	// Matches GP amounts like "~1.2M", "500K", "1,000 GP", "100K GP", "2.5M gp"
	static final Pattern GP_PATTERN = Pattern.compile(
		"~?[\\d,]+\\.?\\d*\\s*[KkMmBb]?\\s*(?:GP|gp|coins)"
	);


	public MarkdownRenderer(OsrsAssistantPlugin plugin, ItemManager itemManager,
							Map<String, ImageIcon> skillIconCache, Map<Integer, ImageIcon> itemIconCache)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		this.skillIconCache = skillIconCache;
		this.itemIconCache = itemIconCache;
	}

	void appendMarkdown(StyledDocument doc, String text) throws BadLocationException
	{
		if (text == null || text.isEmpty())
		{
			return;
		}

		String[] lines = text.split("\n");
		boolean inCodeBlock = false;
		boolean firstLine = true;
		boolean lastWasHeader = false;

		for (String line : lines)
		{
			// Code block toggle
			if (line.trim().startsWith("```"))
			{
				inCodeBlock = !inCodeBlock;
				continue;
			}

			// Empty lines → add spacing
			if (line.trim().isEmpty() && !inCodeBlock)
			{
				if (!firstLine)
				{
					appendText(doc, "\n", Theme.BASE, Color.WHITE, null, null);
				}
				continue;
			}

			if (!firstLine)
			{
				appendText(doc, "\n", Theme.BASE, Color.WHITE, null, null);
			}
			firstLine = false;

			// Code block content
			if (inCodeBlock)
			{
				appendText(doc, line, Theme.CODE, Theme.CODE_FG, Theme.CODE_BG, null);
				lastWasHeader = false;
				continue;
			}

			// Headers — add extra space before (unless first line)
			if (line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ") || line.startsWith("#### "))
			{
				Font headerFont;
				String headerText;
				if (line.startsWith("#### "))
				{
					headerFont = Theme.H3;
					headerText = line.substring(5);
				}
				else if (line.startsWith("### "))
				{
					headerFont = Theme.H3;
					headerText = line.substring(4);
				}
				else if (line.startsWith("## "))
				{
					headerFont = Theme.H2;
					headerText = line.substring(3);
				}
				else
				{
					headerFont = Theme.H1;
					headerText = line.substring(2);
				}
				appendInlineMarkdown(doc, headerText, headerFont);
				lastWasHeader = true;
				continue;
			}

			lastWasHeader = false;

			// Unordered list — only match dash prefix, not asterisk (avoids italic conflict)
			if (line.trim().matches("^[-]\\s.*"))
			{
				appendText(doc, " \u2022 ", Theme.BASE, Theme.TEXT_MUTED, null, null);
				appendInlineMarkdown(doc, line.trim().substring(2), Theme.BASE);
				continue;
			}

			// Also handle `* ` as bullet (but be careful: only at line start with space after)
			if (line.matches("^\\*\\s+.*"))
			{
				appendText(doc, " \u2022 ", Theme.BASE, Theme.TEXT_MUTED, null, null);
				appendInlineMarkdown(doc, line.replaceFirst("^\\*\\s+", ""), Theme.BASE);
				continue;
			}

			// Ordered list
			if (line.trim().matches("^\\d+\\.\\s.*"))
			{
				String num = line.trim().replaceFirst("^(\\d+\\.)\\s.*", "$1");
				String content = line.trim().replaceFirst("^\\d+\\.\\s", "");
				appendText(doc, " " + num + " ", Theme.BASE, Theme.TEXT_MUTED, null, null);
				appendInlineMarkdown(doc, content, Theme.BASE);
				continue;
			}

			// Regular text
			appendInlineMarkdown(doc, line, Theme.BASE);
		}
	}

	void appendInlineMarkdown(StyledDocument doc, String text, Font baseFont) throws BadLocationException
	{
		Matcher matcher = INLINE_PATTERN.matcher(text);
		int lastEnd = 0;

		while (matcher.find())
		{
			// Plain text before this match — scan for skill/GP icons
			if (matcher.start() > lastEnd)
			{
				appendPlainTextWithIcons(doc, text.substring(lastEnd, matcher.start()), baseFont, Color.WHITE);
			}

			if (matcher.group(1) != null)
			{
				// Skill — highlighted text, color based on whether player meets level
				String skillName = matcher.group(1);
				int reqLevel = Integer.parseInt(matcher.group(2).replace("+", ""));
				appendSkillInline(doc, skillName, reqLevel);
			}
			else if (matcher.group(3) != null)
			{
				// Item with ID — icon + highlighted text
				String itemName = matcher.group(3);
				int itemId = Integer.parseInt(matcher.group(4));
				appendItemInline(doc, itemName, itemId);
			}
			else if (matcher.group(5) != null)
			{
				// Item without ID — resolve, then icon + highlighted text
				String itemName = matcher.group(5);
				int resolvedId = searchItemId(itemName);
				appendItemInline(doc, itemName, resolvedId);
			}
			else if (matcher.group(6) != null)
			{
				// Quest — highlighted text, color based on completion state
				String questName = matcher.group(6);
				appendQuestInline(doc, questName);
			}
			else if (matcher.group(7) != null)
			{
				// Goto object {goto:object:Name}
				String objName = matcher.group(7);
				SimpleAttributeSet a = createGotoAttrs();
				a.addAttribute("goto-object", objName);
				a.addAttribute("goto-name", objName);
				doc.insertString(doc.getLength(), objName, a);
			}
			else if (matcher.group(8) != null)
			{
				// Goto NPC {goto:npc:Name}
				String npcName = matcher.group(8);
				SimpleAttributeSet a = createGotoAttrs();
				a.addAttribute("goto-npc", npcName);
				a.addAttribute("goto-name", npcName);
				doc.insertString(doc.getLength(), npcName, a);
			}
			else if (matcher.group(9) != null)
			{
				// Goto with coordinates
				String name = matcher.group(9);
				SimpleAttributeSet a = createGotoAttrs();
				a.addAttribute("goto-x", Integer.parseInt(matcher.group(10)));
				a.addAttribute("goto-y", Integer.parseInt(matcher.group(11)));
				a.addAttribute("goto-name", name);
				doc.insertString(doc.getLength(), name, a);
			}
			else if (matcher.group(12) != null)
			{
				// Goto/loc without coordinates
				String gotoName = matcher.group(12);
				SimpleAttributeSet a = createGotoAttrs();
				a.addAttribute("goto-name", gotoName);
				doc.insertString(doc.getLength(), gotoName, a);
			}
			else if (matcher.group(13) != null)
			{
				// Inline code
				String code = matcher.group(13);
				appendText(doc, code.substring(1, code.length() - 1), Theme.CODE, Theme.CODE_FG, Theme.CODE_BG, null);
			}
			else if (matcher.group(14) != null)
			{
				// Bold italic — recurse to handle nested widget tags
				appendInlineMarkdown(doc, matcher.group(14), Theme.BOLD_ITALIC);
			}
			else if (matcher.group(15) != null)
			{
				// Bold — recurse to handle nested widget tags
				appendInlineMarkdown(doc, matcher.group(15), Theme.BOLD);
			}
			else if (matcher.group(16) != null)
			{
				// Italic — recurse to handle nested widget tags
				appendInlineMarkdown(doc, matcher.group(16), Theme.ITALIC);
			}
			else if (matcher.group(17) != null)
			{
				appendText(doc, matcher.group(17), baseFont, Theme.LINK, null, matcher.group(18));
			}
			else if (matcher.group(19) != null)
			{
				appendText(doc, matcher.group(19), baseFont, Theme.LINK, null, matcher.group(19));
			}

			lastEnd = matcher.end();
		}

		// Remaining text
		if (lastEnd < text.length())
		{
			appendPlainTextWithIcons(doc, text.substring(lastEnd), baseFont, Color.WHITE);
		}
	}

	void appendPlainTextWithIcons(StyledDocument doc, String text, Font font, Color fg) throws BadLocationException
	{
		// Scan plain text for GP amounts and highlight them
		Matcher gpMatcher = GP_PATTERN.matcher(text);
		int lastEnd = 0;

		while (gpMatcher.find())
		{
			// Text before the GP match
			if (gpMatcher.start() > lastEnd)
			{
				appendText(doc, text.substring(lastEnd, gpMatcher.start()), font, fg, null, null);
			}

			// GP amount in gold color
			appendText(doc, gpMatcher.group(), font, Theme.ITEM, null, null);

			lastEnd = gpMatcher.end();
		}

		if (lastEnd < text.length())
		{
			appendText(doc, text.substring(lastEnd), font, fg, null, null);
		}
		else if (lastEnd == 0)
		{
			appendText(doc, text, font, fg, null, null);
		}
	}

	void appendText(StyledDocument doc, String text, Font font, Color fg, Color bg, String linkUrl)
		throws BadLocationException
	{
		appendText(doc, text, font, fg, bg, linkUrl, -1, -1);
	}

	void appendText(StyledDocument doc, String text, Font font, Color fg, Color bg, String linkUrl, int locX, int locY)
		throws BadLocationException
	{
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setFontFamily(attrs, font.getFamily());
		StyleConstants.setFontSize(attrs, font.getSize());
		StyleConstants.setBold(attrs, font.isBold());
		StyleConstants.setItalic(attrs, font.isItalic());
		StyleConstants.setForeground(attrs, fg);
		if (bg != null)
		{
			StyleConstants.setBackground(attrs, bg);
		}
		if (linkUrl != null)
		{
			attrs.addAttribute("link-url", linkUrl);
			StyleConstants.setBackground(attrs, Theme.LINK_BG);
		}
		doc.insertString(doc.getLength(), text, attrs);
	}

	// Highlight colors for clickable inline text

	/**
	 * Create styled attributes for a clickable goto location (inline highlighted text).
	 */
	private SimpleAttributeSet createGotoAttrs()
	{
		SimpleAttributeSet a = new SimpleAttributeSet();
		StyleConstants.setFontFamily(a, Theme.BASE.getFamily());
		StyleConstants.setFontSize(a, Theme.BASE.getSize());
		StyleConstants.setBold(a, true);
		StyleConstants.setForeground(a, Theme.ACCENT);
		StyleConstants.setBackground(a, Theme.ACCENT_BG);
		return a;
	}

	private SimpleAttributeSet createItemAttrs(String itemName, int itemId)
	{
		SimpleAttributeSet a = new SimpleAttributeSet();
		StyleConstants.setFontFamily(a, Theme.BASE.getFamily());
		StyleConstants.setFontSize(a, Theme.BASE.getSize());
		StyleConstants.setBold(a, true);
		StyleConstants.setForeground(a, Theme.ITEM);
		StyleConstants.setBackground(a, Theme.ITEM_BG);
		a.addAttribute("item-name", itemName);
		if (itemId > 0) a.addAttribute("item-id", itemId);
		return a;
	}

	private void appendSkillInline(StyledDocument doc, String skillName, int requiredLevel) throws BadLocationException
	{
		// Check if player meets the requirement
		boolean met = false;
		PlayerContext ctx = plugin.getPlayerContext();
		if (ctx != null && ctx.getSkills() != null)
		{
			for (SkillInfo s : ctx.getSkills())
			{
				if (s.getName().equalsIgnoreCase(skillName))
				{
					met = s.getLevel() >= requiredLevel;
					break;
				}
			}
		}

		Color fg = met ? Theme.SUCCESS : Theme.ERROR;
		Color bg = met ? Theme.SUCCESS_BG : Theme.ERROR_BG;

		// Insert skill sprite if cached
		ImageIcon skillIcon = skillIconCache.get(skillName);
		if (skillIcon != null)
		{
			SimpleAttributeSet iconAttrs = new SimpleAttributeSet();
			StyleConstants.setIcon(iconAttrs, skillIcon);
			doc.insertString(doc.getLength(), " ", iconAttrs);
		}

		// Highlighted text: "Attack 60"
		SimpleAttributeSet a = new SimpleAttributeSet();
		StyleConstants.setFontFamily(a, Theme.BASE.getFamily());
		StyleConstants.setFontSize(a, Theme.BASE.getSize());
		StyleConstants.setBold(a, true);
		StyleConstants.setForeground(a, fg);
		StyleConstants.setBackground(a, bg);
		doc.insertString(doc.getLength(), skillName + " " + requiredLevel, a);
	}

	private void appendQuestInline(StyledDocument doc, String questName) throws BadLocationException
	{
		// Look up quest state
		String state = "NOT_STARTED";
		PlayerContext ctx = plugin.getPlayerContext();
		if (ctx != null && ctx.getQuests() != null)
		{
			for (QuestInfo q : ctx.getQuests())
			{
				if (q.getName().equalsIgnoreCase(questName))
				{
					state = q.getState();
					break;
				}
			}
		}

		Color fg, bg;
		String indicator;
		switch (state)
		{
			case "FINISHED":
				fg = Theme.SUCCESS;
				bg = Theme.SUCCESS_BG;
				indicator = "\u2714 "; // checkmark
				break;
			case "IN_PROGRESS":
				fg = Theme.WARNING;
				bg = Theme.WARNING_BG;
				indicator = "\u25B6 "; // play triangle
				break;
			default:
				fg = Theme.NEUTRAL;
				bg = Theme.NEUTRAL_BG;
				indicator = "\u25CB "; // empty circle
				break;
		}

		SimpleAttributeSet a = new SimpleAttributeSet();
		StyleConstants.setFontFamily(a, Theme.BASE.getFamily());
		StyleConstants.setFontSize(a, Theme.BASE.getSize());
		StyleConstants.setBold(a, true);
		StyleConstants.setForeground(a, fg);
		StyleConstants.setBackground(a, bg);
		a.addAttribute("quest-name", questName);
		doc.insertString(doc.getLength(), indicator + questName, a);
	}

	private void appendItemInline(StyledDocument doc, String itemName, int itemId) throws BadLocationException
	{
		// Try to insert icon before the text
		if (itemId > 0)
		{
			ImageIcon icon = itemIconCache.get(itemId);
			if (icon != null)
			{
				SimpleAttributeSet iconAttrs = new SimpleAttributeSet();
				StyleConstants.setIcon(iconAttrs, icon);
				iconAttrs.addAttribute("item-name", itemName);
				iconAttrs.addAttribute("item-id", itemId);
				doc.insertString(doc.getLength(), " ", iconAttrs);
			}
		}

		// Highlighted text
		doc.insertString(doc.getLength(), itemName, createItemAttrs(itemName, itemId));
	}

	/**
	 * Try to find an item ID by name using ItemManager.search().
	 * Returns -1 if not found.
	 */
	int searchItemId(String itemName)
	{
		try
		{
			List<net.runelite.http.api.item.ItemPrice> results = itemManager.search(itemName);
			if (results != null)
			{
				for (net.runelite.http.api.item.ItemPrice price : results)
				{
					if (price.getName().equalsIgnoreCase(itemName))
					{
						return price.getId();
					}
				}
			}
		}
		catch (Exception e)
		{
			// search may fail or not be available
		}
		return -1;
	}
}
