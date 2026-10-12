package com.corclan.glow;

import com.corclan.icons.MemberCosmetics;
import com.corclan.sync.SyncModels;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Who wears the Kephri dung and until when, on this client's clock. Built from the clan server's list (which
 * says how many seconds each has left, so clocks never need to agree). Immutable, so it can be built on an
 * OkHttp thread and read on the client thread. Pure Java, unit tested.
 */
public final class Shames
{
	/** How long a shame lasts; the clan server uses the same five minutes. */
	public static final long DURATION_MILLIS = 5 * 60_000L;
	/** Same cap as the server, so a broken answer cannot grow memory. */
	static final int MAX_SHAMES = 50;

	public static final Shames NONE = new Shames(Collections.emptyMap());

	/** {@link MemberCosmetics#key} -> epoch millis the shame wears off */
	private final Map<String, Long> until;

	private Shames(Map<String, Long> until)
	{
		this.until = Collections.unmodifiableMap(until);
	}

	/**
	 * @param response   the clan server's {@code GET /v1/shames} answer
	 * @param receivedAt when it arrived, epoch millis
	 */
	public static Shames of(SyncModels.ShamesResponse response, long receivedAt)
	{
		Map<String, Long> until = new HashMap<>();
		for (SyncModels.ActiveShame shame : response.getShames())
		{
			if (shame == null || until.size() >= MAX_SHAMES)
			{
				continue;
			}
			String key = MemberCosmetics.key(shame.getRsn());
			// never longer than a shame can last, whatever the answer says
			long left = Math.min(Math.max(0, shame.getSecondsLeft()) * 1000L, DURATION_MILLIS);
			if (!key.isEmpty() && left > 0)
			{
				until.put(key, receivedAt + left);
			}
		}
		return new Shames(until);
	}

	/** True while {@code name} wears the dung. */
	public boolean isShamed(String name, long now)
	{
		Long end = until.get(MemberCosmetics.key(name));
		return end != null && now < end;
	}

	public boolean isEmpty(long now)
	{
		for (long end : until.values())
		{
			if (now < end)
			{
				return false;
			}
		}
		return true;
	}

	/** A copy with {@code name} shamed for the full duration from {@code now}: our own ::shame, before the next refresh. */
	public Shames with(String name, long now)
	{
		Map<String, Long> next = new HashMap<>(until);
		next.put(MemberCosmetics.key(name), now + DURATION_MILLIS);
		return new Shames(next);
	}

	/** A copy without {@code name}: our own ::unshame, before the next refresh. */
	public Shames without(String name)
	{
		Map<String, Long> next = new HashMap<>(until);
		next.remove(MemberCosmetics.key(name));
		return new Shames(next);
	}
}
