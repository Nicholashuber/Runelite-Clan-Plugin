package com.corclan.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/** A CoR party member's position for the clan map. The sender's name comes from the party, not the message. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CorLocation extends PartyMemberMessage
{
	private int world;
	private int x;
	private int y;
	private int plane;
	private boolean wilderness;
	/** true when the sender stopped sharing (turned it off, entered an instance or the Wilderness) */
	private boolean stopped;
}
