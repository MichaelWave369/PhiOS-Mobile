"""Regression tests for Android DocumentsUI title-vs-preview selection.

The API 35 Google DocumentsUI XML lists an interactive preview icon *before*
the real file's android:id/title text. Selecting the first substring match
tries an unsupported preview instead of returning a document URI.
"""
import unittest
from xml.etree import ElementTree
import emulator_smoke as smoke


def nodes(xml):
    return list(ElementTree.fromstring(xml).iter("node"))


class DocumentsUISelectionTests(unittest.TestCase):
    def test_select_exact_filename_not_preview_icon(self):
        tree = nodes("""<hierarchy>
            <node class="android.widget.FrameLayout"
                content-desc="Preview the file phios-launcher-profile-v1.json"
                resource-id="com.google.android.documentsui:id/preview_icon"
                text="" bounds="[403,675][529,801]" />
            <node class="android.widget.TextView"
                content-desc=""
                resource-id="android:id/title"
                text="phios-launcher-profile-v1.json"
                bounds="[196,1161][497,1212]" />
        </hierarchy>""")
        # Confirm the prior faulty substring-based lookup would have hit
        # DocumentsUI's preview control instead of the desired row.
        self.assertEqual(smoke.locate(tree, "phios-launcher-profile-v1.json"),
                         tree[1])
        selected = smoke.select_document_filename(tree, "phios-launcher-profile-v1.json")
        self.assertIs(selected, tree[2])
        self.assertEqual(smoke.coordinates(selected), ("346", "1186"))

    def test_fail_closed_if_only_preview_icon_exists(self):
        tree = nodes("""<hierarchy><node class="android.widget.FrameLayout"
            content-desc="Preview the file phios-launcher-profile-v1.json"
            resource-id="com.google.android.documentsui:id/preview_icon" text=""
            bounds="[403,675][529,801]" /></hierarchy>""")
        with self.assertRaises(AssertionError):
            smoke.select_document_filename(tree, "phios-launcher-profile-v1.json")

    def test_fail_closed_if_file_title_is_ambiguous(self):
        tree = nodes("""<hierarchy>
            <node class="android.widget.TextView" text="profile.json" resource-id="android:id/title" />
            <node class="android.widget.TextView" text="profile.json" resource-id="android:id/title" />
        </hierarchy>""")
        with self.assertRaises(AssertionError):
            smoke.select_document_filename(tree, "profile.json")

    def test_refuse_near_match(self):
        tree = nodes("""<hierarchy><node class="android.widget.TextView"
            text="phios-launcher-profile-v1.json.bak" resource-id="android:id/title" /></hierarchy>""")
        with self.assertRaises(AssertionError):
            smoke.select_document_filename(tree, "phios-launcher-profile-v1.json")


if __name__ == "__main__":
    unittest.main()
