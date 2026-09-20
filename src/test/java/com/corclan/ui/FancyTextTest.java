package com.corclan.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import org.junit.Test;

public class FancyTextTest
{
	@Test
	public void hexAndColored()
	{
		assertEquals("ff0000", FancyText.hex(Color.RED));
		assertEquals("<col=ff0000>hi</col>", FancyText.colored("hi", Color.RED));
	}

	@Test
	public void gradientEndpoints()
	{
		String s = FancyText.gradient("ab", Color.RED, Color.BLUE);
		assertEquals("<col=ff0000>a</col><col=0000ff>b</col>", s);
	}

	@Test
	public void gradientSkipsSpacesAndHandlesEdgeCases()
	{
		String s = FancyText.gradient("a b", Color.RED, Color.BLUE);
		assertEquals("<col=ff0000>a</col> <col=0000ff>b</col>", s);
		assertEquals("<col=ff0000>a</col>", FancyText.gradient("a", Color.RED, Color.BLUE));
		assertEquals("", FancyText.gradient("", Color.RED, Color.BLUE));
		assertEquals("", FancyText.gradient(null, Color.RED, Color.BLUE));
	}

	@Test
	public void midpointIsBlend()
	{
		Color mid = FancyText.mix(Color.RED, Color.BLUE, 0.5f);
		assertEquals(128, mid.getRed());
		assertEquals(0, mid.getGreen());
		assertEquals(128, mid.getBlue());
	}

	@Test
	public void threeStopGradientCoversWholeString()
	{
		String s = FancyText.gradient("abcd", Color.RED, Color.GREEN, Color.BLUE);
		assertTrue(s.startsWith("<col=ff0000>a</col>"));
		assertTrue(s.endsWith("<col=0000ff>d</col>"));
	}
}
