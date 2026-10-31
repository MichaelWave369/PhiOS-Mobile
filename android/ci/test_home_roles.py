"""Local unit tests for Android emulator HOME role discovery.

These run without an emulator. They never invoke ADB or modify a phone.
"""
import unittest
import emulator_smoke as smoke


class HomeRoleParsingTests(unittest.TestCase):
    def test_fresh_emulator_has_no_role_holder(self):
        self.assertEqual(smoke.role_holders(""), [])
        self.assertEqual(smoke.role_holders("[]\n"), [])

    def test_existing_role_holder(self):
        self.assertEqual(
            smoke.role_holders("com.google.android.apps.nexuslauncher\n"),
            ["com.google.android.apps.nexuslauncher"],
        )

    def test_candidates_from_android_package_query(self):
        output = (
            "3 activities found:\n"
            "  com.android.launcher3/.Launcher\n"
            "  org.enterthefield.phiosmobile/.MainActivity\n"
            "  com.android.settings/.FallbackHome\n"
        )
        self.assertEqual(smoke.home_candidates(output), [
            ("com.android.launcher3", "com.android.launcher3/.Launcher"),
            ("org.enterthefield.phiosmobile",
             "org.enterthefield.phiosmobile/.MainActivity"),
            ("com.android.settings", "com.android.settings/.FallbackHome"),
        ])

    def test_stock_discovery_with_no_assigned_home(self):
        choices = [
            ("org.enterthefield.phiosmobile",
             "org.enterthefield.phiosmobile/.MainActivity"),
            ("com.android.launcher3", "com.android.launcher3/.Launcher"),
        ]
        self.assertEqual(smoke.choose_stock_home(
            choices,
            {"com.android.launcher3", "org.enterthefield.phiosmobile"},
            []
        ), ("com.android.launcher3", "com.android.launcher3/.Launcher"))

    def test_stock_cannot_be_non_system_or_our_app(self):
        choices = [
            ("org.enterthefield.phiosmobile",
             "org.enterthefield.phiosmobile/.MainActivity"),
            ("com.random.fake", "com.random.fake/.Home"),
        ]
        self.assertIsNone(smoke.choose_stock_home(choices, {"com.random.fake"}, []))

    def test_original_holder_must_be_qualified_candidate(self):
        choices = [("com.android.launcher3", "com.android.launcher3/.Launcher")]
        self.assertIsNone(smoke.choose_stock_home(
            choices, {"com.android.launcher3"}, ["com.other.stock"]
        ))


if __name__ == "__main__":
    unittest.main()
