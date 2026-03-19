package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses checklist blocks with groups and steps from AI response.
 *
 * Format:
 * {checklist:Title}
 * {group:Group Title|goto:Location}
 * {step:Description|condition|goto:Location}
 * {/group}
 * {/checklist}
 *
 * Standalone {step:} outside a group creates a single-step group (backward compatible).
 */
@Slf4j
public class ChecklistParser
{
	private static final Pattern CHECKLIST_BLOCK = Pattern.compile(
		"\\{checklist:([^}]+)\\}(.*?)\\{/checklist\\}",
		Pattern.DOTALL
	);

	private static final Pattern GROUP_OPEN = Pattern.compile(
		"\\{group:((?:[^{}]|\\{[^}]*\\})*)\\}"
	);

	private static final Pattern GROUP_CLOSE = Pattern.compile(
		"\\{/group\\}"
	);

	private static final Pattern STEP_PATTERN = Pattern.compile(
		"\\{step:((?:[^{}]|\\{[^}]*\\})*)\\}"
	);

	public static Checklist parse(String text)
	{
		Matcher blockMatcher = CHECKLIST_BLOCK.matcher(text);
		if (!blockMatcher.find())
		{
			return null;
		}

		String title = blockMatcher.group(1).trim();
		String body = blockMatcher.group(2);

		Checklist checklist = new Checklist(title);
		parseBody(body, checklist);

		return checklist.getGroups().isEmpty() ? null : checklist;
	}

	private static void parseBody(String body, Checklist checklist)
	{
		int pos = 0;
		ChecklistGroup currentGroup = null;

		while (pos < body.length())
		{
			// Try to match {group:...} at current position
			Matcher groupOpen = GROUP_OPEN.matcher(body);
			Matcher groupClose = GROUP_CLOSE.matcher(body);
			Matcher stepMatch = STEP_PATTERN.matcher(body);

			int nextGroupOpen = findNext(groupOpen, pos);
			int nextGroupClose = findNext(groupClose, pos);
			int nextStep = findNext(stepMatch, pos);

			// Find the earliest match
			int earliest = Integer.MAX_VALUE;
			if (nextGroupOpen >= 0) earliest = Math.min(earliest, nextGroupOpen);
			if (nextGroupClose >= 0) earliest = Math.min(earliest, nextGroupClose);
			if (nextStep >= 0) earliest = Math.min(earliest, nextStep);

			if (earliest == Integer.MAX_VALUE)
			{
				break; // No more tags
			}

			if (nextGroupOpen == earliest)
			{
				// Start a new group
				groupOpen.find(pos);
				String groupContent = groupOpen.group(1);
				currentGroup = parseGroupHeader(groupContent);
				pos = groupOpen.end();
			}
			else if (nextGroupClose == earliest)
			{
				// Close current group
				groupClose.find(pos);
				if (currentGroup != null && !currentGroup.getSubsteps().isEmpty())
				{
					checklist.addGroup(currentGroup);
				}
				currentGroup = null;
				pos = groupClose.end();
			}
			else if (nextStep == earliest)
			{
				// Parse a step
				stepMatch.find(pos);
				ChecklistStep step = parseStep(stepMatch.group(1));
				pos = stepMatch.end();

				if (step == null)
				{
					continue;
				}

				if (currentGroup != null)
				{
					// Step inside a group
					currentGroup.addStep(step);
				}
				else
				{
					// Standalone step — wrap in a single-step group
					ChecklistGroup wrapper = new ChecklistGroup(step.getDescription());
					wrapper.setGotoName(step.getGotoName());
					wrapper.setGotoX(step.getGotoX());
					wrapper.setGotoY(step.getGotoY());
					wrapper.addStep(step);
					checklist.addGroup(wrapper);
				}
			}
		}

		// Close any unclosed group
		if (currentGroup != null && !currentGroup.getSubsteps().isEmpty())
		{
			checklist.addGroup(currentGroup);
		}
	}

	private static int findNext(Matcher matcher, int from)
	{
		if (matcher.find(from))
		{
			return matcher.start();
		}
		return -1;
	}

	private static ChecklistGroup parseGroupHeader(String content)
	{
		// Split by | — first part is title, rest is goto
		String[] parts = content.split("\\|");
		String title = cleanDescription(parts[0]);
		ChecklistGroup group = new ChecklistGroup(title);

		for (int i = 1; i < parts.length; i++)
		{
			String part = parts[i].trim();
			if (part.startsWith("goto:"))
			{
				parseGoto(part, group);
			}
		}

		return group;
	}

	private static ChecklistStep parseStep(String content)
	{
		String[] parts = content.split("\\|");
		if (parts.length < 2)
		{
			return null;
		}

		String description = cleanDescription(parts[0]);
		StepCondition condition = null;
		String gotoName = null;
		int gotoX = -1, gotoY = -1;

		for (int i = 1; i < parts.length; i++)
		{
			String part = parts[i].trim();

			if (part.startsWith("goto:"))
			{
				String gotoStr = part.substring(5);
				String[] gotoParts = gotoStr.split(":");
				gotoName = gotoParts[0].trim();
				if (gotoParts.length >= 3)
				{
					try
					{
						gotoX = Integer.parseInt(gotoParts[1].trim());
						gotoY = Integer.parseInt(gotoParts[2].trim());
					}
					catch (NumberFormatException e)
					{
						// ignore
					}
				}
			}
			else if (condition == null)
			{
				condition = StepCondition.parse(part);
			}
		}

		if (condition == null)
		{
			return null;
		}

		ChecklistStep step = new ChecklistStep(description, condition);
		step.setGotoName(gotoName);
		step.setGotoX(gotoX);
		step.setGotoY(gotoY);

		if (gotoName != null && (gotoX < 0 || gotoY < 0))
		{
			log.warn("Step goto '{}' has no coordinates — backend failed to resolve", gotoName);
		}

		return step;
	}

	private static void parseGoto(String gotoPart, ChecklistGroup group)
	{
		String gotoStr = gotoPart.substring(5);
		String[] parts = gotoStr.split(":");
		String name = parts[0].trim();
		group.setGotoName(name);
		if (parts.length >= 3)
		{
			try
			{
				group.setGotoX(Integer.parseInt(parts[1].trim()));
				group.setGotoY(Integer.parseInt(parts[2].trim()));
			}
			catch (NumberFormatException e)
			{
				log.warn("Group goto '{}' has invalid coordinates: {}", name, gotoPart);
			}
		}
		else
		{
			log.warn("Group goto '{}' has no coordinates — backend failed to resolve", name);
		}
	}

	private static String cleanDescription(String text)
	{
		return text.trim()
			.replaceAll("\\{skill:[^}]*\\}", "")
			.replaceAll("\\{item:[^}]*\\}", "")
			.replaceAll("\\{quest:[^}]*\\}", "")
			.replaceAll("\\{loc:[^}]*\\}", "")
			.replaceAll("\\{goto:[^}]*\\}", "")
			.replaceAll("\\s{2,}", " ")
			.trim();
	}

	public static String stripChecklistTags(String text)
	{
		return CHECKLIST_BLOCK.matcher(text).replaceAll("").trim();
	}
}
