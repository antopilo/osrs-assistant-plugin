package com.osrsassistant;

import java.awt.*;

/**
 * Unified color and font palette for the entire plugin UI.
 * All colors are defined here — no inline new Color() calls elsewhere.
 */
public final class Theme
{
	private Theme() {}

	// ---- Base surfaces ----
	public static final Color BG = new Color(25, 25, 28);
	public static final Color SURFACE = new Color(35, 35, 40);
	public static final Color SURFACE_HOVER = new Color(50, 50, 58);
	public static final Color BORDER = new Color(55, 55, 62);

	// ---- Text ----
	public static final Color TEXT = new Color(220, 220, 225);
	public static final Color TEXT_DIM = new Color(140, 140, 150);
	public static final Color TEXT_MUTED = new Color(120, 120, 120);
	public static final Color TEXT_USER = new Color(190, 200, 220);

	// ---- Semantic colors ----
	public static final Color SUCCESS = new Color(80, 200, 100);
	public static final Color SUCCESS_BG = new Color(80, 200, 100, 25);
	public static final Color WARNING = new Color(240, 180, 50);
	public static final Color WARNING_BG = new Color(240, 180, 50, 25);
	public static final Color ERROR = new Color(220, 80, 70);
	public static final Color ERROR_BG = new Color(220, 80, 70, 25);

	// ---- Accent (interactive / navigation) ----
	public static final Color ACCENT = new Color(80, 210, 255);
	public static final Color ACCENT_BG = new Color(80, 210, 255, 25);
	public static final Color ACCENT_STRONG = new Color(0, 180, 230);
	public static final Color ACCENT_HOVER = new Color(0, 200, 255);

	// ---- Item (gold) ----
	public static final Color ITEM = new Color(255, 190, 60);
	public static final Color ITEM_BG = new Color(255, 190, 60, 25);

	// ---- Neutral (not started / unknown) ----
	public static final Color NEUTRAL = new Color(180, 180, 190);
	public static final Color NEUTRAL_BG = new Color(180, 180, 190, 20);

	// ---- Message bubbles ----
	public static final Color MSG_USER = new Color(35, 55, 80);
	public static final Color MSG_ASSISTANT = new Color(40, 40, 45);
	public static final Color SENDER_USER = new Color(120, 170, 255);
	public static final Color SENDER_ASSISTANT = new Color(100, 220, 160);
	public static final Color BAR_USER = new Color(70, 130, 200);

	// ---- Links ----
	public static final Color LINK = new Color(93, 173, 226);
	public static final Color LINK_BG = new Color(93, 173, 226, 20);

	// ---- Code ----
	public static final Color CODE_BG = new Color(30, 30, 30);
	public static final Color CODE_FG = new Color(230, 219, 116);

	// ---- Cancel / destructive ----
	public static final Color CANCEL = new Color(200, 80, 80);

	// ---- Fonts ----
	public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
	public static final Font BOLD = BASE.deriveFont(Font.BOLD);
	public static final Font ITALIC = BASE.deriveFont(Font.ITALIC);
	public static final Font BOLD_ITALIC = BASE.deriveFont(Font.BOLD | Font.ITALIC);
	public static final Font CODE = new Font("Consolas", Font.PLAIN, 12);
	public static final Font SENDER = new Font("Segoe UI", Font.BOLD, 11);
	public static final Font H1 = new Font("Segoe UI", Font.BOLD, 16);
	public static final Font H2 = new Font("Segoe UI", Font.BOLD, 15);
	public static final Font H3 = new Font("Segoe UI", Font.BOLD, 14);
	public static final Font SMALL = BASE.deriveFont(10f);
	public static final Font SMALL_BOLD = BASE.deriveFont(Font.BOLD, 11f);
}
