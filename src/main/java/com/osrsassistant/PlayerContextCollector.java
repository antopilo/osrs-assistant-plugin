package com.osrsassistant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PlayerContextCollector
{
	private final Client client;
	private final ItemManager itemManager;
	private volatile List<ItemInfo> cachedBank = new ArrayList<>();
	private final Gson gson = new Gson();
	private File bankCacheFile;

	public PlayerContextCollector(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		loadBankCache();
	}

	private File getBankCacheFile()
	{
		if (bankCacheFile == null)
		{
			String home = System.getProperty("user.home");
			File dir = new File(home, ".runelite/osrs-assistant");
			dir.mkdirs();
			bankCacheFile = new File(dir, "bank_cache.json");
		}
		return bankCacheFile;
	}

	private void loadBankCache()
	{
		try
		{
			File file = getBankCacheFile();
			if (file.exists())
			{
				String json = Files.readString(file.toPath());
				List<ItemInfo> loaded = gson.fromJson(json, new TypeToken<List<ItemInfo>>(){}.getType());
				if (loaded != null && !loaded.isEmpty())
				{
					cachedBank = loaded;
					log.info("Loaded {} bank items from cache", cachedBank.size());
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Failed to load bank cache: {}", e.getMessage());
		}
	}

	private void saveBankCache()
	{
		try
		{
			String json = gson.toJson(cachedBank);
			Files.writeString(getBankCacheFile().toPath(), json);
			log.debug("Saved {} bank items to cache", cachedBank.size());
		}
		catch (IOException e)
		{
			log.debug("Failed to save bank cache: {}", e.getMessage());
		}
	}

	public PlayerContext collect()
	{
		PlayerContext ctx = new PlayerContext();

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return ctx;
		}

		ctx.setPlayerName(localPlayer.getName());
		ctx.setAccountType(client.getAccountType().name());
		ctx.setCombatLevel(localPlayer.getCombatLevel());
		ctx.setWorld(client.getWorld());

		if (localPlayer.getWorldLocation() != null)
		{
			ctx.setLocationX(localPlayer.getWorldLocation().getX());
			ctx.setLocationY(localPlayer.getWorldLocation().getY());
			ctx.setLocationPlane(localPlayer.getWorldLocation().getPlane());
			ctx.setRegionId(localPlayer.getWorldLocation().getRegionID());
		}

		ctx.setSkills(collectSkills());
		ctx.setInventory(collectItems(InventoryID.INVENTORY));
		ctx.setEquipment(collectItems(InventoryID.EQUIPMENT));
		ctx.setQuests(collectQuests());

		// Snapshot bank when open, otherwise use disk-cached version
		List<ItemInfo> bankItems = collectItems(InventoryID.BANK);
		if (!bankItems.isEmpty())
		{
			cachedBank = bankItems;
			saveBankCache();
		}
		ctx.setBank(cachedBank);

		return ctx;
	}

	private List<SkillInfo> collectSkills()
	{
		List<SkillInfo> skills = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}
			skills.add(new SkillInfo(
				skill.getName(),
				client.getRealSkillLevel(skill),
				client.getBoostedSkillLevel(skill)
			));
		}
		return skills;
	}

	private List<QuestInfo> collectQuests()
	{
		List<QuestInfo> quests = new ArrayList<>();
		for (Quest quest : Quest.values())
		{
			QuestState state = quest.getState(client);
			if (state == QuestState.FINISHED || state == QuestState.IN_PROGRESS)
			{
				quests.add(new QuestInfo(quest.getName(), state.name()));
			}
		}
		return quests;
	}

	private List<ItemInfo> collectItems(InventoryID inventoryID)
	{
		List<ItemInfo> items = new ArrayList<>();
		ItemContainer container = client.getItemContainer(inventoryID);
		if (container == null)
		{
			return items;
		}

		for (Item item : container.getItems())
		{
			if (item.getId() == -1 || item.getId() == 6512)
			{
				continue;
			}
			String name = itemManager.getItemComposition(item.getId()).getName();
			items.add(new ItemInfo(name, item.getQuantity()));
		}
		return items;
	}
}
