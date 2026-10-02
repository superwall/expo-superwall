#!/usr/bin/env python3
"""Print the CHANGELOG.md sections of a native Superwall SDK between two versions.

Usage: native-changelog.py <changelog-path> <old-version> <new-version>

Prints every "## <version>" section with old < version <= new, newest first,
headings included. A wrapper can lag several native releases behind, so the
whole range matters, not just the newest entry.

Only "## <semver>" lines count as section boundaries: Superwall-Android also
uses "## Fixes" / "## Enhancements" as sub-headings inside a version.
"""
import re
import sys

HEADING = re.compile(r"^##\s+v?(\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?)\s*$")


def key(version):
    core, _, pre = version.partition("-")
    # A release sorts after its own prereleases (2.9.0-beta.1 < 2.9.0).
    return tuple(int(p) for p in core.split(".")), pre == "", pre


def main():
    path, old, new = sys.argv[1:4]
    lo, hi = key(old), key(new)
    out, keep = [], False
    with open(path, encoding="utf-8") as f:
        for line in f:
            m = HEADING.match(line.rstrip("\n"))
            if m:
                keep = lo < key(m.group(1)) <= hi
            if keep:
                out.append(line)
    sys.stdout.write("".join(out).strip() + "\n" if out else "")


if __name__ == "__main__":
    main()
