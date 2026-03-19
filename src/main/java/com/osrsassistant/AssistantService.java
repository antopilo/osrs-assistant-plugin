package com.osrsassistant;

public interface AssistantService
{
	String sendMessage(String message, String sessionId, PlayerContext context);

	String decomposeChecklist(String checklistText, PlayerContext context);

	void cancel();
}
