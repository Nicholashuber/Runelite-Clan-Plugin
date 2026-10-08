package com.corclan.glow;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Gem League looks, each in its gem's in-game colour: a soft haze, then (from Diamond up) a glow
 * inside it, then a crisp edge on the model. Each gem up the ladder glows much wider and brighter, and from
 * Diamond up stars twinkle on the wearer ({@link #sparkle}, {@link Twinkle}). Pure Java so the look
 * (including the animation) can be unit tested.
 */
public final class GemStyle
{
	/** Opal's sheen drifting through pastel hues */
	static final long OPAL_SHEEN_MILLIS = 6000L;
	/** the calm breath of most gems */
	static final long CALM_MILLIS = 3500L;
	/** Ruby's quicker pulse */
	static final long RUBY_PULSE_MILLIS = 1600L;
	/** Diamond's and Onyx's glint on the edge */
	static final long GLINT_MILLIS = 700L;
	static final long ZENYTE_PULSE_MILLIS = 2600L;
	static final long ZENYTE_SHIMMER_MILLIS = 900L;

	/**
	 * How a gem's stars look: how many twinkle at once, their colour, the colour of the bright point at
	 * their centre and their size in pixels at full bloom.
	 */
	public static final class Sparkle
	{
		public final int count;
		public final Color color;
		public final Color core;
		public final int radius;

		Sparkle(int count, Color color, Color core, int radius)
		{
			this.count = count;
			this.color = color;
			this.core = core;
			this.radius = radius;
		}
	}

	private GemStyle()
	{
	}

	/** 0 for Opal up to 7 for Zenyte. */
	static int tier(GemGlow gem)
	{
		return GemGlow.values().length - 1 - gem.ordinal();
	}

	/** @param now current time in millis; drives the animation */
	public static List<GlowStyle.Layer> layers(GemGlow gem, long now)
	{
		int tier = tier(gem);
		double calm = GlowStyle.breath(now, CALM_MILLIS);
		double glint = GlowStyle.breath(now, GLINT_MILLIS);
		switch (gem)
		{
			case OPAL:
			{
				// a pale haze whose hue slowly drifts, like an opal's play of colour
				float hue = (now % OPAL_SHEEN_MILLIS) / (float) OPAL_SHEEN_MILLIS;
				return layers(
					haze(tier, Color.getHSBColor(hue, 0.3f, 1f), calm),
					edge(tier, new Color(245, 245, 255)));
			}
			case SAPPHIRE:
				return layers(
					haze(tier, new Color(20, 70, 220), calm),
					edge(tier, new Color(110, 165, 255)));
			case EMERALD:
				return layers(
					haze(tier, new Color(0, 165, 70), calm),
					edge(tier, new Color(120, 240, 150)));
			case RUBY:
				// pulses quicker than the calm gems
				return layers(
					haze(tier, new Color(190, 0, 30), GlowStyle.breath(now, RUBY_PULSE_MILLIS)),
					edge(tier, new Color(255, 80, 100)));
			case DIAMOND:
				// icy haze and an edge that glints white to pale cyan
				return layers(
					haze(tier, new Color(190, 230, 255), calm),
					inner(tier, new Color(225, 245, 255)),
					edge(tier, new Color(lerp(255, 170, glint), 255, 255)));
			case DRAGONSTONE:
				return layers(
					haze(tier, new Color(130, 30, 200), calm),
					inner(tier, new Color(200, 80, 235)),
					edge(tier, new Color(235, 185, 255)));
			case ONYX:
			{
				// black smoke would vanish against dark ground on its own, so the edge glints silver
				int silver = lerp(170, 255, glint);
				return layers(
					haze(tier, new Color(8, 8, 14), calm),
					inner(tier, new Color(45, 45, 58)),
					edge(tier, new Color(silver, silver, Math.min(255, silver + 15))));
			}
			case ZENYTE:
			{
				// a swelling orange aura, an amber glow and a shimmering pale gold edge
				double shimmer = GlowStyle.breath(now, ZENYTE_SHIMMER_MILLIS);
				return layers(
					haze(tier, new Color(255, 115, 0), GlowStyle.breath(now, ZENYTE_PULSE_MILLIS)),
					inner(tier, new Color(255, 165, 30)),
					edge(tier, new Color(255, lerp(225, 255, shimmer), lerp(130, 215, shimmer))));
			}
			default:
				return Collections.emptyList();
		}
	}

	/** The stars twinkling on a wearer of {@code gem}, or null below Diamond. */
	public static Sparkle sparkle(GemGlow gem)
	{
		switch (gem)
		{
			case DIAMOND:
				return new Sparkle(4, new Color(220, 245, 255), Color.WHITE, 10);
			case DRAGONSTONE:
				return new Sparkle(6, new Color(235, 175, 255), Color.WHITE, 12);
			case ONYX:
				// black stars; a dim silver point keeps them reading as twinkles, not holes
				return new Sparkle(8, new Color(5, 5, 10), new Color(110, 110, 125), 14);
			case ZENYTE:
				return new Sparkle(10, new Color(255, 75, 20), new Color(255, 215, 150), 16);
			default:
				return null;
		}
	}

	/**
	 * The soft outer haze, much wider and brighter each tier up: Opal 4-5 px at alpha 60-110, up to
	 * Zenyte 18-21 px at 228-250.
	 * @param breath 0..1, swells it; higher tiers swell further
	 */
	private static GlowStyle.Layer haze(int tier, Color color, double breath)
	{
		return new GlowStyle.Layer(4 + 2 * tier + lerp(0, 1 + tier / 3, breath),
			alpha(color, lerp(60 + 24 * tier, 110 + 20 * tier, breath)), 4);
	}

	/** The glow inside the haze, Diamond and up: 4 px at Diamond to 7 at Zenyte, brighter each tier. */
	private static GlowStyle.Layer inner(int tier, Color color)
	{
		return new GlowStyle.Layer(tier, alpha(color, 190 + 20 * (tier - 4)), 2);
	}

	/** The crisp edge on the model, thicker from Diamond up. */
	private static GlowStyle.Layer edge(int tier, Color color)
	{
		return new GlowStyle.Layer(tier >= 4 ? 3 : 2, alpha(color, 255), 0);
	}

	private static List<GlowStyle.Layer> layers(GlowStyle.Layer... layers)
	{
		return Collections.unmodifiableList(Arrays.asList(layers));
	}

	private static Color alpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static int lerp(int from, int to, double t)
	{
		return (int) Math.round(from + (to - from) * t);
	}
}
