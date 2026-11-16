#!/usr/bin/env python3
"""Checks every relative link in docs/wiki/*.md (and README.md): the target file must exist and, when the link has a
#anchor, the heading must exist in it. Also lists wiki pages that Home.md does not link. Exit code 1 when a link is broken.

usage: python tools/dev/check_wiki_links.py [--no-anchors]
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
WIKI = ROOT / "docs" / "wiki"
LINK = re.compile(r"(?<!!)\[[^\]\n]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")


def slug(heading):
    h = re.sub(r"[`*_]|<[^>]+>", "", heading.strip().lower())
    h = re.sub(r"[^\w\- ]", "", h)
    return h.replace(" ", "-")


def anchors(path):
    out, fence = set(), False
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("```"):
            fence = not fence
        m = None if fence else re.match(r"^#{1,6}\s+(.*)$", line)
        if m:
            out.add(slug(m.group(1)))
    return out


def main():
    check_anchors = "--no-anchors" not in sys.argv
    broken, cache = [], {}
    files = sorted(WIKI.glob("*.md")) + [ROOT / "README.md"]
    for f in files:
        fence = False
        for n, line in enumerate(f.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
            if line.startswith("```"):
                fence = not fence
            if fence:
                continue
            for target in LINK.findall(line):
                if re.match(r"^(https?:|mailto:)", target):
                    continue
                path, _, frag = target.partition("#")
                dest = f if not path else (f.parent / path).resolve()
                if not dest.exists():
                    broken.append(f"{f.relative_to(ROOT)}:{n}: missing file {target}")
                elif frag and check_anchors and dest.suffix == ".md":
                    a = cache.setdefault(dest, anchors(dest))
                    if frag.lower() not in a:
                        broken.append(f"{f.relative_to(ROOT)}:{n}: missing anchor {target}")
    home = (WIKI / "Home.md").read_text(encoding="utf-8")
    unlinked = [p.name for p in sorted(WIKI.glob("*.md")) if not p.name.startswith("_") and p.name != "Home.md" and p.name not in home]
    for b in broken:
        print("BROKEN", b)
    for u in unlinked:
        print("NOT IN HOME", u)
    print(f"{len(broken)} broken link(s), {len(unlinked)} page(s) not linked from Home.md")
    return 1 if broken else 0


if __name__ == "__main__":
    sys.exit(main())
