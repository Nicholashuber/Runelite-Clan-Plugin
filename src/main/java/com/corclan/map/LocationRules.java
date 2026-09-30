package com.corclan.map;

/** When a player's own position may be sent to the clan map. Pure logic, unit tested. */
public final class LocationRules
{
	public enum Decision
	{
		/** send the position */
		SEND,
		/** inside the Wilderness and "Share in Wilderness" is off */
		HIDE_WILDERNESS,
		/** inside an instance (raids, some bosses), where coordinates are not real map tiles */
		HIDE_INSTANCE
	}

	private LocationRules()
	{
	}

	public static Decision decide(boolean inWilderness, boolean inInstance, boolean wildernessAllowed)
	{
		if (inInstance)
		{
			return Decision.HIDE_INSTANCE;
		}
		if (inWilderness && !wildernessAllowed)
		{
			return Decision.HIDE_WILDERNESS;
		}
		return Decision.SEND;
	}
}
