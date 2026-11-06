package org.enterthefield.phiosmobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppManagementPolicyTest {
    @Test public void appInfoOnlyForObservedCurrentUserApps() {
        assertTrue(AppManagementPolicy.mayOpenAppInfo("org.example.target", true));
        assertFalse(AppManagementPolicy.mayOpenAppInfo("org.example.target", false));
        assertFalse(AppManagementPolicy.mayOpenAppInfo("bad/../../target", true));
        assertFalse(AppManagementPolicy.mayOpenAppInfo("", true));
    }

    @Test public void uninstallNeedsObservedNonSystemThirdPartyNotOwnHome() {
        assertTrue(AppManagementPolicy.canRequestUninstall(
                "org.example.fixture", "org.enterthefield.phiosmobile", true, false));
        assertFalse(AppManagementPolicy.canRequestUninstall(
                "org.example.fixture", "org.enterthefield.phiosmobile", false, false));
        assertFalse(AppManagementPolicy.canRequestUninstall(
                "com.android.settings", "org.enterthefield.phiosmobile", true, true));
        assertFalse(AppManagementPolicy.canRequestUninstall(
                "org.enterthefield.phiosmobile", "org.enterthefield.phiosmobile", true, false));
        assertFalse(AppManagementPolicy.canRequestUninstall(
                "bad/../../target", "org.enterthefield.phiosmobile", true, false));
    }
}
