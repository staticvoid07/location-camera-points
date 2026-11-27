package com.locationcamerapoints;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import com.locationcamerapoints.ui.CameraPointsPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Location Camera Points",
	description = "Save camera positions for locations and restore them when you return",
	tags = {"camera", "location", "zoom", "yaw", "pitch", "angle"}
)
public class LocationCameraPointsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private LocationCameraPointsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LocationCameraPointsOverlay overlay;

	@Inject
	private Gson gson;

	/**
	 * Read on the client thread (game tick, overlay) and modified on the Swing thread (panel),
	 * hence copy-on-write.
	 */
	@Getter
	private final List<CameraPoint> points = new CopyOnWriteArrayList<>();

	/**
	 * The point the player is currently standing in. The camera is only changed when this changes,
	 * so the player can still move the camera freely while inside an area.
	 */
	private CameraPoint activePoint;

	private CameraPointsPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		loadPoints();

		panel = new CameraPointsPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Location Camera Points")
			.icon(createIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
		points.clear();
		activePoint = null;
		panel = null;
		navButton = null;
	}

	@Provides
	LocationCameraPointsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LocationCameraPointsConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			// Re-apply the point on the next login even if the player logs in where they logged out
			activePoint = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		WorldPoint location = getPlayerLocation();
		if (location == null)
		{
			return;
		}

		CameraPoint best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (CameraPoint point : points)
		{
			if (!point.isEnabled())
			{
				continue;
			}
			int distance = point.distanceIfInside(location);
			if (distance >= 0 && distance < bestDistance)
			{
				best = point;
				bestDistance = distance;
			}
		}

		if (best != activePoint)
		{
			activePoint = best;
			if (best != null)
			{
				applyCamera(best);
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.showTileMenu() || !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		MenuAction type = event.getMenuEntry().getType();
		if (type != MenuAction.WALK && type != MenuAction.SET_HEADING)
		{
			return;
		}

		WorldView wv = client.getWorldView(event.getMenuEntry().getWorldViewId());
		if (wv == null)
		{
			return;
		}
		Tile tile = wv.getSelectedSceneTile();
		if (tile == null)
		{
			return;
		}

		WorldPoint target = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
		client.getMenu().createMenuEntry(-1)
			.setOption("Add camera point")
			.setTarget("Tile")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> addPointAt(target));
	}

	/**
	 * Applies a point's camera settings. Must be called on the client thread.
	 */
	void applyCamera(CameraPoint point)
	{
		CameraDirection direction = point.getDirection();
		if (direction != CameraDirection.KEEP)
		{
			client.runScript(CameraDirection.COMPASS_ANGLE_SCRIPT_ID, direction.getScriptValue());
		}
		if (point.isZoomEnabled())
		{
			client.runScript(ScriptID.CAMERA_DO_ZOOM, point.getZoom(), point.getZoom());
		}

		if (config.chatMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"Camera point applied: " + displayName(point), null);
		}
	}

	// ---- Panel API (called on the Swing thread) ----

	public void testPoint(CameraPoint point)
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				applyCamera(point);
			}
		});
	}

	/**
	 * Creates a point at the player's location using the current camera direction and zoom.
	 */
	public void addPointAtPlayer()
	{
		clientThread.invoke(() ->
		{
			WorldPoint location = getPlayerLocation();
			if (location != null)
			{
				addPointAt(location);
			}
		});
	}

	/**
	 * Reads the player's location on the client thread and hands it to the callback on the Swing thread.
	 */
	public void fetchPlayerLocation(Consumer<WorldPoint> callback)
	{
		clientThread.invoke(() ->
		{
			WorldPoint location = getPlayerLocation();
			if (location != null)
			{
				SwingUtilities.invokeLater(() -> callback.accept(location));
			}
		});
	}

	/**
	 * Reads the current camera zoom on the client thread and hands it to the callback on the Swing thread.
	 */
	public void fetchCurrentZoom(Consumer<Integer> callback)
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				int zoom = getCurrentZoom();
				SwingUtilities.invokeLater(() -> callback.accept(zoom));
			}
		});
	}

	public void removePoint(CameraPoint point)
	{
		points.remove(point);
		savePoints();
		rebuildPanel();
	}

	public void movePoint(CameraPoint point, int offset)
	{
		List<CameraPoint> copy = new ArrayList<>(points);
		int index = copy.indexOf(point);
		int target = index + offset;
		if (index < 0 || target < 0 || target >= copy.size())
		{
			return;
		}
		Collections.swap(copy, index, target);
		points.clear();
		points.addAll(copy);
		savePoints();
		rebuildPanel();
	}

	/**
	 * Persists the current points. Call after editing a point's fields.
	 */
	public void savePoints()
	{
		if (points.isEmpty())
		{
			configManager.unsetConfiguration(LocationCameraPointsConfig.GROUP, LocationCameraPointsConfig.POINTS_KEY);
		}
		else
		{
			configManager.setConfiguration(LocationCameraPointsConfig.GROUP, LocationCameraPointsConfig.POINTS_KEY,
				gson.toJson(new ArrayList<>(points)));
		}
	}

	public static String displayName(CameraPoint point)
	{
		return Strings.isNullOrEmpty(point.getName())
			? point.getX() + ", " + point.getY() + ", " + point.getPlane()
			: point.getName();
	}

	// ---- Internals ----

	/**
	 * Must be called on the client thread.
	 */
	private void addPointAt(WorldPoint location)
	{
		int yaw = client.getCameraYawTarget();
		int zoom = getCurrentZoom();
		CameraPoint point = new CameraPoint(
			"Point " + (points.size() + 1),
			location.getX(), location.getY(), location.getPlane(),
			config.defaultRadius(),
			CameraDirection.fromYaw(yaw),
			true, zoom,
			true, false);

		// The new point captures the current camera, so triggering it right away is harmless
		points.add(point);
		savePoints();
		rebuildPanel();
	}

	private int getCurrentZoom()
	{
		return client.getVarcIntValue(client.isResized() ? VarClientID.CAMERA_ZOOM_BIG : VarClientID.CAMERA_ZOOM_SMALL);
	}

	private WorldPoint getPlayerLocation()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		// The true (server) tile. getLocalLocation() is the rendered position, which trails behind
		// it while the walking animation plays, delaying the trigger.
		WorldPoint trueTile = player.getWorldLocation();

		WorldView wv = client.getTopLevelWorldView();
		if (wv.isInstance())
		{
			// Convert instance coordinates back to the template's real world coordinates
			LocalPoint local = LocalPoint.fromWorld(wv, trueTile);
			if (local != null)
			{
				return WorldPoint.fromLocalInstance(client, local);
			}
		}
		return trueTile;
	}

	private void loadPoints()
	{
		points.clear();
		String json = configManager.getConfiguration(LocationCameraPointsConfig.GROUP, LocationCameraPointsConfig.POINTS_KEY);
		if (Strings.isNullOrEmpty(json))
		{
			return;
		}
		try
		{
			List<CameraPoint> loaded = gson.fromJson(json, new TypeToken<List<CameraPoint>>()
			{
			}.getType());
			if (loaded != null)
			{
				points.addAll(loaded);
			}
		}
		catch (JsonSyntaxException e)
		{
			log.warn("Unable to load camera points", e);
		}
	}

	private void rebuildPanel()
	{
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::rebuild);
		}
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// Map pin
		g.setColor(new Color(0, 180, 255));
		g.fillOval(3, 0, 10, 10);
		g.fillPolygon(new int[]{4, 12, 8}, new int[]{7, 7, 15}, 3);
		// Lens
		g.setColor(new Color(30, 30, 30));
		g.fillOval(5, 2, 6, 6);
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(1f));
		g.drawOval(6, 3, 4, 4);
		g.dispose();
		return image;
	}
}
