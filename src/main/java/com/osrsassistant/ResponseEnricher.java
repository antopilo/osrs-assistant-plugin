package com.osrsassistant;

import java.util.regex.Pattern;

/**
 * Enriches LLM response tags with coordinates.
 * Runs client-side so standalone mode doesn't need the backend for tag resolution.
 * {loc:} tags are converted to {goto:} tags for unified handling.
 */
public class ResponseEnricher
{
	public static String enrich(String text)
	{
		// Convert any remaining {loc:Name} to {goto:Name} for unified handling
		text = text.replace("{loc:", "{goto:");

		// {goto:Name} -> {goto:Name:X:Y} (but NOT {goto:object:...} or {goto:npc:...})
		text = Pattern.compile("\\{goto:(?!object:|npc:)([^}:]+)\\}").matcher(text).replaceAll(match ->
		{
			String name = match.group(1);
			int[] coords = LocationDatabase.getCoordinates(name);
			if (coords != null)
			{
				return "{goto:" + name + ":" + coords[0] + ":" + coords[1] + "}";
			}
			return match.group();
		});

		// Checklist step gotos: |goto:Name} -> |goto:Name:X:Y}
		text = Pattern.compile("(?<=\\|)goto:([^}|:]+)(?=\\s*\\})").matcher(text).replaceAll(match ->
		{
			String name = match.group(1).trim();
			int[] coords = LocationDatabase.getCoordinates(name);
			if (coords != null)
			{
				return "goto:" + name + ":" + coords[0] + ":" + coords[1];
			}
			return match.group();
		});

		return text;
	}
}
