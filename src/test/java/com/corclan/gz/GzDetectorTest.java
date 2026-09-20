package com.corclan.gz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GzDetectorTest
{
	private static final int MAX = 40;

	@Test
	public void plainForms()
	{
		assertTrue(GzDetector.isGz("gz", MAX));
		assertTrue(GzDetector.isGz("GZ", MAX));
		assertTrue(GzDetector.isGz("Gz!", MAX));
		assertTrue(GzDetector.isGz("grats", MAX));
		assertTrue(GzDetector.isGz("gratz", MAX));
		assertTrue(GzDetector.isGz("congrats", MAX));
		assertTrue(GzDetector.isGz("Congratulations", MAX));
	}

	@Test
	public void repeatedLettersAndPunctuation()
	{
		assertTrue(GzDetector.isGz("GZZZZZ!!!", MAX));
		assertTrue(GzDetector.isGz("gzzzzzzzzzz", MAX));
		assertTrue(GzDetector.isGz("graaaats", MAX));
		assertTrue(GzDetector.isGz("gz gz gz", MAX));
		assertTrue(GzDetector.isGz("ggzz", MAX));
	}

	@Test
	public void withinShortSentences()
	{
		assertTrue(GzDetector.isGz("big gz mate", MAX));
		assertTrue(GzDetector.isGz("gz on the pet", MAX));
		assertTrue(GzDetector.isGz("huge grats!!", MAX));
		assertTrue(GzDetector.isGz("wow gz :)", MAX));
	}

	@Test
	public void notGz()
	{
		assertFalse(GzDetector.isGz("", MAX));
		assertFalse(GzDetector.isGz("   ", MAX));
		assertFalse(GzDetector.isGz(null, MAX));
		assertFalse(GzDetector.isGz("anyone doing cox?", MAX));
		assertFalse(GzDetector.isGz("lol", MAX));
		assertFalse(GzDetector.isGz("gzu", MAX));
		assertFalse(GzDetector.isGz("gzeg is my friend", MAX));
	}

	@Test
	public void longMessagesAreIgnored()
	{
		assertFalse(GzDetector.isGz("did anyone see the gz that guy got earlier in the raid lol", MAX));
		assertTrue(GzDetector.isGz("did anyone see the gz that guy got earlier in the raid lol", 200));
	}

	@Test
	public void normalizeCollapsesRepeats()
	{
		assertEquals("gz", GzDetector.normalize("GZZZZ!!!"));
		assertEquals("big gz mate", GzDetector.normalize("  Big   GZZ,  mate "));
		assertEquals("grats", GzDetector.normalize("graaaatsss"));
	}
}
