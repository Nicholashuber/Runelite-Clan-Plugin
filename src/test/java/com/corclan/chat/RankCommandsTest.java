package com.corclan.chat;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RankCommandsTest
{
	@Test
	public void tierShowsRankAndIcon()
	{
		assertEquals(" <col=1046fb>[<img=5>Zenyte]</col>", RankCommands.suffix("Zenyte", "<img=5>", null));
	}

	@Test
	public void rankAddsGzGiven()
	{
		assertEquals(" <col=1046fb>[<img=5><img=9>Gnome child] 152 gz given</col>",
			RankCommands.suffix("Gnome child", "<img=5><img=9>", 152));
		assertEquals(" <col=1046fb>[Opal] 0 gz given</col>", RankCommands.suffix("Opal", "", 0));
	}

	@Test
	public void titlesCantInjectChatMarkup()
	{
		assertEquals(" <col=1046fb>[Opal]</col>", RankCommands.suffix("<col=ff0000>Opal", "", null));
	}

	@Test
	public void commandIsTheFirstWordInAnyCase()
	{
		assertEquals("!rank", RankCommands.command("!RANK"));
		assertEquals("!tier", RankCommands.command("  !Tier please "));
		assertEquals("", RankCommands.command(null));
	}
}
