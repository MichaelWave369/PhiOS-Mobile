#!/usr/bin/env python3
"""Black-box Android emulator smoke, without changing the default Home app.

A PASS proves only these limited behaviors on the specific disposable emulator.
It is not physical-device, accessibility, emergency-call, or ROM qualification.
"""
from __future__ import annotations
import hashlib
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / "out"
APK = ROOT / "android/app/build/outputs/apk/debug/app-debug.apk"
PKG = "org.enterthefield.phiosmobile"
COMPONENT = PKG + "/.MainActivity"
RESULTS = []
ROLE_EVIDENCE = {"attempted": False, "restored": False, "originalPackage": None}


def cmd(*args, timeout=60, check=True):
    r = subprocess.run(args, capture_output=True, text=True, timeout=timeout)
    if check and r.returncode:
        raise RuntimeError("Command failed: " + " ".join(args) + "\n" + r.stderr[-900:])
    return r.stdout + r.stderr

def adb(*args, **kwargs):
    return cmd("adb", *args, **kwargs)

def demand(ok, reason):
    if not ok:
        raise AssertionError(reason)

def step(name, fn):
    print("RUN", name, flush=True)
    try:
        fn()
    except Exception as exc:
        RESULTS.append({"name": name, "result": "FAIL", "detail": str(exc)[:900]})
        print("FAIL", name, str(exc), flush=True)
        raise
    else:
        RESULTS.append({"name": name, "result": "PASS"})
        print("PASS", name, flush=True)

def xml_snapshot(label):
    for attempt in range(4):
        try:
            resp = adb("shell", "uiautomator", "dump", "/sdcard/window.xml", timeout=40)
            demand("ERROR" not in resp.upper(), "UIAutomator dump failed: " + resp[-200:])
            path = OUT / (label + "-" + str(attempt) + ".xml")
            adb("pull", "/sdcard/window.xml", str(path))
            return list(ElementTree.parse(path).iter("node"))
        except Exception:
            if attempt == 3:
                raise
            time.sleep(2)

def node_label(node):
    return node.attrib.get("text", "") + " " + node.attrib.get("content-desc", "")

def locate(nodes, label):
    return next((n for n in nodes if label.casefold() in node_label(n).casefold()), None)

def visible(label, stage, tries=5):
    for i in range(tries):
        nodes = xml_snapshot(stage + str(i))
        if locate(nodes, label) is not None:
            return nodes
        time.sleep(2)
    raise AssertionError("Missing visible Android UI element: " + label)

