package org.enterthefield.phiosmobile;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;

/** Read-only Android-provided graphics API hint; not a GPU benchmark or hardware unlock. */
public final class GpuCapabilitySummary {
    private GpuCapabilitySummary() {}

    public static String describe(Context context) {
        ActivityManager manager = context.getSystemService(ActivityManager.class);
        ConfigurationInfo info = manager == null ? null : manager.getDeviceConfigurationInfo();
        String glEs = info == null ? null : info.getGlEsVersion();
        String reported = glEs == null || glEs.isEmpty() ? "Unavailable" : glEs;
        return "Android-reported OpenGL ES version: " + reported
                + "\n\nThis is an operating-system capability hint, not a benchmark."
                + "\nGPU model, VRAM, bandwidth, thermal limits, tensor throughput and utilization:"
                + " not measured by this launcher."
                + "\n\nNo extra privileges, root, firmware access, driver changes or data upload.";
    }
}
