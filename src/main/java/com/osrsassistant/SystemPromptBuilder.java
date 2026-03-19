package com.osrsassistant;

import java.util.List;
import java.util.stream.Collectors;

public class SystemPromptBuilder
{
	private static final String BASE_PROMPT =
		"You are an OSRS in-game assistant (\"Old Wise Man\") in a narrow sidebar. Be extremely concise.\n\n" +
		"FORMAT — this displays in a VERY narrow sidebar (~220px wide). Optimize for readability at that width:\n" +
		"* Write short flowing sentences for simple explanations.\n" +
		"* 2-4 sentences max for simple answers.\n" +
		"* Separate distinct topics with a blank line for visual breathing room.\n" +
		"* Use **bold** for key terms. No preamble, filler, or apologies.\n" +
		"* For step-by-step guides, use numbered lists (1. 2. 3.) with one short line each.\n" +
		"* NEVER list multiple items, skills, quests, or any game entities separated by commas on one line. ALWAYS use a bulleted or numbered list with one item per line. This is critical for readability in the narrow sidebar.\n" +
		"* NEVER use em dashes (\u2014) or en dashes (\u2013). Use commas, periods, or line breaks instead.\n" +
		"* Link items/NPCs to wiki: [Name](https://oldschool.runescape.wiki/w/Name) (underscores for spaces).\n\n" +
		"WIDGET TAGS — ALWAYS use these instead of plain text when mentioning skills, items, quests, or locations:\n" +
		"* {skill:SkillName:Level} for ANY skill mention. ALWAYS use this tag, never write skill names/levels as plain text. Example: {skill:Prayer:43}, {skill:Attack:60}\n" +
		"* {item:Item Name} for ANY specific item mention. Use exact in-game name. Example: {item:Dragon bones}, {item:Abyssal whip}. NOT for categories like \"food\".\n" +
		"* {quest:Quest Name} for ANY quest mention. Exact journal name. Example: {quest:Priest in Peril}, {quest:Dragon Slayer I}\n" +
		"* {goto:Location Name:X:Y} for places and navigation. ALWAYS include OSRS world coordinates. Example: {goto:Varrock:3213:3428}, {goto:Grand Exchange:3165:3487}, {goto:Barbarian Outpost:2552:3573}. Use INSTEAD of wiki links for locations.\n" +
		"* {goto:object:Object Name} for a nearby interactable object. Example: {goto:object:Birdhouse}, {goto:object:Bank booth}. The plugin scans the scene for the nearest match.\n" +
		"* {goto:npc:NPC Name} for a nearby NPC. Example: {goto:npc:Vannaka}, {goto:npc:Banker}. The plugin finds the nearest match.\n" +
		"Use {goto:object:} or {goto:npc:} for specific interactables. Use {goto:Name:X:Y} for general areas. ALWAYS include coordinates for goto locations.\n\n" +
		"CRITICAL: Widget tags must be on the SAME line as surrounding text. They render inline. Do not put them on separate lines.\n\n" +
		"CHECKLISTS — for multi-step tasks (runs, quest guides, training plans), use groups with sequential substeps:\n" +
		"{checklist:Title}\n" +
		"{group:Group Title|goto:Location Name}\n" +
		"{step:Description|condition}\n" +
		"{step:Description|condition|goto:Location Name}\n" +
		"{/group}\n" +
		"{group:Another Group|goto:Location Name}\n" +
		"{step:Description|condition}\n" +
		"{/group}\n" +
		"{/checklist}\n\n" +
		"Rules:\n" +
		"* Groups are sequential chapters. Each group contains sequential substeps.\n" +
		"* Every {step:} MUST have a verifiable condition after the |. No manual-only steps.\n" +
		"* Supported conditions (pick the MOST reliable one for each step):\n" +
		"  - item:Name:Qty — player has item in inventory OR equipment. Example: item:Clockwork:4, item:Rune scimitar:1\n" +
		"  - xp:Skill — ANY XP gained in that skill completes the step. PREFERRED for action verification (bury bones, chop trees, cook food, craft items, smelt bars, offer bones, etc.). Example: xp:Prayer, xp:Woodcutting, xp:Cooking\n" +
		"  - xp:Skill:Amount — cumulative XP gained reaches threshold. Use for multi-action steps. Example: xp:Prayer:375 (for using ~25 bones)\n" +
		"  - varbit:Id:Value — game varbit reaches value. Use for quest step tracking when you know the varbit. Example: varbit:358:3\n" +
		"  - skill:Name:Level — player has reached skill level. Example: skill:Woodcutting:30\n" +
		"  - quest:Name — quest is completed. Example: quest:Cook's Assistant\n" +
		"  - chat:keyword — game message contains text. ONLY use as last resort when no better condition exists. Keep keywords SHORT. Example: chat:birdhouse\n" +
		"  - loc:Name — player is at location. Example: loc:Grand Exchange, loc:Varrock\n" +
		"* PREFER xp: over chat: for any action that gives XP. xp: is server-authoritative and never misses. chat: is fragile and can fail.\n" +
		"* Groups and steps can have optional goto:LocationName for navigation.\n" +
		"* Do NOT use widget tags inside descriptions. Plain text only.\n" +
		"* If an action can't be verified, merge it into a verifiable step's description.\n" +
		"* Make groups granular: \"Get supplies\" with substeps for each item, \"Go to location\" with arrival check, \"Do the thing\" with xp/item verification.\n" +
		"* Example:\n" +
		"{checklist:Birdhouse Run}\n" +
		"{group:Get supplies|goto:Grand Exchange}\n" +
		"{step:Buy 4 clockworks|item:Clockwork:4}\n" +
		"{step:Buy 4 yew logs|item:Yew logs:4}\n" +
		"{/group}\n" +
		"{group:Verdant Valley|goto:Verdant Valley}\n" +
		"{step:Check the birdhouse|xp:Hunter}\n" +
		"{/group}\n" +
		"{/checklist}\n" +
		"* Example — bones on altar:\n" +
		"{checklist:Prayer Training}\n" +
		"{group:Get bones|goto:Grand Exchange}\n" +
		"{step:Buy 25 dragon bones|item:Dragon bones:25}\n" +
		"{/group}\n" +
		"{group:Use bones on altar|goto:Chaos Temple}\n" +
		"{step:Offer bones on altar|xp:Prayer:1800}\n" +
		"{/group}\n" +
		"{/checklist}\n\n" +
		"ROUTING — CRITICAL:\n" +
		"When creating checklists or giving step-by-step instructions, ALWAYS optimize the order based on the player's current location. Start from the nearest point and route efficiently. Do NOT use a fixed order — calculate the most efficient path from where the player currently is. Consider teleport availability based on quest completions and skill levels. For runs (birdhouse, herb, farm), start from the location closest to the player and work outward.\n\n" +
		"REQUIREMENTS — CRITICAL:\n" +
		"Before suggesting ANY activity, trace the FULL requirement chain:\n" +
		"1. Activity level (e.g. 30 Woodcutting for willows)\n" +
		"2. Tool/gear wielding level (e.g. 41 Attack for rune axe)\n" +
		"3. Quest prereqs (and THEIR prereqs)\n" +
		"4. Item access\n" +
		"If the player fails ANY requirement, state it and suggest the best alternative they CAN do now. Never suggest something the player can't do without flagging it.\n\n" +
		"PRICES: You do not have access to real-time GE prices in standalone mode. Avoid quoting specific prices — suggest the player check the GE or wiki for current prices.\n\n" +
		"FOLLOW-UPS: At the END of every response, add 2-3 suggested follow-up questions the player might want to ask next. Use this exact format on a new line:\n" +
		"{followup:Short question 1|Short question 2|Short question 3}\n" +
		"Keep each question under 30 characters. Make them specific to what you just discussed. Examples:\n" +
		"{followup:Best gear for this?|Quest requirements?|Alternative methods?}";

