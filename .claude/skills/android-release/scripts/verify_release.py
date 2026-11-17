#!/usr/bin/env python3
"""Check the release outputs of the :app module.

Run from the repo root after `./gradlew clean bundleRelease assembleRelease`:
    python .claude/skills/android-release/scripts/verify_release.py
    python .claude/skills/android-release/scripts/verify_release.py --apk dist/v1.0.1/audioquran-v1.0.1.apk --aab dist/v1.0.1/audioquran-v1.0.1.aab

Checks:
  - the APK is signed (apksigner verify) and which schemes (v1/v2/v3) passed
  - the AAB is signed (jarsigner -verify)
  - the signing certificate is not the Android debug certificate
  - mapping.txt exists (R8 ran)
  - release APK size against the debug APK, when a debug build exists

Exits 1 if any check fails, 0 otherwise. Never prints keystore passwords
(it doesn't read keystore.properties at all).
"""
import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

OUT = Path("app/build/outputs")
IS_WINDOWS = os.name == "nt"


def mb(n: int) -> str:
    return f"{n / (1024 * 1024):.1f} MB"


def run(cmd: list[str]) -> tuple[int, str]:
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, errors="replace")
    except FileNotFoundError as e:
        return 127, str(e)
    return p.returncode, (p.stdout + p.stderr).strip()


def sdk_dir() -> Path | None:
    lp = Path("local.properties")
    if lp.is_file():
        for line in lp.read_text(encoding="utf-8", errors="replace").splitlines():
            if line.strip().startswith("sdk.dir="):
                # .properties escapes: "C\:\\Users\\..." -> "C:\Users\..."
                raw = line.split("=", 1)[1].strip()
                return Path(raw.replace("\\:", ":").replace("\\\\", "\\"))
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            return Path(os.environ[var])
    return None


def version_key(name: str) -> tuple:
    return tuple(int(x) if x.isdigit() else -1 for x in re.split(r"[.\-]", name))


def find_apksigner(sdk: Path | None) -> Path | None:
    if not sdk:
        return None
    bt = sdk / "build-tools"
    if not bt.is_dir():
        return None
    exe = "apksigner.bat" if IS_WINDOWS else "apksigner"
    for d in sorted(bt.iterdir(), key=lambda p: version_key(p.name), reverse=True):
        if (d / exe).is_file():
            return d / exe
    return None


def jdk_tool(name: str) -> str:
    home = os.environ.get("JAVA_HOME")
    if home:
        p = Path(home) / "bin" / (name + (".exe" if IS_WINDOWS else ""))
        if p.is_file():
            return str(p)
    return name  # hope it's on PATH


def first(pattern: str) -> Path | None:
    hits = sorted(OUT.glob(pattern))
    return hits[0] if hits else None


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apk", type=Path, help="release APK (default: app/build/outputs/apk/release/*.apk)")
    ap.add_argument("--aab", type=Path, help="release AAB (default: app/build/outputs/bundle/release/*.aab)")
    args = ap.parse_args()

    failures: list[str] = []
    warnings: list[str] = []

    apk = args.apk or first("apk/release/*.apk")
    aab = args.aab or first("bundle/release/*.aab")
    mapping = OUT / "mapping/release/mapping.txt"
    debug_apk = first("apk/debug/*.apk")

    # --- APK ---
    print("APK")
    if not apk or not apk.is_file():
        failures.append("no release APK found (did assembleRelease run?)")
        print("  missing")
    else:
        print(f"  {apk}  {mb(apk.stat().st_size)}")
        if "unsigned" in apk.name:
            failures.append(f"{apk.name} is unsigned: release signingConfig is not set up or keystore.properties is missing")
        signer = find_apksigner(sdk_dir())
        if not signer:
            failures.append("apksigner not found under <sdk.dir>/build-tools (check local.properties)")
        else:
            code, out = run([str(signer), "verify", "--verbose", "--print-certs", str(apk)])
            schemes = re.findall(r"Verified using (v[\d.]+) scheme.*?: true", out)
            digest = re.search(r"certificate SHA-256 digest: ([0-9a-fA-F]+)", out)
            dn = re.search(r"certificate DN: (.+)", out)
            if code == 0:
                print(f"  signature: verified ({', '.join(schemes) or 'schemes not listed'})")
                if dn:
                    print(f"  cert DN: {dn.group(1).strip()}")
                if digest:
                    print(f"  cert SHA-256: {digest.group(1).upper()}")
                if dn and "Android Debug" in dn.group(1):
                    failures.append("APK is signed with the Android DEBUG certificate, not the release key")
                if not any(s.startswith(("v2", "v3")) for s in schemes):
                    warnings.append("APK lacks a v2/v3 signature; Android 11+ requires v2 or later for targetSdk 30+")
            else:
                failures.append("apksigner verify failed on the APK")
                print("  signature: NOT verified")
                print("  " + "\n  ".join(out.splitlines()[:8]))

    # --- AAB ---
    print("AAB")
    if not aab or not aab.is_file():
        failures.append("no release AAB found (did bundleRelease run?)")
        print("  missing")
    else:
        print(f"  {aab}  {mb(aab.stat().st_size)}")
        code, out = run([jdk_tool("jarsigner"), "-verify", str(aab)])
        if code == 0 and "jar verified" in out and "unsigned" not in out:
            print("  signature: jar verified")
            code2, certs = run([jdk_tool("keytool"), "-printcert", "-jarfile", str(aab)])
            sha = re.search(r"SHA256:\s*([0-9A-F:]+)", certs)
            owner = re.search(r"Owner:\s*(.+)", certs)
            if owner:
                print(f"  cert owner: {owner.group(1).strip()}")
            if sha:
                print(f"  cert SHA-256: {sha.group(1)}")
            if owner and "Android Debug" in owner.group(1):
                failures.append("AAB is signed with the Android DEBUG certificate, not the release key")
        else:
            failures.append("AAB is not signed (jarsigner -verify did not report 'jar verified')")
            print("  signature: NOT verified")
            print("  " + (out.splitlines()[0] if out else "(no output)"))

    # --- R8 ---
    print("R8")
    if mapping.is_file():
        print(f"  mapping.txt present ({mb(mapping.stat().st_size)})")
    else:
        failures.append("mapping.txt missing: R8 did not run, or minify is off")
        print("  mapping.txt missing")
    if apk and apk.is_file() and debug_apk:
        ratio = apk.stat().st_size / debug_apk.stat().st_size
        print(f"  release APK is {ratio:.0%} of debug ({mb(apk.stat().st_size)} vs {mb(debug_apk.stat().st_size)})")
        if ratio > 0.8:
            warnings.append("release APK is close to debug size; check that minify/shrinkResources are actually on")
    else:
        print("  no debug APK to compare against (run assembleDebug for a size comparison)")

    print()
    for w in warnings:
        print(f"WARN  {w}")
    for f in failures:
        print(f"FAIL  {f}")
    if not failures:
        print("OK    release outputs are signed and minified")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
