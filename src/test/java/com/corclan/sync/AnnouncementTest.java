package com.corclan.sync;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class AnnouncementTest
{
	private static ClanSnapshot snapshot(String json)
	{
		return ClanSnapshot.of(new Gson().fromJson(json, SyncModels.ClanResponse.class));
	}

	@Test
	public void readsTheBannerFromTheClanFeed()
	{
		Announcement a = snapshot("{\"players\":[],\"announcement\":{\"text\":\"Bandos mass tonight\\n20:00 UTC, W330\","
			+ "\"by\":\"Lavasockz\",\"updatedAt\":\"2026-10-12T17:55:00.000Z\"}}").getAnnouncement();
		assertEquals("Bandos mass tonight\n20:00 UTC, W330", a.text);
		assertEquals("Lavasockz", a.by);
		assertEquals(java.time.Instant.parse("2026-10-12T17:55:00.000Z").toEpochMilli(), a.updatedAt);
	}

	@Test
	public void noBannerWhenMissingNullOrEmpty()
	{
		assertNull(snapshot("{\"players\":[]}").getAnnouncement());
		assertNull(snapshot("{\"announcement\":null}").getAnnouncement());
		assertNull(snapshot("{\"announcement\":{\"text\":\"  <b></b> \\n \",\"by\":\"Lavasockz\"}}").getAnnouncement());
	}

	@Test
	public void anUnreadableTimeStillShowsTheBanner()
	{
		Announcement a = snapshot("{\"announcement\":{\"text\":\"hi\",\"by\":\"JoeBoob\",\"updatedAt\":\"yesterday\"}}").getAnnouncement();
		assertEquals("hi", a.text);
		assertEquals(0L, a.updatedAt);
	}

	@Test
	public void markupNeverGetsThrough()
	{
		assertEquals("Party at the GE", Announcement.clean("<col=ff0000>Party</col> at <img=3> the GE"));
		assertEquals("a c", Announcement.clean("a < b > c"));
		assertEquals("html", Announcement.clean("<html>html"));
		Announcement a = snapshot("{\"announcement\":{\"text\":\"x\",\"by\":\"<b>Lava</b>\\nsockz\"}}").getAnnouncement();
		assertEquals("Lava sockz", a.by);
	}

	@Test
	public void tidiesSpacesAndLines()
	{
		assertEquals("one two\nthree", Announcement.clean("  one    two \r\n\n\n three  "));
		StringBuilder many = new StringBuilder();
		for (int i = 0; i < Announcement.MAX_LINES + 4; i++)
		{
			many.append("line ").append(i).append('\n');
		}
		assertEquals(Announcement.MAX_LINES, Announcement.clean(many.toString()).split("\n").length);
		StringBuilder longText = new StringBuilder();
		for (int i = 0; i < Announcement.MAX_LENGTH + 50; i++)
		{
			longText.append('x');
		}
		assertEquals(Announcement.MAX_LENGTH, Announcement.clean(longText.toString()).length());
		assertEquals("", Announcement.clean(null));
	}
}
