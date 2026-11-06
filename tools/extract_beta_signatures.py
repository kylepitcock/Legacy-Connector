#!/usr/bin/env python3
"""Builds the Beta 1.7.3 reference data the loader ships for API synthesis and deobfuscation:

  build/devtools/legacy-b1.7.3.sig                                    (every class/field/method, named; for the other tools)
  loader/src/main/resources/legacyconnector/legacy/b1.7.3/hierarchy.txt  (<name> <super|-> <iface,...|-> <access>)

Sources: the b1.7.3 client and server jars with Risugami's ModLoader, ModLoaderMP and Forge 1.0.7 laid over them in the order
players installed them (their patched copies of vanilla classes add members, e.g. Forge's Block.isLadder), all named through the
official -> named table that :loader:genBetaMappings builds (Babric intermediary + biny). ModLoader/ModLoaderMP classes (default
package) are named net/minecraft/src/<Name> and Forge's forge/<Name> net/minecraft/src/forge/<Name>, as in MCP. Client and
server copies of a class are merged. Only names, descriptors and supertypes are recorded, never code.

usage: tools/fetch_devtools.sh; ./gradlew :loader:genBetaMappings; tools/extract_beta_signatures.py
then:  tools/extract_legacy_fields.py b1.7.3; tools/extract_legacy_methods.py b1.7.3
"""
import os, re, struct, sys, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DEV = os.path.join(ROOT, "build", "devtools", "b1.7.3")
TABLE = os.path.join(ROOT, "loader", "build", "generated", "beta-mappings", "legacyconnector", "mappings", "b1.7.3", "official-named.tsv")
OUT_SIG = os.path.join(ROOT, "build", "devtools", "legacy-b1.7.3.sig")
OUT_HIER = os.path.join(ROOT, "loader", "src", "main", "resources", "legacyconnector", "legacy", "b1.7.3", "hierarchy.txt")

# Must match org.kylepitcock.legacyconnector.loader.BetaJars.MODLOADER_API
MODLOADER_API = {"BaseMod", "ModLoader", "MLProp", "ModTextureStatic", "ModTextureAnimation", "EntityRendererProxy",
                 "BaseModMp", "ModLoaderMp", "ISpawnable", "Packet230ModLoader", "NetClientHandlerEntity", "EntityTrackerEntry2"}
API_PREFIX = "net/minecraft/src/"

SIDES = {
    "c": ["client.jar", "modloader.jar", "modloadermp-client.jar", "forge-client.zip"],
    "s": ["server.jar", "modloadermp-server.jar", "forge-server.zip"],
}


def parse_class(data):
    """Minimal class file reader: (name, super, interfaces, access, fields, methods)."""
    pos = 8
    count = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
    cp = [None] * count
    i = 1
    while i < count:
        tag = data[pos]; pos += 1
        if tag == 1:
            ln = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
            cp[i] = data[pos:pos + ln].decode("utf-8", "replace"); pos += ln
        elif tag in (3, 4): pos += 4
        elif tag in (5, 6): pos += 8; i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = ("ref", struct.unpack(">H", data[pos:pos + 2])[0]); pos += 2
        elif tag in (9, 10, 11, 12, 17, 18): pos += 4
        elif tag == 15: pos += 3
        else: raise ValueError("bad tag %d" % tag)
        i += 1
    cls = lambda idx: cp[cp[idx][1]] if idx else None
    access, this, sup = struct.unpack(">HHH", data[pos:pos + 6]); pos += 6
    n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
    ifaces = [cls(struct.unpack(">H", data[pos + 2 * k:pos + 2 * k + 2])[0]) for k in range(n)]; pos += 2 * n

    def members(pos):
        out = []
        n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
        for _ in range(n):
            acc, ni, di, ac = struct.unpack(">HHHH", data[pos:pos + 8]); pos += 8
            for _ in range(ac):
                ln = struct.unpack(">I", data[pos + 2:pos + 6])[0]; pos += 6 + ln
            out.append((cp[ni], cp[di], acc))
        return out, pos

    fields, pos = members(pos)
    methods, pos = members(pos)
    return cls(this), cls(sup), ifaces, access, fields, methods


