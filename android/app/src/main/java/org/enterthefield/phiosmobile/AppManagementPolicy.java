package org.enterthefield.phiosmobile;

/**
 * Strict current-user app management R5 policy.
 * The OS remains solely responsible for actual uninstall, permissions,
 * updates, restore points and any resulting dialogs or failure.
 */
public final class AppManagementPolicy {
    private AppManagementPolicy() {}

    public static boolean canRequestUninstall(
            String targetPackage, String ownPackage, boolean installedLaunchable,
            boolean systemOrUpdatedSystemApp) {
        return installedLaunchable
                && !systemOrUpdatedSystemApp
                && targetPackage != null
                && ownPackage != null
                && !targetPackage.equals(ownPackage)
                && targetPackage.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }

    public static boolean mayOpenAppInfo(String targetPackage, boolean installedLaunchable) {
        return installedLaunchable && targetPackage != null
                && targetPackage.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }
}
