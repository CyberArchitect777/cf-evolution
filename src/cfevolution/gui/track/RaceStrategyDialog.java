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

import cfevolution.data.track.Footer;
import cfevolution.data.track.RaceStrategy;
import java.awt.*;
import javax.swing.*;

/**
 * Edits a track's race length and the computer cars' pit stop plan
 * (Edit > Race Strategy...). Nothing changes until OK.
 *
 * @author barrie
 */
public class RaceStrategyDialog extends JDialog
{
    private final Footer footer;
    private final JSpinner laps, firstStop, chance;
    private final int lapsShown, firstStopShown, chanceShown;
    private boolean changed;

    public RaceStrategyDialog(Component parent, Footer footer)
    {
        super(SwingUtilities.getWindowAncestor(parent), "Race Strategy", ModalityType.APPLICATION_MODAL);
        this.footer = footer;
        int lapsNow = RaceStrategy.getRaceLaps(footer);
        laps = spinner(Math.max(3, Math.min(150, lapsNow)), 3, 150);
        firstStop = spinner(Math.max(1, Math.min(149, RaceStrategy.getFirstStopLap(footer))), 1, 149);
        chance = spinner(RaceStrategy.getStopChancePercent(footer), 0, 100);
        lapsShown = value(laps);
        firstStopShown = value(firstStop);
        chanceShown = value(chance);
        laps.setName("raceLaps");
        firstStop.setName("firstStopLap");
        chance.setName("stopChance");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        int y = 0;
        y = row(form, y, "Laps in a full race", laps,
                "The original circuits have 44 to 81. Shorter race distances use a share of these.");
        y = row(form, y, "First pit stop on lap", firstStop,
                "For a full race; shorter races stop at the same point in the race. The originals stop about a third to half way.");
        y = row(form, y, "Chance a computer car plans a pit stop (%)", chance,
                "Monaco, Imola and Adelaide have 0, so hardly any car stops. Only in dry races of at least "
                + "75% distance (the executable editor can change that).");

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { apply(); }
        });
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        }, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private static JSpinner spinner(int value, int min, int max)
    {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(s, "#");
        editor.getTextField().setColumns(5);
        s.setEditor(editor);
        return s;
    }

    private static int row(JPanel form, int y, String label, JSpinner spinner, String hint)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 0, 12);
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.insets = new Insets(6, 0, 0, 0);
        form.add(spinner, c);
        c.gridx = 0;
        c.gridy = y + 1;
        c.gridwidth = 2;
        c.insets = new Insets(2, 0, 6, 0);
        JLabel hintLabel = new JLabel("<html><div style='width:380px'>" + hint + "</div></html>");
        hintLabel.setForeground(new Color(0x6A7380));
        form.add(hintLabel, c);
        return y + 2;
    }

    private static int value(JSpinner s)
    {
        try { s.commitEdit(); } catch (java.text.ParseException e) { /* keeps its last good value */ }
        return ((Number) s.getValue()).intValue();
    }

    private void apply()
    {
        int newLaps = value(laps), newStop = value(firstStop), newChance = value(chance);
        if (newStop >= newLaps)
        {
            JOptionPane.showMessageDialog(this,
                "The first pit stop has to come before the last lap. Choose a lap below " + newLaps + ".",
                "Race Strategy", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Only what the user changed is written, so a value outside the limits
        // above (from another tool) survives OK untouched
        if (newLaps != lapsShown) { RaceStrategy.setRaceLaps(footer, newLaps); changed = true; }
        if (newStop != firstStopShown) { RaceStrategy.setFirstStopLap(footer, newStop); changed = true; }
        if (newChance != chanceShown) { RaceStrategy.setStopChancePercent(footer, newChance); changed = true; }
        dispose();
    }

    /** True if OK changed anything. */
    public boolean wasChanged()
    {
        return changed;
    }
}
