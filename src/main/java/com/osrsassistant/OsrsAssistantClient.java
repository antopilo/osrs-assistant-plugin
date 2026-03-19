package com.osrsassistant;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OsrsAssistantClient
{
	private final HttpClient httpClient;
	private final Gson gson;
	private volatile CompletableFuture<HttpResponse<String>> activeFuture;

	private String apiKey = "";

	public OsrsAssistantClient()
	{
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		this.gson = new Gson();
	}

	public void setApiKey(String apiKey)
	{
		this.apiKey = apiKey != null ? apiKey : "";
	}

	public String sendMessage(String backendUrl, ChatRequest request)
	{
		try
		{
			String json = gson.toJson(request);
			HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(backendUrl + "/api/chat"))
				.header("Content-Type", "application/json")
				.header("X-Api-Key", apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.timeout(Duration.ofSeconds(120))
				.build();

			activeFuture = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
			HttpResponse<String> response = activeFuture.join();
			activeFuture = null;

			if (response.statusCode() != 200)
			{
				log.error("Backend returned status {}: {}", response.statusCode(), response.body());
				return "Error: Backend returned status " + response.statusCode();
			}

			ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
			return chatResponse.getReply();
		}
		catch (java.util.concurrent.CancellationException e)
		{
			log.debug("Request cancelled");
			return null;
		}
		catch (Exception e)
		{
			log.error("Failed to send message to backend", e);
			return "Error: Could not reach the backend. Is it running?";
		}
	}

	/**
	 * Send a checklist to the backend for decomposition into granular micro-steps.
	 * Uses a fast LLM to break high-level steps into actionable sub-steps.
	 * Returns the decomposed checklist text, or the original on failure.
	 */
	public String decomposeChecklist(String backendUrl, String checklistText, PlayerContext playerContext)
	{
		try
		{
			ChatRequest request = new ChatRequest();
			request.setMessage(checklistText);
			request.setSessionId("decompose");
			request.setPlayerContext(playerContext);

			String json = gson.toJson(request);
			HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(backendUrl + "/api/chat/decompose"))
				.header("Content-Type", "application/json")
				.header("X-Api-Key", apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.timeout(Duration.ofSeconds(15))
				.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200)
			{
				log.warn("Decompose returned status {}", response.statusCode());
				return checklistText;
			}

			ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
			String result = chatResponse.getReply();
			if (result != null && !result.isEmpty() && result.contains("{checklist:"))
			{
				log.info("Checklist decomposed ({} -> {} chars)", checklistText.length(), result.length());
				return result;
			}

			return checklistText;
		}
		catch (Exception e)
		{
			log.warn("Failed to decompose checklist: {}", e.getMessage());
			return checklistText;
		}
	}

	public void cancel()
	{
		CompletableFuture<HttpResponse<String>> future = activeFuture;
		if (future != null)
		{
			future.cancel(true);
			activeFuture = null;
		}
	}
}
