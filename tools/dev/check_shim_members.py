#!/usr/bin/env python3
"""Guards the public member set of the hand-written shim: members may be added, never silently removed or changed.

Why: a mod links against a shim member by name, descriptor and static-ness. A refactor that renames a method, changes its signature or
flips static/instance turns every mod that used it from "links" into "unresolved reference" (the probe then reports "gap_shim"; this
check catches it locally, before a release). The snapshot is tools/dev/shim-members.txt.gz.

usage: check_shim_members.py [--index build/generated/shimIndex/legacyconnector/shim-index.txt] [--snapshot tools/dev/shim-members.txt.gz]
                             [--update]

Without --update: compares the index (written by ./gradlew generateShimIndex) with the snapshot; exit 1 when a class, a public/protected
field or a public/protected method of the snapshot is gone (also not inherited from a shim super class or interface). Additions are fine
(run --update to record them). With --update: accepts the current state as the new snapshot, which is how an INTENDED removal is
approved: say so in the commit message. Private, synthetic and mcc$ (layer-internal) members are not part of the contract.
"""
import argparse, gzip, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ACC_PUBLIC, ACC_PROTECTED, ACC_STATIC, ACC_SYNTHETIC = 0x1, 0x4, 0x8, 0x1000


def load_index(path):
    classes = {}  # name -> dict(super, ifaces, members:set)
    cur = None
    for line in path.read_text(encoding="utf-8").splitlines():
        p = line.split(" ")
        if p[0] == "C":
            cur = classes.setdefault(p[1], {"super": p[2], "ifaces": [] if p[3] == "-" else p[3].split(","), "members": set()})
        elif p[0] in ("M", "F") and cur is not None:
            acc = int(p[3])
            name = p[1]
            if not acc & (ACC_PUBLIC | ACC_PROTECTED) or acc & ACC_SYNTHETIC:
                continue
            if name.startswith("mcc$") or name.startswith("lambda$") or name == "<clinit>":
                continue
            cur["members"].add((p[0], name, p[2], bool(acc & ACC_STATIC)))
    return classes


def present(classes, cls, member, seen=None):
    """True when the class or a shim super class / interface declares the member (inherited members still link)."""
    seen = seen if seen is not None else set()
    if cls in seen or cls not in classes:
        return False
    seen.add(cls)
    info = classes[cls]
    if member in info["members"]:
        return True
    return any(present(classes, s, member, seen) for s in [info["super"], *info["ifaces"]])


def write_snapshot(classes, path):
    out = []
    for name in sorted(classes):
        out.append("C " + name)
        for kind, mname, desc, static in sorted(classes[name]["members"]):
            out.append(f"{kind} {mname} {desc}{' S' if static else ''}")
    # mtime=0: a byte-identical snapshot for an identical member set
    with open(path, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as gz:
        gz.write(("\n".join(out) + "\n").encode("utf-8"))


def read_snapshot(path):
    snap, cur = {}, None
    for line in gzip.open(path, "rt", encoding="utf-8").read().splitlines():
        p = line.split(" ")
        if p[0] == "C":
            cur = snap.setdefault(p[1], set())
        else:
            cur.add((p[0], p[1], p[2], len(p) > 3 and p[3] == "S"))
    return snap


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--index", default=str(ROOT / "build/generated/shimIndex/legacyconnector/shim-index.txt"))
    ap.add_argument("--snapshot", default=str(ROOT / "tools/dev/shim-members.txt.gz"))
    ap.add_argument("--update", action="store_true")
    a = ap.parse_args()
    index = pathlib.Path(a.index)
    if not index.exists():
        print("shim index not found (run ./gradlew generateShimIndex first): " + str(index))
        return 2
    classes = load_index(index)
    snap_path = pathlib.Path(a.snapshot)
    if a.update or not snap_path.exists():
        write_snapshot(classes, snap_path)
        print(f"shim member snapshot written: {len(classes)} classes, {sum(len(c['members']) for c in classes.values())} members")
        return 0
    snap = read_snapshot(snap_path)
    missing, added = [], 0
    for cls, members in sorted(snap.items()):
        if cls not in classes:
            missing.append(f"class {cls} (removed)")
            continue
        for m in sorted(members):
            if not present(classes, cls, m):
                kind, name, desc, static = m
                missing.append(f"{cls}.{name}{'' if kind == 'F' else ''} {desc}{' (static)' if static else ''} [{'field' if kind == 'F' else 'method'}]")
    for cls, info in classes.items():
        added += len(info["members"] - snap.get(cls, set()))
    if missing:
        print(f"shim members removed or changed since the snapshot ({len(missing)}): mods that linked against them now fail with unresolved references.")
        for m in missing[:60]:
            print("  - " + m)
        if len(missing) > 60:
            print(f"  ... and {len(missing) - 60} more")
        print("Restore them (a changed signature needs the old one kept beside the new), or accept an intended removal with:")
        print("  python tools/dev/check_shim_members.py --update")
        return 1
    print(f"shim members: none removed ({added} added since the snapshot; run --update to record them)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
