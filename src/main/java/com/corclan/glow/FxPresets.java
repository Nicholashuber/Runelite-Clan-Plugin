package com.corclan.glow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.SpotanimID;

/**
 * The named game graphics the effect preview commands (::glowzap, ::glowshock, ::glowstrike, ::dayfx, ::naturefx)
 * let anyone pick from. RuneLite's Plugin Hub does not accept player-typed game ids, but a fixed list is fine, so a
 * raw spot anim id only works in RuneLite's developer mode, to try out graphics before adding them here.
 */
public final class FxPresets
{
	/** Preset name to spot anim id, in the order the chat lists them. */
	public static final Map<String, Integer> ALL;

	static
	{
		Map<String, Integer> presets = new LinkedHashMap<>();
		// the auras' own graphics
		presets.put("Sepulchre lightning", SpotanimID.HALLOWED_STATUE_LIGHTNING_STRIKE);
		presets.put("Killerwatt shock", SpotanimID.SKELETON_KILLERWATT_ELECTRICSHOCK);
		presets.put("Lucien lightning", SpotanimID.LUC2_LUCIEN_LIGHTNING_SPOT);
		presets.put("Mahjarrat shadow", SpotanimID.VFX_MAHJARRAT_HUMAN_TELEPORT_SHADOW);
		presets.put("Fairy ring", SpotanimID.FAIRY_FLOWER_RING);
		presets.put("Growing leaves", SpotanimID.LEAVES_GROWING);
		presets.put("Druid bloom", SpotanimID.DRUIDICSPIRIT_BLOOM_PLAYER_SPOTANIM);
		// the fires and swirls the recolored parts were made for
		for (TintedFx.Def def : TintedFx.CATALOG.values())
		{
			presets.putIfAbsent(def.name, def.spotId);
		}
		ALL = Collections.unmodifiableMap(presets);
	}

	/** A picked graphic. */
	public static final class Pick
	{
		public final int id;
		/** the number typed after it (swirl rise or height), or null */
		public final Integer extra;
		/** what to call it in chat: the preset's name, or the id typed in developer mode */
		public final String label;

		Pick(int id, Integer extra, String label)
		{
			this.id = id;
			this.extra = extra;
			this.label = label;
		}
	}

	private FxPresets()
	{
	}

	/**
	 * Reads what was typed after the command (and after the part, for ::dayfx and ::naturefx): a preset name in any
	 * case (the start of a name is enough when only one matches), optionally followed by a number for the rise or
	 * height. With {@code rawIds} a spot anim id can be typed instead of a name.
	 * @return null when nothing matches, or an id was typed without {@code rawIds}
	 */
	public static Pick parse(List<String> args, boolean rawIds)
	{
		if (args.isEmpty())
		{
			return null;
		}
		Integer first = number(args.get(0));
		if (first != null)
		{
			return rawIds ? new Pick(first, args.size() > 1 ? number(args.get(1)) : null, String.valueOf(first)) : null;
		}
		List<String> words = new ArrayList<>(args);
		Integer extra = words.size() > 1 ? number(words.get(words.size() - 1)) : null;
		if (extra != null)
		{
			words.remove(words.size() - 1);
		}
		String name = find(key(String.join(" ", words)));
		return name == null ? null : new Pick(ALL.get(name), extra, name);
	}

	/** True when the first word typed is a number: a raw id, which only developer mode takes. */
	public static boolean typedAnId(List<String> args)
	{
		return !args.isEmpty() && number(args.get(0)) != null;
	}

	/** The preset names, for the chat. */
	public static String names()
	{
		return String.join(", ", ALL.keySet());
	}

	private static String find(String typed)
	{
		if (typed.isEmpty())
		{
			return null;
		}
		String startsWith = null;
		int matches = 0;
		for (String name : ALL.keySet())
		{
			String k = key(name);
			if (k.equals(typed))
			{
				return name;
			}
			if (k.startsWith(typed))
			{
				startsWith = name;
				matches++;
			}
		}
		return matches == 1 ? startsWith : null;
	}

	/** Lower case letters and digits only, so "beef tornado", "Beef-Tornado" and "beeftornado" are the same. */
	private static String key(String text)
	{
		return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static Integer number(String word)
	{
		try
		{
			return Integer.parseInt(word);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
