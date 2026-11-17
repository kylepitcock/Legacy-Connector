"""Forge/FML hook shape check: methods of the Forge universal jar of a version vs the shim index.
usage: forgegap.py <ver> <universal jar>"""
import sys, re, subprocess, zipfile, pathlib, collections
ROOT = pathlib.Path(__file__).resolve().parents[2]
ver, jar = sys.argv[1], sys.argv[2]
R = ROOT / "loader/src/main/resources/legacyconnector"
SH = ROOT / "build/generated/shimIndex/legacyconnector/shim-index.txt"
chain = {"1.6.4": ["1.6.4"], "1.5.2": ["1.5.2", "1.6.4"], "1.4.7": ["1.4.7", "1.5.2", "1.6.4"]}[ver]
obf = {}
for l in (R / f"legacy/{ver}/notch.txt").read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if p[0] == "C": obf[p[1]] = p[2]
def rows(path):
    p = R / path
    return [l.rstrip("\n").split("\t") for l in p.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")] if p.exists() else []
cren = {}
for v in reversed(chain):
    if v == "1.4.7":
        for r in rows("forward/1.4.7/flat-names.tsv"): cren[r[0]] = r[1]
    else:
        for r in rows(f"forward/{v}/class-renames.tsv"):
            if len(r) >= 2: cren[r[0]] = r[1]
def mapcls(c):
    c = obf.get(c, c)
    n = 0
    while c in cren and n < 4 and cren[c] != c:
        c = cren[c]; n += 1
    i = c.find("$")
    if i > 0 and c not in cren:
        return mapcls(c[:i]) + c[i:]
    return c
def mapdesc(d):
    return re.sub(r"L([^;]+);", lambda m: "L" + mapcls(m.group(1)) + ";", d)

covered = set(); bridged = set()
for v in chain:
    for f in ("member-renames", "member-heuristic", "member-manual", "bridge-methods"):
        for r in rows(f"forward/{v}/{f}.tsv"):
            if r[0] in ("M", "R") and len(r) >= 5: covered.add((r[1], r[2], r[3]))
            elif f == "bridge-methods" and len(r) >= 5: covered.add((r[0], r[1], r[2]))

P7, P6 = "legacyconnector/v1_7_10/", "legacyconnector/v1_6_4/"
shim = collections.defaultdict(dict)
hier = {}
cur = None
for l in SH.read_text(encoding="utf-8").splitlines():
    p = l.split(" ")
    if p[0] == "C":
        cur = p[1]
        hier[cur] = [x for x in [p[2]] + (p[3].split(",") if len(p) > 3 else []) if x and x != "-"]
    elif p[0] == "M" and cur:
        shim[cur][(p[1], p[2])] = 1
def anc(c):
    out, st, seen = [], [c], set()
    while st:
        x = st.pop()
        if x in seen: continue
        seen.add(x); out.append(x); st += hier.get(x, [])
    return out
def norm_shim_desc(d):
    return d.replace(P7, "").replace(P6, "")

zf = zipfile.ZipFile(jar)
classes = [n[:-6] for n in zf.namelist() if n.endswith(".class") and (n.startswith("cpw/mods/fml/") or n.startswith("net/minecraftforge/")) and "$" not in n
           and not n.startswith("cpw/mods/fml/relauncher") and "/asm/" not in n and "/libraries/" not in n]
print("classes", len(classes), file=sys.stderr)
JP = "C:/Program Files/Java/jdk-21/bin/javap.exe"
meths = []
for i in range(0, len(classes), 60):
    chunk = classes[i:i + 60]
    out = subprocess.run([JP, "-p", "-s", "-cp", jar] + [c.replace("/", ".") for c in chunk], capture_output=True, text=True, errors="replace").stdout
    cls = None; flags = ""
    lines = out.splitlines()
    j = 0
    while j < len(lines):
        l = lines[j]
        m = re.match(r"^(?:public |protected |abstract |final |static )*(class|interface|enum) ([\w.$]+)", l)
        if m:
            cls = m.group(2).replace(".", "/"); iface = m.group(1) == "interface"
        elif l.startswith("  ") and "(" in l and j + 1 < len(lines) and "descriptor:" in lines[j + 1]:
            sig = l.strip()
            d = lines[j + 1].split("descriptor:")[1].strip()
            nm = re.search(r"([\w$<>]+)\(", sig).group(1)
            if "private" not in sig and "static" not in sig and nm not in ("<init>", "<clinit>") and cls:
                meths.append((cls, nm, d, "abstract" in sig or iface))
        j += 1
res = []
for cls, nm, d, abstract in meths:
    c7 = P7 + mapcls(cls)
    if P6 + mapcls(cls) in hier:   # 1.6.4 hand-written class of its own
        continue
    if c7 not in hier:
        res.append(("NOCLASS", cls, nm, d)); continue
    nd = mapdesc(d)
    ok = any((nm, nd.replace("L", "L" + P7).replace("L" + P7 + P7, "L" + P7)) in shim.get(a, {}) for a in anc(c7))
    # compare via normalized shim descs
    found = False
    alts = []
    for a in anc(c7):
        for (n2, d2) in shim.get(a, {}):
            if n2 == nm:
                if norm_shim_desc(d2) == nd:
                    found = True
                alts.append(a.rsplit("/", 1)[-1] + "." + norm_shim_desc(d2))
    if found: continue
    if (mapcls(cls), nm, nd) in covered or (cls, nm, d) in covered: continue
    res.append(("SAMENAME" if alts else "NONE", cls, nm, nd, " | ".join(alts[:3])))
for r in sorted(res):
    print(*r)
