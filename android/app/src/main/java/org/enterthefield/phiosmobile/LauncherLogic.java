package org.enterthefield.phiosmobile;

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/** Side-effect-free rules shared by native launcher logic and local unit tests. */
public final class LauncherLogic {
    private LauncherLogic() {}

    /**
     * Select up to maxItems of the installed apps in their existing stable order.
     * Unknown pinned keys never create phantom launch targets.
     */
    public static List<String> shelfKeys(List<String> available, Set<String> pinned, int maxItems) {
        List<String> result = new ArrayList<>();
        if (available == null || pinned == null || maxItems <= 0) return result;
        for (String key : new LinkedHashSet<>(available)) {
            if (key != null && pinned.contains(key)) {
                result.add(key);
                if (result.size() == maxItems) break;
            }
        }
        return result;
    }

    public static boolean matchesQuery(String label, String query) {
        if (label == null) return false;
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return label.toLowerCase(Locale.ROOT).contains(normalized);
    }

    /** Clockwise from the top: 0=N, 1=NE, 2=SE, 3=S, 4=SW, 5=NW. */
    public static int dialSector(float x, float y, float deadZone) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(deadZone)
                || deadZone < 0 || Math.hypot(x, y) < deadZone) return -1;
        double offset = Math.atan2(y, x) + Math.PI / 2;
        return Math.floorMod((int) Math.round(offset / (Math.PI / 3)), 6);
    }
}
