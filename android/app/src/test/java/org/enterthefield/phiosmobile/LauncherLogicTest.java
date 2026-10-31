package org.enterthefield.phiosmobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherLogicTest {
    @Test public void queryIsCaseInsensitiveAndTrimmed() {
        assertTrue(LauncherLogic.matchesQuery("Field Notes", "  NOTES "));
        assertTrue(LauncherLogic.matchesQuery("PhiSurf", ""));
        assertFalse(LauncherLogic.matchesQuery("Notes", "camera"));
        assertFalse(LauncherLogic.matchesQuery(null, ""));
    }

    @Test public void dialSectorsMatchNativeWheel() {
        assertEquals(0, LauncherLogic.dialSector(0, -100, 35));
        assertEquals(1, LauncherLogic.dialSector(90, -50, 35));
        assertEquals(2, LauncherLogic.dialSector(90, 50, 35));
        assertEquals(3, LauncherLogic.dialSector(0, 100, 35));
        assertEquals(4, LauncherLogic.dialSector(-90, 50, 35));
        assertEquals(5, LauncherLogic.dialSector(-90, -50, 35));
    }

    @Test public void centerAndNonFinitePointsSelectNothing() {
        assertEquals(-1, LauncherLogic.dialSector(1, 1, 35));
        assertEquals(-1, LauncherLogic.dialSector(Float.NaN, 100, 35));
        assertEquals(-1, LauncherLogic.dialSector(Float.POSITIVE_INFINITY, 30, 35));
    }
}
