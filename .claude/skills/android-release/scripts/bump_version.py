#!/usr/bin/env python3
"""Bump versionCode and versionName in app/build.gradle.kts.

versionCode always goes up by exactly 1. versionName is treated as
MAJOR.MINOR.PATCH (a short name like "1.0" is read as 1.0.0).

Usage (run from the repo root):
    python .claude/skills/android-release/scripts/bump_version.py patch
    python .claude/skills/android-release/scripts/bump_version.py minor --dry-run
    python .claude/skills/android-release/scripts/bump_version.py --set 2.0.0

Prints one JSON object with the old and new values. Exits non-zero, without
writing, if the file has zero or several versionCode/versionName lines, or if
--set would not move the version forward.
"""
import argparse
import json
import re
import sys
from pathlib import Path

CODE_RE = re.compile(r'^(\s*versionCode\s*=\s*)(\d+)(\s*)$', re.MULTILINE)
NAME_RE = re.compile(r'^(\s*versionName\s*=\s*")([^"]*)("\s*)$', re.MULTILINE)


def parse_semver(name: str) -> tuple[int, int, int]:
    parts = name.strip().split(".")
    if not 1 <= len(parts) <= 3 or not all(p.isdigit() for p in parts):
        raise ValueError(f'versionName "{name}" is not MAJOR[.MINOR[.PATCH]] with numbers only')
    nums = [int(p) for p in parts] + [0] * (3 - len(parts))
    return nums[0], nums[1], nums[2]


def bump(ver: tuple[int, int, int], part: str) -> tuple[int, int, int]:
    major, minor, patch = ver
    if part == "major":
        return major + 1, 0, 0
    if part == "minor":
        return major, minor + 1, 0
    return major, minor, patch + 1


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    group = ap.add_mutually_exclusive_group(required=True)
    group.add_argument("part", nargs="?", choices=["major", "minor", "patch"])
    group.add_argument("--set", dest="set_to", metavar="X.Y.Z", help="set versionName explicitly")
    ap.add_argument("--file", default="app/build.gradle.kts", help="path to the module build file")
    ap.add_argument("--dry-run", action="store_true", help="print the change without writing")
    args = ap.parse_args()

    path = Path(args.file)
    if not path.is_file():
        print(f"error: {path} not found (run from the repo root or pass --file)", file=sys.stderr)
        return 2
    # Read/write bytes so CRLF line endings survive untouched.
    text = path.read_bytes().decode("utf-8")

    codes = CODE_RE.findall(text)
    names = NAME_RE.findall(text)
    if len(codes) != 1 or len(names) != 1:
        print(
            f"error: expected exactly one literal versionCode and versionName line in {path}, "
            f"found {len(codes)} and {len(names)}. Edit it by hand.",
            file=sys.stderr,
        )
        return 2

    old_code = int(codes[0][1])
    old_name = names[0][1]
    try:
        old_ver = parse_semver(old_name)
        new_ver = parse_semver(args.set_to) if args.set_to else bump(old_ver, args.part)
    except ValueError as e:
        print(f"error: {e}", file=sys.stderr)
        return 2
    if new_ver <= old_ver:
        print(f"error: {'.'.join(map(str, new_ver))} is not newer than {old_name}", file=sys.stderr)
        return 2

    new_code = old_code + 1
    new_name = ".".join(map(str, new_ver))

    if not args.dry_run:
        text = CODE_RE.sub(lambda m: f"{m.group(1)}{new_code}{m.group(3)}", text)
        text = NAME_RE.sub(lambda m: f"{m.group(1)}{new_name}{m.group(3)}", text)
        path.write_bytes(text.encode("utf-8"))

    print(json.dumps({
        "file": str(path),
        "old": {"versionCode": old_code, "versionName": old_name},
        "new": {"versionCode": new_code, "versionName": new_name},
        "tag": f"v{new_name}",
        "written": not args.dry_run,
    }, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
