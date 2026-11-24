#!/usr/bin/env python3
"""Version index and table generator (plan milestone 1, docs/wiki/Legacy-1.16-1.19-Plan.md).

Compares two Minecraft versions from Mojang's official mappings (and, for the hierarchy, the client jars) and emits candidate rename/move rows in the
formats of the stage tables (legacyconnector/forward/<dir>/mc-classes.tsv, mc-members.tsv). Everything it downloads goes to a gitignored cache
(build/version-index/<version>/), nothing of Mojang's is committed. Needs no SRG data: class and member identity across versions comes from the names
themselves (same name = same symbol), moves from the unique simple name, renames from the shape of the class (its members) and, for methods and fields,
from the unique descriptor among the members that vanished and appeared in the same class.

  python tools/dev/version_index.py diff 1.20.1 1.21.1 [--out DIR] [--no-jar]     # writes class-diff / member-diff / hierarchy-diff TSVs
  python tools/dev/version_index.py emit 1.19.2 1.20.1                             # writes the high-confidence rows into forward/1.19.2/ (provenance column)
  python tools/dev/version_index.py compare 1.20.1 1.21.1 [--forward-dir 1.20.1]   # diff + how many hand-made rows of the stage it reproduces

Output files (in --out, default build/version-index/diff-<a>-<b>/):
  classes.tsv      old TAB new TAB kind(moved|renamed) TAB confidence TAB reason        candidate rows for mc-classes.tsv
  members.tsv      owner TAB name TAB descriptor TAB newName TAB kind(method|field) TAB confidence      candidate rows for mc-members.tsv (owner in the OLD name)
  desc-changed.tsv owner TAB name TAB oldDesc TAB newDesc(s)        same name, other descriptor: calls/bridges work list (not a rename)
  removed.tsv      kind TAB owner TAB name TAB descriptor          gone with no candidate
  hierarchy.tsv    class TAB oldSuper TAB newSuper TAB interfaces-removed TAB interfaces-added
"""
import argparse, collections, difflib, gzip, io, json, os, pathlib, re, struct, sys, urllib.request, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
CACHE = ROOT / "build" / "version-index"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FORWARD = ROOT / "loader" / "src" / "main" / "resources" / "legacyconnector" / "forward"


def get(url):
    with urllib.request.urlopen(url, timeout=120) as r:
        return r.read()


def fetch(version, jar=True):
    d = CACHE / version
    d.mkdir(parents=True, exist_ok=True)
    maps, client = d / "client.txt", d / "client.jar"
    if not maps.exists() or (jar and not client.exists()):
        manifest = json.loads(get(MANIFEST))
        entry = next(v for v in manifest["versions"] if v["id"] == version)
        info = json.loads(get(entry["url"]))["downloads"]
        if not maps.exists():
            maps.write_bytes(get(info["client_mappings"]["url"]))
        if jar and not client.exists():
            client.write_bytes(get(info["client"]["url"]))
    return maps, (client if client.exists() else None)


PRIM = {"void": "V", "int": "I", "long": "J", "float": "F", "double": "D", "boolean": "Z", "byte": "B", "short": "S", "char": "C"}


def jtype(t):
    dims = 0
    while t.endswith("[]"):
        dims += 1
        t = t[:-2]
    base = PRIM.get(t) or "L" + t.replace(".", "/") + ";"
    return "[" * dims + base