def coordinates(node):
    bounds = node.attrib.get("bounds", "")
    found = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    demand(found is not None, "No valid UI bounds: " + bounds)
    x1, y1, x2, y2 = map(int, found.groups())
    return str((x1+x2)//2), str((y1+y2)//2)

def tap(nodes, label):
    node = locate(nodes, label)
    demand(node is not None, "No control called " + label)
    adb("shell", "input", "tap", *coordinates(node))
    time.sleep(1)

def screenshot(label):
    try:
        with (OUT / (label+".png")).open("wb") as image:
            r = subprocess.run(["adb", "exec-out", "screencap", "-p"],
                               stdout=image, stderr=subprocess.PIPE, timeout=30)
        if r.returncode:
            (OUT / (label+".png")).unlink(missing_ok=True)
    except Exception as exc:
        print("Screenshot unavailable:", exc, flush=True)

def install():
    demand(APK.is_file(), "APK does not exist")
    demand("Success" in adb("install", "-r", str(APK), timeout=120), "Install failed")
    demand("package:" in adb("shell", "pm", "path", PKG), "Package not found")

def permissions():
    aapt = Path(os.environ["ANDROID_HOME"]) / "build-tools/36.0.0/aapt"
    demand(aapt.is_file(), "Build tools aapt missing")
    report = cmd(str(aapt), "dump", "permissions", str(APK))
    (OUT / "compiled-apk-permissions.txt").write_text(report)
    for forbidden in ("INTERNET", "QUERY_ALL_PACKAGES", "READ_CONTACTS", "READ_SMS",
                      "READ_CALL_LOG", "ACCESS_FINE_LOCATION"):
        demand("android.permission." + forbidden not in report, "Unexpected permission: " + forbidden)

def launch():
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("PHIOS MOBILE", "home")
    for expected in ("YOUR APPS", "All apps", "SURF"):
        demand(locate(nodes, expected) is not None, "Home UI missing: " + expected)
    screenshot("01-native-home")

def surf_status():
    tap(visible("SURF", "before-surf"), "SURF")
    nodes = visible("ΦSURF", "surf-open")
    demand(locate(nodes, "Pinned apps") is not None, "PhiSurf button alternatives missing")
    screenshot("02-phisurf")
    tap(nodes, "Status / Home")
    nodes = visible("Choose default Home", "status")
    demand(locate(nodes, "Clear pinned list") is not None, "Status state controls absent")
    screenshot("03-status")
    tap(nodes, "Close")

def search():
    nodes = visible("Search launchable apps", "search-before")
    tap(nodes, "Search launchable apps")
    adb("shell", "input", "text", "zzphiosnomatch777")
    visible("No launchable apps match", "search-no-match")
    screenshot("04-search-no-match")

def pin():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("Tap to open", "pin-before")
    x, y = coordinates(locate(nodes, "Tap to open"))
    adb("shell", "input", "swipe", x, y, x, y, "1300")
    visible("★ Pinned", "pin-after")
    screenshot("05-pinned")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("★ Pinned", "pin-persist")
    tap(nodes, "Pinned")
    visible("★ Pinned", "pinned-filter")
    screenshot("06-pinned-filter")


def layout_preferences():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("Layout", "layout-before")
    tap(nodes, "Layout")
    nodes = visible("Home layout", "layout-dialog")
    demand(locate(nodes, "Show welcome card") is not None, "Welcome preference absent")
    demand(locate(nodes, "Compact app rows") is not None, "Compact preference absent")
    tap(nodes, "Show welcome card")
    nodes = visible("Save", "layout-save")
    tap(nodes, "Save")
    nodes = visible("PHIOS MOBILE", "layout-saved")
    demand(locate(nodes, "A different kind of mobile.") is None, "Hero remained visible after disabling")
    screenshot("07-layout-hero-hidden")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("PHIOS MOBILE", "layout-reload")
    demand(locate(nodes, "A different kind of mobile.") is None, "Home layout didn't persist")
    tap(nodes, "Layout")
    nodes = visible("Reset", "layout-reset")
    tap(nodes, "Reset")
    visible("A different kind of mobile.", "layout-restored")
    screenshot("08-layout-restored")

def home_role_roundtrip():
    role = "android.app.role.HOME"
    before = adb("shell", "cmd", "role", "get-role-holders", role)
    original = re.findall(r"[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+", before)
    demand(len(original) == 1 and original[0] != PKG,
           "Need exactly one pre-existing stock HOME package before changing role: " + before)
    original_pkg = original[0]
    ROLE_EVIDENCE["originalPackage"] = original_pkg
    resolve_before = adb("shell", "cmd", "package", "resolve-activity", "--brief",
                         "-a", "android.intent.action.MAIN",
                         "-c", "android.intent.category.HOME")
    original_component = next((line.strip() for line in reversed(resolve_before.splitlines())
                               if line.strip().startswith(original_pkg + "/")), None)
    demand(original_component is not None,
           "Could not capture the exact original Home component for restoration: " + resolve_before)
    try:
        ROLE_EVIDENCE["attempted"] = True
        adb("shell", "cmd", "package", "set-home-activity", COMPONENT)
        changed = adb("shell", "cmd", "role", "get-role-holders", role)
        demand(PKG in changed, "PhiOS Mobile was not assigned HOME role: " + changed)
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        visible("PHIOS MOBILE", "home-role-switched")
        screenshot("09-phi-default-home")
    finally:
        try:
            adb("shell", "cmd", "role", "add-role-holder", role, original_pkg)
        except RuntimeError as error:
            print("Role restoration attempt failed; trying original component:", error, flush=True)
        current = adb("shell", "cmd", "role", "get-role-holders", role, check=False)
        if original_pkg not in current or PKG in current:
            adb("shell", "cmd", "package", "set-home-activity", original_component)
        restored = adb("shell", "cmd", "role", "get-role-holders", role)
        ROLE_EVIDENCE["restored"] = original_pkg in restored and PKG not in restored
        demand(ROLE_EVIDENCE["restored"], "Original default HOME was not restored: " + restored)
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        screenshot("10-stock-home-restored")

def evidence():
    try:
        screenshot("99-final")
        for name, args in (
            ("package-dump.txt", ["shell", "dumpsys", "package", PKG]),
            ("activity-dump.txt", ["shell", "dumpsys", "activity", "activities"]),
            ("recent-logcat.txt", ["logcat", "-d", "-t", "500"]),
        ):
            (OUT / name).write_text(adb(*args, check=False)[-110000:])
    except Exception as exc:
        print("Optional diagnostic capture failed:", exc, flush=True)
    receipt = {
        "scope": "Disposable Android emulator; no physical device qualification",
        "apiLevel": adb("shell", "getprop", "ro.build.version.sdk", check=False).strip(),
        "model": adb("shell", "getprop", "ro.product.model", check=False).strip(),
        "fingerprint": adb("shell", "getprop", "ro.build.fingerprint", check=False).strip(),
        "apkSha256": hashlib.sha256(APK.read_bytes()).hexdigest() if APK.exists() else None,
        "defaultHomeChanged": ROLE_EVIDENCE["attempted"],
        "defaultHomeRestored": ROLE_EVIDENCE["restored"],
        "originalHomePackage": ROLE_EVIDENCE["originalPackage"],
        "physicalDeviceTested": False,
        "tests": RESULTS,
        "outcome": "FAIL" if any(t["result"] == "FAIL" for t in RESULTS) else "PASS",
    }
    (OUT / "emulator-receipt.json").write_text(json.dumps(receipt, indent=2)+"\n")
    print(json.dumps(receipt, indent=2), flush=True)

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    failed = False
    try:
        for name, fn in [
            ("Install debug APK", install),
            ("Check compiled APK permissions", permissions),
            ("Launch PhiOS and render app list", launch),
            ("Exercise PhiSurf and status", surf_status),
            ("Filter installed apps", search),
            ("Pin app and verify persistence", pin),
            ("Home layout survives restart and reset", layout_preferences),
            ("Temporarily select PhiOS HOME and restore stock HOME", home_role_roundtrip),
        ]:
            step(name, fn)
    except Exception:
        failed = True
    finally:
        evidence()
    return 1 if failed else 0

if __name__ == "__main__":
    sys.exit(main())
