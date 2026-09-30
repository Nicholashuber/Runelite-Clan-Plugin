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

	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	ClanApi(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	/**
	 * @param done receives true when the batch was handled (stored, or refused as invalid and not worth
	 *             retrying) and false when it should be retried later (network error, rate limit, server error)
	 */
	public void sendReport(SyncModels.ReportPayload payload, Consumer<Boolean> done)
	{
		Request request = new Request.Builder()
			.url(BASE_URL.resolve("v1/reports"))
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CoR sync: sending report failed", e);
				done.accept(false);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					int code = r.code();
					if (r.isSuccessful())
					{
						done.accept(true);
					}
					else if (code == 400 || code == 403)
					{
						// invalid batch or not in the CoR clan: retrying would not help
						log.debug("CoR sync: report refused with {}", code);
						done.accept(true);
					}
					else
					{
						log.debug("CoR sync: report failed with {}", code);
						done.accept(false);
					}
				}
			}
		});
	}

	/** Sends the clan's rank names (rank number + title only) so admins can pick an icon per rank. */
	public void sendRanks(SyncModels.RanksPayload payload)
	{
		Request request = new Request.Builder()
			.url(BASE_URL.resolve("v1/ranks"))
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CoR sync: sending ranks failed", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					log.debug("CoR sync: ranks sent, {}", r.code());
				}
			}
		});
	}

	public void fetchLeaderboard(Consumer<SyncModels.Leaderboard> onSuccess)
	{
		get("v1/leaderboard?limit=5", SyncModels.Leaderboard.class, onSuccess);
	}

	public void fetchCosmetics(Consumer<SyncModels.CosmeticsResponse> onSuccess)
	{
		get("v1/cosmetics", SyncModels.CosmeticsResponse.class, onSuccess);
	}

	/** Chat icon images set on the admin page (small PNGs, base64). */
	public void fetchIcons(Consumer<SyncModels.IconsResponse> onSuccess)
	{
		get("v1/icons", SyncModels.IconsResponse.class, onSuccess);
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
