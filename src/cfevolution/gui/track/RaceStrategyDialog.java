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

import cfevolution.edit.RaceStrategyForm;
import java.awt.*;
import javax.swing.*;

/**
 * Edits a track's race length and the computer cars' pit stop plan
 * (Edit > Race Strategy...). Nothing changes until OK. The words, limits and
 * rules are edit/RaceStrategyForm's.
 *
 * @author barrie
 */
public class RaceStrategyDialog extends JDialog
{
    private final RaceStrategyForm form;
    private final JSpinner laps, firstStop, chance;
    private boolean changed;

    public RaceStrategyDialog(Component parent, RaceStrategyForm form)
    {
        super(SwingUtilities.getWindowAncestor(parent), RaceStrategyForm.TITLE, ModalityType.APPLICATION_MODAL);
        this.form = form;
        laps = spinner(form, RaceStrategyForm.LAPS);
        firstStop = spinner(form, RaceStrategyForm.FIRST_STOP);
        chance = spinner(form, RaceStrategyForm.CHANCE);
        laps.setName("raceLaps");
        firstStop.setName("firstStopLap");
        chance.setName("stopChance");

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        int y = 0;
        JSpinner[] spinners = { laps, firstStop, chance };
        for (int i = 0; i < spinners.length; i++)
            y = row(grid, y, RaceStrategyForm.LABELS[i], spinners[i], RaceStrategyForm.HINTS[i]);

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

        getContentPane().add(grid, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private static JSpinner spinner(RaceStrategyForm form, int i)
    {
        JSpinner s = new JSpinner(new SpinnerNumberModel(form.shown(i), RaceStrategyForm.MIN[i], RaceStrategyForm.MAX[i], 1));
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
        boolean[] did = new boolean[1];
        cfevolution.edit.Message refused = form.apply(new int[] { value(laps), value(firstStop), value(chance) }, did);
        if (refused != null)
        {
            cfevolution.gui.MessageBoxes.show(this, refused);
            return;
        }
        changed = did[0];
        dispose();
    }

    /** True if OK changed anything. */
    public boolean wasChanged()
    {
        return changed;
    }
}
