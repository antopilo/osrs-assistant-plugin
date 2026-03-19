package com.osrsassistant;

import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import net.runelite.client.util.AsyncBufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OsrsAssistantPanel extends PluginPanel
{
	private final OsrsAssistantPlugin plugin;
	private final OsrsAssistantConfig config;
	private AssistantService assistantService;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final MarkdownRenderer markdownRenderer;
	private String sessionId;
	private final Map<String, ImageIcon> skillIconCache = new HashMap<>();
	private final Map<Integer, ImageIcon> itemIconCache = new HashMap<>();
	private volatile boolean iconsLoaded = false;

	private static final Map<String, Integer> SKILL_SPRITES = new HashMap<>();
	static
	{
		SKILL_SPRITES.put("Attack", SpriteID.SKILL_ATTACK);
		SKILL_SPRITES.put("Strength", SpriteID.SKILL_STRENGTH);
		SKILL_SPRITES.put("Defence", SpriteID.SKILL_DEFENCE);
		SKILL_SPRITES.put("Ranged", SpriteID.SKILL_RANGED);
		SKILL_SPRITES.put("Prayer", SpriteID.SKILL_PRAYER);
		SKILL_SPRITES.put("Magic", SpriteID.SKILL_MAGIC);
		SKILL_SPRITES.put("Hitpoints", SpriteID.SKILL_HITPOINTS);
		SKILL_SPRITES.put("Agility", SpriteID.SKILL_AGILITY);
		SKILL_SPRITES.put("Herblore", SpriteID.SKILL_HERBLORE);
		SKILL_SPRITES.put("Thieving", SpriteID.SKILL_THIEVING);
		SKILL_SPRITES.put("Crafting", SpriteID.SKILL_CRAFTING);
		SKILL_SPRITES.put("Fletching", SpriteID.SKILL_FLETCHING);
		SKILL_SPRITES.put("Mining", SpriteID.SKILL_MINING);
		SKILL_SPRITES.put("Smithing", SpriteID.SKILL_SMITHING);
		SKILL_SPRITES.put("Fishing", SpriteID.SKILL_FISHING);
		SKILL_SPRITES.put("Cooking", SpriteID.SKILL_COOKING);
		SKILL_SPRITES.put("Firemaking", SpriteID.SKILL_FIREMAKING);
		SKILL_SPRITES.put("Woodcutting", SpriteID.SKILL_WOODCUTTING);
		SKILL_SPRITES.put("Runecraft", SpriteID.SKILL_RUNECRAFT);
		SKILL_SPRITES.put("Slayer", SpriteID.SKILL_SLAYER);
		SKILL_SPRITES.put("Farming", SpriteID.SKILL_FARMING);
		SKILL_SPRITES.put("Hunter", SpriteID.SKILL_HUNTER);
		SKILL_SPRITES.put("Construction", SpriteID.SKILL_CONSTRUCTION);
	}

	private final JTextPane chatPane;
	private final JScrollPane scrollPane;
	private final JTextArea inputField;
	private final JButton sendButton;

	private JPanel followupPanel;
	private JPanel contentPanel;
	private JFrame popoutFrame;
	private int thinkingStartOffset = -1;
	private volatile Thread activeRequest;

	private static final java.util.regex.Pattern FOLLOWUP_PATTERN =
		java.util.regex.Pattern.compile("\\{followup:([^}]+)\\}");

	private static final String PLACEHOLDER_TEXT = "Ask anything about OSRS...";


	public OsrsAssistantPanel(OsrsAssistantPlugin plugin, OsrsAssistantConfig config, SpriteManager spriteManager, ItemManager itemManager)
	{
		super(false); // disable PluginPanel's built-in wrapping scroll pane

		this.plugin = plugin;
		this.config = config;
		this.assistantService = createService();
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		this.markdownRenderer = new MarkdownRenderer(plugin, itemManager, skillIconCache, itemIconCache);
		this.sessionId = UUID.randomUUID().toString();

		setLayout(new BorderLayout());
		setBackground(Theme.BG);

		contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBackground(Theme.BG);

		// Header — New Chat + Pop Out buttons
		JPanel headerPanel = new JPanel(new BorderLayout(4, 0));
		headerPanel.setBackground(Theme.BG);
		headerPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

		JButton newChatButton = createHeaderButton("+ New Chat");
		newChatButton.addActionListener(e -> startNewChat());
		headerPanel.add(newChatButton, BorderLayout.CENTER);

		JButton popoutButton = createHeaderButton("Pop out");
		popoutButton.setToolTipText("Open in a resizable window");
		popoutButton.addActionListener(e -> togglePopout());
		headerPanel.add(popoutButton, BorderLayout.EAST);

		contentPanel.add(headerPanel, BorderLayout.NORTH);

		// Chat pane with styled document — override getScrollableTracksViewportWidth
		// so text wraps to the viewport width instead of growing infinitely
		chatPane = new JTextPane()
		{
			@Override
			public boolean getScrollableTracksViewportWidth()
			{
				return true;
			}

			@Override
			public boolean getScrollableTracksViewportHeight()
			{
				return false;
			}
		};
		chatPane.setEditable(false);
		chatPane.setBackground(Theme.BG);
		chatPane.setFont(Theme.BASE);
		chatPane.setCursor(Cursor.getDefaultCursor());

		// Handle link clicks and right-click context menu
		chatPane.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					// Left-click goto pill → start navigation
					int[] gotoCoords = getGotoAtPoint(e.getPoint());
					String gotoObj = getAttrAtPoint(e.getPoint(), "goto-object");
					String gotoNpc = getAttrAtPoint(e.getPoint(), "goto-npc");
					if (gotoObj != null)
					{
						plugin.navigateToObject(gotoObj);
						return;
					}
					if (gotoNpc != null)
					{
						plugin.navigateToNpc(gotoNpc);
						return;
					}
					if (gotoCoords != null)
					{
						String gName = getGotoNameAtPoint(e.getPoint());
						plugin.navigateTo(gotoCoords[0], gotoCoords[1], gName);
						return;
					}
					// Goto by name only (no coordinates) — resolve via scene scan or known locations
					String gotoByName = getGotoNameAtPoint(e.getPoint());
					if (gotoByName != null)
					{
						plugin.navigateToByName(gotoByName);
						return;
					}

					String url = getLinkAtPoint(e.getPoint());
					if (url != null)
					{
						openUrl(url);
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showLinkContextMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showLinkContextMenu(e);
				}
			}
		});

		chatPane.addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				String url = getLinkAtPoint(e.getPoint());
				String quest = getQuestNameAtPoint(e.getPoint());
				int[] gotoC = getGotoAtPoint(e.getPoint());
				String gotoObj = getAttrAtPoint(e.getPoint(), "goto-object");
				String gotoNpc = getAttrAtPoint(e.getPoint(), "goto-npc");
				String gotoName = getGotoNameAtPoint(e.getPoint());
				String itemName = getAttrAtPoint(e.getPoint(), "item-name");

				boolean isGoto = gotoC != null || gotoObj != null || gotoNpc != null || gotoName != null;
				boolean isClickable = url != null || quest != null || itemName != null || isGoto;

				chatPane.setCursor(isClickable
					? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
					: Cursor.getDefaultCursor());

				// Tooltip
				if (isGoto)
				{
					String name = gotoObj != null ? gotoObj : (gotoNpc != null ? gotoNpc : gotoName);
					chatPane.setToolTipText("Click to navigate to " + name);
				}
				else if (itemName != null)
				{
					chatPane.setToolTipText("Right-click for options");
				}
				else if (quest != null)
				{
					chatPane.setToolTipText("Right-click for options");
				}
				else if (url != null)
				{
					chatPane.setToolTipText("Open link");
				}
				else
				{
					chatPane.setToolTipText(null);
				}
			}
		});

		scrollPane = new JScrollPane(chatPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(4, 4, 0, 4));
		scrollPane.setBackground(Theme.BG);
		scrollPane.getViewport().setBackground(Theme.BG);

		contentPanel.add(scrollPane, BorderLayout.CENTER);

		// Input panel with rounded border
		JPanel inputPanel = new JPanel(new BorderLayout(0, 0));
		inputPanel.setBackground(Theme.BG);
		inputPanel.setBorder(new EmptyBorder(6, 4, 4, 4));

		// Input container with visible border
		JPanel inputContainer = new JPanel(new BorderLayout(0, 0)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Theme.SURFACE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
				g2.setColor(inputField.hasFocus() ? Theme.ACCENT_STRONG : Theme.BORDER);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
				g2.dispose();
			}
		};
		inputContainer.setOpaque(false);
		inputContainer.setBorder(new EmptyBorder(2, 2, 2, 2));

		inputField = new JTextArea(2, 20);
		inputField.setWrapStyleWord(true);
		inputField.setLineWrap(true);
		inputField.setBackground(Theme.SURFACE);
		inputField.setForeground(Theme.TEXT);
		inputField.setCaretColor(Theme.ACCENT_STRONG);
		inputField.setFont(Theme.BASE.deriveFont(12f));
		inputField.setBorder(new EmptyBorder(6, 8, 6, 8));
		inputField.setOpaque(false);

		// Placeholder text
		inputField.setText(PLACEHOLDER_TEXT);
		inputField.setForeground(Theme.TEXT_DIM);
		inputField.addFocusListener(new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusGained(java.awt.event.FocusEvent e)
			{
				if (inputField.getText().equals(PLACEHOLDER_TEXT))
				{
					inputField.setText("");
					inputField.setForeground(Theme.TEXT);
				}
				inputContainer.repaint();
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e)
			{
				if (inputField.getText().trim().isEmpty())
				{
					inputField.setText(PLACEHOLDER_TEXT);
					inputField.setForeground(Theme.TEXT_DIM);
				}
				inputContainer.repaint();
			}
		});

		inputField.addKeyListener(new KeyListener()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown())
				{
					e.consume();
					sendMessage();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {}

			@Override
			public void keyTyped(KeyEvent e) {}
		});

		JScrollPane inputScrollPane = new JScrollPane(inputField);
		inputScrollPane.setBorder(null);
		inputScrollPane.setOpaque(false);
		inputScrollPane.getViewport().setOpaque(false);
		inputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		inputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		inputContainer.add(inputScrollPane, BorderLayout.CENTER);

		// Send button — styled to match
		sendButton = new JButton("Send") {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = getModel().isRollover() ? Theme.ACCENT_HOVER : Theme.ACCENT_STRONG;
				if ("Cancel".equals(getText())) bg = Theme.CANCEL;
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		sendButton.setForeground(Color.WHITE);
		sendButton.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
		sendButton.setFocusPainted(false);
		sendButton.setContentAreaFilled(false);
		sendButton.setBorderPainted(false);
		sendButton.setOpaque(false);
		sendButton.setBorder(new EmptyBorder(8, 14, 8, 14));
		sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		sendButton.setPreferredSize(new Dimension(65, 0));
		sendButton.addActionListener(e ->
		{
			if (activeRequest != null)
			{
				cancelRequest();
			}
			else
			{
				sendMessage();
			}
		});

		JPanel bottomRow = new JPanel(new BorderLayout(4, 0));
		bottomRow.setOpaque(false);
		bottomRow.add(inputContainer, BorderLayout.CENTER);
		bottomRow.add(sendButton, BorderLayout.EAST);
		inputPanel.add(bottomRow, BorderLayout.CENTER);

		// Follow-up suggestions panel (above input)
		followupPanel = new JPanel();
		followupPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
		followupPanel.setBackground(Theme.BG);
		followupPanel.setBorder(new EmptyBorder(2, 4, 0, 4));
		followupPanel.setVisible(false);

		// Bottom section: followups + input
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setBackground(Theme.BG);
		southPanel.add(followupPanel, BorderLayout.NORTH);
		southPanel.add(inputPanel, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);

		add(contentPanel, BorderLayout.CENTER);

		addMessage("Old Wise Man", "Greetings, adventurer! Ask me anything about the game.", false);
		showStarterPrompts();
	}

	private void setThinking(boolean thinking)
	{
		StyledDocument doc = chatPane.getStyledDocument();
		if (thinking)
		{
			try
			{
				// Insert thinking message as if the assistant is typing
				if (doc.getLength() > 0)
				{
					markdownRenderer.appendText(doc, "\n\n", Theme.BASE, Theme.BG, null, null);
				}
				markdownRenderer.appendText(doc, "\u2502 ", Theme.SENDER, Theme.ACCENT_STRONG, null, null);
				markdownRenderer.appendText(doc, "Old Wise Man\n", Theme.SENDER, Theme.SENDER_ASSISTANT, null, null);
				thinkingStartOffset = doc.getLength();
				markdownRenderer.appendText(doc, "Thinking...", Theme.ITALIC, Theme.TEXT_DIM, null, null);

				// Scroll to bottom
				chatPane.setCaretPosition(doc.getLength());
			}
			catch (BadLocationException e)
			{
				// ignore
			}
		}
		else
		{
			// Remove the thinking message
			if (thinkingStartOffset >= 0)
			{
				try
				{
					// Find the start of the thinking block (go back past sender label + bar)
					// We inserted: \n\n + bar + sender + thinking text
					// Find the \n\n before the bar
					String text = doc.getText(0, doc.getLength());
					int removeFrom = text.lastIndexOf("\u2502 Old Wise Man\nThinking");
					if (removeFrom < 0) removeFrom = thinkingStartOffset - 20;
					if (removeFrom < 0) removeFrom = 0;
					// Also remove the \n\n spacing before
					if (removeFrom >= 2 && text.substring(removeFrom - 2, removeFrom).equals("\n\n"))
					{
						removeFrom -= 2;
					}
					doc.remove(removeFrom, doc.getLength() - removeFrom);
				}
				catch (BadLocationException e)
				{
					// ignore
				}
				thinkingStartOffset = -1;
			}
		}
	}

	private String getLinkAtPoint(Point point)
	{
		int pos = chatPane.viewToModel2D(point);
		if (pos >= 0)
		{
			StyledDocument doc = chatPane.getStyledDocument();
			Element elem = doc.getCharacterElement(pos);
			AttributeSet attrs = elem.getAttributes();
			return (String) attrs.getAttribute("link-url");
		}
		return null;
	}

	private String getLinkTextAtPoint(Point point)
	{
		int pos = chatPane.viewToModel2D(point);
		if (pos >= 0)
		{
			StyledDocument doc = chatPane.getStyledDocument();
			Element elem = doc.getCharacterElement(pos);
			try
			{
				return doc.getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset()).trim();
			}
			catch (BadLocationException e)
			{
				// ignore
			}
		}
		return null;
	}

	private void openUrl(String url)
	{
		try
		{
			Desktop.getDesktop().browse(new URI(url));
		}
		catch (Exception ex)
		{
			// ignore
		}
	}

	private String getAttrAtPoint(Point point, String attrName)
	{
		int pos = chatPane.viewToModel2D(point);
		if (pos >= 0)
		{
			StyledDocument doc = chatPane.getStyledDocument();
			Element elem = doc.getCharacterElement(pos);
			Object val = elem.getAttributes().getAttribute(attrName);
			if (val instanceof String) return (String) val;
		}
		return null;
	}

	private int[] getGotoAtPoint(Point point)
	{
		int pos = chatPane.viewToModel2D(point);
		if (pos >= 0)
		{
			StyledDocument doc = chatPane.getStyledDocument();
			Element elem = doc.getCharacterElement(pos);
			AttributeSet attrs = elem.getAttributes();
			Object x = attrs.getAttribute("goto-x");
			Object y = attrs.getAttribute("goto-y");
			if (x instanceof Integer && y instanceof Integer)
			{
				return new int[]{(Integer) x, (Integer) y};
			}
		}
		return null;
	}

	private String getGotoNameAtPoint(Point point)
	{
		int pos = chatPane.viewToModel2D(point);
		if (pos >= 0)
		{
			StyledDocument doc = chatPane.getStyledDocument();
			Element elem = doc.getCharacterElement(pos);
			AttributeSet attrs = elem.getAttributes();
			Object name = attrs.getAttribute("goto-name");
			if (name instanceof String)
			{
				return (String) name;
			}
		}
		return null;
	}

	private String getQuestNameAtPoint(Point point)
	{
		int pos = chatPane.viewToModel2D(point);
		if (pos >= 0)
		{
			StyledDocument doc = chatPane.getStyledDocument();
			Element elem = doc.getCharacterElement(pos);
			AttributeSet attrs = elem.getAttributes();
			Object name = attrs.getAttribute("quest-name");
			if (name instanceof String)
			{
				return (String) name;
			}
		}
		return null;
	}

	private void showLinkContextMenu(MouseEvent e)
	{
		String url = getLinkAtPoint(e.getPoint());
		String questName = getQuestNameAtPoint(e.getPoint());
		String itemName = getAttrAtPoint(e.getPoint(), "item-name");
		int[] gotoCoords = getGotoAtPoint(e.getPoint());
		String gotoName = getGotoNameAtPoint(e.getPoint());

		String gotoObj = getAttrAtPoint(e.getPoint(), "goto-object");
		String gotoNpc = getAttrAtPoint(e.getPoint(), "goto-npc");

		if (url == null && questName == null && itemName == null && gotoCoords == null && gotoName == null && gotoObj == null && gotoNpc == null)
		{
			return;
		}

		JPopupMenu menu = new JPopupMenu();

		// Goto by name only (no coordinates) — object, npc, or location name
		if (gotoCoords == null && (gotoObj != null || gotoNpc != null || gotoName != null))
		{
			String displayName = gotoObj != null ? gotoObj : (gotoNpc != null ? gotoNpc : gotoName);
			JMenuItem navItem = new JMenuItem("Navigate to " + displayName);
			navItem.addActionListener(ev ->
			{
				if (gotoObj != null) plugin.navigateToObject(gotoObj);
				else if (gotoNpc != null) plugin.navigateToNpc(gotoNpc);
				else plugin.navigateToByName(gotoName);
			});
			menu.add(navItem);

			JMenuItem cancelItem = new JMenuItem("Cancel navigation");
			cancelItem.addActionListener(ev -> plugin.cancelNavigation());
			menu.add(cancelItem);

			menu.show(chatPane, e.getX(), e.getY());
			return;
		}

		if (gotoCoords != null)
		{
			JMenuItem navItem = new JMenuItem("Navigate here");
			final int gx = gotoCoords[0], gy = gotoCoords[1];
			navItem.addActionListener(ev -> plugin.navigateTo(gx, gy, gotoName));
			menu.add(navItem);

			JMenuItem mapItem = new JMenuItem("Show on World Map");
			mapItem.addActionListener(ev -> plugin.showOnWorldMap(gx, gy));
			menu.add(mapItem);

			if (gotoName != null)
			{
				JMenuItem cancelItem = new JMenuItem("Cancel navigation");
				cancelItem.addActionListener(ev -> plugin.cancelNavigation());
				menu.add(cancelItem);
			}

			menu.show(chatPane, e.getX(), e.getY());
			return;
		}

		if (questName != null)
		{
			String wikiUrl = "https://oldschool.runescape.wiki/w/" + questName.replace(' ', '_');
			JMenuItem wikiItem = new JMenuItem("Open Quest on Wiki");
			wikiItem.addActionListener(ev -> openUrl(wikiUrl));
			menu.add(wikiItem);

			JMenuItem askItem = new JMenuItem("Ask about " + questName);
			askItem.addActionListener(ev -> askAssistant("Give me a guide for " + questName));
			menu.add(askItem);

			menu.show(chatPane, e.getX(), e.getY());
			return;
		}

		if (itemName != null)
		{
			String wikiUrl = "https://oldschool.runescape.wiki/w/" + itemName.replace(' ', '_');
			JMenuItem wikiItem = new JMenuItem("Open on Wiki");
			wikiItem.addActionListener(ev -> openUrl(wikiUrl));
			menu.add(wikiItem);

			JMenuItem askItem = new JMenuItem("Ask about " + itemName);
			askItem.addActionListener(ev -> askAssistant("Tell me about the item: " + itemName));
			menu.add(askItem);

			JMenuItem copyItem = new JMenuItem("Copy name");
			copyItem.addActionListener(ev ->
			{
				java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(itemName);
				java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
			});
			menu.add(copyItem);

			menu.show(chatPane, e.getX(), e.getY());
			return;
		}

		if (url != null)
		{
			String linkText = getLinkTextAtPoint(e.getPoint());
			String subject = linkText != null && !linkText.startsWith("http") ? linkText : extractSubjectFromUrl(url);

			JMenuItem openItem = new JMenuItem("Open link");
			openItem.addActionListener(ev -> openUrl(url));
			menu.add(openItem);

			JMenuItem askItem = new JMenuItem("Ask Assistant about this");
			askItem.addActionListener(ev -> askAssistant("Tell me about: " + subject));
			menu.add(askItem);
		}

		menu.show(chatPane, e.getX(), e.getY());
	}

	private static String extractSubjectFromUrl(String url)
	{
		// Handle OSRS wiki URLs like https://oldschool.runescape.wiki/w/Dragon_scimitar
		if (url.contains("runescape.wiki/w/"))
		{
			String page = url.substring(url.indexOf("/w/") + 3);
			return page.replace("_", " ").replaceAll("#.*", "").replaceAll("\\?.*", "");
		}
		// Fallback: use the last path segment
		String path = url.replaceAll("\\?.*", "").replaceAll("#.*", "");
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash >= 0 && lastSlash < path.length() - 1)
		{
			return path.substring(lastSlash + 1).replace("_", " ").replace("-", " ");
		}
		return url;
	}


	private JButton createFollowupBubble(String label)
	{
		JButton btn = new JButton(label) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(getModel().isRollover() ? Theme.SURFACE_HOVER : Theme.SURFACE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
				g2.setColor(getModel().isRollover() ? Theme.ACCENT_STRONG : Theme.BORDER);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		btn.setForeground(Theme.TEXT_DIM);
		btn.setFont(Theme.SMALL);
		btn.setFocusPainted(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		btn.setOpaque(false);
		btn.setBorder(new EmptyBorder(5, 10, 5, 10));
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return btn;
	}

	private void setFollowups(String[] questions)
	{
		followupPanel.removeAll();
		if (questions == null || questions.length == 0)
		{
			followupPanel.setVisible(false);
			return;
		}

		for (String question : questions)
		{
			String q = question.trim();
			if (q.isEmpty()) continue;

			JButton btn = createFollowupBubble(q);
			btn.addActionListener(e ->
			{
				followupPanel.setVisible(false);
				askAssistant(q);
			});
			followupPanel.add(btn);
		}

		followupPanel.setVisible(true);
		followupPanel.revalidate();
		followupPanel.repaint();
	}

	/**
	 * Extract {followup:Q1|Q2|Q3} from response text.
	 * Returns the questions array and strips the tag from the text.
	 */
	private String stripAndExtractFollowups(String text)
	{
		java.util.regex.Matcher m = FOLLOWUP_PATTERN.matcher(text);
		if (m.find())
		{
			String raw = m.group(1);
			String[] questions = raw.split("\\|");
			SwingUtilities.invokeLater(() -> setFollowups(questions));
			return m.replaceAll("").trim();
		}
		SwingUtilities.invokeLater(() -> setFollowups(null));
		return text;
	}

	/**
	 * Pre-fetch item icons referenced in the response text.
	 * Called on the worker thread before rendering so icons are cached.
	 */
	private void prefetchItemIcons(String text)
	{
		java.util.List<Integer> ids = new java.util.ArrayList<>();

		// Match {item:Name:ID} tags
		java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("\\{item:([^}:]+):(\\d+)\\}").matcher(text);
		while (m1.find())
		{
			try
			{
				int id = Integer.parseInt(m1.group(2));
				if (id > 0 && !itemIconCache.containsKey(id))
				{
					ids.add(id);
				}
			}
			catch (NumberFormatException ignored) {}
		}

		// Match {item:Name} tags without ID — resolve via search
		java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("\\{item:([^}:]+)\\}").matcher(text);
		while (m2.find())
		{
			String name = m2.group(1);
			int id = markdownRenderer.searchItemId(name);
			if (id > 0 && !itemIconCache.containsKey(id))
			{
				ids.add(id);
			}
		}

		if (ids.isEmpty()) return;

		// Request all icons and wait for them with a short timeout
		java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(ids.size());
		for (int id : ids)
		{
			try
			{
				AsyncBufferedImage asyncImg = itemManager.getImage(id);
				asyncImg.onLoaded(() ->
				{
					Image scaled = asyncImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
					itemIconCache.put(id, new ImageIcon(scaled));
					latch.countDown();
				});
			}
			catch (Exception e)
			{
				latch.countDown();
			}
		}

		try
		{
			// Wait up to 1 second for icons to load
			latch.await(1, java.util.concurrent.TimeUnit.SECONDS);
		}
		catch (InterruptedException ignored)
		{
			Thread.currentThread().interrupt();
		}
	}

	private JButton createHeaderButton(String label)
	{
		JButton btn = new JButton(label) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(getModel().isRollover() ? Theme.ACCENT_STRONG.darker() : Theme.SURFACE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.setColor(getModel().isRollover() ? Theme.ACCENT_HOVER : Theme.BORDER);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		btn.setForeground(Theme.TEXT);
		btn.setFont(Theme.SMALL_BOLD);
		btn.setFocusPainted(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		btn.setOpaque(false);
		btn.setBorder(new EmptyBorder(7, 0, 7, 0));
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return btn;
	}

	private void togglePopout()
	{
		if (popoutFrame != null)
		{
			// Pop back in — return content to sidebar
			popoutFrame.dispose();
			popoutFrame = null;
			removeAll();
			setLayout(new BorderLayout());
			add(contentPanel, BorderLayout.CENTER);
			revalidate();
			repaint();
			return;
		}

		// Pop out — move content to a floating window
		remove(contentPanel);

		// Show placeholder in sidebar
		JPanel placeholder = new JPanel(new BorderLayout());
		placeholder.setBackground(Theme.BG);
		JLabel label = new JLabel("Chat is in a separate window", JLabel.CENTER);
		label.setForeground(Theme.TEXT_DIM);
		label.setFont(Theme.ITALIC);
		placeholder.add(label, BorderLayout.CENTER);
		JButton popinButton = createHeaderButton("Pop back in");
		popinButton.addActionListener(ev -> togglePopout());
		JPanel btnWrap = new JPanel(new BorderLayout());
		btnWrap.setBackground(Theme.BG);
		btnWrap.setBorder(new EmptyBorder(0, 20, 20, 20));
		btnWrap.add(popinButton, BorderLayout.CENTER);
		placeholder.add(btnWrap, BorderLayout.SOUTH);
		add(placeholder, BorderLayout.CENTER);
		revalidate();
		repaint();

		popoutFrame = new JFrame("OSRS Assistant");
		popoutFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		popoutFrame.setSize(400, 700);
		popoutFrame.setMinimumSize(new Dimension(300, 400));
		popoutFrame.getContentPane().setBackground(Theme.BG);
		popoutFrame.add(contentPanel);

		// When closed via X button, return content to sidebar
		popoutFrame.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(java.awt.event.WindowEvent e)
			{
				popoutFrame = null;
				removeAll();
				setLayout(new BorderLayout());
				add(contentPanel, BorderLayout.CENTER);
				revalidate();
				repaint();
			}
		});

		popoutFrame.setLocationRelativeTo(null);
		popoutFrame.setVisible(true);
	}

	private static final String[][] STARTER_PROMPTS = {
		{"What should I do next?", "Based on my current stats, gear, and quest progress, what should I focus on next for the most efficient progression?"},
		{"Gear check", "Review my current equipment and inventory. Am I using the best gear available for my levels? What upgrades should I get?"},
		{"Money making", "What are the best money-making methods available to me right now with my current stats and quests completed?"},
		{"Train what?", "Which of my skills should I prioritize training next? Consider quest unlocks, diary requirements, and useful level thresholds."},
	};

	private void showStarterPrompts()
	{
		followupPanel.removeAll();
		for (String[] pair : STARTER_PROMPTS)
		{
			String label = pair[0];
			String prompt = pair[1];
			JButton btn = createFollowupBubble(label);
			btn.addActionListener(e ->
			{
				followupPanel.setVisible(false);
				askAssistant(prompt);
			});
			followupPanel.add(btn);
		}
		followupPanel.setVisible(true);
		followupPanel.revalidate();
		followupPanel.repaint();
	}

	private void startNewChat()
	{
		sessionId = UUID.randomUUID().toString();
		chatPane.setText("");
		skillIconCache.clear();
		addMessage("Old Wise Man", "What would you like to know?", false);
		showStarterPrompts();

		inputField.setText(PLACEHOLDER_TEXT);
		inputField.setForeground(Theme.TEXT_DIM);
	}

	public void askAssistant(String question)
	{
		SwingUtilities.invokeLater(() -> sendQuestion(question));
	}

	public AssistantService getAssistantService()
	{
		return assistantService;
	}

	public void refreshService()
	{
		assistantService.cancel();
		assistantService = createService();
	}

	private AssistantService createService()
	{
		LlmProvider provider = config.llmProvider();
		if (provider == LlmProvider.OPENAI || provider == LlmProvider.ANTHROPIC)
		{
			return new DirectLlmService(provider, config.apiKey(), config.llmModelId(), config.customPrompt(), config);
		}
		return new CustomBackendService(config);
	}

	private void sendMessage()
	{
		String text = inputField.getText().trim();
		if (text.isEmpty() || text.equals(PLACEHOLDER_TEXT))
		{
			return;
		}

		inputField.setText("");
		inputField.setForeground(Theme.TEXT);
		sendQuestion(text);
	}

	private void cancelRequest()
	{
		assistantService.cancel();
		Thread thread = activeRequest;
		if (thread != null)
		{
			thread.interrupt();
			activeRequest = null;
		}
		setThinking(false);
		sendButton.setText("Send");
		inputField.setEnabled(true);
		inputField.requestFocusInWindow();
	}

	private void sendQuestion(String text)
	{
		PlayerContext ctx = plugin.getPlayerContext();
		String senderName = (ctx != null && ctx.getPlayerName() != null) ? ctx.getPlayerName() : "You";
		addMessage(senderName, text, true);

		sendButton.setText("Cancel");
		sendButton.repaint();
		inputField.setEnabled(false);
		setThinking(true);

		PlayerContext playerContext = plugin.getPlayerContext();

		Thread thread = new Thread(() ->
		{
			String reply = null;
			boolean cancelled = false;
			try
			{
				reply = assistantService.sendMessage(text, sessionId, playerContext);
			}
			catch (Exception e)
			{
				reply = "Error: " + e.getMessage();
			}

			// If reply contains a checklist, decompose it into granular micro-steps
			if (reply != null && reply.contains("{checklist:") && !Thread.currentThread().isInterrupted())
			{
				try
				{
					reply = assistantService.decomposeChecklist(reply, playerContext);
				}
				catch (Exception de)
				{
					// Decomposition failed — use original reply
				}
			}

			// Pre-fetch item icons before rendering so they're cached
			if (reply != null && !Thread.currentThread().isInterrupted())
			{
				prefetchItemIcons(reply);
			}

			cancelled = Thread.currentThread().isInterrupted() || reply == null;
			final String finalReply = reply;
			final boolean wasCancelled = cancelled;

			SwingUtilities.invokeLater(() ->
			{
				activeRequest = null;
				setThinking(false);
				if (!wasCancelled && finalReply != null)
				{
					addMessage("Old Wise Man", finalReply, false);
				}
				sendButton.setText("Send");
				sendButton.repaint();
				inputField.setEnabled(true);
				inputField.requestFocusInWindow();
			});
		});
		activeRequest = thread;
		thread.start();
	}

	private void addMessage(String sender, String text, boolean isUser)
	{
		StyledDocument doc = chatPane.getStyledDocument();

		try
		{
			// Add spacing between messages
			if (doc.getLength() > 0)
			{
				markdownRenderer.appendText(doc, "\n\n", Theme.BASE, Theme.BG, null, null);
			}

			// Visual separator — colored bar before the sender name
			Color accentBar = isUser ? Theme.BAR_USER : Theme.ACCENT_STRONG;
			markdownRenderer.appendText(doc, "\u2502 ", Theme.SENDER, accentBar, null, null);

			// Sender label
			Color senderColor = isUser ? Theme.SENDER_USER : Theme.SENDER_ASSISTANT;
			markdownRenderer.appendText(doc, sender + "\n", Theme.SENDER, senderColor, null, null);

			// Message content
			if (isUser)
			{
				markdownRenderer.appendText(doc, text, Theme.BASE, Theme.TEXT_USER, null, null);
				// Clear followups when user sends a message
				setFollowups(null);
			}
			else
			{
				// Extract follow-up suggestions before rendering
				text = stripAndExtractFollowups(text);

				// Parse and activate checklist if present
				Checklist checklist = ChecklistParser.parse(text);
				if (checklist != null)
				{
					if (plugin.getChecklistManager().hasChecklist())
					{
						// Ask user: add alongside or replace
						int choice = javax.swing.JOptionPane.showOptionDialog(
							this,
							"You already have an active checklist. What would you like to do?",
							"New Checklist",
							javax.swing.JOptionPane.DEFAULT_OPTION,
							javax.swing.JOptionPane.QUESTION_MESSAGE,
							null,
							new String[]{"Add alongside", "Replace", "Cancel"},
							"Add alongside"
						);
						if (choice == 0)
						{
							plugin.addChecklist(checklist);
						}
						else if (choice == 1)
						{
							plugin.setChecklist(checklist);
						}
						// choice == 2 or closed: do nothing
					}
					else
					{
						plugin.setChecklist(checklist);
					}
					text = ChecklistParser.stripChecklistTags(text);
				}

				if (!text.isEmpty())
				{
					markdownRenderer.appendMarkdown(doc, text);
				}
			}
		}
		catch (BadLocationException e)
		{
			// ignore
		}

		// Auto-scroll to bottom
		SwingUtilities.invokeLater(() ->
		{
			chatPane.setCaretPosition(doc.getLength());
			JScrollBar vertical = scrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());
		});
	}

	/**
	 * Must be called on the client thread (e.g. from onGameTick).
	 * Loads all skill sprites and the coin icon into the cache
	 * so they're available when rendering on the EDT.
	 */
	public void preloadIcons()
	{
		if (iconsLoaded)
		{
			return;
		}

		for (Map.Entry<String, Integer> entry : SKILL_SPRITES.entrySet())
		{
			if (skillIconCache.containsKey(entry.getKey()))
			{
				continue;
			}
			final String skillName = entry.getKey();
			spriteManager.getSpriteAsync(entry.getValue(), 0, img ->
			{
				if (img != null)
				{
					Image scaled = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
					skillIconCache.put(skillName, new ImageIcon(scaled));
				}
			});
		}

		// Mark loaded once all skills are cached
		if (skillIconCache.size() >= SKILL_SPRITES.size())
		{
			iconsLoaded = true;
		}
	}


	/**
	 * Call from the client thread to preload an item icon into the cache.
	 */
	public void preloadItemIcon(int itemId)
	{
		if (itemIconCache.containsKey(itemId))
		{
			return;
		}
		try
		{
			AsyncBufferedImage asyncImg = itemManager.getImage(itemId);
			asyncImg.onLoaded(() ->
			{
				Image scaled = asyncImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
				itemIconCache.put(itemId, new ImageIcon(scaled));
			});
		}
		catch (Exception | AssertionError e)
		{
			// ignore
		}
	}

}
