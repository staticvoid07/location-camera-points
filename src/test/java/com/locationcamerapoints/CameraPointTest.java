package com.locationcamerapoints;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class CameraPointTest
{
	private final CameraPoint point = new CameraPoint("Bank", 3200, 3200, 0, 2, CameraDirection.EAST, true, 600, true, true);

	@Test
	public void insideRadiusReturnsDistance()
	{
		assertEquals(0, point.distanceIfInside(new WorldPoint(3200, 3200, 0)));
		assertEquals(2, point.distanceIfInside(new WorldPoint(3202, 3198, 0)));
	}

	@Test
	public void outsideRadiusOrOtherPlaneIsMinusOne()
	{
		assertEquals(-1, point.distanceIfInside(new WorldPoint(3203, 3200, 0)));
		assertEquals(-1, point.distanceIfInside(new WorldPoint(3200, 3200, 1)));
	}

	@Test
	public void directionFromYawRoundsToNearestCardinal()
	{
		assertEquals(CameraDirection.SOUTH, CameraDirection.fromYaw(0));
		assertEquals(CameraDirection.SOUTH, CameraDirection.fromYaw(2000));
		assertEquals(CameraDirection.WEST, CameraDirection.fromYaw(600));
		assertEquals(CameraDirection.NORTH, CameraDirection.fromYaw(1024));
		assertEquals(CameraDirection.EAST, CameraDirection.fromYaw(1500));
	}

	@Test
	public void survivesJsonRoundTrip()
	{
		Gson gson = new Gson();
		assertEquals(point, gson.fromJson(gson.toJson(point), CameraPoint.class));
	}

	@Test
	public void missingDirectionDefaultsToKeep()
	{
		CameraPoint loaded = new Gson().fromJson("{\"x\":1,\"y\":2}", CameraPoint.class);
		assertEquals(CameraDirection.KEEP, loaded.getDirection());
	}
}
