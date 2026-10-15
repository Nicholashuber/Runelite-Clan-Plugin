package com.corclan.glow;

import com.corclan.icons.MemberCosmetics;
import com.corclan.sync.SyncModels;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Who wears the Kephri dung, from the clan server's list. A shame is a light switch on the server: it stays on until a
 * leader takes it off with ::unshame. The plugin asks for the list every 10 seconds, and a name it lists counts as
 * shamed for {@link #FRESH_MILLIS} after the list arrived. So the dung stays on while the server says so, and stops
 * soon after a leader unshames or the server can't be reached. Immutable, so it can be built on an OkHttp thread and
 * read on the client thread. Pure Java, unit tested.
 */
public final class Shames
{
	/** How long a listed name counts as shamed after the list arrived: a few missed refreshes, then it stops. */
	public static final long FRESH_MILLIS = 60_000L;
	/** Same cap as the server, so a broken answer cannot grow memory. */
	static final int MAX_SHAMES = 50;

	public static final Shames NONE = new Shames(Collections.emptyMap());

	/** {@link MemberCosmetics#key} -> epoch millis this client stops showing the dung unless a newer list says so */
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
			if (!key.isEmpty())
			{
				until.put(key, receivedAt + FRESH_MILLIS);
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

	/** A copy with {@code name} shamed from {@code now}: our own ::shame, before the next refresh confirms it. */
	public Shames with(String name, long now)
	{
		Map<String, Long> next = new HashMap<>(until);
		next.put(MemberCosmetics.key(name), now + FRESH_MILLIS);
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
