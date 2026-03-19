package com.osrsassistant;

import lombok.Data;

import java.util.List;

@Data
public class PlayerContext
{
	private String playerName;
	private String accountType; // NORMAL, IRONMAN, HARDCORE_IRONMAN, ULTIMATE_IRONMAN, GROUP_IRONMAN
	private int combatLevel;
	private int world;
	private int locationX;
	private int locationY;
	private int locationPlane;
	private int regionId;
	private List<SkillInfo> skills;
	private List<ItemInfo> inventory;
	private List<ItemInfo> equipment;
	private List<ItemInfo> bank;
	private List<QuestInfo> quests;
}
