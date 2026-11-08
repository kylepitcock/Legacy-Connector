#!/usr/bin/env python3
"""Builds a 1.20.1 server-test batch: copies seed jars (name fragments) of a mods folder plus the jars providing their required dependencies
(closure over mods.toml / jarjar-free) into run-subtest/mods/1.20.1.

usage: make_batch_1201.py <source mods dir> <target dir> <name-fragment>...
The source mods are read only. Target is emptied first.
"""
import os, re, shutil, sys, zipfile

SKIP_DEPS = {"minecraft", "forge", "neoforge", "java"}


def mod_info(path):
    """Returns (set of provided mod ids, set of required dep ids) from META-INF/mods.toml."""
    try:
        with zipfile.ZipFile(path) as z:
            if "META-INF/mods.toml" not in z.namelist():
                return set(), set()
            text = z.read("META-INF/mods.toml").decode("utf-8", "replace")
    except Exception:
        return set(), set()
    provided, deps = set(), set()
    blocks = re.split(r"(?m)^\s*\[\[", text)
    for b in blocks:
        if b.startswith("mods]]"):
            m = re.search(r'modId\s*=\s*["\']([^"\']+)', b)
            if m:
                provided.add(m.group(1))
        elif b.startswith("dependencies."):
            m = re.search(r'modId\s*=\s*["\']([^"\']+)', b)
            mand = re.search(r'mandatory\s*=\s*(true|false)', b)
            if m and (mand is None or mand.group(1) == "true"):
                deps.add(m.group(1))
    return provided, deps


def main():
    src, dst, frags = sys.argv[1], sys.argv[2], [f.lower() for f in sys.argv[3:]]
    jars = sorted(f for f in os.listdir(src) if f.endswith(".jar"))
    info = {j: mod_info(os.path.join(src, j)) for j in jars}
    by_id = {}
    for j, (prov, _) in info.items():
        for p in prov:
            by_id.setdefault(p, j)
    chosen = [j for j in jars if any(f in j.lower() for f in frags)]
    seen = set(chosen)
    queue = list(chosen)
    missing = set()
    while queue:
        j = queue.pop()
        for d in info[j][1]:
            if d in SKIP_DEPS:
                continue
            owner = by_id.get(d)
            if owner is None:
                missing.add(d)
            elif owner not in seen:
                seen.add(owner)
                queue.append(owner)
    if os.path.isdir(dst):
        shutil.rmtree(dst)
    os.makedirs(dst)
    for j in sorted(seen):
        shutil.copy2(os.path.join(src, j), os.path.join(dst, j))
    print(f"{len(chosen)} seeds, {len(seen)} jars copied to {dst}; dependencies not in the source: {sorted(missing)}")
    for j in sorted(seen):
        print("  ", j, "(dep)" if j not in chosen else "")


if __name__ == "__main__":
    main()
