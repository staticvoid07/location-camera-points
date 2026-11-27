package com.locationcamerapoints;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The direction the camera should face when a point triggers.
 * <p>
 * The camera is turned with the game's own compass script (the one behind the compass's
 * "Look North/East/South/West" options), which takes 1-4 for north, east, south and west.
 */
@Getter
@RequiredArgsConstructor
public enum CameraDirection
{
	NORTH("North", 1),
	EAST("East", 2),
	SOUTH("South", 3),
	WEST("West", 4),
	KEEP("Don't change", 0);

	/**
	 * Client script that rotates the camera to a compass direction.
	 */
	public static final int COMPASS_ANGLE_SCRIPT_ID = 1050;

	private final String displayName;
	private final int scriptValue;

	/**
	 * @param yaw camera yaw in client units (0-2047, a full turn), where 0 faces south,
	 * 512 west, 1024 north and 1536 east
	 * @return the compass direction closest to the given yaw
	 */
	public static CameraDirection fromYaw(int yaw)
	{
		int quadrant = Math.floorMod(Math.round(yaw / 512f), 4);
		switch (quadrant)
		{
			case 1:
				return WEST;
			case 2:
				return NORTH;
			case 3:
				return EAST;
			default:
				return SOUTH;
		}
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
