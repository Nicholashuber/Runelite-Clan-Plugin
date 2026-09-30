package com.corclan.glow;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * How a rank's glow is drawn: stacked outlines, outermost first. Each layer is one
 * ModelOutlineRenderer pass. Pure Java so the look (including the animation) can be unit tested.
 */
public final class GlowStyle
{
	/** One outline pass. */
	public static final class Layer
	{
		public final int width;
		public final Color color;
		/** 0 (hard edge) to 4 (softest) */
		public final int feather;

		Layer(int width, Color color, int feather)
		{
			this.width = width;
			this.color = color;
			this.feather = feather;
		}
	}

	/** One slow breath of the Owner's aura: it swells and brightens, then settles. */
	static final long HOLY_PULSE_MILLIS = 3000L;
	/** The core edge's quicker white/gold shimmer. */
	static final long HOLY_SHIMMER_MILLIS = 900L;

	private GlowStyle()
	{
	}

	/**
	 * The Owner's holy glow: a wide gold aura that breathes (swelling and brightening), a bright
	 * golden-white glow inside it, and a crisp edge on the model that shimmers white to pale gold.
	 * @param now current time in millis; drives the animation
	 */
	public static List<Layer> holy(long now)
	{
		double breath = breath(now, HOLY_PULSE_MILLIS);
		double shimmer = breath(now, HOLY_SHIMMER_MILLIS);
		return Collections.unmodifiableList(Arrays.asList(
			new Layer(lerp(10, 15, breath), new Color(255, 190, 40, lerp(110, 200, breath)), 4),
			new Layer(6, new Color(255, 225, 120, lerp(210, 255, breath)), 2),
			new Layer(2, new Color(255, lerp(255, 240, shimmer), lerp(255, 170, shimmer), 255), 0)));
	}

	/** 0..1..0 over one period, eased (sine) so it breathes rather than blinks. */
	static double breath(long now, long periodMillis)
	{
		double phase = (now % periodMillis) / (double) periodMillis;
		return (1 - Math.cos(phase * 2 * Math.PI)) / 2;
	}

	private static int lerp(int from, int to, double t)
	{
		return (int) Math.round(from + (to - from) * t);
	}
}
