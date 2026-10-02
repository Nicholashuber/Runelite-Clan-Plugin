package com.corclan.icons;

import com.corclan.gz.GzStats;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This week's top gz givers get a gold, silver or bronze trophy in clan chat. Same order as the weekly
 * podium in the panel: most gz's first, ties broken by name.
 */
public final class WeeklyTrophies
{
	/** Icon keys for 1st, 2nd and 3rd place. */
	public static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
		"week_1", "week_2", "week_3"));

	private WeeklyTrophies()
	{
	}

	/** @return {@link MemberCosmetics#key} -> trophy icon key, for up to three givers with at least one gz */
	public static Map<String, String> of(GzStats weekly)
	{
		Map<String, String> out = new HashMap<>();
		List<Map.Entry<String, Integer>> top = GzStats.top(weekly.getGiven(), KEYS.size());
		for (int i = 0; i < top.size(); i++)
		{
			if (top.get(i).getValue() > 0)
			{
				out.put(MemberCosmetics.key(top.get(i).getKey()), KEYS.get(i));
			}
		}
		return Collections.unmodifiableMap(out);
	}
}
