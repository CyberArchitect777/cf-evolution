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

package cfevolution.gui.track;

import cfevolution.data.track.Track;
import java.awt.*;
import javax.swing.*;

/**
 * Tools > Check Track: the track checker's verdict and each check, with a
 * Fix button where the editor can fix the problem itself. Fixing re-runs
 * the checks.
 *
 * @author barrie
 */
public class TrackCheckDialog extends JDialog
{
    /** Runs a check's fix; supplied by the track window. */
    public interface Fixer
    {
        void fix(int action);
    }

    private static final Color OK = new Color(0x2E7D32), WARNING = new Color(0xB26A00), ERROR = new Color(0xC62828);

    private final Track track;
    private final Fixer fixer;
    private final JPanel list = new JPanel();
    private final JLabel verdict = new JLabel();

    public TrackCheckDialog(Component parent, Track track, Fixer fixer)
    {
        super(SwingUtilities.getWindowAncestor(parent), "Check Track", ModalityType.APPLICATION_MODAL);
        this.track = track;
        this.fixer = fixer;

        verdict.setName("verdict");
        verdict.setFont(verdict.getFont().deriveFont(Font.BOLD, 16f));
        verdict.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(0, 14, 6, 14));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(620, 430));

        JButton again = new JButton("Check Again");
        JButton close = new JButton("Close");
        again.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { refresh(); }
        });
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(again);
        buttons.add(close);
        getRootPane().registerKeyboardAction(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        }, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getContentPane().add(verdict, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        refresh();
        pack();
        setLocationRelativeTo(parent);
    }

    private void refresh()
    {
        TrackCheck.Report report = TrackCheck.check(track);
        int overall = report.overall();
        verdict.setText(TrackCheck.verdict(overall));
        verdict.setForeground(colour(overall));
        list.removeAll();
        String area = null;
        // problems first, then what is fine
        for (int pass = 0; pass < 2; pass++)
            for (final TrackCheck.Item item : report.items)
            {
                if ((pass == 0) == (item.status == TrackCheck.OK))
                    continue;
                if (pass == 1 && area == null)
                {
                    JLabel fine = new JLabel(report.count(TrackCheck.OK) == report.items.size()
                                             ? "Everything checked is fine:" : "These are fine:");
                    fine.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
                    fine.setAlignmentX(LEFT_ALIGNMENT);
                    list.add(fine);
                    area = "";
                }
                list.add(row(item));
            }
        list.revalidate();
        list.repaint();
    }

    private static Color colour(int status)
    {
        return status == TrackCheck.ERROR ? ERROR : status == TrackCheck.WARNING ? WARNING : OK;
    }

    private JComponent row(final TrackCheck.Item item)
    {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setName("checkItem");
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JLabel mark = new JLabel(item.status == TrackCheck.OK ? "✔" : item.status == TrackCheck.WARNING ? "!" : "✖");
        mark.setForeground(colour(item.status));
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 15f));
        mark.setVerticalAlignment(SwingConstants.TOP);
        mark.setPreferredSize(new Dimension(18, 18));
        String text = "<html><b>" + item.area + ":</b> " + item.title
                    + (item.detail == null ? "" : "<br><span style='color:#6A7380'>" + item.detail + "</span>") + "</html>";
        JLabel label = new JLabel(text);
        row.add(mark, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        if (item.action != TrackCheck.ACTION_NONE)
        {
            JButton fix = new JButton(item.action == TrackCheck.ACTION_GENERATE_LINE ? "Generate..." : "Fix");
            fix.setName("fix");
            fix.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    fixer.fix(item.action);
                    refresh();
                }
            });
            JPanel holder = new JPanel(new BorderLayout());
            holder.add(fix, BorderLayout.NORTH);
            row.add(holder, BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 20));
        return row;
    }
}