class Version:
    """classes: mojang name -> {'obf', 'methods': {(name, desc)}, 'fields': {(name, desc)}}"""

    def __init__(self, maps_path):
        self.classes = {}
        self.by_obf = {}
        cur = None
        cls = re.compile(r"^(\S+) -> (\S+):$")
        meth = re.compile(r"^\s+(?:\d+:\d+:)?(\S+) (\S+)\((.*)\)(?::\d+:\d+)? -> (\S+)$")
        fld = re.compile(r"^\s+(\S+) (\S+) -> (\S+)$")
        for line in open(maps_path, encoding="utf-8"):
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            m = cls.match(line)
            if m:
                name = m.group(1).replace(".", "/")
                cur = self.classes[name] = {"obf": m.group(2).replace(".", "/"), "methods": set(), "fields": set(), "obfm": {}}
                self.by_obf[cur["obf"]] = name
                continue
            m = meth.match(line)
            if m and cur is not None:
                args = "".join(jtype(a) for a in m.group(3).split(",") if a)
                cur["methods"].add((m.group(2), "(" + args + ")" + jtype(m.group(1))))
                continue
            m = fld.match(line)
            if m and cur is not None:
                cur["fields"].add((m.group(2), jtype(m.group(1))))
        self.hier = {}

    def load_hierarchy(self, jar_path):
        """class -> (super, [interfaces]) in mapped names, from the client jar's class headers."""
        with zipfile.ZipFile(jar_path) as z:
            for n in z.namelist():
                if not n.endswith(".class"):
                    continue
                obf = n[:-6]
                name = self.by_obf.get(obf)
                if name is None:
                    continue
                sup, ifs = class_header(z.read(n))
                f = lambda o: self.by_obf.get(o, o)
                self.hier[name] = (f(sup) if sup else None, sorted(f(i) for i in ifs))


def class_header(data):
    """(super name, interface names) of a class file, without reading anything else."""
    i = 10
    count = struct.unpack(">H", data[8:10])[0]
    utf = {}
    cls = {}
    idx = 1
    while idx < count:
        tag = data[i]
        if tag == 1:
            n = struct.unpack(">H", data[i + 1:i + 3])[0]
            utf[idx] = data[i + 3:i + 3 + n].decode("utf-8", "replace")
            i += 3 + n
        elif tag in (3, 4):
            i += 5
        elif tag in (5, 6):
            i += 9
            idx += 1
        elif tag == 7:
            cls[idx] = struct.unpack(">H", data[i + 1:i + 3])[0]
            i += 3
        elif tag in (8, 16, 19, 20):
            i += 3
        elif tag in (9, 10, 11, 12, 17, 18):
            i += 5
        elif tag == 15:
            i += 4
        else:
            raise ValueError(f"constant pool tag {tag}")
        idx += 1
    _acc, _this, sup, nif = struct.unpack(">HHHH", data[i:i + 8])
    ifs = [utf[cls[struct.unpack(">H", data[i + 8 + 2 * k:i + 10 + 2 * k])[0]]] for k in range(nif)]
    return (utf[cls[sup]] if sup else None), ifs


def simple(name):
    return name.rsplit("/", 1)[-1]


def shape(c):
    """Class fingerprint independent of package: member names with descriptors reduced to simple type names."""
    red = lambda d: re.sub(r"L([^;]*/)?([^;/]*);", r"L\2;", d)
    return {("m", n, red(d)) for n, d in c["methods"]} | {("f", n, red(d)) for n, d in c["fields"]}


