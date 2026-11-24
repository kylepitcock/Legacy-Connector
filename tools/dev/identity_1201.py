#!/usr/bin/env python3
"""Byte-identity check of the 1.20.1 transformer (engine refactoring, plan milestone 1).

Translates a fixed, deterministic selection of Forge 1.20.1 jars with the real transformer (./gradlew gap1201 -Pgap.translate=true, which writes the
translated jars) and compares the entries of two runs, so a refactoring of the transformer can be proven not to change its output.

  python tools/dev/identity_1201.py run <label> [--mods DIR] [--count 40] [--work DIR]   # translate the selection into <work>/<label>/
  python tools/dev/identity_1201.py compare <labelA> <labelB> [--work DIR]               # entry-by-entry comparison (names + SHA-256 of bytes)

Mods dir: $ATM9_MODS or the ATM 9 instance of tools/dev/verify.sh. The selection takes every Nth jar of the sorted list (so it is varied and stable);
work dir: build/identity-1201 (gitignored). Needs build/moddev/artifacts (a worktree reuses the main checkout's), like tools/compat/probe.py.
"""
import argparse, hashlib, os, pathlib, shutil, subprocess, sys, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_MODS = os.environ.get("ATM9_MODS", "X:/Modded games/Prism Launcher/instances/ATM 9 20026/minecraft/mods")


def select(mods_dir, count):
    jars = sorted(p for p in pathlib.Path(mods_dir).glob("*.jar") if p.is_file())
    step = max(1, len(jars) // count)
    return jars[::step][:count]


def run(a):
    work = pathlib.Path(a.work) / a.label
    if work.exists():
        shutil.rmtree(work)
    src = work / "in"
    src.mkdir(parents=True)
    jars = select(a.mods, a.count)
    for j in jars:
        shutil.copyfile(j, src / j.name)
    sys.path.insert(0, str(ROOT / "tools" / "compat"))
    import probe  # noqa: E402  (gap1201_targets)
    listing = work / "targets.txt"
    listing.write_text(probe.gap1201_targets(), encoding="utf-8")
    out = work / "translated"
    report = work / "report.txt"
    cmd = [str(ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew")), "-q", "gap1201", f"-PtargetJars=@{listing.as_posix()}",
           f"-PgapJars={src}", f"-PgapReport={report}", "-Pgap.translate=true", f"-Pgap.translated.dir={out}"]
    print(f"translating {len(jars)} jars into {out} ...", flush=True)
    r = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (work / "gradle.log").write_text(r.stdout, encoding="utf-8")
    if r.returncode != 0:
        sys.exit(r.stdout[-3000:] + "\nrun failed")
    print(f"done: {len(list(out.glob('*.jar')))} translated jars; gap counts: " + " | ".join(l for l in r.stdout.splitlines() if l.startswith("MC-gap")))


def digest(jar):
    d = {}
    with zipfile.ZipFile(jar) as z:
        for i in z.infolist():
            if not i.is_dir() and i.filename != "META-INF/legacyconnector/provenance.json":   # carries a timestamp
                d[i.filename] = hashlib.sha256(z.read(i)).hexdigest()
    return d


def compare(a):
    da, db = pathlib.Path(a.work) / a.a / "translated", pathlib.Path(a.work) / a.b / "translated"
    names = sorted({p.name for p in da.glob("*.jar")} | {p.name for p in db.glob("*.jar")})
    bad = 0
    total = 0
    for n in names:
        if not (da / n).exists() or not (db / n).exists():
            print(f"ONLY IN ONE: {n}")
            bad += 1
            continue
        x, y = digest(da / n), digest(db / n)
        total += len(x)
        diff = [k for k in sorted(set(x) | set(y)) if x.get(k) != y.get(k)]
        if diff:
            bad += 1
            print(f"DIFF {n}: {len(diff)} entries, e.g. {diff[:5]}")
    for f in ("report.txt", "report.txt.detail.tsv"):
        pa, pb = pathlib.Path(a.work) / a.a / f, pathlib.Path(a.work) / a.b / f
        if pa.exists() and pb.exists() and pa.read_bytes() != pb.read_bytes():
            print(f"REPORT DIFFERS: {f}")
            bad += 1
    print(f"{len(names)} jars, {total} entries compared: " + ("IDENTICAL" if not bad else f"{bad} differences"))
    sys.exit(1 if bad else 0)


p = argparse.ArgumentParser()
sub = p.add_subparsers(dest="cmd", required=True)
r = sub.add_parser("run")
r.add_argument("label")
r.add_argument("--mods", default=DEFAULT_MODS)
r.add_argument("--count", type=int, default=40)
r.add_argument("--work", default=str(ROOT / "build" / "identity-1201"))
c = sub.add_parser("compare")
c.add_argument("a")
c.add_argument("b")
c.add_argument("--work", default=str(ROOT / "build" / "identity-1201"))
args = p.parse_args()
run(args) if args.cmd == "run" else compare(args)