# ------------------------------------------------------------------------------------------- mapping table
classes = {"c": {}, "s": {}}
fields = {"c": {}, "s": {}}
methods = {"c": {}, "s": {}}
for line in open(TABLE, encoding="utf-8"):
    if line.startswith("#"): continue
    p = line.rstrip("\n").split("\t")
    if p[0] == "C": classes[p[1]][p[2]] = p[3]
    elif p[0] == "F": fields[p[1]][(p[2], p[3], p[4])] = p[5]
    elif p[0] == "M": methods[p[1]][(p[2], p[3], p[4])] = p[5]


def map_class(side, name):
    if name is None: return None
    if name in classes[side]: return classes[side][name]
    if "/" not in name and name in MODLOADER_API: return API_PREFIX + name
    if name.startswith("forge/"): return API_PREFIX + name
    return name


def map_desc(side, desc):
    return re.sub(r"L([^;]+);", lambda m: "L" + map_class(side, m.group(1)) + ";", desc)


# ------------------------------------------------------------------------------------------- read both sides
def read_side(side):
    """official name -> parsed class, later jars replacing earlier copies (mod installation order)."""
    out = {}
    for jar in SIDES[side]:
        with zipfile.ZipFile(os.path.join(DEV, jar)) as z:
            for entry in z.namelist():
                if not entry.endswith(".class") or entry.startswith("META-INF/"): continue
                c = parse_class(z.read(entry))
                out[c[0]] = c
    return out


def resolve(side, raw, owner, kind, name, desc, seen=None):
    """Named member, looked up in the declaring class or (for overrides the table lists only once) its supertypes."""
    table = methods[side] if kind == "M" else fields[side]
    seen = seen or set()
    while owner is not None and owner not in seen:
        seen.add(owner)
        hit = table.get((owner, name, desc))
        if hit: return hit
        c = raw.get(owner)
        if c is None: return None
        for i in c[2]:
            hit = resolve(side, raw, i, kind, name, desc, seen)
            if hit: return hit
        owner = c[1]
    return None


merged = {}  # named -> [super, ifaces, access, {field key: line}, {method key: line}]
for side in ("c", "s"):
    raw = read_side(side)
    for official, (name, sup, ifaces, acc, fs, ms) in raw.items():
        named = map_class(side, official)
        if not named.startswith("net/minecraft/"): continue  # bundled libraries (argo, ...)
        entry = merged.setdefault(named, [map_class(side, sup) if sup else "-", [map_class(side, i) for i in ifaces], 0, {}, {}])
        entry[2] |= acc
        for i in (map_class(side, i) for i in ifaces):
            if i not in entry[1]: entry[1].append(i)
        for fn, fd, fa in fs:
            nn = resolve(side, raw, official, "F", fn, fd) or fn
            d = map_desc(side, fd)
            entry[3].setdefault(nn + " " + d, "F %s %s %d %s" % (nn, d, fa, nn))
        for mn, md, ma in ms:
            nn = mn if mn.startswith("<") else (resolve(side, raw, official, "M", mn, md) or mn)
            d = map_desc(side, md)
            entry[4].setdefault(nn + d, "M %s %s %d" % (nn, d, ma))

lines, hier = [], []
for named in sorted(merged):
    sup, ifaces, acc, fs, ms = merged[named]
    lines.append("C %s %s %s %d" % (named, sup, ",".join(ifaces) or "-", acc))
    lines.extend(fs.values())
    lines.extend(ms.values())
    hier.append("%s %s %s %d" % (named, sup, ",".join(ifaces) or "-", acc))
os.makedirs(os.path.dirname(OUT_SIG), exist_ok=True)
open(OUT_SIG, "w", encoding="utf-8").write("\n".join(lines) + "\n")
os.makedirs(os.path.dirname(OUT_HIER), exist_ok=True)
open(OUT_HIER, "w", encoding="utf-8", newline="\n").write("\n".join(hier) + "\n")
print("wrote", OUT_SIG, "and", OUT_HIER, len(hier), "classes")
