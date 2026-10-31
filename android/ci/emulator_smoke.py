#!/usr/bin/env python3
"""Black-box Android emulator smoke with a reversible HOME-role test.

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
FIXTURE_APK = ROOT / "android/fixture/build/outputs/apk/debug/fixture-debug.apk"
FIXTURE_PKG = "org.enterthefield.phiosmobile.citarget"
PKG = "org.enterthefield.phiosmobile"
COMPONENT = PKG + "/.MainActivity"
RESULTS = []
ROLE_EVIDENCE = {
    "attempted": False, "restored": False, "originalPackage": None,
    "initialRoleHolders": [], "stockPackage": None, "stockComponent": None,
    "baselineSeeded": False, "stockRestored": False,
}


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
    demand(FIXTURE_APK.is_file(), "Deterministic CI test app APK does not exist")
    demand("Success" in adb("install", "-r", str(FIXTURE_APK), timeout=120),
           "CI fixture app installation failed")
    demand("package:" in adb("shell", "pm", "path", FIXTURE_PKG),
           "Installed CI fixture package missing")
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
    start_result = adb("shell", "am", "start", "-W", "-n", COMPONENT)
    (OUT / "launch-command.txt").write_text(start_result)
    try:
        nodes = visible("PHIOS MOBILE", "home", tries=9)
    except Exception:
        print("LAUNCH COMMAND:", start_result, flush=True)
        activity = adb("shell", "dumpsys", "activity", "activities", check=False)
        (OUT / "launch-activity-dump.txt").write_text(activity[-30000:])
        logcat = adb("logcat", "-d", "-t", "1300", check=False)
        (OUT / "launch-logcat.txt").write_text(logcat[-150000:])
        keywords = ("AndroidRuntime", "FATAL EXCEPTION", "phiosmobile", "ActivityTaskManager",
                    "WindowManager", "ProcessRecord", "Force finishing", "ANR", "Exception")
        relevant = [line for line in logcat.splitlines() if any(k in line for k in keywords)]
        print("LAUNCH DIAGNOSTICS:", "\n".join(relevant[-90:]), flush=True)
        raise
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


def real_app_launch_and_shelf():
    # A deterministic launchable Android APK is installed with the emulator
    # workflow. This verifies the launcher actually opens *another* app, not
    # just a simulated UI or hardcoded system action.
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("PINNED SHELF", "shelf-before")
    demand(locate(nodes, "from pinned shelf") is not None,
           "Previously pinned app not rendered on native Home shelf")
    screenshot("06b-pinned-shelf")

    tap(visible("Search launchable apps", "fixture-search"), "Search launchable apps")
    adb("shell", "input", "text", "CI%sTarget")
    nodes = visible("PhiOS CI Target", "fixture-matched")
    demand(locate(nodes, "Tap to open") is not None,
           "Fixture app missing from real Android LauncherApps discovery")

    # Launch from the real installed-app list.
    tap(nodes, "Tap to open")
    visible("PHIOS CI TARGET LIVE", "fixture-launched-list")
    screenshot("06c-real-app-launched-from-list")

    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    nodes = visible("Search launchable apps", "fixture-return")
    tap(nodes, "Search launchable apps")
    adb("shell", "input", "text", "CI%sTarget")
    nodes = visible("Tap to open", "fixture-research")
    x, y = coordinates(locate(nodes, "Tap to open"))
    adb("shell", "input", "swipe", x, y, x, y, "1300")
    nodes = visible("Open PhiOS CI Target from pinned shelf", "fixture-shelf")
    screenshot("06d-fixture-pinned-shelf")

    # Tap the pinned shelf, not an ordinary app row.
    tap(nodes, "Open PhiOS CI Target from pinned shelf")
    visible("PHIOS CI TARGET LIVE", "fixture-launched-shelf")
    screenshot("06e-real-app-launched-from-shelf")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", COMPONENT)
    visible("Open PhiOS CI Target from pinned shelf", "fixture-shelf-reload")


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

def role_holders(output):
    """Parse only Android package names; empty output is a valid unassigned role."""
    return re.findall(r"[A-Za-z_][A-Za-z_0-9]*(?:\.[A-Za-z_][A-Za-z_0-9]*)+", output)


def home_candidates(output):
    """Identify concrete package/activity components, ignoring resolver headings."""
    pairs = re.findall(
        r"([A-Za-z_][A-Za-z_0-9]*(?:\.[A-Za-z_][A-Za-z_0-9]*)+)/([.$A-Za-z_][.$A-Za-z_0-9]*)",
        output,
    )
    return list(dict.fromkeys((package, package + "/" + activity) for package, activity in pairs))


def choose_stock_home(candidates, system_packages, original_holders):
    """Use a qualified installed system HOME candidate, never PhiOS or a resolver."""
    available = [
        (pkg, component) for pkg, component in candidates
        if pkg in system_packages and pkg != PKG
        and "fallback" not in pkg.lower()
        and "setup" not in pkg.lower()
    ]
    if original_holders:
        choices = [(pkg, component) for pkg, component in available if pkg in original_holders]
        return choices[0] if choices else None
    launchers = [candidate for candidate in available if "launcher" in candidate[0].lower()]
    return (launchers or available)[0] if available else None


def current_home_holders():
    # Explicit user 0: the disposable emulator's current/main Android user.
    raw = adb("shell", "cmd", "role", "get-role-holders", "--user", "0",
              "android.app.role.HOME")
    return role_holders(raw)


def await_role(expected, label):
    for _ in range(8):
        observed = current_home_holders()
        if observed == expected:
            return
        time.sleep(1)
    raise AssertionError(label + ": expected " + repr(expected)
                         + ", observed " + repr(current_home_holders()))


def home_role_roundtrip():
    role = "android.app.role.HOME"
    initial = current_home_holders()
    demand(len(initial) <= 1 and PKG not in initial,
           "Unexpected existing HOME role state; refusing modification: " + repr(initial))

    query = adb("shell", "cmd", "package", "query-activities", "--components",
                "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
    candidates = home_candidates(query)
    system_packages = set(
        name.removeprefix("package:").strip()
        for name in adb("shell", "pm", "list", "packages", "-s").splitlines()
        if name.startswith("package:")
    )
    stock = choose_stock_home(candidates, system_packages, initial)
    demand(stock is not None,
           "No verified system HOME candidate to restore; refusing switch. Candidates: "
           + repr(candidates))
    stock_pkg, stock_component = stock
    ROLE_EVIDENCE["originalPackage"] = initial[0] if initial else None
    ROLE_EVIDENCE["initialRoleHolders"] = initial
    ROLE_EVIDENCE["stockPackage"] = stock_pkg
    ROLE_EVIDENCE["stockComponent"] = stock_component
    ROLE_EVIDENCE["baselineSeeded"] = not bool(initial)
    (OUT / "home-candidates.txt").write_text(
        "Initial HOME holders: " + repr(initial) + "\n"
        + "HOME candidates:\n" + query + "\n"
        + "Selected system HOME: " + stock_component + "\n"
    )

    # Some fresh Android emulator images have *no* assigned HOME role. That's a
    # valid state, not a broken installation. Seed a known stock launcher ONLY
    # on this disposable emulator so reversible role switching can be tested.
    # Finally clear the role again if it was initially empty, preserving state.
    attempted_baseline = False
    try:
        if not initial:
            attempted_baseline = True
            adb("shell", "cmd", "package", "set-home-activity", stock_component)
            await_role([stock_pkg], "Seeding stock HOME before switch")
        ROLE_EVIDENCE["attempted"] = True
        adb("shell", "cmd", "package", "set-home-activity", COMPONENT)
        await_role([PKG], "Assigning PhiOS HOME")
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        visible("PHIOS MOBILE", "home-role-switched")
        screenshot("09-phi-default-home")
    finally:
        if ROLE_EVIDENCE["attempted"] or attempted_baseline:
            # Recovery does not depend on the prior assertion succeeding.
            try:
                adb("shell", "cmd", "package", "set-home-activity", stock_component)
                await_role([stock_pkg], "Recovering system stock launcher")
                ROLE_EVIDENCE["stockRestored"] = True
                adb("shell", "input", "keyevent", "KEYCODE_HOME")
                screenshot("10-stock-home-restored")
            finally:
                # A fresh emulator with no holder must finish with no holder,
                # not a falsely described 'original' launcher selection.
                if not initial:
                    adb("shell", "cmd", "role", "clear-role-holders", "--user", "0", role)
                await_role(initial, "Restoring original HOME role state")
                ROLE_EVIDENCE["restored"] = True

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
        "testFixtureApkSha256": hashlib.sha256(FIXTURE_APK.read_bytes()).hexdigest()
                                  if FIXTURE_APK.exists() else None,
        "defaultHomeChanged": ROLE_EVIDENCE["attempted"],
        "initialHomeRoleHolders": ROLE_EVIDENCE["initialRoleHolders"],
        "baselineStockHomeSeeded": ROLE_EVIDENCE["baselineSeeded"],
        "stockLauncherComponent": ROLE_EVIDENCE["stockComponent"],
        "stockHomeRecovered": ROLE_EVIDENCE["stockRestored"],
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
            ("Launch real Android fixture from list and pinned shelf", real_app_launch_and_shelf),
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