def diff_classes(a, b):
    gone = sorted(set(a.classes) - set(b.classes))
    new = sorted(set(b.classes) - set(a.classes))
    out = []
    by_simple = collections.defaultdict(list)
    for n in new:
        by_simple[simple(n)].append(n)
    taken = set()
    pending = []
    for n in gone:
        c = by_simple.get(simple(n), [])
        c = [x for x in c if x not in taken]
        if len(c) == 1 and sum(1 for g in gone if simple(g) == simple(n)) == 1:
            out.append((n, c[0], "moved", "high", "unique simple name"))
            taken.add(c[0])
        else:
            pending.append(n)
    # renamed in place: same package, similar simple name, unique
    avail = [n for n in new if n not in taken]
    rest = []
    for n in pending:
        pkg = n.rsplit("/", 1)[0] if "/" in n else ""
        cands = [(difflib.SequenceMatcher(None, simple(n), simple(m)).ratio(), m) for m in avail
                 if m not in taken and (m.rsplit("/", 1)[0] if "/" in m else "") == pkg]
        cands.sort(reverse=True)
        if cands and cands[0][0] >= 0.8 and (len(cands) == 1 or cands[0][0] - cands[1][0] >= 0.1):
            out.append((n, cands[0][1], "renamed", "high", f"name similarity {cands[0][0]:.2f} in the same package"))
            taken.add(cands[0][1])
        else:
            rest.append(n)
    pending = rest
    # renamed: the best unique pair by member-shape similarity, among what is still unmatched
    avail = [n for n in new if n not in taken]
    shapes_b = {n: shape(b.classes[n]) for n in avail}
    for n in pending:
        sa = shape(a.classes[n])
        if len(sa) < 4:
            continue
        best = []
        for m, sb in shapes_b.items():
            if m in taken or not sb:
                continue
            j = len(sa & sb) / len(sa | sb)
            best.append((j, m))
        best.sort(reverse=True)
        if best and best[0][0] >= 0.6 and (len(best) == 1 or best[0][0] - best[1][0] >= 0.15):
            out.append((n, best[0][1], "renamed", "medium", f"member shape {best[0][0]:.2f}"))
            taken.add(best[0][1])
    return out


def diff_members(a, b, cmap):
    """Member candidates for every class of A that exists in B (under its own name or its candidate new name)."""
    renames, desc_changed, removed = [], [], []
    ren = lambda d: re.sub(r"L([^;]+);", lambda m: "L" + cmap.get(m.group(1), m.group(1)) + ";", d)
    for name, ca in sorted(a.classes.items()):
        tgt = cmap.get(name, name)
        cb = b.classes.get(tgt)
        if cb is None:
            continue
        for kind, key in (("method", "methods"), ("field", "fields")):
            sa, sb = ca[key], cb[key]
            sa_mapped = {(n, ren(d)) for n, d in sa}
            gone = sorted((n, d) for n, d in sa if (n, ren(d)) not in sb)
            added = sorted(sb - sa_mapped)
            used = set()
            for n, d in gone:
                nd = ren(d)
                same_name = [x for x in added if x[0] == n]
                same_desc = [x for x in added if x[1] == nd and x not in used]
                if same_desc and len([g for g in gone if ren(g[1]) == nd]) == 1 and len(same_desc) == 1:
                    if n.startswith("lambda$"):
                        continue
                    used.add(same_desc[0])
                    sim = difflib.SequenceMatcher(None, n, same_desc[0][0]).ratio()
                    only = len(gone) == 1 and len(added) == 1
                    renames.append((name, n, d, same_desc[0][0], kind, "high" if sim >= 0.5 or only else "low"))
                elif same_name:
                    desc_changed.append((name, n, d, " | ".join(x[1] for x in same_name)))
                elif kind == "method" and n not in ("<init>", "<clinit>"):
                    removed.append((kind, name, n, d))
                elif kind == "field":
                    removed.append((kind, name, n, d))
    return renames, desc_changed, removed


def diff_hierarchy(a, b, cmap):
    out = []
    for name, (sa, ia) in sorted(a.hier.items()):
        tgt = cmap.get(name, name)
        if tgt not in b.hier:
            continue
        sb, ib = b.hier[tgt]
        sa2 = cmap.get(sa, sa)
        ia2 = [cmap.get(i, i) for i in ia]
        if sa2 != sb or sorted(ia2) != ib:
            out.append((name, sa or "", sb or "", ",".join(sorted(set(ia2) - set(ib))), ",".join(sorted(set(ib) - set(ia2)))))
    return out


