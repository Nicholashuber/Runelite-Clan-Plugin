package com.corclan.clan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Clan members grouped by in-game clan rank, highest rank first; within a rank, online members
 * first, then by name. Pure Java so it can be unit tested; the plugin feeds it from ClanSettings.
 */
public final class ClanRoster
{
	/** One clan member as the roster needs it. */
	public static final class Member
	{
		public final String name;
		/** RuneLite ClanRank number: 126 owner, 125 deputy owner, 100 admin, ... */
		public final int rank;
		/** The clan's own title for that rank, e.g. "Captain"; null if the game didn't give one */
		public final String title;
		public final boolean online;

		public Member(String name, int rank, String title, boolean online)
		{
			this.name = name;
			this.rank = rank;
			this.title = title;
			this.online = online;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Member))
			{
				return false;
			}
			Member m = (Member) o;
			return rank == m.rank && online == m.online && name.equals(m.name) && Objects.equals(title, m.title);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, rank, title, online);
		}
	}

	/** Everyone holding one rank. */
	public static final class RankGroup
	{
		public final int rank;
		public final String title;
		public final List<Member> members;

		RankGroup(int rank, String title, List<Member> members)
		{
			this.rank = rank;
			this.title = title;
			this.members = Collections.unmodifiableList(members);
		}

		public int onlineCount()
		{
			return (int) members.stream().filter(m -> m.online).count();
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof RankGroup))
			{
				return false;
			}
			RankGroup g = (RankGroup) o;
			return rank == g.rank && Objects.equals(title, g.title) && members.equals(g.members);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(rank, title, members);
		}
	}

	private ClanRoster()
	{
	}

	/** @return rank groups, highest rank first; empty if there are no members */
	public static List<RankGroup> group(List<Member> members)
	{
		Map<Integer, List<Member>> byRank = new TreeMap<>(Comparator.reverseOrder());
		for (Member m : members)
		{
			if (m != null && m.name != null && !m.name.isEmpty())
			{
				byRank.computeIfAbsent(m.rank, r -> new ArrayList<>()).add(m);
			}
		}

		List<RankGroup> groups = new ArrayList<>();
		byRank.forEach((rank, list) ->
		{
			list.sort(Comparator.comparing((Member m) -> !m.online)
				.thenComparing(m -> m.name, String.CASE_INSENSITIVE_ORDER));
			groups.add(new RankGroup(rank, titleOf(rank, list), list));
		});
		return groups;
	}

	private static String titleOf(int rank, List<Member> members)
	{
		for (Member m : members)
		{
			if (m.title != null && !m.title.isEmpty())
			{
				return m.title;
			}
		}
		return "Rank " + rank;
	}

	public static int total(List<RankGroup> groups)
	{
		return groups.stream().mapToInt(g -> g.members.size()).sum();
	}

	public static int online(List<RankGroup> groups)
	{
		return groups.stream().mapToInt(RankGroup::onlineCount).sum();
	}
}
