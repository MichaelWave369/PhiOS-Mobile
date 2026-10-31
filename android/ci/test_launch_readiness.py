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


    def _stock_anr_nodes(self, title="Pixel Launcher isn't responding",
                         close_package="android"):
        from xml.etree import ElementTree
        root = ElementTree.fromstring(
            '<hierarchy>'
            '<node class="android.widget.TextView" package="android" '
            'resource-id="android:id/alertTitle" text="' + title.replace("'", "&apos;") + '" />'
            '<node class="android.widget.Button" package="' + close_package + '" '
            'resource-id="android:id/aerr_close" text="Close app" bounds="[70,1170][1010,1296]" />'
            '</hierarchy>'
        )
        return list(root.iter("node"))

    def test_stock_pixel_launcher_anr_can_be_dismissed_only_when_focused(self):
        nodes = self._stock_anr_nodes()
        focused = ("mFocusedWindow=Window{5bf875d u0 Application Not Responding: "
                   "com.google.android.apps.nexuslauncher}")
        close = smoke.stock_launcher_anr_close_button(nodes, focused)
        # ElementTree.iter("node") excludes the <hierarchy> root, so the
        # two real Android UI nodes occupy indices 0 and 1.
        self.assertEqual(len(nodes), 2)
        self.assertIs(close, nodes[1])
        self.assertEqual(smoke.coordinates(close), ("540", "1233"))

    def test_other_app_anr_cannot_be_dismissed(self):
        nodes = self._stock_anr_nodes()
        for focused in (
            "mFocusedWindow=Window{1 u0 Application Not Responding: org.enterthefield.phiosmobile}",
            "mFocusedWindow=Window{1 u0 Application Not Responding: com.example.fake}",
            "mFocusedWindow=Window{1 u0 org.enterthefield.phiosmobile/.MainActivity}",
            "",
        ):
            self.assertIsNone(smoke.stock_launcher_anr_close_button(nodes, focused))

    def test_stock_anr_requires_exact_system_title_and_button(self):
        focused = ("mFocusedWindow=Window{5bf875d u0 Application Not Responding: "
                   "com.google.android.apps.nexuslauncher}")
        self.assertIsNone(smoke.stock_launcher_anr_close_button(
            self._stock_anr_nodes("PhiOS isn't responding"), focused))
        self.assertIsNone(smoke.stock_launcher_anr_close_button(
            self._stock_anr_nodes(close_package="com.attacker.fake"), focused))
        from xml.etree import ElementTree
        only_title = list(ElementTree.fromstring(
            '<hierarchy><node class="android.widget.TextView" package="android" '
            'resource-id="android:id/alertTitle" text="Pixel Launcher isn&apos;t responding" />'
            '</hierarchy>').iter("node"))
        self.assertIsNone(smoke.stock_launcher_anr_close_button(only_title, focused))


if __name__ == "__main__":
    unittest.main()
