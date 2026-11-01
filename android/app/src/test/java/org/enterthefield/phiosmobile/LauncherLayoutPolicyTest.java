package org.enterthefield.phiosmobile;

import static org.junit.Assert.*;
import org.junit.Test;

public class LauncherLayoutPolicyTest {
    @Test public void narrowAndLargeTextStackControls() {
        assertTrue(LauncherLayoutPolicy.stackActions(320, 1f));
        assertTrue(LauncherLayoutPolicy.stackActions(389, 1f));
        assertTrue(LauncherLayoutPolicy.stackActions(400, 1.3f));
        assertFalse(LauncherLayoutPolicy.stackActions(400, 1f));
        assertFalse(LauncherLayoutPolicy.stackActions(600, 1.2f));
    }

    @Test public void rowsGrowToAccommodateFontScale() {
        assertEquals(56, LauncherLayoutPolicy.appRowHeightDp(true, 1f));
        assertEquals(64, LauncherLayoutPolicy.appRowHeightDp(false, 1f));
        assertTrue(LauncherLayoutPolicy.appRowHeightDp(true, 2f) >= 84);
        assertTrue(LauncherLayoutPolicy.appRowHeightDp(false, 2f)
                >= LauncherLayoutPolicy.appRowHeightDp(false, 1f));
    }

    @Test public void dialShrinksButNeverDropsAccessibleButtons() {
        assertEquals(292, LauncherLayoutPolicy.surfDialHeightDp(780, 1f));
        assertEquals(190, LauncherLayoutPolicy.surfDialHeightDp(640, 1f));
        assertEquals(190, LauncherLayoutPolicy.surfDialHeightDp(800, 1.5f));
    }
}
