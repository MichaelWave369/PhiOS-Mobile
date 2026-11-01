package org.enterthefield.phiosmobile;

/** Pure layout decisions for narrow Android screens and enlarged system text. */
public final class LauncherLayoutPolicy {
    private LauncherLayoutPolicy() {}

    /** Keep actions readable rather than compressing four buttons into a row. */
    public static boolean stackActions(int widthDp, float fontScale) {
        return widthDp < 390 || fontScale >= 1.3f;
    }

    /** Give longer labels room at larger Android font scales. */
    public static int appRowHeightDp(boolean compact, float fontScale) {
        float scale = Math.max(1f, fontScale);
        int baseline = compact ? 56 : 64;
        // Preserve the compact preference at normal scale and add vertical
        // room for the separate label/status lines when Android enlarges text.
        return baseline + (int) Math.ceil(28f * (scale - 1f));
    }

    /** Six accessible buttons remain available when the graphic dial is reduced. */
    public static int surfDialHeightDp(int heightDp, float fontScale) {
        return heightDp < 690 || fontScale >= 1.3f ? 190 : 292;
    }
}
