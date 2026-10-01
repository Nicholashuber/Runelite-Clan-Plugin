package com.corclan.discord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

public class DiscordWebhookTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void onlyDiscordWebhooksAreUsed()
	{
		assertTrue(DiscordWebhook.isWebhookUrl("https://discord.com/api/webhooks/123/abc_DEF-9"));
		assertTrue(DiscordWebhook.isWebhookUrl(" https://discordapp.com/api/webhooks/123/abc "));
		assertTrue(DiscordWebhook.isWebhookUrl("https://ptb.discord.com/api/webhooks/123/abc"));
		assertFalse(DiscordWebhook.isWebhookUrl(""));
		assertFalse(DiscordWebhook.isWebhookUrl(null));
		assertFalse(DiscordWebhook.isWebhookUrl("http://discord.com/api/webhooks/123/abc"));
		assertFalse(DiscordWebhook.isWebhookUrl("https://evil.com/api/webhooks/123/abc"));
		assertFalse(DiscordWebhook.isWebhookUrl("https://discord.com.evil.com/api/webhooks/123/abc"));
		assertFalse(DiscordWebhook.isWebhookUrl("https://discord.gg/invite"));
	}

	@Test
	public void payloadNeverPingsAndEscapesFormatting()
	{
		JsonObject json = GSON.fromJson(DiscordWebhook.payload(GSON, "Iron_Nick", "Iron_Nick received a drop: @everyone *Twisted bow*"), JsonObject.class);
		assertEquals("CoR Clan", json.get("username").getAsString());
		assertEquals(0, json.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").size());
		assertEquals("**Iron\\_Nick**: Iron\\_Nick received a drop: @everyone \\*Twisted bow\\*", json.get("content").getAsString());
	}

	@Test
	public void longTextIsCutToDiscordsLimit()
	{
		StringBuilder longText = new StringBuilder();
		for (int i = 0; i < 3000; i++)
		{
			longText.append('a');
		}
		JsonObject json = GSON.fromJson(DiscordWebhook.payload(GSON, "Bob", longText.toString()), JsonObject.class);
		assertEquals(DiscordWebhook.MAX_CONTENT, json.get("content").getAsString().length());
	}
}
