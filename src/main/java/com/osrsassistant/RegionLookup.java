package com.osrsassistant;

import java.util.HashMap;
import java.util.Map;

public class RegionLookup
{
	private static final Map<Integer, String> REGIONS = new HashMap<>();

	static
	{
		// ---- Lumbridge & Surroundings ----
		REGIONS.put(12850, "Lumbridge");
		REGIONS.put(12851, "Lumbridge");
		REGIONS.put(12849, "Lumbridge");
		REGIONS.put(12593, "Lumbridge");
		REGIONS.put(12594, "Lumbridge Swamp");
		REGIONS.put(12595, "Lumbridge Swamp");
		REGIONS.put(12338, "Lumbridge Swamp");
		REGIONS.put(12596, "Champions' Guild");

		// ---- Varrock ----
		REGIONS.put(12853, "Varrock");
		REGIONS.put(12854, "Varrock");
		REGIONS.put(12597, "Varrock South");
		REGIONS.put(12598, "Grand Exchange");
		REGIONS.put(13110, "Varrock East");
		REGIONS.put(13109, "Varrock");
		REGIONS.put(12855, "Varrock Palace");
		REGIONS.put(13111, "Lumber Yard");
		REGIONS.put(12856, "Wilderness Ditch (Varrock)");

		// ---- Edgeville ----
		REGIONS.put(12342, "Edgeville");
		REGIONS.put(12343, "Edgeville");
		REGIONS.put(12344, "Monastery");

		// ---- Falador ----
		REGIONS.put(11828, "Falador");
		REGIONS.put(11829, "Falador");
		REGIONS.put(12084, "Falador");
		REGIONS.put(12085, "Falador");
		REGIONS.put(11827, "Falador");
		REGIONS.put(12086, "Barbarian Village");

		// ---- Draynor ----
		REGIONS.put(12339, "Draynor Village");
		REGIONS.put(12340, "Draynor Village");
		REGIONS.put(12083, "Draynor Manor");

		// ---- Barbarian Village / Stronghold ----
		REGIONS.put(12341, "Barbarian Village");
		REGIONS.put(7505, "Stronghold of Security (Floor 1)");
		REGIONS.put(7249, "Stronghold of Security (Floor 2)");
		REGIONS.put(6993, "Stronghold of Security (Floor 3)");
		REGIONS.put(6737, "Stronghold of Security (Floor 4)");

		// ---- Al Kharid & Desert ----
		REGIONS.put(13106, "Al Kharid");
		REGIONS.put(13107, "Al Kharid");
		REGIONS.put(13362, "PvP Arena");
		REGIONS.put(13105, "Duel Arena");
		REGIONS.put(13363, "Al Kharid Mine");
		REGIONS.put(12589, "Shantay Pass");
		REGIONS.put(12590, "Desert (North)");
		REGIONS.put(12591, "Desert (North)");
		REGIONS.put(12848, "Desert (Uzer)");
		REGIONS.put(13358, "Pollnivneach");
		REGIONS.put(13359, "Pollnivneach");
		REGIONS.put(13104, "Nardah");
		REGIONS.put(13102, "Sophanem");
		REGIONS.put(13103, "Menaphos Gate");
		REGIONS.put(12844, "Bedabin Camp");
		REGIONS.put(12845, "Desert Mining Camp");
		REGIONS.put(13100, "Bandit Camp (Desert)");
		REGIONS.put(13613, "Smoke Dungeon");

		// ---- Port Sarim & Rimmington ----
		REGIONS.put(12082, "Port Sarim");
		REGIONS.put(11826, "Port Sarim / Rimmington");
		REGIONS.put(11570, "Rimmington");
		REGIONS.put(11569, "Crafting Guild");
		REGIONS.put(11825, "Mudskipper Point");

		// ---- Karamja ----
		REGIONS.put(11566, "Brimhaven");
		REGIONS.put(11567, "Brimhaven");
		REGIONS.put(11310, "Tai Bwo Wannai");
		REGIONS.put(11054, "Shilo Village");
		REGIONS.put(11822, "Musa Point");
		REGIONS.put(11823, "Musa Point");
		REGIONS.put(11311, "Karamja (Central)");
		REGIONS.put(11055, "Karamja (South)");
		REGIONS.put(10798, "Feldip Hills");
		REGIONS.put(10799, "Feldip Hills");

		// ---- Catherby & White Wolf ----
		REGIONS.put(11062, "Catherby");
		REGIONS.put(11061, "White Wolf Mountain");
		REGIONS.put(11317, "White Wolf Mountain");

		// ---- Seers' Village & Camelot ----
		REGIONS.put(10806, "Seers' Village");
		REGIONS.put(10550, "Seers' Village");
		REGIONS.put(11063, "Camelot");
		REGIONS.put(10805, "Hemenster");
		REGIONS.put(10549, "Ranging Guild");

		// ---- Ardougne ----
		REGIONS.put(10547, "East Ardougne");
		REGIONS.put(10548, "East Ardougne");
		REGIONS.put(10291, "West Ardougne");
		REGIONS.put(10292, "West Ardougne");
		REGIONS.put(10290, "Witchaven");
		REGIONS.put(10546, "Ardougne Zoo");

		// ---- Yanille & Gu'Tanoth ----
		REGIONS.put(10288, "Yanille");
		REGIONS.put(10032, "Yanille");
		REGIONS.put(10031, "Gu'Tanoth");
		REGIONS.put(10033, "Castle Wars");

		// ---- Tree Gnome Stronghold ----
		REGIONS.put(9781, "Tree Gnome Stronghold");
		REGIONS.put(9782, "Tree Gnome Stronghold");
		REGIONS.put(9525, "Tree Gnome Village");
		REGIONS.put(9526, "Tree Gnome Village");
		REGIONS.put(9783, "Gnome Agility Course");

		// ---- Barbarian Outpost ----
		REGIONS.put(10039, "Barbarian Outpost");
		REGIONS.put(10040, "Barbarian Outpost");

		// ---- Canifis & Morytania ----
		REGIONS.put(13878, "Canifis");
		REGIONS.put(13879, "Canifis");
		REGIONS.put(14133, "Slayer Tower");
		REGIONS.put(14134, "Slayer Tower");
		REGIONS.put(14389, "Port Phasmatys");
		REGIONS.put(14390, "Port Phasmatys");
		REGIONS.put(14646, "Barrows");
		REGIONS.put(13622, "Mort'ton");
		REGIONS.put(13623, "Mort'ton");
		REGIONS.put(13877, "Mort Myre Swamp");
		REGIONS.put(13876, "Mort Myre Swamp");
		REGIONS.put(14388, "Burgh de Rott");
		REGIONS.put(14387, "Burgh de Rott");
		REGIONS.put(14641, "Meiyerditch");
		REGIONS.put(14642, "Meiyerditch");
		REGIONS.put(14385, "Slepe");
		REGIONS.put(14386, "Slepe");
		REGIONS.put(13621, "Paterdomus");
		REGIONS.put(13365, "Haunted Mine");

		// ---- Wilderness ----
		REGIONS.put(12605, "Wilderness (Level 1-5)");
		REGIONS.put(12606, "Wilderness (Level 1-5)");
		REGIONS.put(12861, "Wilderness (Level 5-10)");
		REGIONS.put(12862, "Wilderness (Level 5-10)");
		REGIONS.put(13117, "Wilderness (Level 10-20)");
		REGIONS.put(13118, "Wilderness (Level 10-20)");
		REGIONS.put(13373, "Wilderness (Level 20-30)");
		REGIONS.put(13374, "Wilderness (Level 20-30)");
		REGIONS.put(13629, "Wilderness (Level 30-40)");
		REGIONS.put(13630, "Wilderness (Level 30-40)");
		REGIONS.put(13885, "Wilderness (Level 40-50)");
		REGIONS.put(13886, "Wilderness (Level 40-50)");
		REGIONS.put(12857, "Edgeville Dungeon");
		REGIONS.put(12601, "Wilderness (Lava Maze)");
		REGIONS.put(12602, "Wilderness (Lava Maze)");
		REGIONS.put(12603, "Wilderness (Mage Arena)");
		REGIONS.put(12859, "Wilderness (Graveyard)");
		REGIONS.put(12858, "Wilderness (Dark Warriors)");
		REGIONS.put(13114, "Wilderness (Bandit Camp)");
		REGIONS.put(13115, "Wilderness (Chaos Temple)");
		REGIONS.put(13370, "Wilderness (Demonic Ruins)");
		REGIONS.put(13371, "Wilderness (Rogues' Castle)");
		REGIONS.put(12856, "Wilderness Ditch");
		REGIONS.put(12600, "Ferox Enclave");
		REGIONS.put(12190, "Revenant Caves");
		REGIONS.put(12191, "Revenant Caves");
		REGIONS.put(12446, "Revenant Caves");
		REGIONS.put(12447, "Revenant Caves");

		// ---- Zanaris ----
		REGIONS.put(9285, "Zanaris");
		REGIONS.put(9541, "Zanaris");
		REGIONS.put(9797, "Zanaris");

		// ---- Tutorial Island ----
		REGIONS.put(12336, "Tutorial Island");
		REGIONS.put(12335, "Tutorial Island");

		// ---- Pest Control ----
		REGIONS.put(10536, "Pest Control");
		REGIONS.put(10537, "Pest Control");

		// ---- Castle Wars ----
		REGIONS.put(9776, "Castle Wars");
		REGIONS.put(9520, "Castle Wars (Underground)");

		// ---- Kourend ----
		REGIONS.put(6457, "Hosidius");
		REGIONS.put(6713, "Hosidius");
		REGIONS.put(6714, "Hosidius");
		REGIONS.put(6970, "Hosidius");
		REGIONS.put(6969, "Hosidius");
		REGIONS.put(6458, "Lovakengj");
		REGIONS.put(5941, "Lovakengj");
		REGIONS.put(5942, "Lovakengj");
		REGIONS.put(6198, "Lovakengj");
		REGIONS.put(6201, "Shayzien");
		REGIONS.put(6202, "Shayzien");
		REGIONS.put(5945, "Shayzien");
		REGIONS.put(6461, "Arceuus");
		REGIONS.put(6717, "Arceuus");
		REGIONS.put(6718, "Arceuus");
		REGIONS.put(6205, "Piscarilius");
		REGIONS.put(6462, "Piscarilius");
		REGIONS.put(6971, "Kourend Castle");
		REGIONS.put(6972, "Kourend Castle");
		REGIONS.put(6455, "Forthos Dungeon");
		REGIONS.put(7223, "Catacombs of Kourend");
		REGIONS.put(7479, "Catacombs of Kourend");
		REGIONS.put(6456, "Tithe Farm");
		REGIONS.put(6712, "Woodcutting Guild");
		REGIONS.put(6711, "Woodcutting Guild");

		// ---- Farming Guild ----
		REGIONS.put(4922, "Farming Guild");
		REGIONS.put(5178, "Farming Guild");

		// ---- Fossil Island ----
		REGIONS.put(14908, "Fossil Island");
		REGIONS.put(14652, "Fossil Island");
		REGIONS.put(14651, "Fossil Island");
		REGIONS.put(14907, "Fossil Island");
		REGIONS.put(14909, "Fossil Island (Verdant Valley)");
		REGIONS.put(14653, "Fossil Island (Mushroom Meadow)");

		// ---- Raids ----
		REGIONS.put(12889, "Chambers of Xeric");
		REGIONS.put(13136, "Theatre of Blood");
		REGIONS.put(13137, "Theatre of Blood");
		REGIONS.put(14160, "Tombs of Amascut");
		REGIONS.put(14162, "Tombs of Amascut");

		// ---- Prifddinas ----
		REGIONS.put(12894, "Prifddinas");
		REGIONS.put(13150, "Prifddinas");
		REGIONS.put(12895, "Prifddinas");
		REGIONS.put(13151, "Prifddinas");
		REGIONS.put(12638, "Prifddinas");
		REGIONS.put(12639, "Prifddinas");

		// ---- Miscellania & Etceteria ----
		REGIONS.put(10044, "Miscellania");
		REGIONS.put(10300, "Miscellania");
		REGIONS.put(10043, "Etceteria");

		// ---- Rellekka & Fremennik ----
		REGIONS.put(10553, "Rellekka");
		REGIONS.put(10297, "Rellekka");
		REGIONS.put(10042, "Waterbirth Island");
		REGIONS.put(9786, "Waterbirth Island");
		REGIONS.put(10554, "Lighthouse");
		REGIONS.put(10041, "Jatizso");
		REGIONS.put(9785, "Neitiznot");
		REGIONS.put(9529, "Neitiznot");
		REGIONS.put(8253, "Lunar Isle");
		REGIONS.put(8509, "Lunar Isle");

		// ---- Taverley & Burthorpe ----
		REGIONS.put(11574, "Taverley");
		REGIONS.put(11573, "Taverley");
		REGIONS.put(11318, "Burthorpe");
		REGIONS.put(11319, "Burthorpe");
		REGIONS.put(11575, "Heroes' Guild");

		// ---- Dungeons ----
		REGIONS.put(14679, "Motherlode Mine");
		REGIONS.put(14935, "Motherlode Mine");
		REGIONS.put(11416, "Taverley Dungeon");
		REGIONS.put(11417, "Taverley Dungeon");
		REGIONS.put(11672, "Taverley Dungeon");
		REGIONS.put(11673, "Taverley Dungeon");
		REGIONS.put(12337, "Wizards' Tower");
		REGIONS.put(12693, "Mining Guild");
		REGIONS.put(12949, "Mining Guild");
		REGIONS.put(12954, "Dorgesh-Kaan");
		REGIONS.put(12698, "Dorgesh-Kaan");
		REGIONS.put(11164, "Asgarnian Ice Dungeon");
		REGIONS.put(11165, "Asgarnian Ice Dungeon");
		REGIONS.put(12441, "Varrock Sewers");
		REGIONS.put(12442, "Varrock Sewers");
		REGIONS.put(12185, "Draynor Sewers");
		REGIONS.put(11929, "Edgeville Dungeon");
		REGIONS.put(11930, "Edgeville Dungeon");
		REGIONS.put(10900, "Brimhaven Dungeon");
		REGIONS.put(10901, "Brimhaven Dungeon");
		REGIONS.put(11156, "Brimhaven Dungeon");
		REGIONS.put(11157, "Brimhaven Dungeon");
		REGIONS.put(10388, "Waterbirth Dungeon");
		REGIONS.put(10132, "Waterbirth Dungeon");
		REGIONS.put(9876, "Waterbirth Dungeon (DKS)");
		REGIONS.put(7492, "Blast Furnace");
		REGIONS.put(7748, "Blast Furnace");

		// ---- Fight Caves / Inferno / TzHaar ----
		REGIONS.put(9551, "TzHaar City");
		REGIONS.put(9552, "TzHaar City");
		REGIONS.put(9043, "Fight Caves");
		REGIONS.put(9044, "Fight Caves");
		REGIONS.put(9300, "Inferno");

		// ---- God Wars Dungeon ----
		REGIONS.put(11601, "God Wars Dungeon");
		REGIONS.put(11602, "God Wars Dungeon");
		REGIONS.put(11346, "God Wars Dungeon");
		REGIONS.put(11347, "God Wars Dungeon");

		// ---- Bosses ----
		REGIONS.put(9033, "Zulrah");
		REGIONS.put(9034, "Zulrah");
		REGIONS.put(9023, "Vorkath");
		REGIONS.put(12611, "Corporeal Beast");
		REGIONS.put(12612, "Corporeal Beast");
		REGIONS.put(14231, "Nightmare");
		REGIONS.put(14487, "Nightmare");
		REGIONS.put(11589, "Nex");
		REGIONS.put(11330, "Kalphite Queen");
		REGIONS.put(11331, "Kalphite Queen");
		REGIONS.put(11588, "Kalphite Lair");
		REGIONS.put(11076, "Kraken");
		REGIONS.put(9363, "Cerberus");
		REGIONS.put(6727, "Sarachnis");
		REGIONS.put(5862, "Alchemical Hydra");
		REGIONS.put(6483, "Skotizo");
		REGIONS.put(5536, "Zalcano");
		REGIONS.put(8547, "Phantom Muspah");
		REGIONS.put(6998, "Vardorvis");
		REGIONS.put(6997, "Duke Sucellus");
		REGIONS.put(6996, "The Leviathan");
		REGIONS.put(6995, "The Whisperer");

		// ---- Tempoross ----
		REGIONS.put(12078, "Tempoross Cove");
		REGIONS.put(12079, "Tempoross Cove");

		// ---- Guardians of the Rift ----
		REGIONS.put(14484, "Temple of the Eye");

		// ---- Wintertodt ----
		REGIONS.put(6462, "Wintertodt");
		REGIONS.put(6461, "Wintertodt Camp");

		// ---- Agility Courses ----
		REGIONS.put(9781, "Gnome Agility Course");
		REGIONS.put(13358, "Pollnivneach Rooftops");
		REGIONS.put(10553, "Rellekka Rooftops");
		REGIONS.put(10547, "Ardougne Rooftops");
		REGIONS.put(9525, "Ape Atoll Agility");
		REGIONS.put(10388, "Hallowed Sepulchre");

		// ---- Islands ----
		REGIONS.put(11057, "Crandor");
		REGIONS.put(10793, "Entrana");
		REGIONS.put(10794, "Entrana");
		REGIONS.put(10537, "Void Knights' Outpost");
		REGIONS.put(11059, "Corsair Cove");
		REGIONS.put(10803, "Corsair Cove");
		REGIONS.put(10284, "Ape Atoll");
		REGIONS.put(10796, "Ape Atoll");
		REGIONS.put(10795, "Ape Atoll");
		REGIONS.put(11050, "Harmony Island");
		REGIONS.put(15148, "Fossil Island (Barge)");
		REGIONS.put(15008, "Fossil Island (Camp)");

		// ---- Kebos Lowlands ----
		REGIONS.put(5179, "Mount Quidamortem");
		REGIONS.put(4923, "Kebos Lowlands");
		REGIONS.put(4924, "Kebos Swamp");
		REGIONS.put(5180, "Lizardman Canyon");

		// ---- Morytania (extended) ----
		REGIONS.put(14643, "Ver Sinhaza");
		REGIONS.put(14644, "Darkmeyer");
		REGIONS.put(14645, "Darkmeyer");

		// ---- Miscellaneous ----
		REGIONS.put(10034, "Fight Arena");
		REGIONS.put(10289, "Nightmare Zone");
		REGIONS.put(10035, "Nightmare Zone");
		REGIONS.put(11571, "Makeover Mage");
		REGIONS.put(9264, "Soul Wars");
		REGIONS.put(9265, "Soul Wars");
		REGIONS.put(8748, "Gauntlet");
		REGIONS.put(8749, "Gauntlet");
		REGIONS.put(12080, "Fishing Trawler");
		REGIONS.put(10804, "Fishing Guild");
		REGIONS.put(10292, "Legend's Guild");
		REGIONS.put(10294, "Observatory");
		REGIONS.put(10551, "Sinclair Mansion");
		REGIONS.put(9263, "Warriors' Guild");
		REGIONS.put(11319, "Rogues' Den");
	}

	public static String getLocationName(int regionId)
	{
		return REGIONS.get(regionId);
	}
}
