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
                         tree[0])
        selected = smoke.select_document_filename(tree, "phios-launcher-profile-v1.json")
        self.assertIs(selected, tree[1])
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

    def test_restore_button_not_restore_dialog_title(self):
        tree = nodes("""<hierarchy>
            <node class="android.widget.TextView" text="Preview launcher restore"
                  resource-id="android:id/alertTitle" />
            <node class="android.widget.Button" text="CANCEL" resource-id="android:id/button2" />
            <node class="android.widget.Button" text="RESTORE" resource-id="android:id/button1"
                  bounds="[600,1300][960,1400]" />
        </hierarchy>""")
        self.assertIs(smoke.locate(tree, "Restore"), tree[0])
        self.assertIs(smoke.select_dialog_button(tree, "Restore"), tree[2])
        self.assertEqual(smoke.coordinates(tree[2]), ("780", "1350"))

    def test_missing_restore_button_is_not_silently_accepted(self):
        tree = nodes("""<hierarchy><node class="android.widget.TextView"
            text="Preview launcher restore" /></hierarchy>""")
        with self.assertRaises(AssertionError):
            smoke.select_dialog_button(tree, "Restore")

    def test_refuse_near_match(self):
        tree = nodes("""<hierarchy><node class="android.widget.TextView"
            text="phios-launcher-profile-v1.json.bak" resource-id="android:id/title" /></hierarchy>""")
        with self.assertRaises(AssertionError):
            smoke.select_document_filename(tree, "phios-launcher-profile-v1.json")


if __name__ == "__main__":
    unittest.main()
