package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.corclan.CorClanConfig;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import org.junit.Test;

/** With "Sync with clan server" off, no request leaves the plugin and every caller still hears back. */
public class ClanApiOffTest
{
	private final AtomicInteger requests = new AtomicInteger();
	private final OkHttpClient http = new OkHttpClient.Builder()
		.addInterceptor(chain ->
		{
			requests.incrementAndGet();
			throw new AssertionError("a request went out with sync off: " + chain.request().url());
		})
		.build();
	private final CorClanConfig syncOff = new CorClanConfig()
	{
		@Override
		public boolean syncEnabled()
		{
			return false;
		}
	};
	private final ClanApi api = new ClanApi(http, new Gson(), syncOff);
	private final SyncModels.Reporter me = new SyncModels.Reporter("-1234567890123", "Lavasockz", "C o R");

	@Test
	public void nothingIsSentAndCallersAreToldItFailed()
	{
		AtomicReference<ClanApi.Outcome> report = new AtomicReference<>();
		api.sendReport(new SyncModels.ReportPayload(me, 0, Collections.emptyList()), report::set);
		assertEquals(ClanApi.Outcome.RETRY, report.get());

		AtomicReference<ClanApi.Outcome> gm = new AtomicReference<>();
		api.sendGm(new SyncModels.GmPayload(me, "gm"), (outcome, result) -> gm.set(outcome));
		assertEquals(ClanApi.Outcome.RETRY, gm.get());

		AtomicReference<String> ping = new AtomicReference<>();
		api.pingRaid(new SyncModels.RaidStopPayload(me), ping::set);
		assertNotNull(ping.get());

		api.fetchClan(response ->
		{
			throw new AssertionError("no answer can arrive with sync off");
		});
		api.fetchRaids(response ->
		{
			throw new AssertionError("no answer can arrive with sync off");
		});
		assertEquals(0, requests.get());
	}
}
