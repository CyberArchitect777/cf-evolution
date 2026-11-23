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

package cfevolution.edit;

import cfevolution.data.executable.Executable;
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GameLiveries;
import cfevolution.data.executable.GameTables;
import cfevolution.data.executable.GameTeams;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Revert Page for GP.EXE (Hrvoje, 2026-11-15; the owner, 2026-11-23): one page,
 * or one Liveries tab, goes back to how it was when GP.EXE was opened, through
 * the same values the page edits; the other pages keep their changes. (When the
 * Drivers/Teams redesign links pages, e.g. team order and liveries, the linked
 * pages will have to be reverted together.)
 */
public final class ExecutableRevert
{
    /** The pages, in the order of ExecutableScreen.PAGES and the desktop's page list. */
    public static final int DRIVERS = 0, TEAMS = 1, POINTS = 2, LIVERIES = 3, SETTINGS = 4, PIT_LANE = 5,
                            SETUPS = 6, LAP_RECORDS = 7, PHYSICS = 8, OPPOSITION = 9;

    private ExecutableRevert()
    {
    }

    /** GP.EXE as it was when opened, read into a second Executable; null if it was not kept or cannot be read. */
    public static Executable whenOpened(File exeFile)
    {
        byte[] bytes = Revert.whenOpened(exeFile);
        if (bytes == null)
            return null;
        File copy = null;
        try
        {
            copy = File.createTempFile("cfe-revert", ".exe");
            Files.write(copy.toPath(), bytes);
            Executable old = new Executable(copy);
            return old.loadData() == 0 ? old : null;
        }
        catch (IOException e)
        {
            return null;
        }
        finally
        {
            if (copy != null)
                copy.delete();
        }
    }

    /** The name of a page (with its tab on Liveries) as the question says it. */
    public static String pageName(String pageTitle, int page, int liveriesTab)
    {
        if (page != LIVERIES)
            return "The " + pageTitle + " page";
        return "The " + new String[] { "Cars", "Helmets", "Pit Crews" }[liveriesTab] + " tab of " + pageTitle;
    }

    /** Puts one page's values (on Liveries, one tab's) back from old into exe. */
    public static void page(Executable exe, Executable old, int page, int liveriesTab)
    {
        GameTables t = exe.getGameTables(), o = old.getGameTables();
        switch (page)
        {
            case DRIVERS:
            {
                GameDrivers d = exe.getGameDrivers(), od = old.getGameDrivers();
                for (int i = 0; i < ExecutableEdits.DRIVERS; i++)
                {
                    d.setDriverName(i, od.getDriverName(i));
                    d.setQualifyingGrip(i, od.getQualifyingGrip(i));
                    d.setRaceGrip(i, od.getRaceGrip(i));
                }
                break;
            }
            case TEAMS:
            {
                GameTeams m = exe.getGameTeams(), om = old.getGameTeams();
                for (int i = 0; i < ExecutableEdits.TEAMS; i++)
                {
                    m.setTeamName(i, om.getTeamName(i));
                    m.setEngineName(i, om.getEngineName(i));
                    m.setBHPSetting(i, om.getBHPSetting(i));
                    for (int s = 0; s < 2; s++)
                        m.setTeamCar(i, s, om.getTeamCar(i, s));
                }
                break;
            }
            case POINTS:
                for (int i = 0; i < ExecutableEdits.POINTS; i++)
                    exe.getGamePoints().setPointValue(i, old.getGamePoints().getPointValue(i));
                break;
            case LIVERIES:
            {
                GameLiveries l = exe.getGameLiveries(), ol = old.getGameLiveries();
                if (liveriesTab == LiveriesPage.CARS)
                    for (int r = 0; r < GameLiveries.TEAMS; r++)
                        for (int p = 0; p < GameLiveries.CAR_PARTS.length; p++)
                            l.setCarColour(r, p, ol.getCarColour(r, p));
                else if (liveriesTab == LiveriesPage.HELMETS)
                    for (int r = 0; r < GameLiveries.DRIVERS; r++)
                        for (int p = 0; p < GameLiveries.HELMET_PARTS.length; p++)
                            l.setHelmetColour(r, p, ol.getHelmetColour(r, p));
                else
                    for (int r = 0; r < GameLiveries.TEAMS; r++)
                        for (int p = 0; p < GameLiveries.PIT_CREW_PARTS.length; p++)
                            l.setPitCrewColour(r, p, ol.getPitCrewColour(r, p));
                break;
            }
            case SETTINGS:
            {
                List<GameSettingsPage.Group> now = GameSettingsPage.groups(exe), then = GameSettingsPage.groups(old);
                for (int g = 0; g < now.size(); g++)
                    for (int i = 0; i < now.get(g).items.size(); i++)
                    {
                        GameSettingsPage.Item item = now.get(g).items.get(i);
                        if (item.kind != GameSettingsPage.Kind.NOTE)
                            item.set(then.get(g).items.get(i).get());
                    }
                break;
            }
            case PIT_LANE:
                t.pitOrder.copyFrom(o.pitOrder);
                break;
            case SETUPS:
                t.carSetups.copyFrom(o.carSetups);
                t.circuitSetups.copyFrom(o.circuitSetups);
                t.separateSetups.copyFrom(o.separateSetups);
                break;
            case LAP_RECORDS:
                t.lapRecords.copyFrom(o.lapRecords);
                break;
            case PHYSICS:
                for (GameTables.Table[] pair : new GameTables.Table[][] {
                        { t.torqueRace, o.torqueRace }, { t.torqueQualifying, o.torqueQualifying },
                        { t.tyreGripDry, o.tyreGripDry }, { t.tyreGripWet, o.tyreGripWet },
                        { t.minimumGrip, o.minimumGrip }, { t.carWeight, o.carWeight }, { t.fuel, o.fuel },
                        { t.wingDrag, o.wingDrag }, { t.wingDownforce, o.wingDownforce },
                        { t.downforceFactor, o.downforceFactor }, { t.engineBraking, o.engineBraking },
                        { t.revLimits, o.revLimits } })
                    pair[0].copyFrom(pair[1]);
                break;
            case OPPOSITION:
                for (GameTables.Table[] pair : new GameTables.Table[][] {
                        { t.oppPowerDeduction, o.oppPowerDeduction }, { t.oppRankPower, o.oppRankPower },
                        { t.oppOversteerLow, o.oppOversteerLow }, { t.oppOversteerHigh, o.oppOversteerHigh },
                        { t.oppCornerBurst, o.oppCornerBurst }, { t.oppVariationLow, o.oppVariationLow },
                        { t.oppVariationHigh, o.oppVariationHigh }, { t.globalCornerSpeed, o.globalCornerSpeed },
                        { t.oppWingVariation, o.oppWingVariation }, { t.wetRacePower, o.wetRacePower } })
                    pair[0].copyFrom(pair[1]);
                break;
            default:
                throw new IllegalArgumentException("no page " + page);
        }
    }
}
