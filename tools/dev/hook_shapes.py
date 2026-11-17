"""Enumerate overridable legacy methods whose 1.7.10 counterpart has another descriptor and no bridge/rename row.
usage: hookgap.py <ver> -> prints candidate lines: class name desc | 1.7.10 candidates"""
import sys, re, pathlib, collections
ROOT = pathlib.Path(__file__).resolve().parents[2]
R = ROOT / "loader/src/main/resources/legacyconnector"
ver = sys.argv[1]
chain = {"1.6.4": ["1.6.4"], "1.5.2": ["1.5.2", "1.6.4"], "1.4.7": ["1.4.7", "1.5.2", "1.6.4"]}[ver]

def rows(path):
    p = R / path
    if not p.exists(): return []
    return [l.rstrip("\n").split("\t") for l in p.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]

# class renames, applied in chain order
cren = {}
for v in reversed(chain):
    if v == "1.4.7":
        for r in rows("forward/1.4.7/flat-names.tsv"): cren[r[0]] = r[1]
    else:
        for r in rows(f"forward/{v}/class-renames.tsv"):
            if len(r) >= 2: cren[r[0]] = r[1]
def mapcls(c, stages=None):
    seen = 0
    while c in cren and seen < 4:
        c2 = cren[c];
        if c2 == c: break
        c = c2; seen += 1
    i = c.find("$")
    if i > 0 and c not in cren:
        o = mapcls(c[:i]); return o + c[i:]
    return c
def mapdesc(d):
    return re.sub(r"L([^;]+);", lambda m: "L" + mapcls(m.group(1)) + ";", d)

# 1.7.10
m710 = collections.defaultdict(dict)  # cls -> (name,desc)->flags
byname = collections.defaultdict(lambda: collections.defaultdict(list))
for l in (R / "legacy/1.7.10/methods.txt").read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if len(p) < 4: continue
    m710[p[0]][(p[1], p[2])] = int(p[3])
    byname[p[0]][p[1]].append(p[2])
SH = ROOT / "build/generated/shimIndex/legacyconnector/shim-index.txt"
P = "legacyconnector/v1_7_10/"
cur = None
shier = {}
for l in SH.read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if p[0] == "C":
        cur = p[1][len(P):] if p[1].startswith(P) else None
        if cur: shier[cur] = [x.replace(P, "") for x in [p[2]] + (p[3].split(",") if len(p) > 3 else []) if x and x != "-"]
    elif p[0] == "M" and cur:
        d = p[2].replace(P, "")
        m710[cur][(p[1], d)] = 0
        byname[cur][p[1]].append(d)
hier = {}
for l in (R / "legacy/1.7.10/hierarchy.txt").read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if len(p) >= 3: hier[p[0]] = [x for x in [p[1]] + p[2].split(",") if x and x != "-"]
for k, v in shier.items(): hier.setdefault(k, v)
def ancestors(c):
    out = []; st = [c]; seen = set()
    while st:
        x = st.pop()
        if x in seen: continue
        seen.add(x); out.append(x); st += hier.get(x, [])
    return out

# coverage rows: any table, keyed by (owner,name,desc) in both legacy name forms
covered = set()
for v in chain:
    for f in ("member-renames", "member-heuristic", "member-manual"):
        for r in rows(f"forward/{v}/{f}.tsv"):
            if r[0] == "M" and len(r) >= 5: covered.add((r[1], r[2], r[3]))
            if r[0] == "R" and len(r) >= 5: covered.add((r[1], r[2], r[3]))
bridged = collections.defaultdict(set)  # (name,desc) -> owners
for v in chain:
    for r in rows(f"forward/{v}/bridge-methods.tsv"):
        if len(r) >= 5: covered.add((r[0], r[1], r[2])); bridged[(r[1], r[2])].add(r[0])

legacy = collections.defaultdict(dict)
for l in (R / f"legacy/{ver}/methods.txt").read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if len(p) < 4: continue
    legacy[p[0]][(p[1], p[2])] = int(p[3])
lhier = {}
for l in (R / f"legacy/{ver}/hierarchy.txt").read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if len(p) >= 3: lhier[p[0]] = [x for x in [p[1]] + p[2].split(",") if x and x != "-"]
def lanc(c):
    out = []; st = [c]; seen = set()
    while st:
        x = st.pop()
        if x in seen: continue
        seen.add(x); out.append(x); st += lhier.get(x, [])
    return out

def cov(owner, name, desc):
    for a in lanc(owner):
        if (a, name, desc) in covered or (mapcls(a), name, mapdesc(desc)) in covered: return True
    return False

res = []
for cls, ms in legacy.items():
    ncls = mapcls(cls)
    if ncls not in m710 and ncls not in hier: continue
    anc = ancestors(ncls)
    for (name, desc), fl in ms.items():
        if fl & (8 | 2) or name in ("<init>", "<clinit>"): continue  # static / private
        nd = mapdesc(desc)
        found = any((name, nd) in m710.get(a, {}) for a in anc)
        if found: continue
        if cov(cls, name, desc): continue
        # same name other descriptor in 1.7.10?
        alts = []
        for a in anc:
            for d in byname.get(a, {}).get(name, []): alts.append(a + "." + d)
        # must be declared by this class's own table (not inherited duplicate): list at declaring class
        res.append((cls, name, desc, fl, alts))
tag = lambda r: "SAMENAME" if r[4] else "NONE"
for r in sorted(res, key=lambda r: (tag(r), r[0], r[1])):
    print(tag(r), mapcls(r[0]), r[1], mapdesc(r[2]), "->", " | ".join(r[4][:4]))
