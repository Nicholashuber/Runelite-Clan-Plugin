package com.corclan.party;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/** One CoR party member's view of the gz counts (their own counts merged with what the party sent them). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CorGzCounts extends PartyMemberMessage
{
	/** epoch millis of the Sunday 00:00 UTC the weekly counts belong to */
	private long weekStart;
	private Map<String, Integer> weekly = new HashMap<>();
	private Map<String, Integer> given = new HashMap<>();
	private Map<String, Integer> received = new HashMap<>();
}
