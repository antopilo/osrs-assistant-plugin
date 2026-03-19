package com.osrsassistant;

import lombok.Data;

@Data
public class ChatRequest
{
	private String message;
	private String sessionId;
	private PlayerContext playerContext;
}
