package com.osrsassistant;

import lombok.Data;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;

/**
 * A verifiable condition for a checklist step.
 * Each condition must be checkable via game state — no manual-only conditions allowed.
 *
 * Supported condition types:
 *   item:Name:Qty     — player has item in inventory or equipment
 *   skill:Name:Level  — player has reached skill level
 *   quest:Name        — quest is completed
 *   chat:text         — game message contains text (sticky once triggered)
 *   loc:Name/RegionId — player is at location
 *   xp:Skill          — any XP gained in skill (sticky once triggered)
 *   xp:Skill:Amount   — cumulative XP gained >= amount since step activated (sticky)
 *   varbit:Id:Value    — game varbit equals value (server-authoritative state)
 */
@Data
public class StepCondition
{
	public enum Type
	{
		ITEM,     // item:Name:Qty — player has item in inventory or equipment
		SKILL,    // skill:Name:Level — player has reached level
		QUEST,    // quest:Name — quest is completed
		CHAT,     // chat:partial text — game message contains text
		LOCATION, // loc:RegionId or loc:Name — player is at region
		XP,       // xp:Skill or xp:Skill:Amount — XP gained since step activated
		VARBIT    // varbit:Id:Value — game varbit equals value
	}

	private final Type type;
	private final String param1;
	private final String param2;

	// For chat conditions — set to true when the message has been seen
	private boolean chatTriggered = false;

	// For XP conditions — tracks cumulative XP gained since activation
	private boolean xpActivated = false;
	private int xpBaseline = -1; // XP when step became active (-1 = not yet captured)
	private boolean xpTriggered = false;

	public StepCondition(Type type, String param1, String param2)
	{
		this.type = type;
		this.param1 = param1;
		this.param2 = param2;
	}

	/**
	 * Parse a condition string like "item:Clockwork:4" or "xp:Prayer"
	 */
	public static StepCondition parse(String condition)
	{
		if (condition == null || condition.isEmpty())
		{
			return null;
		}

		String[] parts = condition.split(":", 3);
		if (parts.length < 2)
		{
			return null;
		}

		String typeStr = parts[0].trim().toLowerCase();
		String p1 = parts[1].trim();
		String p2 = parts.length > 2 ? parts[2].trim() : null;

		switch (typeStr)
		{
			case "item":
				return new StepCondition(Type.ITEM, p1, p2 != null ? p2 : "1");
			case "skill":
				return new StepCondition(Type.SKILL, p1, p2 != null ? p2 : "1");
			case "quest":
				return new StepCondition(Type.QUEST, p1, null);
			case "chat":
				// Rejoin parts 1+ since chat text may contain colons
				String chatText = condition.substring(condition.indexOf(':') + 1).trim();
				return new StepCondition(Type.CHAT, chatText, null);
			case "loc":
				return new StepCondition(Type.LOCATION, p1, null);
			case "xp":
				// xp:Skill or xp:Skill:Amount
				return new StepCondition(Type.XP, p1, p2);
			case "varbit":
				if (p2 == null) return null; // varbit requires id:value
				return new StepCondition(Type.VARBIT, p1, p2);
			default:
				return null;
		}
	}

	/**
	 * Check if this condition is currently satisfied.
	 */
	public boolean check(Client client, ItemManager itemManager)
	{
		switch (type)
		{
			case ITEM:
				return checkItem(client, itemManager);
			case SKILL:
				return checkSkill(client);
			case QUEST:
				return checkQuest(client);
			case CHAT:
				return chatTriggered;
			case LOCATION:
				return checkLocation(client);
			case XP:
				return checkXp(client);
			case VARBIT:
				return checkVarbit(client);
			default:
				return false;
		}
	}

	/**
	 * Reset sticky state (for Undo).
	 */
	public void reset()
	{
		chatTriggered = false;
		xpActivated = false;
		xpBaseline = -1;
		xpTriggered = false;
	}

	// ---- Chat handling ----

	public void onChatMessage(String message)
	{
		if (type == Type.CHAT && !chatTriggered)
		{
			String msgLower = message.toLowerCase();
			String condLower = param1.toLowerCase();

			// Exact substring match
			if (msgLower.contains(condLower))
			{
				chatTriggered = true;
				return;
			}

			// Fuzzy match: if most key words from the condition appear in the message
			String[] words = condLower.split("\\s+");
			if (words.length >= 2)
			{
				int matched = 0;
				int meaningful = 0;
				for (String word : words)
				{
					if (isStopWord(word)) continue;
					meaningful++;
					if (msgLower.contains(word))
					{
						matched++;
					}
				}
				// Match if at least half the meaningful words are found
				if (meaningful > 0 && matched >= Math.ceil(meaningful * 0.5))
				{
					chatTriggered = true;
				}
			}
		}
	}

