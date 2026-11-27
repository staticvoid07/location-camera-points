package com.locationcamerapoints;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Highlights the square area each enabled camera point covers, plus its centre tile.
 */
class LocationCameraPointsOverlay extends Overlay
{
	private final Client client;
	private final LocationCameraPointsPlugin plugin;
	private final LocationCameraPointsConfig config;

	@Inject
	LocationCameraPointsOverlay(Client client, LocationCameraPointsPlugin plugin, LocationCameraPointsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		WorldView wv = client.getTopLevelWorldView();
		Color fill = config.overlayColor();
		Color border = new Color(fill.getRed(), fill.getGreen(), fill.getBlue());

		for (CameraPoint point : plugin.getPoints())
		{
			if (!point.isEnabled())
			{
				continue;
			}

			// A point inside an instance template can map to several places in the loaded scene
			for (WorldPoint instancePoint : WorldPoint.toLocalInstance(wv, point.toWorldPoint()))
			{
				LocalPoint centre = LocalPoint.fromWorld(wv, instancePoint);
				if (centre == null)
				{
					continue;
				}

				int size = point.getRadius() * 2 + 1;
				Polygon area = Perspective.getCanvasTileAreaPoly(client, centre, size);
				if (area != null)
				{
					OverlayUtil.renderPolygon(graphics, area, border, fill, new BasicStroke(1f));
				}

				Polygon tile = Perspective.getCanvasTilePoly(client, centre);
				if (tile != null)
				{
					OverlayUtil.renderPolygon(graphics, tile, border, fill, new BasicStroke(2f));
				}
			}
		}
		return null;
	}
}
