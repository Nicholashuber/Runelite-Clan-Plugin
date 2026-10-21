package com.corclan.raid;

import com.corclan.sync.SyncModels;
import com.google.gson.Gson;
import java.util.Collections;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TfaRoomsTest
{
	private static RaidListing listing(String json)
	{
		return RaidListing.board(new Gson().fromJson("{\"raids\":[" + json + "]}", SyncModels.RaidsResponse.class)).get(0);
	}

	@Test
	public void bossesTellTheRoom()
	{
		assertEquals("Archive Guardian", TfaRooms.roomOf(NpcID.TFA_GUARDIAN_UNSHIELDED));
		assertEquals("Bork and the Smorks", TfaRooms.roomOf(NpcID.TFA_SMORK));
		assertEquals("Dreadnoth", TfaRooms.roomOf(NpcID.TFA_JELLY_BOSS));
		assertEquals("Dagon'hai Elder", TfaRooms.roomOf(NpcID.TFA_DAGONHAI_LEADER));
		assertEquals("Ocellus", TfaRooms.roomOf(NpcID.TFA_OCELLUS_P3));
		assertEquals(TfaRooms.VAULT, TfaRooms.roomOf(NpcID.TFA_BLADE_NPC_VAULT_ENTRY));
		// minions and helpers don't move the room on
		assertNull(TfaRooms.roomOf(NpcID.TFA_MOIA_MINION));
		assertNull(TfaRooms.roomOf(NpcID.TFA_DAGONHAI_MINION_1));
	}

	@Test
	public void labelsNumberTheEncounters()
	{
		assertEquals("Room 1/5: Archive Guardian", TfaRooms.label("Archive Guardian"));
		assertEquals("Room 3/5: Moia", TfaRooms.label("Moia"));
		assertEquals("Room 4/5: Xarth", TfaRooms.label("Xarth"));
		assertEquals("Room 5/5: Ocellus", TfaRooms.label("Ocellus"));
		assertEquals("Vault", TfaRooms.label(TfaRooms.VAULT));
		assertEquals("Somewhere new", TfaRooms.label("Somewhere new"));
		assertEquals("", TfaRooms.label(null));
	}

	@Test
	public void boardShowsTheRoomOnlyWhileRaiding()
	{
		RaidListing in = listing("{\"rsn\":\"Bob\",\"raid\":\"tfa\",\"filled\":4,\"size\":5,\"raiding\":true,\"room\":\"<b>Moia</b>\"}");
		assertEquals("Moia", in.room);
		assertEquals("Room 3/5: Moia", in.roomLabel());
		assertEquals("CoR raid finder: TFA 4/5 raiding at Moia (Bob). See the CoR side panel.",
			RaidLines.advert(Collections.singletonList(in)));
		assertEquals("", listing("{\"rsn\":\"Bob\",\"raid\":\"tfa\",\"filled\":4,\"size\":5,\"room\":\"Moia\"}").room);
		// older servers send no room
		assertEquals("", listing("{\"rsn\":\"Bob\",\"raid\":\"tfa\",\"filled\":4,\"size\":5,\"raiding\":true}").room);
	}
}