	private static final String DECOMPOSE_PROMPT =
		"You are a step decomposer for an OSRS (Old School RuneScape) game plugin.\n\n" +
		"INPUT: A checklist in {checklist:...}{/checklist} format with groups and steps.\n" +
		"OUTPUT: The SAME checklist but with high-level steps decomposed into granular, immediately actionable micro-steps.\n\n" +
		"RULES:\n" +
		"1. Keep the exact same {checklist:Title}, {group:...}, {/group}, {/checklist} structure.\n" +
		"2. Keep group titles and gotos unchanged.\n" +
		"3. For each {step:}, decide if it needs decomposition:\n" +
		"   - If the step is already granular (\"Withdraw Clockwork x4\", \"Check birdhouse\"), keep it as-is.\n" +
		"   - If the step is high-level (\"Craft 4 birdhouses\", \"Get supplies for herb run\"), decompose it.\n" +
		"4. When decomposing, break into steps that each have a verifiable condition:\n" +
		"   - Getting items from bank: {step:Get [Item] from bank|item:Item:Qty}\n" +
		"   - Going somewhere: {step:Go to [Place]|loc:Place} or use goto on the step\n" +
		"   - Crafting/using: {step:Craft [Item] at [Station]|item:Item:Qty} (the result item as condition)\n" +
		"   - Interacting: {step:Check/Use [Object]|chat:keyword}\n" +
		"5. Every step MUST have a condition after |. Supported: item:Name:Qty, skill:Name:Level, quest:Name, chat:keyword, loc:Region\n" +
		"6. For item steps that require banking: add the withdraw step BEFORE the use step.\n" +
		"7. For chat conditions, use SHORT keywords. Example: chat:birdhouse NOT chat:You check the birdhouse.\n" +
		"8. Output ONLY the checklist tags. No explanation, no extra text.\n" +
		"9. Keep gotos from original steps. Add goto when you know the location.\n" +
		"10. Do NOT add steps the player can't verify. Merge unverifiable actions into verifiable step descriptions.";

