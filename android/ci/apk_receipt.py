#!/usr/bin/env python3
"""Fail-closed provenance receipt and SHA-256 for the CI-built DEBUG test APK.

This does NOT produce a release-signed APK or establish reproducible builds.
Only the exact APK in this bundle is identified by the evidence.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile
from pathlib import Path

PACKAGE_ID = "org.enterthefield.phiosmobile"
EXPECTED_LAUNCHER = PACKAGE_ID + ".MainActivity"


def required(pattern, data, label):
    match = re.search(pattern, data, flags=re.MULTILINE)
    if not match:
        raise ValueError("Missing " + label + " in build evidence")
    return match


def gradle_expectations(text):
    return {
        "versionCode": int(required(r"^\s*versionCode\s+(\d+)\s*$", text, "versionCode").group(1)),
        "versionName": required(r"^\s*versionName\s+'([^']+)'\s*$", text, "versionName").group(1),
        "minSdk": int(required(r"^\s*minSdk\s+(\d+)\s*$", text, "minSdk").group(1)),
        "targetSdk": int(required(r"^\s*targetSdk\s+(\d+)\s*$", text, "targetSdk").group(1)),
    }


def parse_badging(text):
    package = required(
        r"^package:\s+name='([^']+)'\s+versionCode='(\d+)'\s+versionName='([^']+)'",
        text, "APK package/version",
    )
    min_sdk = required(r"^sdkVersion:'(\d+)'", text, "APK min SDK")
    target_sdk = required(r"^targetSdkVersion:'(\d+)'", text, "APK target SDK")
    launcher = required(r"^launchable-activity:\s+name='([^']+)'",
                        text, "launchable Android activity")
    return {
        "packageId": package.group(1),
        "versionCode": int(package.group(2)),
        "versionName": package.group(3),
        "minSdk": int(min_sdk.group(1)),
        "targetSdk": int(target_sdk.group(1)),
        "launchableActivity": launcher.group(1),
    }


def parse_permissions(text):
    return sorted(set(re.findall(
        r"^(?:uses-permission|uses-permission-sdk-23):\s+name='([^']+)'",
        text, flags=re.MULTILINE,
    )))


def parse_debug_signature(text):
    # apksigner verifies the APK signature before we parse the certificate.
    digest = required(
        r"^Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]{64})\s*$",
        text, "signing-certificate SHA-256",
    ).group(1).lower()
    dn = required(r"^Signer #1 certificate DN:\s*(.+)$",
                  text, "signer certificate subject").group(1)
    if "CN=Android Debug" not in dn:
        raise ValueError("This handoff is debug-only; an unexpected signer was found")
    return {"sha256": digest, "subject": dn, "trust": "ephemeral Android debug signer; NOT a release key"}


def run_tool(*args):
    proc = subprocess.run(args, text=True, capture_output=True, timeout=120)
    if proc.returncode:
        raise RuntimeError("Verification tool failed: " + args[0]
                           + "\n" + proc.stderr[-900:])
    return proc.stdout + proc.stderr


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as inp:
        for chunk in iter(lambda: inp.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def produce(apk, gradle, aapt, apksigner, out):
    if not apk.is_file() or not gradle.is_file():
        raise ValueError("Missing APK or Android build.gradle")
    expected = gradle_expectations(gradle.read_text(encoding="utf-8"))
    with zipfile.ZipFile(apk) as archive:
        if "AndroidManifest.xml" not in archive.namelist():
            raise ValueError("Not a valid Android APK with a manifest")
        corrupt = archive.testzip()
        if corrupt:
            raise ValueError("Corrupted APK archive member: " + corrupt)

    package = parse_badging(run_tool(str(aapt), "dump", "badging", str(apk)))
    if package["packageId"] != PACKAGE_ID:
        raise ValueError("Unexpected application ID: " + package["packageId"])
    if package["launchableActivity"] != EXPECTED_LAUNCHER:
        raise ValueError("Unexpected launchable Activity")
    for field, value in expected.items():
        if package[field] != value:
            raise ValueError("APK " + field + " differs from Gradle configuration")

    permissions = parse_permissions(run_tool(str(aapt), "dump", "permissions", str(apk)))
    if permissions:
        raise ValueError("Native launcher unexpectedly requests Android permissions: "
                         + ", ".join(permissions))
    signature = parse_debug_signature(
        run_tool(str(apksigner), "verify", "--verbose", "--print-certs", str(apk))
    )

    out.mkdir(parents=True, exist_ok=True)
    target = out / "app-debug.apk"
    shutil.copyfile(apk, target)
    before, after = sha256_file(apk), sha256_file(target)
    if before != after:
        raise ValueError("APK SHA-256 changed during artifact packaging")
    receipt = {
        "schema": "org.enterthefield.phiosmobile.debug-apk-receipt",
        "version": 1,
        "artifactClass": "DEBUG_TEST_ONLY",
        "releaseQualified": False,
        "deviceQualified": False,
        "reproducibleBuildProven": False,
        "sha256": after,
        "bytes": target.stat().st_size,
        "android": package,
        "requestedAndroidPermissions": permissions,
        "signature": signature,
        "build": {
            "repository": os.environ.get("GITHUB_REPOSITORY", "local"),
            "sourceCommit": os.environ.get("PHIOS_SOURCE_SHA",
                                           os.environ.get("GITHUB_SHA", "local-untracked")),
            "ciCheckoutSha": os.environ.get("GITHUB_SHA", "local-untracked"),
            "workflowRun": os.environ.get("GITHUB_RUN_ID", "local"),
            "event": os.environ.get("GITHUB_EVENT_NAME", "local"),
        },
    }
    (out / "receipt.json").write_text(
        json.dumps(receipt, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    (out / "SHA256SUMS").write_text(after + "  app-debug.apk\n", encoding="utf-8")
    (out / "README.txt").write_text(
        "PhiOS Mobile native Android DEBUG TEST APK\n"
        "Not a release-signed package. Do not treat this as a production release.\n"
        "Check SHA256SUMS against app-debug.apk before installing.\n"
        "The certificate shown in receipt.json is an ephemeral CI debug identity.\n"
        "Different CI runs may produce different debug keys and incompatible updates.\n"
        "Back up your launcher settings; test only on a spare device.\n"
        "To roll back, choose your stock Home under Android Default apps, then uninstall.\n"
        "No firmware, bootloader, cellular or emergency-function qualification is implied.\n",
        encoding="utf-8",
    )
    print("PASS verified debug APK SHA-256:", after, flush=True)
    print("PASS package", package["packageId"], package["versionName"], flush=True)
    print("PASS no requested Android permissions; debug certificate verified", flush=True)
    return receipt


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--gradle", type=Path, required=True)
    parser.add_argument("--aapt", type=Path, required=True)
    parser.add_argument("--apksigner", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    produce(args.apk, args.gradle, args.aapt, args.apksigner, args.out)


if __name__ == "__main__":
    main()
