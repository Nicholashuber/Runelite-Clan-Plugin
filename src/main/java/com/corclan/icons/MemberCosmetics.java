package com.corclan.icons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Which member icons and title each player gets. Immutable, so chat rendering on the client thread can
 * read it while a new one is built on another thread.
 *
 * Layers, later ones win per player: built-in defaults, then the player's own "Member icons" config lines.
 */
public final class MemberCosmetics
{
	public static final int MAX_ICONS = 4;
	public static final int MAX_TITLE_LENGTH = 20;
	public static final MemberCosmetics EMPTY = new MemberCosmetics(Collections.emptyMap(), Collections.emptyMap());

	private final Map<String, List<String>> icons;
	private final Map<String, String> titles;

	private MemberCosmetics(Map<String, List<String>> icons, Map<String, String> titles)
	{
		this.icons = icons;
		this.titles = titles;
	}

	/** @return icon keys for a {@link #key} (empty if none) */
	public List<String> iconsFor(String key)
	{
		return icons.getOrDefault(key, Collections.emptyList());
	}

	/** @return the title for a {@link #key}, or null */
	public String titleFor(String key)
	{
		return titles.get(key);
	}

	/** Lookup key: tags removed, non-breaking spaces and underscores as spaces, trimmed, lower case. */
	public static String key(String name)
	{
		if (name == null)
		{
			return "";
		}
		return name.replaceAll("<[^>]*>", "")
			.replace(' ', ' ')
			.replace('_', ' ')
			.trim()
			.toLowerCase(Locale.ROOT);
	}

	public static MemberCosmetics build(
		Map<String, List<String>> builtinIcons,
		Map<String, String> builtinTitles,
		String configLines,
		Predicate<String> isIconKey)
	{
		Map<String, List<String>> icons = new HashMap<>(builtinIcons);
		Map<String, String> titles = new HashMap<>(builtinTitles);

		if (configLines != null)
		{
			for (String line : configLines.split("\\r?\\n"))
			{
				int eq = line.indexOf('=');
				if (eq <= 0)
				{
					continue;
				}
				String key = key(line.substring(0, eq));
				if (key.isEmpty())
				{
					continue;
				}
				String rest = line.substring(eq + 1);
				String iconPart = rest;
				String title = null;
				int bar = rest.indexOf('|');
				if (bar >= 0)
				{
					iconPart = rest.substring(0, bar);
					title = rest.substring(bar + 1);
				}
				List<String> keys = new ArrayList<>();
				for (String icon : iconPart.split(","))
				{
					keys.add(icon.trim().toLowerCase(Locale.ROOT));
				}
				apply(icons, titles, key, cleanIcons(keys, isIconKey), cleanTitle(title));
			}
		}

		return new MemberCosmetics(Collections.unmodifiableMap(icons), Collections.unmodifiableMap(titles));
	}

	/** One layer replaces both icons and title for that player. */
	private static void apply(Map<String, List<String>> icons, Map<String, String> titles, String key, List<String> iconList, String title)
	{
		icons.remove(key);
		titles.remove(key);
		if (!iconList.isEmpty())
		{
			icons.put(key, iconList);
		}
		if (title != null)
		{
			titles.put(key, title);
		}
	}

	static List<String> cleanIcons(List<String> raw, Predicate<String> isIconKey)
	{
		List<String> out = new ArrayList<>();
		for (String icon : raw)
		{
			if (icon != null && isIconKey.test(icon) && !out.contains(icon) && out.size() < MAX_ICONS)
			{
				out.add(icon);
			}
		}
		return Collections.unmodifiableList(out);
	}

	/** Titles are drawn inside chat markup, so tags and angle brackets are never allowed through. */
	static String cleanTitle(String raw)
	{
		if (raw == null)
		{
			return null;
		}
		String t = raw.replaceAll("<[^>]*>", "").replace("<", "").replace(">", "").trim();
		return t.isEmpty() || t.length() > MAX_TITLE_LENGTH ? null : t;
	}
}