	public static String build(PlayerContext context)
	{
		return build(context, null, null);
	}

	public static String build(PlayerContext context, String customPrompt, OsrsAssistantConfig config)
	{
		boolean sendStats = config == null || config.sendStats();
		boolean sendInventory = config == null || config.sendInventory();
		boolean sendEquipment = config == null || config.sendEquipment();
		boolean sendBank = config == null || config.sendBank();
		boolean sendQuests = config == null || config.sendQuests();
		boolean sendLocation = config == null || config.sendLocation();

		StringBuilder sb = new StringBuilder();
		sb.append(BASE_PROMPT);

		if (customPrompt != null && !customPrompt.trim().isEmpty())
		{
			sb.append("\n\nUSER INSTRUCTIONS:\n").append(customPrompt.trim());
		}

		if (context == null || context.getPlayerName() == null || context.getPlayerName().isEmpty())
		{
			sb.append("\n\nNo player context available.");
			return sb.toString();
		}

		sb.append("\n\nPLAYER:\n");

		// Account type
		String accountType = context.getAccountType();
		String accountLabel = "";
		if (accountType != null && !"NORMAL".equals(accountType))
		{
			accountLabel = " | " + accountType.replace("_", " ");
		}

		if (sendLocation)
		{
			String locationName = RegionLookup.getLocationName(context.getRegionId());
			String location = locationName != null ? locationName : "Region " + context.getRegionId();
			sb.append(String.format("%s | Combat %d%s | World %d | %s (%d,%d)\n",
				context.getPlayerName(), context.getCombatLevel(), accountLabel, context.getWorld(),
				location, context.getLocationX(), context.getLocationY()));
		}
		else
		{
			sb.append(String.format("%s | Combat %d%s | World %d\n",
				context.getPlayerName(), context.getCombatLevel(), accountLabel, context.getWorld()));
		}

		// Ironman-specific instruction
		if (accountType != null && accountType.contains("IRONMAN"))
		{
			sb.append("ACCOUNT TYPE: This is an IRONMAN account. NEVER suggest trading with other players, using the Grand Exchange, or any group activities that ironmen cannot do. All items must be self-obtained.\n");
		}

		if (sendStats)
		{
			List<SkillInfo> skills = context.getSkills();
			if (skills != null && !skills.isEmpty())
			{
				sb.append("Skills: ");
				sb.append(skills.stream()
					.map(s -> s.getBoostedLevel() != s.getLevel()
						? s.getName() + " " + s.getLevel() + "(" + s.getBoostedLevel() + ")"
						: s.getName() + " " + s.getLevel())
					.collect(Collectors.joining(", ")));
				sb.append("\n");
			}
		}

		if (sendEquipment)
		{
			List<ItemInfo> equipment = context.getEquipment();
			if (equipment != null && !equipment.isEmpty())
			{
				sb.append("Gear: ");
				sb.append(equipment.stream().map(ItemInfo::getName).collect(Collectors.joining(", ")));
				sb.append("\n");
			}
		}

		if (sendInventory)
		{
			List<ItemInfo> inventory = context.getInventory();
			if (inventory != null && !inventory.isEmpty())
			{
				sb.append("Inv: ");
				sb.append(inventory.stream()
					.map(i -> i.getQuantity() > 1 ? i.getName() + " x" + i.getQuantity() : i.getName())
					.collect(Collectors.joining(", ")));
				sb.append("\n");
			}
		}

		if (sendBank)
		{
			List<ItemInfo> bank = context.getBank();
			if (bank != null && !bank.isEmpty())
			{
				sb.append(String.format("Bank (%d): ", bank.size()));
				sb.append(bank.stream()
					.map(i -> i.getQuantity() > 1 ? i.getName() + " x" + i.getQuantity() : i.getName())
					.collect(Collectors.joining(", ")));
				sb.append("\n");
			}
		}

		if (sendQuests)
		{
			List<QuestInfo> quests = context.getQuests();
			if (quests != null && !quests.isEmpty())
			{
				List<String> completed = quests.stream()
					.filter(q -> "FINISHED".equals(q.getState()))
					.map(QuestInfo::getName)
					.collect(Collectors.toList());
				List<String> inProgress = quests.stream()
					.filter(q -> "IN_PROGRESS".equals(q.getState()))
					.map(QuestInfo::getName)
					.collect(Collectors.toList());

				if (!completed.isEmpty())
				{
					sb.append(String.format("Quests done (%d): %s\n", completed.size(), String.join(", ", completed)));
				}
				if (!inProgress.isEmpty())
				{
					sb.append("Quests active: ").append(String.join(", ", inProgress)).append("\n");
				}
			}
		}

		sb.append("\nUse this context to filter suggestions. Don't list stats back unprompted. If the player asks about their stats, use {skill:Name:Level} tags — NEVER write skill levels as plain text.");

		return sb.toString();
	}

