package com.corclan.glow;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Lavasockz's molten outline: a dark red heat haze, an orange body of flame and a white-hot core, all
 * flickering like a fire rather than breathing like the Owner's gold glow. Pure Java so it can be tested.
 */
public final class MoltenStyle
{
	private MoltenStyle()
	{
	}

	/** @param now current time in millis; drives the flicker */
	public static List<GlowStyle.Layer> molten(long now)
	{
		double flicker = flicker(now);
		double fast = flicker(now * 3 + 1234);
		return Collections.unmodifiableList(Arrays.asList(
			new GlowStyle.Layer(lerp(9, 14, flicker), new Color(150, lerp(10, 30, fast), 0, lerp(120, 190, flicker)), 4),
			new GlowStyle.Layer(lerp(4, 6, fast), new Color(255, lerp(80, 130, flicker), 0, 235), 2),
			new GlowStyle.Layer(2, new Color(255, lerp(200, 245, fast), lerp(60, 170, fast), 255), 0)));
	}

	/**
	 * 0..1, irregular like flames: three sines at unrelated speeds added together, so it never settles
	 * into an obvious rhythm.
	 */
	static double flicker(long now)
	{
		double t = now / 1000.0;
		double v = Math.sin(t * 7.3) + 0.6 * Math.sin(t * 13.1 + 1.7) + 0.4 * Math.sin(t * 23.9 + 4.2);
		return (v / 2.0 + 1) / 2;
	}

	private static int lerp(int from, int to, double t)
	{
		double clamped = Math.max(0, Math.min(1, t));
		return (int) Math.round(from + (to - from) * clamped);
	}
}
