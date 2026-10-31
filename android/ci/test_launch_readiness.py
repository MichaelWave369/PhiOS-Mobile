"""Pure regression tests for bounded emulator startup classification."""
import unittest
import emulator_smoke as smoke


class LaunchReadinessTests(unittest.TestCase):
    def test_crash_belongs_to_phi_only(self):
        report = (
            "10-31 E AndroidRuntime: FATAL EXCEPTION: main\n"
            "10-31 E AndroidRuntime: Process: org.enterthefield.phiosmobile, PID: 123\n"
            "10-31 E AndroidRuntime: java.lang.RuntimeException: boom\n"
        )
        self.assertTrue(smoke.is_phi_crash(report))
        self.assertFalse(smoke.is_phi_crash(report, "org.enterthefield.phiosmobile.citarget"))

    def test_unrelated_android_exception_does_not_count(self):
        report = (
            "10-31 E AndroidRuntime: FATAL EXCEPTION: main\n"
            "10-31 E AndroidRuntime: Process: com.google.android.gms, PID: 100\n"
            "10-31 W System: org.enterthefield.phiosmobile was installed\n"
        )
        self.assertFalse(smoke.is_phi_crash(report))

    def test_ordinary_error_not_mistaken_for_crash(self):
        report = "10-31 W NetworkScheduler: java.lang.IllegalArgumentException: timeout\n"
        self.assertFalse(smoke.is_phi_crash(report))

    def test_resumed_activity_is_required_not_stale_stack_member(self):
        active = "  mResumedActivity: ActivityRecord{9 u0 org.enterthefield.phiosmobile/.MainActivity t12}"
        stale = "  * Hist #1: ActivityRecord{9 u0 org.enterthefield.phiosmobile/.MainActivity t12}"
        self.assertTrue(smoke.launcher_has_foreground(active))
        self.assertFalse(smoke.launcher_has_foreground(stale))

    def test_other_foreground_not_mistaken_for_launcher(self):
        data = (
            "  topResumedActivity=ActivityRecord{7 u0 com.android.settings/.Settings t1}\n"
            "  * Hist #1: ActivityRecord{9 u0 org.enterthefield.phiosmobile/.MainActivity t12}"
        )
        self.assertFalse(smoke.launcher_has_foreground(data))


if __name__ == "__main__":
    unittest.main()
