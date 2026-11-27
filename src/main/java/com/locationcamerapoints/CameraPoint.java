package com.locationcamerapoints;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * A saved location that changes the camera when the player walks into its radius.
 * Serialized to JSON in the plugin's config, so field names are part of the save format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraPoint
{
	private String name;
	private int x;
	private int y;
	private int plane;
	/**
	 * Distance in tiles (square area, like the game's own "within N tiles") around the point.
	 */
	private int radius;
	private CameraDirection direction;
	private boolean zoomEnabled;
	private int zoom;
	private boolean enabled;

	public WorldPoint toWorldPoint()
	{
		return new WorldPoint(x, y, plane);
	}

	/**
	 * @return the tile distance from the player to this point, or -1 if the player is outside its radius
	 */
	public int distanceIfInside(WorldPoint player)
	{
		if (player.getPlane() != plane)
		{
			return -1;
		}
		int distance = Math.max(Math.abs(player.getX() - x), Math.abs(player.getY() - y));
		return distance <= radius ? distance : -1;
	}

	public CameraDirection getDirection()
	{
		return direction == null ? CameraDirection.KEEP : direction;
	}
}
