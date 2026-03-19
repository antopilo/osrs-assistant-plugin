package com.osrsassistant;

import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all known location coordinates.
 * Consolidates data from ResponseEnricher, StepResolver, and StepCondition.
 */
public class LocationDatabase
{
	private static final Map<String, int[]> LOCATIONS = new HashMap<>();

	static
	{
		// === Major Cities ===
		LOCATIONS.put("lumbridge", new int[]{3222, 3218});
		LOCATIONS.put("lumbridge castle", new int[]{3222, 3218});
		LOCATIONS.put("varrock", new int[]{3213, 3428});
		LOCATIONS.put("varrock palace", new int[]{3212, 3475});
		LOCATIONS.put("falador", new int[]{2965, 3380});
		LOCATIONS.put("falador park", new int[]{2994, 3383});
		LOCATIONS.put("edgeville", new int[]{3094, 3500});
		LOCATIONS.put("draynor village", new int[]{3093, 3244});
		LOCATIONS.put("draynor manor", new int[]{3108, 3349});
		LOCATIONS.put("al kharid", new int[]{3293, 3174});
		LOCATIONS.put("ardougne", new int[]{2662, 3305});
		LOCATIONS.put("east ardougne", new int[]{2662, 3305});
		LOCATIONS.put("west ardougne", new int[]{2528, 3305});
		LOCATIONS.put("camelot", new int[]{2757, 3477});
		LOCATIONS.put("seers' village", new int[]{2725, 3485});
		LOCATIONS.put("catherby", new int[]{2813, 3447});
		LOCATIONS.put("yanille", new int[]{2544, 3089});
		LOCATIONS.put("canifis", new int[]{3496, 3488});
		LOCATIONS.put("rellekka", new int[]{2660, 3657});
		LOCATIONS.put("prifddinas", new int[]{3239, 6075});
		LOCATIONS.put("burthorpe", new int[]{2889, 3528});
		LOCATIONS.put("taverley", new int[]{2895, 3454});

		// === Towns & Villages ===
		LOCATIONS.put("port sarim", new int[]{3023, 3208});
		LOCATIONS.put("rimmington", new int[]{2957, 3214});
		LOCATIONS.put("barbarian village", new int[]{3082, 3421});
		LOCATIONS.put("tree gnome stronghold", new int[]{2461, 3444});
		LOCATIONS.put("tree gnome village", new int[]{2461, 3444});
		LOCATIONS.put("port phasmatys", new int[]{3688, 3502});
		LOCATIONS.put("burgh de rott", new int[]{3491, 3233});
		LOCATIONS.put("shilo village", new int[]{2852, 2955});
		LOCATIONS.put("tai bwo wannai", new int[]{2795, 3065});
		LOCATIONS.put("brimhaven", new int[]{2771, 3178});
		LOCATIONS.put("musa point", new int[]{2913, 3169});
		LOCATIONS.put("nardah", new int[]{3422, 2917});
		LOCATIONS.put("pollnivneach", new int[]{3360, 2981});
		LOCATIONS.put("sophanem", new int[]{3315, 2783});
		LOCATIONS.put("miscellania", new int[]{2538, 3860});
		LOCATIONS.put("jatizso", new int[]{2407, 3813});
		LOCATIONS.put("neitiznot", new int[]{2336, 3805});
		LOCATIONS.put("lunar isle", new int[]{2109, 3914});
		LOCATIONS.put("zanaris", new int[]{2412, 4434});
		LOCATIONS.put("mort'ton", new int[]{3489, 3288});
		LOCATIONS.put("slepe", new int[]{3734, 3350});

		// === Kourend ===
		LOCATIONS.put("kourend", new int[]{1643, 3673});
		LOCATIONS.put("great kourend", new int[]{1643, 3673});
		LOCATIONS.put("hosidius", new int[]{1744, 3517});
		LOCATIONS.put("lovakengj", new int[]{1504, 3840});
		LOCATIONS.put("shayzien", new int[]{1490, 3630});
		LOCATIONS.put("arceuus", new int[]{1690, 3745});
		LOCATIONS.put("piscarilius", new int[]{1803, 3745});
		LOCATIONS.put("port piscarilius", new int[]{1803, 3745});
		LOCATIONS.put("farming guild", new int[]{1248, 3726});

		// === Islands ===
		LOCATIONS.put("fossil island", new int[]{3764, 3869});
		LOCATIONS.put("fossil island camp", new int[]{3764, 3869});
		LOCATIONS.put("crandor", new int[]{2851, 3238});
		LOCATIONS.put("waterbirth island", new int[]{2527, 3740});
		LOCATIONS.put("ape atoll", new int[]{2755, 2785});
		LOCATIONS.put("entrana", new int[]{2834, 3335});
		LOCATIONS.put("karamja", new int[]{2913, 3169});

		// === Dungeons & Instances ===
		LOCATIONS.put("god wars dungeon", new int[]{2882, 5310});
		LOCATIONS.put("tzhaar city", new int[]{2444, 5178});
		LOCATIONS.put("fight caves", new int[]{2444, 5178});
		LOCATIONS.put("inferno", new int[]{2444, 5178});
		LOCATIONS.put("motherlode mine", new int[]{3748, 5653});
		LOCATIONS.put("motherload mine", new int[]{3748, 5653});
		LOCATIONS.put("slayer tower", new int[]{3429, 3534});
		LOCATIONS.put("stronghold of security", new int[]{1859, 5243});
		LOCATIONS.put("catacombs of kourend", new int[]{1664, 10050});
		LOCATIONS.put("barrows", new int[]{3565, 3316});

		// === Key Locations ===
		LOCATIONS.put("grand exchange", new int[]{3165, 3487});
		LOCATIONS.put("duel arena", new int[]{3366, 3265});
		LOCATIONS.put("pvp arena", new int[]{3366, 3265});
		LOCATIONS.put("castle wars", new int[]{2440, 3090});
		LOCATIONS.put("pest control", new int[]{2658, 2676});
		LOCATIONS.put("warriors' guild", new int[]{2843, 3543});
		LOCATIONS.put("ferox enclave", new int[]{3131, 3633});
		LOCATIONS.put("crafting guild", new int[]{2933, 3289});
		LOCATIONS.put("cooking guild", new int[]{3143, 3444});
		LOCATIONS.put("mining guild", new int[]{3046, 9756});
		LOCATIONS.put("champions' guild", new int[]{3189, 3358});
		LOCATIONS.put("legends' guild", new int[]{2728, 3348});
		LOCATIONS.put("wizards' tower", new int[]{3109, 3167});
		LOCATIONS.put("ranging guild", new int[]{2658, 3440});
		LOCATIONS.put("fishing guild", new int[]{2604, 3414});
		LOCATIONS.put("woodcutting guild", new int[]{1591, 3481});

		// === Raids ===
		LOCATIONS.put("chambers of xeric", new int[]{1233, 3558});
		LOCATIONS.put("theatre of blood", new int[]{3680, 3219});
		LOCATIONS.put("tombs of amascut", new int[]{3370, 2748});

		// === Bosses ===
		LOCATIONS.put("corporeal beast", new int[]{2966, 4382});
		LOCATIONS.put("dagannoth kings", new int[]{2442, 10147});
		LOCATIONS.put("kalphite queen", new int[]{3226, 3108});
		LOCATIONS.put("zulrah", new int[]{2204, 3056});
		LOCATIONS.put("vorkath", new int[]{2273, 4049});
		LOCATIONS.put("nightmare", new int[]{3808, 9758});
		LOCATIONS.put("nex", new int[]{2910, 5203});
		LOCATIONS.put("phantom muspah", new int[]{2881, 6374});

		// === Altars & Temples ===
		LOCATIONS.put("chaos temple", new int[]{3236, 3635});
		LOCATIONS.put("nature altar", new int[]{2400, 4841});
		LOCATIONS.put("law altar", new int[]{2464, 4818});
		LOCATIONS.put("ourania altar", new int[]{3060, 5578});

		// === Wilderness ===
		LOCATIONS.put("wilderness", new int[]{3200, 3600});
		LOCATIONS.put("edgeville dungeon", new int[]{3132, 9917});
		LOCATIONS.put("wilderness agility course", new int[]{2998, 3917});
		LOCATIONS.put("mage arena", new int[]{3105, 3951});
		LOCATIONS.put("revenant caves", new int[]{3196, 10056});

		// === Misc ===
		LOCATIONS.put("tempoross cove", new int[]{3135, 2840});
		LOCATIONS.put("guardians of the rift", new int[]{2447, 5824});
		LOCATIONS.put("temple of the eye", new int[]{2447, 5824});
		LOCATIONS.put("wintertodt", new int[]{1630, 3982});
		LOCATIONS.put("blast furnace", new int[]{1940, 4960});
		LOCATIONS.put("hallowed sepulchre", new int[]{2393, 5970});
		LOCATIONS.put("nightmare zone", new int[]{2611, 3116});

		// === Transport ===
		LOCATIONS.put("verdant valley", new int[]{3680, 3810});
		LOCATIONS.put("mushroom meadow", new int[]{3764, 3869});
		LOCATIONS.put("fairy ring", new int[]{3128, 3496});
		LOCATIONS.put("spirit tree", new int[]{2461, 3444});
		LOCATIONS.put("ectofuntus", new int[]{3659, 3522});
		LOCATIONS.put("digsite", new int[]{3360, 3416});
		LOCATIONS.put("digsite barge", new int[]{3362, 3445});
		LOCATIONS.put("barge", new int[]{3362, 3445});
		LOCATIONS.put("tar swamp", new int[]{3681, 3815});
		LOCATIONS.put("port sarim docks", new int[]{3041, 3202});
		LOCATIONS.put("ardougne docks", new int[]{2683, 3275});
		LOCATIONS.put("rellekka docks", new int[]{2621, 3685});
		LOCATIONS.put("gnome glider", new int[]{2465, 3501});

		// === Agility Courses ===
		LOCATIONS.put("gnome agility course", new int[]{2474, 3436});
		LOCATIONS.put("draynor agility course", new int[]{3104, 3279});
		LOCATIONS.put("varrock agility course", new int[]{3222, 3414});
		LOCATIONS.put("canifis agility course", new int[]{3505, 3489});
		LOCATIONS.put("falador agility course", new int[]{3036, 3342});
		LOCATIONS.put("seers' agility course", new int[]{2729, 3488});
		LOCATIONS.put("ardougne agility course", new int[]{2674, 3298});
		LOCATIONS.put("prifddinas agility course", new int[]{3253, 6109});

		// === Training Spots ===
		LOCATIONS.put("sand crabs", new int[]{1726, 3463});
		LOCATIONS.put("rock crabs", new int[]{2673, 3711});
		LOCATIONS.put("ammonite crabs", new int[]{3764, 3877});

		// === Banks ===
		LOCATIONS.put("bank", new int[]{3094, 3492});
		LOCATIONS.put("bank booth", new int[]{3094, 3492});
		LOCATIONS.put("bank chest", new int[]{3094, 3492});
		LOCATIONS.put("banker", new int[]{3094, 3492});
		LOCATIONS.put("lumbridge bank", new int[]{3208, 3220});
		LOCATIONS.put("varrock west bank", new int[]{3185, 3436});
		LOCATIONS.put("varrock east bank", new int[]{3253, 3420});
		LOCATIONS.put("falador west bank", new int[]{2946, 3368});
		LOCATIONS.put("falador east bank", new int[]{3013, 3355});
		LOCATIONS.put("edgeville bank", new int[]{3094, 3492});
		LOCATIONS.put("draynor bank", new int[]{3092, 3245});
		LOCATIONS.put("al kharid bank", new int[]{3269, 3167});
		LOCATIONS.put("catherby bank", new int[]{2808, 3441});
		LOCATIONS.put("seers' bank", new int[]{2725, 3493});
		LOCATIONS.put("ardougne bank", new int[]{2655, 3283});
		LOCATIONS.put("canifis bank", new int[]{3512, 3480});
		LOCATIONS.put("hosidius bank", new int[]{1749, 3599});

		// === Farming Patches ===
		LOCATIONS.put("falador farming patch", new int[]{3054, 3311});
		LOCATIONS.put("ardougne farming patch", new int[]{2670, 3374});
		LOCATIONS.put("catherby farming patch", new int[]{2813, 3463});
		LOCATIONS.put("hosidius farming patch", new int[]{1738, 3550});
		LOCATIONS.put("farming guild patch", new int[]{1248, 3726});
	}

	/**
	 * Look up coordinates by location name.
	 * Tries exact match first, then partial match (case-insensitive).
	 * Returns int[]{x, y} or null if not found.
	 */
	public static int[] getCoordinates(String name)
	{
		if (name == null || name.isEmpty())
		{
			return null;
		}

		String lower = name.toLowerCase().trim();

		// Exact match
		int[] coords = LOCATIONS.get(lower);
		if (coords != null)
		{
			return coords;
		}

		// Partial match
		for (Map.Entry<String, int[]> entry : LOCATIONS.entrySet())
		{
			if (lower.contains(entry.getKey()) || entry.getKey().contains(lower))
			{
				return entry.getValue();
			}
		}

		return null;
	}

	/**
	 * Look up a WorldPoint by location name.
	 * Tries exact match first, then partial match (case-insensitive).
	 * Returns WorldPoint or null if not found.
	 */
	public static WorldPoint getWorldPoint(String name)
	{
		int[] coords = getCoordinates(name);
		if (coords != null)
		{
			return new WorldPoint(coords[0], coords[1], 0);
		}
		return null;
	}
}
