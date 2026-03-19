package com.osrsassistant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConversationMessage
{
	private String role;
	private String content;
}
