package com.osrsassistant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class DirectLlmService implements AssistantService
{
	private static final int MAX_HISTORY = 20;

	private final LlmProvider provider;
	private final String apiKey;
	private final String model;
	private final String customPrompt;
	private final OsrsAssistantConfig config;
	private final HttpClient httpClient;
	private final Gson gson;

	private final List<ConversationMessage> history = new ArrayList<>();
	private String currentSessionId;
	private volatile CompletableFuture<HttpResponse<String>> activeFuture;

	public DirectLlmService(LlmProvider provider, String apiKey, String modelId, String customPrompt, OsrsAssistantConfig config)
	{
		this.provider = provider;
		this.apiKey = apiKey;
		this.model = (modelId != null && !modelId.trim().isEmpty()) ? modelId.trim() : defaultModel(provider);
		this.customPrompt = customPrompt;
		this.config = config;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		this.gson = new Gson();
	}

	private static String defaultModel(LlmProvider provider)
	{
		switch (provider)
		{
			case OPENAI:
				return "gpt-4o-mini";
			case ANTHROPIC:
				return "claude-sonnet-4-20250514";
			default:
				return "gpt-4o-mini";
		}
	}

	@Override
	public String sendMessage(String message, String sessionId, PlayerContext context)
	{
		if (apiKey == null || apiKey.isEmpty())
		{
			return "Error: No API key configured. Add your " + provider.getDisplayName()
				+ " API key in the plugin settings.";
		}

		// Reset history on new session
		if (!sessionId.equals(currentSessionId))
		{
			history.clear();
			currentSessionId = sessionId;
		}

		String systemPrompt = SystemPromptBuilder.build(context, customPrompt, config);
		history.add(new ConversationMessage("user", message));
		trimHistory();

		try
		{
			String reply = callLlm(systemPrompt, history);

			if (reply != null)
			{
				reply = ResponseEnricher.enrich(reply);
				history.add(new ConversationMessage("assistant", reply));
				trimHistory();
			}

			return reply;
		}
		catch (java.util.concurrent.CancellationException e)
		{
			// Remove the user message we just added since it was cancelled
			if (!history.isEmpty())
			{
				history.remove(history.size() - 1);
			}
			log.debug("Request cancelled");
			return null;
		}
		catch (Exception e)
		{
			// Remove the user message on error too
			if (!history.isEmpty())
			{
				history.remove(history.size() - 1);
			}
			log.error("LLM request failed", e);
			return "Error: " + e.getMessage();
		}
	}

	@Override
	public String decomposeChecklist(String checklistText, PlayerContext context)
	{
		if (apiKey == null || apiKey.isEmpty())
		{
			return checklistText;
		}

		try
		{
			String systemPrompt = SystemPromptBuilder.buildDecompose(context, config);
			List<ConversationMessage> messages = new ArrayList<>();
			messages.add(new ConversationMessage("user", checklistText));

			String reply = callLlm(systemPrompt, messages);
			if (reply != null && reply.contains("{checklist:"))
			{
				return ResponseEnricher.enrich(reply);
			}
			return checklistText;
		}
		catch (Exception e)
		{
			log.warn("Failed to decompose checklist: {}", e.getMessage());
			return checklistText;
		}
	}

	@Override
	public void cancel()
	{
		CompletableFuture<HttpResponse<String>> future = activeFuture;
		if (future != null)
		{
			future.cancel(true);
			activeFuture = null;
		}
	}

	private String callLlm(String systemPrompt, List<ConversationMessage> messages)
	{
		switch (provider)
		{
			case OPENAI:
				return callOpenAi(systemPrompt, messages);
			case ANTHROPIC:
				return callAnthropic(systemPrompt, messages);
			default:
				throw new IllegalStateException("Unsupported provider: " + provider);
		}
	}

	private String callOpenAi(String systemPrompt, List<ConversationMessage> messages)
	{
		JsonObject body = new JsonObject();
		body.addProperty("model", model);
		body.addProperty("max_tokens", 2048);

		JsonArray messagesArray = new JsonArray();

		// System message (use "developer" role for o-series models)
		JsonObject systemMsg = new JsonObject();
		systemMsg.addProperty("role", model.startsWith("o") ? "developer" : "system");
		systemMsg.addProperty("content", systemPrompt);
		messagesArray.add(systemMsg);

		for (ConversationMessage msg : messages)
		{
			JsonObject msgObj = new JsonObject();
			msgObj.addProperty("role", msg.getRole());
			msgObj.addProperty("content", msg.getContent());
			messagesArray.add(msgObj);
		}

		body.add("messages", messagesArray);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("https://api.openai.com/v1/chat/completions"))
			.header("Content-Type", "application/json")
			.header("Authorization", "Bearer " + apiKey)
			.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
			.timeout(Duration.ofSeconds(120))
			.build();

		return sendRequest(request, "openai");
	}

	private String callAnthropic(String systemPrompt, List<ConversationMessage> messages)
	{
		JsonObject body = new JsonObject();
		body.addProperty("model", model);
		body.addProperty("max_tokens", 4096);

		// System prompt as array of content blocks
		JsonArray systemArray = new JsonArray();
		JsonObject systemBlock = new JsonObject();
		systemBlock.addProperty("type", "text");
		systemBlock.addProperty("text", systemPrompt);
		systemArray.add(systemBlock);
		body.add("system", systemArray);

		JsonArray messagesArray = new JsonArray();
		for (ConversationMessage msg : messages)
		{
			JsonObject msgObj = new JsonObject();
			msgObj.addProperty("role", msg.getRole());
			msgObj.addProperty("content", msg.getContent());
			messagesArray.add(msgObj);
		}
		body.add("messages", messagesArray);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("https://api.anthropic.com/v1/messages"))
			.header("Content-Type", "application/json")
			.header("x-api-key", apiKey)
			.header("anthropic-version", "2023-06-01")
			.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
			.timeout(Duration.ofSeconds(120))
			.build();

		return sendRequest(request, "anthropic");
	}

	private String sendRequest(HttpRequest request, String providerName)
	{
		activeFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> response = activeFuture.join();
		activeFuture = null;

		if (response.statusCode() == 429)
		{
			return "Error: Rate limited by " + providerName + ". Please wait a moment and try again.";
		}

		if (response.statusCode() != 200)
		{
			String body = response.body();
			log.error("{} returned status {}: {}", providerName, response.statusCode(), body);

			// Try to extract the error message from the API response
			String detail = "";
			try
			{
				JsonObject err = new JsonParser().parse(body).getAsJsonObject();
				if (err.has("error"))
				{
					JsonObject errorObj = err.getAsJsonObject("error");
					if (errorObj.has("message"))
					{
						detail = ": " + errorObj.get("message").getAsString();
					}
				}
			}
			catch (Exception ignored) {}

			return "Error: " + providerName + " returned status " + response.statusCode() + detail;
		}

		return parseResponse(response.body(), providerName);
	}

	private String parseResponse(String responseBody, String providerName)
	{
		JsonObject json = new JsonParser().parse(responseBody).getAsJsonObject();

		if ("openai".equals(providerName))
		{
			return json.getAsJsonArray("choices")
				.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content").getAsString();
		}
		else
		{
			// Anthropic format
			return json.getAsJsonArray("content")
				.get(0).getAsJsonObject()
				.get("text").getAsString();
		}
	}

	private void trimHistory()
	{
		while (history.size() > MAX_HISTORY)
		{
			history.remove(0);
		}
	}
}
