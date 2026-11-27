package com.locationcamerapoints;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LocationCameraPointsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LocationCameraPointsPlugin.class);
		RuneLite.main(args);
	}
}
