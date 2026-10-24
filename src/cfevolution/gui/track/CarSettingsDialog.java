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

import cfevolution.data.track.CCSetup;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Edits a track's car and computer car settings (Edit > Car and AI
 * Settings...): the 38-byte CC setup block. Nothing changes until OK, and
 * only the values the user changed are written, so stored values that are
 * not whole percentages survive.
 *
 * Each hint gives the range the 16 original circuits use.
 *
 * @author barrie
 */
public class CarSettingsDialog extends JDialog
{
    private static final int PERCENT = 0, WORD = 1, SETUP_BYTE = 2, TYRE = 3;

    /** One editable value. */
    private class Field
    {
        final int kind, index;
        final JComponent editor;
        final int shown;

        Field(int kind, int index, int min, int max)
        {
            this.kind = kind;
            this.index = index;
            int value = read();
            if (kind == TYRE)
            {
                JComboBox<String> box = new JComboBox<String>(new String[] { "A", "B", "C", "D" });
                box.setSelectedIndex(Math.max(0, Math.min(3, value)));
                editor = box;
                shown = box.getSelectedIndex();
            }
            else
            {
                JSpinner s = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, value)), min, max, 1));
                JSpinner.NumberEditor ed = new JSpinner.NumberEditor(s, "#");
                ed.getTextField().setColumns(kind == SETUP_BYTE ? 3 : 5);
                s.setEditor(ed);
                editor = s;
                shown = current();
            }
        }

        int read()
        {
            switch (kind)
            {
                case PERCENT: return setup.getPercent(index);
                case WORD: return setup.getWord(index);
                case SETUP_BYTE: return setup.getByte(index) - CCSetup.SETUP_OFFSET;
                default: return setup.getByte(index) - CCSetup.TYRE_OFFSET;
            }
        }

        int current()
        {
            if (editor instanceof JComboBox)
                return ((JComboBox<?>) editor).getSelectedIndex();
            JSpinner s = (JSpinner) editor;
            try { s.commitEdit(); } catch (java.text.ParseException e) { /* keeps its last good value */ }
            return ((Number) s.getValue()).intValue();
        }

        /** Writes the value if the user changed it; returns true if so. */
        boolean apply()
        {
            int v = current();
            if (v == shown)
                return false;
            switch (kind)
            {
                case PERCENT: setup.setPercent(index, v); break;
                case WORD: setup.setWord(index, v); break;
                case SETUP_BYTE: setup.setByte(index, v + CCSetup.SETUP_OFFSET); break;
                default: setup.setByte(index, v + CCSetup.TYRE_OFFSET); break;
            }
            return true;
        }
    }

    private final CCSetup setup;
    private final List<Field> fields = new ArrayList<Field>();
    private boolean changed;

    public CarSettingsDialog(Component parent, CCSetup setup)
    {
        super(SwingUtilities.getWindowAncestor(parent), "Car and AI Settings", ModalityType.APPLICATION_MODAL);
        this.setup = setup;

        JPanel all = group("All cars at this circuit");
        row(all, "Tyre grip (%)", PERCENT, CCSetup.GRIP, 50, 150, "Originals: 100 to 102");
        row(all, "Engine power (%)", PERCENT, CCSetup.POWER, 10, 200, "Originals: 100 (Mexico 80)");
        row(all, "Air resistance (%)", PERCENT, CCSetup.AIR_RESISTANCE, 10, 200, "Originals: 100 (Mexico 80)");
        row(all, "Tyre wear in qualifying (%)", PERCENT, CCSetup.WEAR_QUALIFYING, 0, 399, "Originals: 73");
        row(all, "Tyre wear in practice and the race (%)", PERCENT, CCSetup.WEAR_OTHER, 0, 399, "Originals: 53 to 207");
        row(all, "Fuel load", WORD, CCSetup.FUEL, 1, 2000, "Originals: 306 to 396");

        JPanel clock = group("Lap times shown");
        note(clock, "The game's clock runs this much faster than real time, so lap times look longer.");
        row(clock, "In practice and qualifying (%)", PERCENT, CCSetup.CLOCK_PRACTICE, 50, 200, "Originals: 100 to 113");
        row(clock, "In the race (%)", PERCENT, CCSetup.CLOCK_RACE, 50, 200, "Originals: 101 to 112");

        JPanel ai = group("Computer cars");
        row(ai, "Cornering in practice and qualifying (%)", PERCENT, CCSetup.AGGRESSION_PRACTICE, 50, 150, "Originals: 98 to 103");
        row(ai, "Cornering in the race (%)", PERCENT, CCSetup.AGGRESSION_RACE, 50, 150, "Originals: 98 to 102");
        row(ai, "Cornering in a wet race (%)", PERCENT, CCSetup.AGGRESSION_WET, 50, 150, "Originals: 98");
        row(ai, "Power at the easier opposition levels (%)", PERCENT, CCSetup.CC_POWER, 10, 200,
            "Originals: 49 to 84. Lower slows them more; Ace is not affected");
        row(ai, "Formation lap gap", WORD, CCSetup.FORMATION_GAP, 0, 4000, "Originals: 128 to 512");
        row(ai, "Braking threshold for bends", WORD, CCSetup.BRAKE_THRESHOLD, 0, 4000, "Originals: 256 to 640");

        JPanel car = group("Computer cars' set-up");
        row(car, "Front wing", SETUP_BYTE, CCSetup.FRONT_WING, 0, 104, "Originals: 18 to 58");
        row(car, "Rear wing", SETUP_BYTE, CCSetup.REAR_WING, 0, 104, "Originals: 18 to 58");
        JPanel gears = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (int g = 0; g < 6; g++)
        {
            Field f = new Field(SETUP_BYTE, CCSetup.GEAR_1 + g, 0, 104);
            f.editor.setName("gear" + (g + 1));
            fields.add(f);
            gears.add(new JLabel(String.valueOf(g + 1)));
            gears.add(f.editor);
        }
        addRow(car, "Gears", gears, "Originals: 23 to 66");
        row(car, "Tyre compound", TYRE, CCSetup.TYRE, 0, 3, "Originals: C");

        JPanel left = column(all, clock), right = column(ai, car);
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 4, 14));
        body.add(left);
        body.add(right);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                for (Field f : fields)
                    changed |= f.apply();
                dispose();
            }
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

        getContentPane().add(body, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private static JPanel column(JPanel a, JPanel b)
    {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(a);
        c.add(Box.createVerticalStrut(10));
        c.add(b);
        c.add(Box.createVerticalGlue());
        return c;
    }

    private static JPanel group(String title)
    {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),
                                                       BorderFactory.createEmptyBorder(2, 6, 6, 6)));
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private static void note(JPanel group, String text)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0);
        JLabel l = new JLabel("<html><div style='width:330px'>" + text + "</div></html>");
        l.setForeground(new Color(0x6A7380));
        group.add(l, c);
    }

    private void row(JPanel group, String label, int kind, int index, int min, int max, String hint)
    {
        Field f = new Field(kind, index, min, max);
        f.editor.setName(label);
        fields.add(f);
        addRow(group, label, f.editor, hint);
    }

    private static void addRow(JPanel group, String label, JComponent editor, String hint)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 0, 10);
        group.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 0, 0);
        group.add(editor, c);
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 2, 0);
        JLabel h = new JLabel(hint);
        h.setFont(h.getFont().deriveFont(h.getFont().getSize2D() - 1f));
        h.setForeground(new Color(0x6A7380));
        group.add(h, c);
    }

    /** True if OK changed anything. */
    public boolean wasChanged()
    {
        return changed;
    }
}
