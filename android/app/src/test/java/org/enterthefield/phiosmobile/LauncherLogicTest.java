package org.enterthefield.phiosmobile;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
    @Test public void shelfListsOnlyAvailablePinnedAppsInStableOrder() {
        assertEquals(Arrays.asList("a", "c"),
                LauncherLogic.shelfKeys(Arrays.asList("a", "b", "c", "a"),
                        new HashSet<>(Arrays.asList("c", "a", "uninstalled")), 8));
    }

    @Test public void shelfClampsItemsAndHandlesEmptyInputs() {
        assertEquals(Arrays.asList("a"),
                LauncherLogic.shelfKeys(Arrays.asList("a", "b"), new HashSet<>(Arrays.asList("a", "b")), 1));
        assertTrue(LauncherLogic.shelfKeys(Arrays.asList("a"), Collections.singleton("a"), 0).isEmpty());
        assertTrue(LauncherLogic.shelfKeys(null, Collections.singleton("a"), 8).isEmpty());
        assertTrue(LauncherLogic.shelfKeys(Arrays.asList("a"), null, 8).isEmpty());
    }
}
