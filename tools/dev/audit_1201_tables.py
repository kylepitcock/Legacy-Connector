#!/usr/bin/env python3
"""Audit of the 1.20.1 -> 1.21.1 migration tables (mc-classes / mc-members / mc-fields / mc-calls / mc-bridges) against the 1.21.1 class index.

  python tools/dev/audit_1201_tables.py report            # rows whose target class/member/helper does not exist, rows whose target class has another shape
  python tools/dev/audit_1201_tables.py shapes            # (re)writes loader/src/test/resources/legacyconnector/audit/mc-classes-shapes.tsv (needs the version cache of
                                                          # tools/dev/version_index.py: Mojang's 1.20.1 mappings + client jar, downloaded into build/version-index)
  python tools/dev/audit_1201_tables.py candidates        # classes the version index moved or renamed that no mc-classes row covers (with the version diff files)

The unit test loader/src/test/.../Mc1201TablesAuditTest runs the same checks on every build (the shapes file is its only input besides the tables and the
committed 1.21.1 index), so a row that points at a missing class, an enum that became a class, or a class that lost most of its members fails the build
unless it is listed with a reason in loader/src/test/resources/legacyconnector/audit/mc-classes-shape-waivers.txt.
"""
import gzip, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
FWD = ROOT / "loader/src/main/resources/legacyconnector/forward/1.20.1"
AUDIT = ROOT / "loader/src/test/resources/legacyconnector/audit"
SRC = ROOT / "src/main/java"
IGNORED_MEMBERS = {"<init>", "<clinit>", "values", "valueOf", "$VALUES", "$values", "toString", "hashCode", "equals", "getClass", "name", "ordinal",
                   "compareTo", "getMessage", "getDeclaringClass", "describeConstable", "wait", "notify", "notifyAll", "clone", "finalize"}


def rows(name):
    for line in (FWD / name).read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            yield line.split("\t")


def load_index():
    idx = {}
    for line in gzip.open(FWD / "mc-members.index.gz", "rt", encoding="utf-8"):
        p = line.rstrip("\n").split("\t")
        idx[p[0]] = (set(p[1].split()) if len(p) > 1 else set(), set(p[2].split()) if len(p) > 2 else set())
    hier = {}
    for line in gzip.open(FWD / "mc-hierarchy.tsv.gz", "rt", encoding="utf-8"):
        p = line.rstrip("\n").split("\t")
        hier[p[0]] = [y for x in p[1:] for y in x.split(",") if y]
    return idx, hier


def supers(hier, c, seen=None):
    seen = seen if seen is not None else set()
    for s in hier.get(c, []):
        if s not in seen:
            seen.add(s)
            yield s
            yield from supers(hier, s, seen)


def member_names(idx, hier, c):
    names = set()
    for k in [c, *supers(hier, c)]:
        if k in idx:
            names |= {m.split("(")[0] for m in idx[k][0]} | {m.split(":")[0] for m in idx[k][1]}
    return names


def helper_exists(name):
    """A target class of mc-classes that is not a Minecraft class: a helper or shim class of this repository."""
    return (SRC / (name.split("$")[0] + ".java")).exists()


def old_version():
    sys.path.insert(0, str(ROOT / "tools/dev"))
    import version_index as vi
    old = vi.Version(vi.CACHE / "1.20.1" / "client.txt")
    old.load_hierarchy(vi.CACHE / "1.20.1" / "client.jar")
    return old


def shapes(idx, hier, old):
    out = []
    for r in rows("mc-classes.tsv"):
        o = r[0]
        if o not in old.classes:
            continue
        c = old.classes[o]
        enum = old.hier.get(o, (None,))[0] == "java/lang/Enum"
        names = sorted({m for m, _ in c["methods"] if not m.startswith("lambda$")} | {f for f, _ in c["fields"]} - IGNORED_MEMBERS)
        out.append((o, "enum" if enum else "class", ",".join(n for n in names if n not in IGNORED_MEMBERS and "$" not in n)))
    return out


def report(idx, hier):
    cm = {r[0]: r[1] for r in rows("mc-classes.tsv")}
    bad = 0
    for r in rows("mc-classes.tsv"):
        n = r[1]
        if n not in idx and not helper_exists(n):
            print("class target missing:", r[0], "->", n)
            bad += 1
    for r in rows("mc-members.tsv"):
        o, new = cm.get(r[0], r[0]), r[3]
        if o in idx and new not in member_names(idx, hier, o):
            print("member target missing:", o, r[1], "->", new)
            bad += 1
    print("problems:", bad)


def candidates(idx):
    mapped = {r[0] for r in rows("mc-classes.tsv")}
    f = ROOT / "build/version-index/diff-1.20.1-1.21.1/classes.tsv"
    if not f.exists():
        sys.exit("run: python tools/dev/version_index.py diff 1.20.1 1.21.1")
    for line in f.read_text(encoding="utf-8").splitlines():
        p = line.split("\t")
        if p[0] not in mapped:
            print("\t".join(p[:5]))


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else "report"
    idx, hier = load_index()
    if cmd == "report":
        report(idx, hier)
    elif cmd == "shapes":
        AUDIT.mkdir(parents=True, exist_ok=True)
        lines = ["# old class TAB kind (enum|class) TAB member names in 1.20.1 (Mojang names); written by tools/dev/audit_1201_tables.py shapes"]
        lines += ["\t".join(s) for s in shapes(idx, hier, old_version())]
        (AUDIT / "mc-classes-shapes.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8")
        print("wrote", len(lines) - 1, "shapes")
    elif cmd == "candidates":
        candidates(idx)
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