def run_diff(va, vb, out, jar=True):
    ma, ja = fetch(va, jar)
    mb, jb = fetch(vb, jar)
    a, b = Version(ma), Version(mb)
    if jar and ja and jb:
        a.load_hierarchy(ja)
        b.load_hierarchy(jb)
    classes = diff_classes(a, b)
    cmap = {o: n for o, n, *_ in classes}
    members, desc, removed = diff_members(a, b, cmap)
    hier = diff_hierarchy(a, b, cmap) if a.hier and b.hier else []
    out.mkdir(parents=True, exist_ok=True)
    w = lambda f, rows: (out / f).write_text("".join("\t".join(map(str, r)) + "\n" for r in rows), encoding="utf-8")
    w("classes.tsv", classes)
    w("members.tsv", members)
    w("desc-changed.tsv", desc)
    w("removed.tsv", removed)
    w("hierarchy.tsv", hier)
    gone = len(set(a.classes) - set(b.classes))
    print(f"{va} -> {vb}: classes {len(a.classes)} -> {len(b.classes)}; gone {gone}, candidates {len(classes)} "
          f"({sum(1 for c in classes if c[2] == 'moved')} moved, {sum(1 for c in classes if c[2] == 'renamed')} renamed); "
          f"member renames {len(members)}, descriptor changes {len(desc)}, removed members {len(removed)}, hierarchy changes {len(hier)}")
    return a, b, classes, members, desc, removed


def rows(path):
    if not path.exists():
        return []
    return [l.rstrip("\n").split("\t") for l in open(path, encoding="utf-8") if l.strip() and not l.startswith("#")]


def compare(va, vb, fdir, out):
    a, b, classes, members, desc, removed = run_diff(va, vb, out)
    base = FORWARD / fdir
    cmap = {o: n for o, n, *_ in classes}
    gone = set(a.classes) - set(b.classes)
    rep = []

    def line(s):
        rep.append(s)
        print(s)

    # mc-classes.tsv: old -> new
    hand = rows(base / "mc-classes.tsv")
    cnt = collections.Counter()
    detail = collections.defaultdict(list)
    for r in hand + rows(base / "lib-classes.tsv"):
        old, new = r[0], r[1]
        if old not in a.classes:
            k = "not a 1.20.1 Minecraft class (library or Forge)"
        elif new not in b.classes:
            k = "target is not a 1.21.1 class (layer helper)"
        elif cmap.get(old) == new:
            k = "reproduced"
        elif old in cmap:
            k = "contradicted"
        elif old in gone:
            k = "missed (gone, no candidate)"
        else:
            k = "old name still exists in 1.21.1 (hand row is a deliberate redirect)"
        cnt[k] += 1
        detail[k].append(f"{old} -> {new}" + (f"   [generated: {cmap[old]}]" if k == "contradicted" else ""))
    line(f"mc-classes.tsv + lib-classes.tsv: {sum(cnt.values())} rows")
    for k, n in cnt.most_common():
        line(f"  {n:4d}  {k}")
    # mc-members.tsv: owner, name, desc(* or real), new
    gen = {(o, n): nn for o, n, d, nn, k, c in members}
    gen_any = collections.defaultdict(set)
    for o, n, d, nn, k, c in members:
        gen_any[(o, n)].add(nn)
    desc_keys = {(o, n) for o, n, od, nd in desc}
    mc = collections.Counter()
    mdetail = collections.defaultdict(list)
    for r in rows(base / "mc-members.tsv") + rows(base / "mc-fields.tsv"):
        owner, name = r[0], r[1]
        new = r[3] if len(r) == 4 else r[3]
        if owner not in a.classes:
            k = "owner not a 1.20.1 Minecraft class"
        elif new in gen_any.get((owner, name), ()) or new in gen_any.get((cmap.get(owner, owner), name), ()):
            k = "reproduced"
        elif (owner, name) in gen_any:
            k = "contradicted"
        elif (owner, name) in desc_keys:
            k = "detected only (descriptor changed)"
        else:
            k = "missed (generator sees no change for this member)"
        mc[k] += 1
        mdetail[k].append("\t".join(r[:4]))
    line(f"mc-members.tsv + mc-fields.tsv: {sum(mc.values())} rows")
    for k, n in mc.most_common():
        line(f"  {n:4d}  {k}")
    # mc-calls.tsv: owner, name, desc, helper...: does the generator see the symbol change at all?
    cc = collections.Counter()
    rem = {(o, n) for _k, o, n, _d in removed}
    for r in rows(base / "mc-calls.tsv"):
        owner, name = r[0], r[1]
        if owner not in a.classes:
            k = "owner not a 1.20.1 Minecraft class"
        elif owner in gone:
            k = "detected (class gone or moved)"
        elif (owner, name) in desc_keys or (owner, name) in rem or (owner, name) in gen_any:
            k = "detected (member gone, renamed or descriptor changed)"
        elif name == "<init>":
            k = "constructor (generator sees no change)"
        else:
            k = "not detected (behaviour change or inherited member)"
        cc[k] += 1
    line(f"mc-calls.tsv: {sum(cc.values())} rows (call redirects to helpers need semantics; the generator can only flag the symbol)")
    for k, n in cc.most_common():
        line(f"  {n:4d}  {k}")
    (out / "compare.txt").write_text("\n".join(rep) + "\n", encoding="utf-8")
    full = []
    for title, d in (("classes", detail), ("members", mdetail)):
        for k, v in d.items():
            full.append(f"== {title}: {k} ({len(v)})")
            full += ["  " + x for x in v]
    (out / "compare-detail.txt").write_text("\n".join(full) + "\n", encoding="utf-8")


