package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomBackendService implements AssistantService
{
	private final OsrsAssistantClient client;
	private final OsrsAssistantConfig config;

	public CustomBackendService(OsrsAssistantConfig config)
	{
		this.config = config;
		this.client = new OsrsAssistantClient();
	}

	@Override
	public String sendMessage(String message, String sessionId, PlayerContext context)
	{
		client.setApiKey(config.apiKey());

		ChatRequest request = new ChatRequest();
		request.setMessage(message);
		request.setSessionId(sessionId);
		request.setPlayerContext(context);

		return client.sendMessage(config.backendUrl(), request);
	}

	@Override
	public String decomposeChecklist(String checklistText, PlayerContext context)
	{
		client.setApiKey(config.apiKey());
		return client.decomposeChecklist(config.backendUrl(), checklistText, context);
	}

	@Override
	public void cancel()
	{
		client.cancel();
	}
}
