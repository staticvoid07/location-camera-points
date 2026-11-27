package com.locationcamerapoints;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(LocationCameraPointsConfig.GROUP)
public interface LocationCameraPointsConfig extends Config
{
	String GROUP = "locationcamerapoints";
	String POINTS_KEY = "points";

	@Range(max = 50)
	@Units(" tiles")
	@ConfigItem(
		keyName = "defaultRadius",
		name = "Default radius",
		description = "Radius used for newly created camera points. Each point's radius can be changed in the side panel.",
		position = 1
	)
	default int defaultRadius()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "chatMessage",
		name = "Chat message on trigger",
		description = "Print a game message when a camera point changes the camera",
		position = 2
	)
	default boolean chatMessage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show trigger areas",
		description = "Highlight the area covered by each enabled camera point",
		position = 3
	)
	default boolean showOverlay()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayColor",
		name = "Area color",
		description = "Color used to highlight camera point areas",
		position = 4
	)
	default Color overlayColor()
	{
		return new Color(0, 180, 255, 60);
	}

	@ConfigItem(
		keyName = "showTileMenu",
		name = "Shift right-click menu",
		description = "Add an 'Add camera point' option when shift right-clicking a tile",
		position = 5
	)
	default boolean showTileMenu()
	{
		return true;
	}
}
