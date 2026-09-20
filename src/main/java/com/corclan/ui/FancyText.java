package com.corclan.ui;

import java.awt.Color;

/** Builds OSRS chat strings with per-character colour tags. Pure Java, unit tested. */
public final class FancyText
{
	public static final Color COR_RED = new Color(0xD3, 0x2F, 0x2F);
	public static final Color COR_BLUE = new Color(0x10, 0x46, 0xFB);
	public static final Color GOLD = new Color(0xFF, 0xC1, 0x07);
	public static final Color WHITE = new Color(0xF2, 0xF2, 0xF2);
	public static final Color ICE = new Color(0x7F, 0xD8, 0xFF);

	private FancyText()
	{
	}

	public static String hex(Color c)
	{
		return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	public static String colored(String text, Color c)
	{
		return "<col=" + hex(c) + ">" + text + "</col>";
	}

	/**
	 * Colours each character on a straight line from {@code from} to {@code to}. Spaces are left
	 * untagged so the string stays short.
	 */
	public static String gradient(String text, Color from, Color to)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length() * 18);
		int visible = 0;
		for (int i = 0; i < text.length(); i++)
		{
			if (text.charAt(i) != ' ')
			{
				visible++;
			}
		}
		int step = 0;
		for (int i = 0; i < text.length(); i++)
		{
			char ch = text.charAt(i);
			if (ch == ' ')
			{
				sb.append(' ');
				continue;
			}
			float t = visible <= 1 ? 0f : (float) step / (visible - 1);
			step++;
			sb.append("<col=").append(hex(mix(from, to, t))).append('>').append(ch).append("</col>");
		}
		return sb.toString();
	}

	/** A three-stop gradient: from -> mid -> to. */
	public static String gradient(String text, Color from, Color mid, Color to)
	{
		int half = text.length() / 2;
		return gradient(text.substring(0, half), from, mid) + gradient(text.substring(half), mid, to);
	}

	static Color mix(Color a, Color b, float t)
	{
		t = Math.max(0f, Math.min(1f, t));
		return new Color(
			Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
			Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
			Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}
}
