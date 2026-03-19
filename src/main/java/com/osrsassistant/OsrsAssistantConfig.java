package com.osrsassistant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("osrsassistant")
public interface OsrsAssistantConfig extends Config
{
	@ConfigSection(
		name = "AI Provider",
		description = "Configure which AI service to use",
		position = 0
	)
	String aiSection = "aiSection";

	@ConfigItem(
		keyName = "llmProvider",
		name = "Provider",
		description = "OpenAI/Anthropic connect directly using your API key. Custom Backend uses your own server.",
		position = 0,
		section = aiSection
	)
	default LlmProvider llmProvider()
	{
		return LlmProvider.CUSTOM_BACKEND;
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API Key",
		description = "Your API key for the selected provider.",
		secret = true,
		position = 1,
		section = aiSection
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "llmModelId",
		name = "Model",
		description = "Model ID to use. Leave empty for default. OpenAI: gpt-4o, gpt-4o-mini, o4-mini. Anthropic: claude-sonnet-4-20250514, claude-haiku-4-5-20251001.",
		position = 2,
		section = aiSection
	)
	default String llmModelId()
	{
		return "";
	}

	@ConfigItem(
		keyName = "customPrompt",
		name = "Custom Instructions",
		description = "Extra instructions for the AI (e.g. 'I'm an ironman', 'respond in Spanish', 'keep answers short').",
		position = 3,
		section = aiSection
	)
	default String customPrompt()
	{
		return "";
	}

	// ---- Player Context ----

	@ConfigSection(
		name = "Player Context",
		description = "Control what player information is sent to the AI. Disabling these gives the AI less context but improves privacy.",
		position = 1
	)
	String contextSection = "contextSection";

	@ConfigItem(
		keyName = "sendStats",
		name = "Send Skills",
		description = "Include your skill levels in AI requests.",
		position = 0,
		section = contextSection
	)
	default boolean sendStats()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendInventory",
		name = "Send Inventory",
		description = "Include your inventory contents in AI requests.",
		position = 1,
		section = contextSection
	)
	default boolean sendInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendEquipment",
		name = "Send Equipment",
		description = "Include your worn equipment in AI requests.",
		position = 2,
		section = contextSection
	)
	default boolean sendEquipment()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendBank",
		name = "Send Bank",
		description = "Include your bank contents in AI requests.",
		position = 3,
		section = contextSection
	)
	default boolean sendBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendQuests",
		name = "Send Quests",
		description = "Include your quest progress in AI requests.",
		position = 4,
		section = contextSection
	)
	default boolean sendQuests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendLocation",
		name = "Send Location",
		description = "Include your current world location in AI requests.",
		position = 5,
		section = contextSection
	)
	default boolean sendLocation()
	{
		return true;
	}

	// ---- Custom Backend ----

	@ConfigSection(
		name = "Custom Backend",
		description = "Settings for Custom Backend provider only",
		position = 2,
		closedByDefault = true
	)
	String backendSection = "backendSection";

	@ConfigItem(
		keyName = "backendUrl",
		name = "Backend URL",
		description = "URL of your Custom Backend server.",
		position = 0,
		section = backendSection
	)
	default String backendUrl()
	{
		return "http://localhost:5000";
	}

}
