#!/usr/bin/env python3
"""Fills <gamedir>/mods with the jars of a 1.20.1 suite (default atm9-1201-all): `1.20.1 <fragment>` jars into mods/1.20.1/, `lib <fragment>` (native 1.21.1) into mods/.
usage: atm9_client_mods.py <gamedir> [suite]   (the ATM 9 / ATM 10 jar folders are only read)"""
import pathlib, shutil, sys

ATM9 = pathlib.Path("X:/Modded games/Prism Launcher/instances/ATM 9 20026/minecraft/mods")
ATM10 = pathlib.Path("X:/Modded games/Prism Launcher/instances/ATM10 2026/minecraft/mods")
root = pathlib.Path(__file__).resolve().parents[2]
game = pathlib.Path(sys.argv[1])
suite = sys.argv[2] if len(sys.argv) > 2 else "atm9-1201-all"
mods = game / "mods"
if (mods / "1.20.1").is_dir():
    shutil.rmtree(mods / "1.20.1")
for old in mods.glob("*.jar"):
    old.unlink()
(mods / "1.20.1").mkdir(parents=True, exist_ok=True)


def find(frag, d):
    frag = frag.lower()
    for p in sorted(d.glob("*.jar")):
        if frag in p.name.lower():
            return p
    return None


n = 0
for line in (root / "tools/dev/verify/suites" / f"{suite}.txt").read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if not line or line.startswith("#"):
        continue
    ver, frag = line.split(" ", 1)
    src = find(frag, ATM10 if ver == "lib" else ATM9)
    if not src:
        print("MISSING", line)
        continue
    shutil.copy2(src, (mods if ver == "lib" else mods / ver) / src.name)
    n += 1
print("copied", n)
