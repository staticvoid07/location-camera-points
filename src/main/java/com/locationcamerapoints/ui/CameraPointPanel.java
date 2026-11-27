package com.locationcamerapoints.ui;

import com.locationcamerapoints.CameraDirection;
import com.locationcamerapoints.CameraPoint;
import com.locationcamerapoints.LocationCameraPointsPlugin;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Editor for a single camera point. Every change is written to the point and saved immediately.
 */
class CameraPointPanel extends JPanel
{
	// Matches the range the core Camera plugin allows with its expanded zoom limits
	private static final int MIN_ZOOM = -400;
	private static final int MAX_ZOOM = 1400;
	private static final int MAX_RADIUS = 104;
	private static final int ROW_GAP = 4;

	private final LocationCameraPointsPlugin plugin;
	private final CameraPoint point;

	CameraPointPanel(LocationCameraPointsPlugin plugin, CameraPoint point, boolean canMoveUp, boolean canMoveDown)
	{
		this.plugin = plugin;
		this.point = point;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, point.isEnabled() ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(6, 6, 6, 6)));

		add(createHeader(), BorderLayout.NORTH);
		add(createFields(), BorderLayout.CENTER);
		add(createActions(canMoveUp, canMoveDown), BorderLayout.SOUTH);
	}

	private JComponent createHeader()
	{
		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 6, 0));

		JTextField name = new JTextField(point.getName());
		name.setToolTipText("Point name");
		name.getDocument().addDocumentListener(new DocumentListener()
		{
			private void changed()
			{
				point.setName(name.getText());
				plugin.savePoints();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}
		});
		header.add(name, BorderLayout.CENTER);

		JCheckBox enabled = new JCheckBox();
		enabled.setOpaque(false);
		enabled.setSelected(point.isEnabled());
		enabled.setToolTipText("Enabled");
		enabled.addActionListener(e ->
		{
			point.setEnabled(enabled.isSelected());
			plugin.savePoints();
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, point.isEnabled() ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR),
				new EmptyBorder(6, 6, 6, 6)));
		});
		header.add(enabled, BorderLayout.EAST);

		return header;
	}

	private JComponent createFields()
	{
		JPanel fields = new JPanel();
		fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
		fields.setOpaque(false);

		// Location: labels sit above the fields so each spinner gets half the panel width
		JSpinner x = spinner(point.getX(), 0, 20000, point::setX);
		JSpinner y = spinner(point.getY(), 0, 20000, point::setY);
		JSpinner plane = spinner(point.getPlane(), 0, 3, point::setPlane);
		JSpinner radius = spinner(point.getRadius(), 0, MAX_RADIUS, point::setRadius);
		radius.setToolTipText("Trigger distance in tiles around the point");
		fields.add(row(labeled("X", x), labeled("Y", y)));
		fields.add(row(labeled("Plane", plane), labeled("Radius", radius)));

		JButton here = smallButton("Move to my location");
		here.addActionListener(e -> plugin.fetchPlayerLocation(wp ->
		{
			// Setting the spinners triggers their listeners, which update and save the point
			x.setValue(wp.getX());
			y.setValue(wp.getY());
			plane.setValue(wp.getPlane());
		}));
		fields.add(row(here));

		// Direction
		JComboBox<CameraDirection> direction = new JComboBox<>(CameraDirection.values());
		direction.setSelectedItem(point.getDirection());
		direction.addActionListener(e ->
		{
			point.setDirection((CameraDirection) direction.getSelectedItem());
			plugin.savePoints();
		});
		fields.add(row(labeled("Facing", direction)));

		// Zoom
		JSpinner zoom = spinner(point.getZoom(), MIN_ZOOM, MAX_ZOOM, point::setZoom);
		zoom.setEnabled(point.isZoomEnabled());
		JCheckBox zoomEnabled = new JCheckBox("Zoom");
		zoomEnabled.setOpaque(false);
		zoomEnabled.setFont(FontManager.getRunescapeSmallFont());
		zoomEnabled.setSelected(point.isZoomEnabled());
		zoomEnabled.setToolTipText("Change the zoom when this point triggers");
		zoomEnabled.addActionListener(e ->
		{
			point.setZoomEnabled(zoomEnabled.isSelected());
			zoom.setEnabled(zoomEnabled.isSelected());
			plugin.savePoints();
		});
		JButton currentZoom = smallButton("Current");
		currentZoom.setToolTipText("Use your current camera zoom");
		currentZoom.addActionListener(e -> plugin.fetchCurrentZoom(zoom::setValue));

		JPanel zoomRow = new JPanel(new BorderLayout(4, 0));
		zoomRow.setOpaque(false);
		zoomRow.setBorder(new EmptyBorder(ROW_GAP, 0, 0, 0));
		zoomRow.add(zoomEnabled, BorderLayout.WEST);
		zoomRow.add(zoom, BorderLayout.CENTER);
		zoomRow.add(currentZoom, BorderLayout.EAST);
		fields.add(zoomRow);

		return fields;
	}

	private JComponent createActions(boolean canMoveUp, boolean canMoveDown)
	{
		JPanel actions = new JPanel(new GridLayout(1, 4, 4, 0));
		actions.setOpaque(false);
		actions.setBorder(new EmptyBorder(6, 0, 0, 0));

		JButton test = smallButton("Test");
		test.setToolTipText("Apply this camera now");
		test.addActionListener(e -> plugin.testPoint(point));

		JButton up = smallButton("▲");
		up.setToolTipText("Move up");
		up.setEnabled(canMoveUp);
		up.addActionListener(e -> plugin.movePoint(point, -1));

		JButton down = smallButton("▼");
		down.setToolTipText("Move down");
		down.setEnabled(canMoveDown);
		down.addActionListener(e -> plugin.movePoint(point, 1));

		JButton delete = smallButton("Delete");
		delete.addActionListener(e ->
		{
			int result = JOptionPane.showConfirmDialog(this,
				"Delete camera point \"" + LocationCameraPointsPlugin.displayName(point) + "\"?",
				"Delete camera point", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION)
			{
				plugin.removePoint(point);
			}
		});

		actions.add(test);
		actions.add(up);
		actions.add(down);
		actions.add(delete);
		return actions;
	}

	private JSpinner spinner(int value, int min, int max, IntConsumer setter)
	{
		int clamped = Math.max(min, Math.min(max, value));
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(clamped, min, max, 1));
		// Plain digits: coordinates like 3222 shouldn't render as "3,222"
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		spinner.addChangeListener(e ->
		{
			setter.accept((Integer) spinner.getValue());
			plugin.savePoints();
		});
		return spinner;
	}

	/**
	 * A full-width row splitting its components evenly.
	 */
	private static JPanel row(JComponent... components)
	{
		JPanel row = new JPanel(new GridLayout(1, components.length, 6, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(ROW_GAP, 0, 0, 0));
		for (JComponent component : components)
		{
			row.add(component);
		}
		return row;
	}

	/**
	 * A field with a small label above it.
	 */
	private static JPanel labeled(String text, JComponent field)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 1));
		panel.setOpaque(false);
		panel.add(label(text), BorderLayout.NORTH);
		panel.add(field, BorderLayout.CENTER);
		return panel;
	}

	private static JLabel label(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JButton smallButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setMargin(new Insets(2, 2, 2, 2));
		button.setFocusable(false);
		return button;
	}
}