	private static boolean isStopWord(String word)
	{
		return word.length() <= 2 || word.equals("the") || word.equals("you")
			|| word.equals("your") || word.equals("and") || word.equals("with");
	}

	// ---- XP handling ----

	/**
	 * Called when XP changes. Captures baseline on first call, then tracks gain.
	 */
	public void onXpChanged(Skill skill, int currentXp)
	{
		if (type != Type.XP || xpTriggered) return;

		// Match skill name
		if (!skill.getName().equalsIgnoreCase(param1)) return;

		if (!xpActivated)
		{
			// First tick — capture baseline XP
			xpBaseline = currentXp;
			xpActivated = true;
			return;
		}

		int gained = currentXp - xpBaseline;
		if (gained <= 0) return;

		if (param2 == null)
		{
			// xp:Skill — any XP gain triggers completion
			xpTriggered = true;
		}
		else
		{
			// xp:Skill:Amount — cumulative gain must reach threshold
			int threshold = 1;
			try
			{
				threshold = Integer.parseInt(param2);
			}
			catch (NumberFormatException e)
			{
				threshold = 1;
			}
			if (gained >= threshold)
			{
				xpTriggered = true;
			}
		}
	}

	private boolean checkXp(Client client)
	{
		if (xpTriggered) return true;

		// If not yet activated, capture baseline now
		if (!xpActivated)
		{
			for (Skill skill : Skill.values())
			{
				if (skill.getName().equalsIgnoreCase(param1))
				{
					xpBaseline = client.getSkillExperience(skill);
					xpActivated = true;
					break;
				}
			}
		}
		return false;
	}

	// ---- Item handling ----

	private boolean checkItem(Client client, ItemManager itemManager)
	{
		int requiredQty = 1;
		try
		{
			requiredQty = Integer.parseInt(param2);
		}
		catch (NumberFormatException e)
		{
			// default to 1
		}

		int count = 0;

		// Check inventory
		count += countItemInContainer(client, itemManager, InventoryID.INVENTORY);

		// Check equipment
		count += countItemInContainer(client, itemManager, InventoryID.EQUIPMENT);

		return count >= requiredQty;
	}

	private int countItemInContainer(Client client, ItemManager itemManager, InventoryID containerId)
	{
		ItemContainer container = client.getItemContainer(containerId);
		if (container == null) return 0;

		int count = 0;
		for (Item item : container.getItems())
		{
			if (item.getId() == -1 || item.getId() == 6512) continue;
			String name = itemManager.getItemComposition(item.getId()).getName();
			if (name.equalsIgnoreCase(param1))
			{
				count += item.getQuantity();
			}
		}
		return count;
	}

	// ---- Skill handling ----

	private boolean checkSkill(Client client)
	{
		int requiredLevel = 1;
		try
		{
			requiredLevel = Integer.parseInt(param2);
		}
		catch (NumberFormatException e)
		{
			// default to 1
		}

		for (Skill skill : Skill.values())
		{
			if (skill.getName().equalsIgnoreCase(param1))
			{
				return client.getRealSkillLevel(skill) >= requiredLevel;
			}
		}
		return false;
	}

	// ---- Quest handling ----

	private boolean checkQuest(Client client)
	{
		for (Quest quest : Quest.values())
		{
			if (quest.getName().equalsIgnoreCase(param1))
			{
				return quest.getState(client) == QuestState.FINISHED;
			}
		}
		return false;
	}

	// ---- Varbit handling ----

	private boolean checkVarbit(Client client)
	{
		try
		{
			int varbitId = Integer.parseInt(param1);
			int expectedValue = Integer.parseInt(param2);
			return client.getVarbitValue(varbitId) >= expectedValue;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	// ---- Location handling ----

	private static final int LOCATION_RADIUS = 30; // tiles — how close counts as "at location"

	private boolean checkLocation(Client client)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}

		int playerX = client.getLocalPlayer().getWorldLocation().getX();
		int playerY = client.getLocalPlayer().getWorldLocation().getY();
		int playerRegion = client.getLocalPlayer().getWorldLocation().getRegionID();

		// Try numeric region ID first
		try
		{
			int regionId = Integer.parseInt(param1);
			return playerRegion == regionId;
		}
		catch (NumberFormatException e)
		{
			// Not a number — try name-based proximity check
		}

		// Name-based: check if player is within LOCATION_RADIUS tiles of known coordinates
		int[] coords = LocationDatabase.getCoordinates(param1);
		if (coords != null)
		{
			int dx = playerX - coords[0];
			int dy = playerY - coords[1];
			return (dx * dx + dy * dy) <= (LOCATION_RADIUS * LOCATION_RADIUS);
		}

		return false;
	}
}
