package com.osrsassistant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillInfo
{
	private String name;
	private int level;
	private int boostedLevel;
}
