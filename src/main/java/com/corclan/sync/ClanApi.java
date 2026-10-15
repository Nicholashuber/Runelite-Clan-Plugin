package com.corclan.sync;

import com.corclan.CorClanConfig;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the CoR clan server. Only used when the player turns on "Sync with clan server": every request goes
 * through {@link #enqueue}, which checks that setting first and sends nothing while it is off.
 * Every call is asynchronous on RuneLite's shared OkHttp client; callbacks run on OkHttp's threads.
 */
@Slf4j
@Singleton
public class ClanApi
{
	static final HttpUrl BASE_URL = HttpUrl.get("https://cor-clan-api-production.up.railway.app/");
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	/** error bodies are a few bytes of JSON; never read more than this of one */
	private static final long MAX_ERROR_BYTES = 1024L;

	/** What became of something we sent. */
	public enum Outcome
	{
		/** stored */
		OK,
		/** network error, rate limit or server error: worth sending again later */
		RETRY,
		/** the server will never take this request as it is; sending it again would not help */
		REFUSED,
		/** another account owns this character name until CoR staff unlink it */
		NAME_TAKEN,
		/** this character is not in the CoR clan */
		WRONG_CLAN;

		/** True when the server refuses everything from this player, so nothing more should be sent. */
		public boolean blocksReporter()
		{
			return this == NAME_TAKEN || this == WRONG_CLAN;
		}
	}

	private final OkHttpClient http;
	private final Gson gson;
	private final CorClanConfig config;

	@Inject
	ClanApi(OkHttpClient http, Gson gson, CorClanConfig config)
	{
		this.http = http;
		this.gson = gson;
		this.config = config;
	}

	/**
	 * The only place a request leaves the plugin. With "Sync with clan server" off nothing is sent; {@code whenOff}
	 * runs instead, so whoever waits for an answer is told the request failed.
	 */
	private void enqueue(Request request, Callback callback, Runnable whenOff)
	{
		if (!config.syncEnabled())
		{
			whenOff.run();
			return;
		}
		http.newCall(request).enqueue(callback);
	}

	/**
	 * @param status HTTP status of the answer to a POST
	 * @param error  the {@code error} code in its body, or null if there was none
	 */
	static Outcome classify(int status, String error)
	{
		if (status >= 200 && status < 300)
		{
			return Outcome.OK;
		}
		if ("name_taken".equals(error) || (error == null && status == 409))
		{
			return Outcome.NAME_TAKEN;
		}
		if ("wrong_clan".equals(error) || (error == null && status == 403))
		{
			return Outcome.WRONG_CLAN;
		}
		// rate limited, timed out or a server fault
		if (status == 429 || status == 408 || status >= 500)
		{
			return Outcome.RETRY;
		}
		return Outcome.REFUSED;
	}

	/** Sends the reporter's own gz's and the clan broadcasts about them. */
	public void sendReport(SyncModels.ReportPayload payload, Consumer<Outcome> done)
	{
		post("v1/reports", payload, done);
	}

	/** Sends the reporter's own clan rank and glow picks. */
	public void sendProfile(SyncModels.ProfilePayload payload, Consumer<Outcome> done)
	{
		post("v1/profile", payload, done);
	}

	/** Sends the clan's rank names (rank number + title only) so admins can pick an icon per rank. */
	public void sendRanks(SyncModels.RanksPayload payload, Consumer<Outcome> done)
	{
		post("v1/ranks", payload, done);
	}

	/**
	 * Our own daily gm: the server pays the gm bonus for the first one each UTC day.
	 * @param done the outcome, and on OK the server's answer (null if it could not be read)
	 */
	public void sendGm(SyncModels.GmPayload payload, BiConsumer<Outcome, SyncModels.GmResult> done)
	{
		Request request = new Request.Builder()
			.url(BASE_URL.resolve("v1/gm"))
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();
		enqueue(request, new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CoR sync: POST v1/gm failed", e);
				done.accept(Outcome.RETRY, null);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				Outcome outcome;
				SyncModels.GmResult result = null;
				try (Response r = response)
				{
					outcome = classify(r.code(), r.isSuccessful() ? null : errorCode(r));
					ResponseBody body = r.body();
					if (outcome == Outcome.OK && body != null)
					{
						result = gson.fromJson(body.charStream(), SyncModels.GmResult.class);
					}
				}
				catch (JsonParseException e)
				{
					log.debug("CoR sync: unreadable answer from v1/gm", e);
					outcome = Outcome.OK;
				}
				done.accept(outcome, result);
			}
		}, () -> done.accept(Outcome.RETRY, null));
	}

	/** Puts up or updates our own raid finder listing. */
	public void sendRaid(SyncModels.RaidPayload payload, Consumer<Outcome> done)
	{
		post("v1/raids", payload, done);
	}

	/** Takes our own raid finder listing down. */
	public void stopRaid(SyncModels.RaidStopPayload payload, Consumer<Outcome> done)
	{
		post("v1/raids/stop", payload, done);
	}

	/**
	 * Asks the clan server to post our open raid group in the clan Discord's looking-for-group channel.
	 * @param done on an OkHttp thread: null when Discord took it, else the line to tell the player
	 */
	public void pingRaid(SyncModels.RaidStopPayload payload, Consumer<String> done)
	{
		Request request = new Request.Builder()
			.url(BASE_URL.resolve("v1/raids/ping"))
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();
		enqueue(request, new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CoR sync: POST v1/raids/ping failed", e);
				done.accept("couldn't reach the clan server, try again");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					if (r.isSuccessful())
					{
						done.accept(null);
						return;
					}
					SyncModels.ErrorBody body = null;
					try
					{
						body = gson.fromJson(r.peekBody(MAX_ERROR_BYTES).string(), SyncModels.ErrorBody.class);
					}
					catch (IOException | JsonParseException e)
					{
						log.debug("CoR sync: unreadable answer from v1/raids/ping", e);
					}
					done.accept(pingProblem(body == null ? null : body.getError(), body == null ? 0 : body.getRetryAfterSeconds()));
				}
			}
		}, () -> done.accept("turn on Sync with clan server first"));
	}

	/** What to tell the player when a Discord ping is refused. */
	static String pingProblem(String error, int retryAfterSeconds)
	{
		if ("ping_cooldown".equals(error))
		{
			int minutes = Math.max(1, (retryAfterSeconds + 59) / 60);
			return "you can ping Discord again in " + minutes + (minutes == 1 ? " minute" : " minutes");
		}
		if ("no_group".equals(error))
		{
			return "put your group up first";
		}
		if ("discord_off".equals(error))
		{
			return "Discord pings aren't set up on the clan server yet";
		}
		if ("discord_failed".equals(error))
		{
			return "Discord didn't take the ping, try again in a moment";
		}
		return "the clan server refused the ping";
	}

	/** Every open raid finder listing; skips RuneLite's HTTP cache, since the board changes by the minute. */
	public void fetchRaids(Consumer<SyncModels.RaidsResponse> onSuccess)
	{
		get("v1/raids", SyncModels.RaidsResponse.class, CacheControl.FORCE_NETWORK, onSuccess);
	}

	/** A clan leader sets the announcement banner (an empty text clears it). */
	public void sendAnnouncement(SyncModels.AnnouncementPayload payload, Consumer<Outcome> done)
	{
		post("v1/announcement", payload, done);
	}

	/** Everyone's gz counts, icons, titles and glow picks, the past weekly podiums and the announcement banner. */
	public void fetchClan(Consumer<SyncModels.ClanResponse> onSuccess)
	{
		get("v1/clan", SyncModels.ClanResponse.class, null, onSuccess);
	}

	/** {@link #fetchClan}, skipping RuneLite's HTTP cache: right after our own change, so it shows at once. */
	public void fetchClanNow(Consumer<SyncModels.ClanResponse> onSuccess)
	{
		get("v1/clan", SyncModels.ClanResponse.class, CacheControl.FORCE_NETWORK, onSuccess);
	}

	/** Chat icon images set on the admin page (small PNGs, base64) and the icon picked per clan rank. */
	public void fetchIcons(Consumer<SyncModels.IconsResponse> onSuccess)
	{
		get("v1/icons", SyncModels.IconsResponse.class, null, onSuccess);
	}

	private void post(String path, Object payload, Consumer<Outcome> done)
	{
		Request request = new Request.Builder()
			.url(BASE_URL.resolve(path))
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();

		enqueue(request, new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CoR sync: POST {} failed", path, e);
				done.accept(Outcome.RETRY);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				Outcome outcome;
				try (Response r = response)
				{
					outcome = classify(r.code(), r.isSuccessful() ? null : errorCode(r));
					log.debug("CoR sync: POST {} returned {} ({})", path, r.code(), outcome);
				}
				done.accept(outcome);
			}
		}, () -> done.accept(Outcome.RETRY));
	}

	/** The {@code error} code of a refused request, or null if the body has none. */
	private String errorCode(Response r)
	{
		try
		{
			SyncModels.ErrorBody body = gson.fromJson(r.peekBody(MAX_ERROR_BYTES).string(), SyncModels.ErrorBody.class);
			return body == null ? null : body.getError();
		}
		catch (IOException | JsonParseException e)
		{
			return null;
		}
	}

	/** @param cache null for the usual caching */
	private <T> void get(String path, Class<T> type, CacheControl cache, Consumer<T> onSuccess)
	{
		Request.Builder builder = new Request.Builder().url(BASE_URL.resolve(path)).get();
		if (cache != null)
		{
			builder.cacheControl(cache);
		}
		Request request = builder.build();
		enqueue(request, new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CoR sync: GET {} failed", path, e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					ResponseBody body = r.body();
					if (!r.isSuccessful() || body == null)
					{
						log.debug("CoR sync: GET {} returned {}", path, r.code());
						return;
					}
					T result = gson.fromJson(body.charStream(), type);
					if (result != null)
					{
						onSuccess.accept(result);
					}
				}
				catch (JsonParseException e)
				{
					log.debug("CoR sync: unreadable response from {}", path, e);
				}
			}
		}, () -> { });
	}
}
