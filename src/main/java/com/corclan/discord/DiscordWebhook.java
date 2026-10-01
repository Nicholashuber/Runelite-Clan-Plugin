package com.corclan.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.regex.Pattern;

/** Pure helpers for posting to a Discord webhook. Unit tested. */
public final class DiscordWebhook
{
	/** Only real Discord webhooks, so a typo or a pasted link can't send screenshots anywhere else. */
	private static final Pattern WEBHOOK_URL = Pattern.compile(
		"https://(?:(?:ptb|canary)\\.)?discord(?:app)?\\.com/api/webhooks/\\d+/[A-Za-z0-9_-]+/?");
	/** Discord's message length limit. */
	static final int MAX_CONTENT = 2000;

	private DiscordWebhook()
	{
	}

	public static boolean isWebhookUrl(String url)
	{
		return url != null && WEBHOOK_URL.matcher(url.trim()).matches();
	}

	/** Game text can contain Discord formatting characters; escape them so the line shows as typed. */
	static String escape(String text)
	{
		return text.replaceAll("([\\\\*_~`|>#\\[\\]()-])", "\\\\$1");
	}

	/**
	 * The "payload_json" part: the message text, posted as "CoR Clan", with every kind of ping turned off
	 * (no @everyone, roles or users), whatever the text says.
	 */
	public static String payload(Gson gson, String player, String broadcast)
	{
		String content = "**" + escape(player) + "**: " + escape(broadcast);
		if (content.length() > MAX_CONTENT)
		{
			content = content.substring(0, MAX_CONTENT);
		}
		JsonObject json = new JsonObject();
		json.addProperty("username", "CoR Clan");
		json.addProperty("content", content);
		JsonObject mentions = new JsonObject();
		mentions.add("parse", new JsonArray());
		json.add("allowed_mentions", mentions);
		return gson.toJson(json);
	}
}
