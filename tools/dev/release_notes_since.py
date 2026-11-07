#!/usr/bin/env python3
"""Collects the GitHub release notes of every release from a first tag up to the newest, for a consolidated release note
(the 0.4.0 notes must include everything since 0.3: `python tools/dev/release_notes_since.py v0.3.0 > notes.md`, then
add the new section on top and group by theme).

usage: release_notes_since.py <first-tag> [--last <tag>]   (needs the gh CLI, authenticated)
"""
import argparse, json, subprocess, sys


def gh(*args):
    r = subprocess.run(["gh", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8")
    if r.returncode != 0:
        sys.exit(r.stderr)
    return r.stdout


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("first")
    ap.add_argument("--last", default=None)
    a = ap.parse_args()
    tags = json.loads(gh("release", "list", "--limit", "300", "--json", "tagName,publishedAt"))
    tags.sort(key=lambda t: t["publishedAt"])
    names = [t["tagName"] for t in tags]
    if a.first not in names:
        sys.exit(f"unknown tag {a.first}")
    lo = names.index(a.first)
    hi = names.index(a.last) if a.last else len(names) - 1
    for t in tags[lo:hi + 1]:
        body = json.loads(gh("release", "view", t["tagName"], "--json", "body,name"))
        print(f"## {t['tagName']} ({t['publishedAt'][:10]})\n")
        print((body.get("body") or "").strip() + "\n")


if __name__ == "__main__":
    main()
