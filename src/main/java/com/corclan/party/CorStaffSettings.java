package com.corclan.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * The clan's member icons and rank icons, as last edited by a staff member. Only accepted when the sender
 * holds Administrator rank or higher in the receiver's own clan; the newest edit wins.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CorStaffSettings extends PartyMemberMessage
{
	/** when the staff member last changed either list, epoch millis */
	private long updatedAt;
	private String memberIcons;
	private String rankIcons;
}
