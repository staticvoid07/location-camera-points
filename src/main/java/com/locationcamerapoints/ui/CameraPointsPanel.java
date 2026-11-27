package com.locationcamerapoints.ui;

import com.locationcamerapoints.CameraPoint;
import com.locationcamerapoints.LocationCameraPointsPlugin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

/**
 * Side panel listing every camera point, with controls to add new ones.
 */
public class CameraPointsPanel extends PluginPanel
{
	private final LocationCameraPointsPlugin plugin;
	private final JPanel listPanel = new JPanel(new GridBagLayout());
	private final PluginErrorPanel emptyPanel = new PluginErrorPanel();

	public CameraPointsPanel(LocationCameraPointsPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setBorder(new EmptyBorder(0, 0, 10, 0));

		JLabel title = new JLabel("Location Camera Points");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(title, BorderLayout.NORTH);

		JButton addButton = new JButton("Add point at my location");
		addButton.setToolTipText("Create a point here using your current camera direction and zoom");
		addButton.addActionListener(e -> plugin.addPointAtPlayer());
		header.add(addButton, BorderLayout.CENTER);

		JLabel hint = new JLabel("<html>Tip: shift right-click a tile to add a point there.</html>");
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hint.setFont(FontManager.getRunescapeSmallFont());
		header.add(hint, BorderLayout.SOUTH);

		emptyPanel.setContent("No camera points",
			"Stand somewhere, set up your camera, then click the button above.");

		add(header, BorderLayout.NORTH);
		add(listPanel, BorderLayout.CENTER);

		rebuild();
	}

	/**
	 * Recreates the list from the plugin's points. Must be called on the Swing thread.
	 */
	public void rebuild()
	{
		listPanel.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 10, 0);

		List<CameraPoint> points = plugin.getPoints();
		if (points.isEmpty())
		{
			listPanel.add(emptyPanel, c);
		}

		for (int i = 0; i < points.size(); i++)
		{
			listPanel.add(new CameraPointPanel(plugin, points.get(i), i > 0, i < points.size() - 1), c);
			c.gridy++;
		}

		listPanel.revalidate();
		listPanel.repaint();
	}
}
