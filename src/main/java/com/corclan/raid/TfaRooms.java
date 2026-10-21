package com.corclan.raid;

import net.runelite.api.gameval.NpcID;

/**
 * Which encounter of The Fractured Archive a boss belongs to. A run is five encounters: the Archive Guardian, then a
 * choice of one boss from each of three pairs, then Ocellus; after him the Blade leads to the vault.
 */
public final class TfaRooms
{
	/** the encounters in a run */
	public static final int STEPS = 5;
	/** after Ocellus, at the chests */
	public static final String VAULT = "Vault";
	/** the clan server cuts a room name to this */
	public static final int MAX_ROOM = 24;

	private TfaRooms()
	{
	}

	/** @return the room this NPC stands in, or null when it doesn't tell */
	public static String roomOf(int npcId)
	{
		switch (npcId)
		{
			case NpcID.TFA_GUARDIAN_START:
			case NpcID.TFA_GUARDIAN:
			case NpcID.TFA_GUARDIAN_UNSHIELDED:
				return "Archive Guardian";
			case NpcID.TFA_EZEKIEL:
				return "Ezekiel";
			case NpcID.TFA_BORK:
			case NpcID.TFA_SMORK:
				return "Bork and the Smorks";
			case NpcID.TFA_MOIA:
				return "Moia";
			case NpcID.TFA_JELLY_BOSS:
				return "Dreadnoth";
			case NpcID.TFA_XARTH:
				return "Xarth";
			case NpcID.TFA_DAGONHAI_LEADER:
			case NpcID.TFA_DAGONHAI_LEADER_UNSHIELDED:
				return "Dagon'hai Elder";
			case NpcID.TFA_OCELLUS_INTRO:
			case NpcID.TFA_OCELLUS_P1:
			case NpcID.TFA_OCELLUS_P2:
			case NpcID.TFA_OCELLUS_P3:
				return "Ocellus";
			case NpcID.TFA_BLADE_NPC_VAULT_ENTRY:
			case NpcID.TFA_VAULT_EXIT_SMALL:
				return VAULT;
			default:
				return null;
		}
	}

	/** @return 1-5 for an encounter, 0 for the vault or a room we don't know */
	public static int stepOf(String room)
	{
		if (room == null)
		{
			return 0;
		}
		switch (room)
		{
			case "Archive Guardian":
				return 1;
			case "Ezekiel":
			case "Bork and the Smorks":
				return 2;
			case "Moia":
			case "Dreadnoth":
				return 3;
			case "Xarth":
			case "Dagon'hai Elder":
				return 4;
			case "Ocellus":
				return 5;
			default:
				return 0;
		}
	}

	/** "Room 3/5: Moia", "Vault" or the room as the server sent it; "" for none */
	public static String label(String room)
	{
		if (room == null || room.isEmpty())
		{
			return "";
		}
		int step = stepOf(room);
		return step > 0 ? "Room " + step + "/" + STEPS + ": " + room : room;
	}
}