HEADERS = {
    "mc-classes.tsv": "# Class renames and moves (old TAB new TAB provenance). Provenance: gen = emitted by tools/dev/version_index.py emit (replaced on every run),\n"
                      "# hand = curated; a hand row for the same old name wins over a generated one.\n",
    "mc-members.tsv": "# Renamed methods and fields (owner TAB name TAB descriptor (fields: ':' + descriptor) TAB new name TAB provenance); gen rows are replaced on every run,\n"
                      "# hand rows win.\n",
}


def merge_stage(path, key, gen_rows):
    """Writes gen_rows (lists of columns without provenance) into a stage table, keeping the hand rows and dropping generated rows whose key a hand row has."""
    hand = []
    if path.exists():
        for l in open(path, encoding="utf-8"):
            l = l.rstrip("\n")
            if not l.strip() or l.startswith("#"):
                continue
            c = l.split("\t")
            if c[-1] == "gen":
                continue
            hand.append(c)
    hand_keys = {key(c) for c in hand}
    gen = sorted(r for r in gen_rows if key(r) not in hand_keys)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(HEADERS[path.name])
        for c in hand:
            f.write("\t".join(c if c[-1] == "hand" else c + ["hand"]) + "\n")
        for r in gen:
            f.write("\t".join(r + ["gen"]) + "\n")
    return len(hand), len(gen)


def emit(va, vb, fdir, out):
    a, b, classes, members, desc, removed = run_diff(va, vb, out)
    base = FORWARD / fdir
    cl = [[o, n] for o, n, kind, conf, why in classes if conf == "high"]
    mem = [[o, n, (":" + d if k == "field" else d), nn] for o, n, d, nn, k, conf in members if conf == "high"]
    h1, g1 = merge_stage(base / "mc-classes.tsv", lambda c: c[0], cl)
    h2, g2 = merge_stage(base / "mc-members.tsv", lambda c: (c[0], c[1], c[2]), mem)
    print(f"{base}: mc-classes {h1} hand + {g1} generated; mc-members {h2} hand + {g2} generated")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("cmd", choices=["diff", "compare", "emit"])
    p.add_argument("a")
    p.add_argument("b")
    p.add_argument("--out")
    p.add_argument("--no-jar", action="store_true")
    p.add_argument("--forward-dir")
    args = p.parse_args()
    out = pathlib.Path(args.out) if args.out else CACHE / f"diff-{args.a}-{args.b}"
    if args.cmd == "diff":
        run_diff(args.a, args.b, out, not args.no_jar)
    elif args.cmd == "emit":
        emit(args.a, args.b, args.forward_dir or args.a, out)
    else:
        compare(args.a, args.b, args.forward_dir or args.a, out)


if __name__ == "__main__":
    main()
