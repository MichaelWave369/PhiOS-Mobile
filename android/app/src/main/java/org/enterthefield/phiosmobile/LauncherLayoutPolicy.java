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
        int minimum = compact ? 56 : 64;
        return Math.max(minimum, (int) Math.ceil(44 + 20 * scale));
    }

    /** Six accessible buttons remain available when the graphic dial is reduced. */
    public static int surfDialHeightDp(int heightDp, float fontScale) {
        return heightDp < 690 || fontScale >= 1.3f ? 190 : 292;
    }
}
