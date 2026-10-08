package com.corclan.sync;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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
 * Talks to the CoR clan server. Only used when the player turns on "Sync with clan server".
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

	@Inject
	ClanApi(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
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

	/** Everyone's gz counts, icons, titles and glow picks, plus the past weekly podiums. */
	public void fetchClan(Consumer<SyncModels.ClanResponse> onSuccess)
	{
		get("v1/clan", SyncModels.ClanResponse.class, onSuccess);
	}

	/** Chat icon images set on the admin page (small PNGs, base64) and the icon picked per clan rank. */
	public void fetchIcons(Consumer<SyncModels.IconsResponse> onSuccess)
	{
		get("v1/icons", SyncModels.IconsResponse.class, onSuccess);
	}

	private void post(String path, Object payload, Consumer<Outcome> done)
	{
		Request request = new Request.Builder()
			.url(BASE_URL.resolve(path))
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();

		http.newCall(request).enqueue(new Callback()
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
		});
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

	private <T> void get(String path, Class<T> type, Consumer<T> onSuccess)
	{
		Request request = new Request.Builder().url(BASE_URL.resolve(path)).get().build();
		http.newCall(request).enqueue(new Callback()
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
		});
	}
}
