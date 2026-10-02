package com.corclan.party;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * The glow effects a party member shows on themselves (GlowEffect ids, locked ones included). Receivers
 * only draw the ones the sender's clan rank, as the receiver sees it, unlocks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CorGlowPicks extends PartyMemberMessage
{
	private List<String> glows;
}
