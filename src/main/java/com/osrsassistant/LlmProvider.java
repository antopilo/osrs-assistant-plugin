package com.osrsassistant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LlmProvider
{
	CUSTOM_BACKEND("Custom Backend"),
	OPENAI("OpenAI"),
	ANTHROPIC("Anthropic");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
