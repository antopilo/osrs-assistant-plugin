package com.osrsassistant;

/**
 * Shared text-processing utilities used by multiple classes.
 */
public final class TextUtils
{
	private static final String[] PREFIXES = {
		"check the ", "check ", "empty the ", "empty ", "fill the ", "fill ",
		"use the ", "use ", "talk to ", "speak to ", "trade ", "open the ", "open ",
		"climb the ", "climb ", "enter the ", "enter ", "mine the ", "mine ",
		"chop the ", "chop ", "fish at ", "fish the ", "fish ",
		"pick the ", "pick ", "search the ", "search ",
		"pray at ", "pray at the ", "light the ", "light ",
		"cook at ", "cook on ", "smelt at ", "smith at ",
		"bank at ", "go to ", "visit ",
		"take the ", "take ", "board the ", "board ", "sail the ", "sail ",
		"ride the ", "ride ", "travel to ", "travel via ",
		"squeeze through ", "cross the ", "cross ", "jump over ", "jump ",
		"operate the ", "operate ", "teleport to ", "activate the ", "activate ",
	};

	private static final String[] STOPWORDS = {
		" to ", " from ", " at ", " in ", " on ", " into ", " onto ",
		" towards ", " through ", " across ", " near ", " for ", " with "
	};

	private TextUtils() {}

	/**
	 * Extract a likely object/NPC name from a step description.
	 * Looks for common patterns like "Check the birdhouse", "Talk to Hans", "Use furnace".
	 *
	 * @param desc the step description text
	 * @return the extracted target name, or {@code null} if none found
	 */
	public static String extractTarget(String desc)
	{
		if (desc == null) return null;

		String descLower = desc.toLowerCase();
		for (String prefix : PREFIXES)
		{
			int idx = descLower.indexOf(prefix);
			if (idx >= 0)
			{
				String remainder = desc.substring(idx + prefix.length()).trim();
				int end = remainder.length();

				// Stop at punctuation
				for (int i = 0; i < remainder.length(); i++)
				{
					char c = remainder.charAt(i);
					if (c == ',' || c == '.' || c == '|' || c == '{' || c == '}')
					{
						end = i;
						break;
					}
				}

				// Stop at prepositions
				String chunk = remainder.substring(0, end);
				String chunkLower = chunk.toLowerCase();
				for (String stop : STOPWORDS)
				{
					int stopIdx = chunkLower.indexOf(stop);
					if (stopIdx > 0)
					{
						end = Math.min(end, stopIdx);
						break;
					}
				}

				String target = remainder.substring(0, end).trim();
				if (!target.isEmpty() && target.length() >= 3)
				{
					return target;
				}
			}
		}
		return null;
	}
}
