"""Pure regression tests for Android debug-test artifact provenance."""
import unittest
import apk_receipt as receipt


BADGING = """package: name='org.enterthefield.phiosmobile' versionCode='2' versionName='0.2.0-alpha.1' platformBuildVersionName=''
sdkVersion:'26'
targetSdkVersion:'35'
application-label:'PhiOS Mobile'
launchable-activity: name='org.enterthefield.phiosmobile.MainActivity'  label='PhiOS Mobile' icon=''
"""
SIGNATURE = """Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: CN=Android Debug, O=Android, C=US
Signer #1 certificate SHA-256 digest: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
"""


class ApkReceiptTests(unittest.TestCase):
    def test_parse_badging(self):
        data = receipt.parse_badging(BADGING)
        self.assertEqual(data["packageId"], receipt.PACKAGE_ID)
        self.assertEqual(data["launchableActivity"], receipt.EXPECTED_LAUNCHER)
        self.assertEqual(data["versionCode"], 2)
        self.assertEqual(data["versionName"], "0.2.0-alpha.1")
        self.assertEqual(data["minSdk"], 26)
        self.assertEqual(data["targetSdk"], 35)

    def test_missing_required_fields_fail_closed(self):
        for altered in [
            BADGING.replace("launchable-activity:", "not-launchable:"),
            BADGING.replace("targetSdkVersion:", "notSdkVersion:"),
            BADGING.replace("versionCode='2'", "versionCode='incorrect'"),
        ]:
            with self.assertRaises(ValueError):
                receipt.parse_badging(altered)

    def test_permission_parser(self):
        self.assertEqual(receipt.parse_permissions("package: org.enterthefield.phiosmobile\n"), [])
        bad = (
            "uses-permission: name='android.permission.INTERNET'\n"
            "uses-permission-sdk-23: name='android.permission.READ_SMS'\n"
        )
        self.assertEqual(receipt.parse_permissions(bad), [
            "android.permission.INTERNET", "android.permission.READ_SMS"
        ])

    def test_signed_debug_cert(self):
        cert = receipt.parse_debug_signature(SIGNATURE)
        self.assertEqual(cert["sha256"], "a" * 64)
        self.assertIn("Android Debug", cert["subject"])
        self.assertIn("NOT a release key", cert["trust"])

    def test_wrong_signer_and_missing_digest_fail_closed(self):
        with self.assertRaises(ValueError):
            receipt.parse_debug_signature(SIGNATURE.replace("CN=Android Debug", "CN=Release"))
        with self.assertRaises(ValueError):
            receipt.parse_debug_signature(SIGNATURE.replace("a" * 64, "abc"))

    def test_gradle_expected_metadata(self):
        values = receipt.gradle_expectations(
            "versionCode 2\nversionName '0.2.0-alpha.1'\nminSdk 26\ntargetSdk 35\n"
        )
        self.assertEqual(values, {"versionCode": 2, "versionName": "0.2.0-alpha.1",
                                  "minSdk": 26, "targetSdk": 35})


if __name__ == "__main__":
    unittest.main()
