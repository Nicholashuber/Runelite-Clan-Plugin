package com.corclan.discord;

import com.corclan.CorClanConfig;
import com.google.gson.Gson;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DrawManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Posts your own clan broadcasts (drops, pets, levels, collection log) to the clan's Discord webhook, with a
 * screenshot of the game, like other Discord loot plugins. Only the player the broadcast is about posts it,
 * so the channel gets each one once. Nothing is sent unless a webhook URL is set.
 */
@Slf4j
@Singleton
public class DiscordDrops
{
	/** At most one post this often, so a burst of broadcasts (or a bug) can't flood the channel. */
	static final long MIN_GAP_MILLIS = 5_000L;
	private static final MediaType PNG = MediaType.parse("image/png");

	private final CorClanConfig config;
	private final DrawManager drawManager;
	private final OkHttpClient httpClient;
	private final ScheduledExecutorService executor;
	private final Gson gson;
	private long lastPost;

	@Inject
	DiscordDrops(CorClanConfig config, DrawManager drawManager, OkHttpClient httpClient, ScheduledExecutorService executor, Gson gson)
	{
		this.config = config;
		this.drawManager = drawManager;
		this.httpClient = httpClient;
		this.executor = executor;
		this.gson = gson;
	}

	/** Client thread: a clan broadcast about the logged-in player arrived. */
	public void onOwnBroadcast(String player, String broadcast)
	{
		String url = config.discordWebhookUrl() == null ? "" : config.discordWebhookUrl().trim();
		if (!config.discordDrops() || !DiscordWebhook.isWebhookUrl(url))
		{
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastPost < MIN_GAP_MILLIS)
		{
			log.debug("Discord post skipped, the last one was {} ms ago", now - lastPost);
			return;
		}
		lastPost = now;
		String payload = DiscordWebhook.payload(gson, player, broadcast);
		if (!config.discordScreenshot())
		{
			executor.execute(() -> send(url, payload, null));
			return;
		}
		// the next drawn frame already shows the broadcast in the chatbox
		drawManager.requestNextFrameListener(frame -> executor.execute(() ->
		{
			byte[] png = null;
			try
			{
				png = toPng(frame);
			}
			catch (IOException ex)
			{
				log.debug("Could not encode the Discord screenshot", ex);
			}
			send(url, payload, png);
		}));
	}

	private void send(String url, String payload, byte[] screenshot)
	{
		MultipartBody.Builder body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload_json", payload);
		if (screenshot != null)
		{
			body.addFormDataPart("files[0]", "cor-drop.png", RequestBody.create(PNG, screenshot));
		}
		Request request = new Request.Builder().url(url).post(body.build()).build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Discord webhook post failed", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (!response.isSuccessful())
				{
					log.debug("Discord webhook answered {}", response.code());
				}
				response.close();
			}
		});
	}

	private static byte[] toPng(Image image) throws IOException
	{
		BufferedImage buffered;
		if (image instanceof BufferedImage)
		{
			buffered = (BufferedImage) image;
		}
		else
		{
			buffered = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = buffered.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(buffered, "png", out);
		return out.toByteArray();
	}
}
