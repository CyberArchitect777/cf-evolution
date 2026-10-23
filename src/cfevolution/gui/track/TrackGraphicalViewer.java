/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
 * Copyright (C) 2005-2007  The Chequered Flag Development Team
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

/*
 * TrackGraphicalViewer.java
 *
 * Created on 24 March 2005, 01:27
 */

package cfevolution.gui.track;

import cfevolution.data.track.Track;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

/**
 * The track map, with a slim toolbar for zooming and the layout mode. The
 * map itself pans by dragging and zooms with the mouse wheel; the numeric
 * keypad still pans as before.
 *
 * @author  barrie
 */
public class TrackGraphicalViewer extends JPanel {

    private static final int PAN_AMOUNT = 100;

    private Track m_track;
    private TrackPanel m_trackPanel;
    private TrackWindow m_parentTrackWindow;
    private JToggleButton layoutModeButton;

    private final Action zoomInAction = new AbstractAction("Zoom In") {
        public void actionPerformed(ActionEvent e) { m_trackPanel.zoomIn(); }
    };
    private final Action zoomOutAction = new AbstractAction("Zoom Out") {
        public void actionPerformed(ActionEvent e) { m_trackPanel.zoomOut(); }
    };
    private final Action centreAction = new AbstractAction("Centre Map") {
        public void actionPerformed(ActionEvent e) { m_trackPanel.panCenter(); }
    };
    private final Action layoutAction = new AbstractAction("Layout Mode") {
        public void actionPerformed(ActionEvent e) { toggleLayoutMode(); }
    };

    public TrackGraphicalViewer() {
        super(new BorderLayout());
        m_trackPanel = new TrackPanel();
        m_trackPanel.setViewer(this);
        m_trackPanel.setBackground(Color.WHITE);
        add(createToolbar(), BorderLayout.NORTH);
        add(m_trackPanel, BorderLayout.CENTER);
        initKeys();
    }

    public Action getZoomInAction() { return zoomInAction; }
    public Action getZoomOutAction() { return zoomOutAction; }
    public Action getCentreAction() { return centreAction; }
    public Action getLayoutAction() { return layoutAction; }

    private JToolBar createToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        bar.add(toolButton(zoomOutAction, "\u2212", "Zoom out (Ctrl+Minus)"));
        bar.add(toolButton(zoomInAction, "+", "Zoom in (Ctrl+Plus)"));
        bar.add(toolButton(centreAction, "Centre", "Centre the map (Ctrl+0)"));
        bar.addSeparator();
        layoutModeButton = new JToggleButton("Layout Mode");
        layoutModeButton.setToolTipText("Draw the track as its segment layout");
        layoutModeButton.setFocusable(false);
        layoutModeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { toggleLayoutMode(); }
        });
        bar.add(layoutModeButton);
        bar.add(Box.createHorizontalGlue());
        JLabel hint = new JLabel("Drag to move \u00b7 wheel to zoom \u00b7 click to select");
        hint.setForeground(new Color(0x7A828C));
        bar.add(hint);
        return bar;
    }

    private JButton toolButton(Action action, String text, String tip)
    {
        JButton button = new JButton(action);
        button.setText(text);
        button.setToolTipText(tip);
        button.setFocusable(false);
        return button;
    }

    public void setParentTrackWindow(TrackWindow trackWindow)
    {
        m_parentTrackWindow = trackWindow;
    }

    public java.awt.image.BufferedImage renderMapImage(int nWidth, int nHeight, boolean fWholeTrack)
    {
        // Passes on a map image export request to the track panel
        return m_trackPanel.renderMapImage(nWidth, nHeight, fWholeTrack);
    }

    public void setPreviewProfile(double[] adOffsets)
    {
        // Passes on a generated best line preview to the track panel
        m_trackPanel.setPreviewProfile(adOffsets);
    }

    public void clearPreview()
    {
        m_trackPanel.clearPreview();
    }

    public void mapSegmentClicked(int sectionType, int segmentNo)
    {
        // Passes on a click on a map object.
        // sectionType: 1 - Track, 2 - Pit, 3 - CCLine (as in selectedSegment).
        if (m_parentTrackWindow != null)
            m_parentTrackWindow.selectSegmentFromMap(sectionType, segmentNo);
    }

    public void setTrack(Track track) {
        m_track = track;
        m_trackPanel.setTrack( track );
    }

    public void redrawTrackMap()
    {
        // Passes on a signal to refresh the track map display
        m_trackPanel.redrawMap();
    }

    public void selectedSegment(int sectionType, int segmentNo)
    {
        // Passes on a signal to select a track map object
        m_trackPanel.selectSegment(sectionType, segmentNo);
    }

    private void toggleLayoutMode() {
        // Toggles the layout mode used for the display of the track map
        boolean currentLayoutMode = !m_track.getLayoutMode();
        m_track.setLayoutMode(currentLayoutMode);
        layoutModeButton.setSelected(currentLayoutMode);
        redrawTrackMap();
    }

    private void initKeys() {
        bindKey(KeyEvent.VK_ADD, 0, zoomInAction);
        bindKey(KeyEvent.VK_SUBTRACT, 0, zoomOutAction);
        bindPan(KeyEvent.VK_NUMPAD4, PAN_AMOUNT, 0);
        bindPan(KeyEvent.VK_NUMPAD6, -PAN_AMOUNT, 0);
        bindPan(KeyEvent.VK_NUMPAD8, 0, PAN_AMOUNT);
        bindPan(KeyEvent.VK_NUMPAD2, 0, -PAN_AMOUNT);
        bindPan(KeyEvent.VK_NUMPAD1, PAN_AMOUNT, -PAN_AMOUNT);
        bindPan(KeyEvent.VK_NUMPAD7, PAN_AMOUNT, PAN_AMOUNT);
        bindPan(KeyEvent.VK_NUMPAD9, -PAN_AMOUNT, PAN_AMOUNT);
        bindPan(KeyEvent.VK_NUMPAD3, -PAN_AMOUNT, -PAN_AMOUNT);
        bindKey(KeyEvent.VK_NUMPAD5, 0, centreAction);
    }

    private void bindKey(int keyCode, int modifiers, Action action)
    {
        String name = "map-" + keyCode + "-" + modifiers;
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(keyCode, modifiers), name);
        getActionMap().put(name, action);
    }

    private void bindPan(int keyCode, final int x, final int y)
    {
        bindKey(keyCode, 0, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { m_trackPanel.pan(x, y); }
        });
    }
}
