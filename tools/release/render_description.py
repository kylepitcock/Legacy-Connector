#!/usr/bin/env python3
"""Renders .github/description/description.md for one platform.

Usage: render_description.py <modrinth|curseforge> [out.md]

{{buttons}} becomes logo buttons to the other platform and GitHub, {{links}} plain links to the same.
The leading HTML comment (notes for editors) is dropped.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCE = ROOT / ".github" / "description" / "description.md"

# Same project ids as .github/workflows/publish.yml (modrinth-id, curseforge-id).
MODRINTH = ("Modrinth", "https://modrinth.com/mod/legacyconnector", "00AF5C", "modrinth")
CURSEFORGE = ("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/legacy-connector", "F16436", "curseforge")
GITHUB = ("GitHub", "https://github.com/kylepitcock/Legacy-Connector", "181717", "github")


def button(name, url, color, logo):
    badge = f"https://img.shields.io/badge/{name}-{color}?style=for-the-badge&logo={logo}&logoColor=white"
    return f"[![{name}]({badge})]({url})"


def render(platform):
    targets = {"modrinth": [CURSEFORGE, GITHUB], "curseforge": [MODRINTH, GITHUB]}[platform]
    text = SOURCE.read_text(encoding="utf-8")
    text = re.sub(r"\A\s*<!--.*?-->\s*", "", text, flags=re.S)
    text = text.replace("{{buttons}}", " ".join(button(*t) for t in targets))
    labels = {"GitHub": "Source & issue tracker (GitHub)"}
    text = text.replace("{{links}}", " · ".join(f"[{labels.get(n, n)}]({u})" for n, u, _, _ in targets))
    return text


if __name__ == "__main__":
    out = render(sys.argv[1])
    if len(sys.argv) > 2:
        pathlib.Path(sys.argv[2]).write_text(out, encoding="utf-8")
    else:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stdout.write(out)