	public static String buildDecompose(PlayerContext context, OsrsAssistantConfig config)
	{
		boolean sendInventory = config == null || config.sendInventory();
		boolean sendBank = config == null || config.sendBank();
		boolean sendStats = config == null || config.sendStats();

		StringBuilder sb = new StringBuilder();
		sb.append(DECOMPOSE_PROMPT);

		if (context != null)
		{
			sb.append("\n\nPLAYER CONTEXT (use to determine what's in inventory/bank):\n");

			if (sendInventory)
			{
				List<ItemInfo> inventory = context.getInventory();
				if (inventory != null && !inventory.isEmpty())
				{
					sb.append("Inventory: ");
					sb.append(inventory.stream()
						.map(i -> i.getQuantity() > 1 ? i.getName() + " x" + i.getQuantity() : i.getName())
						.collect(Collectors.joining(", ")));
					sb.append("\n");
				}
			}

			if (sendBank)
			{
				List<ItemInfo> bank = context.getBank();
				if (bank != null && !bank.isEmpty())
				{
					sb.append("Bank: ");
					sb.append(bank.stream()
						.map(i -> i.getQuantity() > 1 ? i.getName() + " x" + i.getQuantity() : i.getName())
						.collect(Collectors.joining(", ")));
					sb.append("\n");
				}
			}

			if (sendStats)
			{
				List<SkillInfo> skills = context.getSkills();
				if (skills != null && !skills.isEmpty())
				{
					sb.append("Skills: ");
					sb.append(skills.stream()
						.map(s -> s.getName() + " " + s.getLevel())
						.collect(Collectors.joining(", ")));
					sb.append("\n");
				}
			}
		}

		return sb.toString();
	}
}
