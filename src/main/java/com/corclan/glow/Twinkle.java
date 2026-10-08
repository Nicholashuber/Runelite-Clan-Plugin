package com.corclan.glow;

import java.util.ArrayList;
import java.util.List;

/**
 * Twinkling stars on a gem glow's wearer. Each star blooms and fades over one period, then reappears
 * somewhere else; the stars are staggered so some are always lit. Positions are fractions of the wearer's
 * on-screen bounds and depend only on the seed and the time, so every frame (and every viewer) agrees
 * without keeping state. Pure Java so it can be unit tested.
 */
public final class Twinkle
{
	/** one star's bloom and fade */
	static final long PERIOD_MILLIS = 1400L;

	/** One star this frame. */
	public static final class Star
	{
		/** 0..1 across the wearer's bounds, left to right */
		public final double x;
		/** 0..1 down the wearer's bounds, top to bottom */
		public final double y;
		/** 0 (unlit) .. 1 (full bloom) */
		public final double size;

		Star(double x, double y, double size)
		{
			this.x = x;
			this.y = y;
			this.size = size;
		}
	}

	private Twinkle()
	{
	}

	/**
	 * @param seed  keeps each wearer's stars in their own places, e.g. their name's hash
	 * @param count stars twinkling at once
	 * @param now   current time in millis
	 */
	public static List<Star> stars(long seed, int count, long now)
	{
		List<Star> stars = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			// stagger the stars evenly through the period
			long t = now + i * PERIOD_MILLIS / count + Math.floorMod(seed, PERIOD_MILLIS);
			long cycle = Math.floorDiv(t, PERIOD_MILLIS);
			double phase = Math.floorMod(t, PERIOD_MILLIS) / (double) PERIOD_MILLIS;
			// a sharp bloom: quick to light, quick to fade
			double bloom = Math.sin(phase * Math.PI);
			long place = mix(seed * 31 + i * 0x9E3779B97F4A7C15L + cycle);
			stars.add(new Star(unit(place), unit(mix(place)), bloom * bloom));
		}
		return stars;
	}

	/** splitmix64: scrambles a number into well spread bits */
	private static long mix(long z)
	{
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** 0..1 from the top 53 bits */
	private static double unit(long bits)
	{
		return (bits >>> 11) / (double) (1L << 53);
	}
}
