package com.corclan.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * A CoR party member's own gz totals, as their client counted them. Only ever about the sender (who is
 * identified by the party, not by the message): nobody reports numbers about other players.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CorGzCounts extends PartyMemberMessage
{
	/** epoch millis of the Sunday 00:00 UTC the weekly count belongs to */
	private long weekStart;
	/** gz's they gave, all time */
	private int given;
	/** gz's they received, all time */
	private int received;
	/** gz's they gave this week */
	private int weekly;
}
