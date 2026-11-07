#!/usr/bin/env python3
"""Computes the automatic part of the 1.6.4 -> 1.7.10 member forward table:
loader/src/main/resources/legacyconnector/forward/1.6.4/member-renames.tsv

A member keeps its SRG id when Minecraft did not rewrite it, but MCP renames members between versions. A 1.6.4 jar calls a member by
its SRG id (or by an MCP name in a dev jar); the loader maps it to the 1.6.4 MCP name and the 1.7.10 shim only has the 1.7.10 name.
Same SRG id with another name is the rename found here:
  * fields: legacy/<ver>/fields.txt carries the SRG id of every field;
  * methods: legacy/<ver>/methods.txt has MCP names only, so a 1.6.4 name is mapped back to its SRG id(s) through the 1.6.4 methods.csv
    and forward through the 1.7.10 methods.csv; the rename counts when the 1.7.10 name (with the same descriptor, class renames
    applied) exists on the forward owner or one of its 1.7.10 supertypes.
Output lines (tab separated):  M owner name desc newName   |   F owner name newName      (owner/desc are 1.6.4 names)

usage: tools/gen_forward_164.py [mappings-dir] [version]   (default loader/build/generated/mappings/legacyconnector/mappings,
                                                  produced by ./gradlew :loader:extractMappings; version 1.6.4 (default) or 1.5.2)

Version 1.5.2 compares the 1.5.2 API data against 1.7.10 the same way, with the 1.6.4 class renames underneath its own, and writes
forward/1.5.2/member-renames.tsv + member-heuristic.tsv without the rows forward/1.6.4 already has (the tables chain, see
LegacyVersion.forwardTableIds()).
"""
import csv, os, re, sys
from collections import defaultdict

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
res = os.path.join(root, "loader", "src", "main", "resources", "legacyconnector")
VER = sys.argv[2] if len(sys.argv) > 2 else "1.6.4"
maps = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] != "-" else os.path.join(root, "loader", "build", "generated", "mappings", "legacyconnector", "mappings")


FLAT = {}
if VER == "1.4.7":
    # 1.4.7 names its classes flat (net/minecraft/src/Name); the tables name them as 1.5.2 does, so the API data is read through forward/1.4.7/flat-names.tsv
    for line in open(os.path.join(res, "forward", "1.4.7", "flat-names.tsv"), encoding="utf-8"):
        if line.strip() and not line.startswith("#"):
            a, b = line.rstrip(chr(10)).split(chr(9))
            FLAT[a] = b


def pk(name):
    if not FLAT or not name.startswith("net/minecraft/src/"):
        return name
    i = name.find("$")
    outer = name if i < 0 else name[:i]
    r = FLAT.get(outer)
    return name if r is None else r + ("" if i < 0 else name[i:])


def pkd(desc):
    return re.sub(r"L([^;]+);", lambda m: "L" + pk(m.group(1)) + ";", desc) if FLAT else desc


def read_csv(path):
    out = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8", newline="") as f:
            for row in csv.reader(f):
                if row and row[0] != "searge" and len(row) > 1:
                    out[row[0]] = row[1]
    return out


m164 = read_csv(os.path.join(maps, VER, "methods.csv"))
m17 = read_csv(os.path.join(maps, "1.7.10", "fallback-methods.csv"))
m17.update(read_csv(os.path.join(maps, "1.7.10", "methods.csv")))
if not m164 or not m17:
    sys.exit("mapping csvs not found in %s (run ./gradlew :loader:extractMappings)" % maps)
name2ids = defaultdict(list)
for k, v in m164.items():
    name2ids[v].append(k)

renames = {}
BASE_ROWS = set()
if VER != "1.6.4":
    for fn in ("member-renames.tsv", "member-heuristic.tsv", "member-manual.tsv"):
        for line in open(os.path.join(res, "forward", "1.6.4", fn), encoding="utf-8"):
            if line.strip() and not line.startswith("#"):
                BASE_ROWS.add(line.rstrip("\n"))
for fid in (["1.6.4", "1.5.2"] if VER == "1.5.2" else ["1.6.4"]):
    for line in open(os.path.join(res, "forward", fid, "class-renames.tsv"), encoding="utf-8"):
        if line.strip() and not line.startswith("#"):
            a, b = line.rstrip("\n").split("\t")
            renames[a] = b


def rd(desc):
    return re.sub(r"L([^;]+);", lambda m: "L" + renames.get(m.group(1), m.group(1)) + ";", desc)


def hier(ver):
    h = {}
    for line in open(os.path.join(res, "legacy", ver, "hierarchy.txt"), encoding="utf-8"):
        p = line.split()
        if len(p) >= 3:
            h[pk(p[0]) if ver == VER else p[0]] = [pk(x) if ver == VER else x for x in [p[1]] + (p[2].split(",") if p[2] != "-" else []) if x and x != "-"]
    return h


h17 = hier("1.7.10")
h16 = hier(VER)


def methods(ver):
    out = defaultdict(set)
    for line in open(os.path.join(res, "legacy", ver, "methods.txt"), encoding="utf-8"):
        p = line.split()
        if len(p) >= 3:
            out[pk(p[0]) if ver == VER else p[0]].add((p[1], pkd(p[2]) if ver == VER else p[2]))
    return out


meth16 = methods(VER)
meth17 = methods("1.7.10")


def has17(owner, name, desc, seen=None):
    seen = seen if seen is not None else set()
    if owner in seen:
        return False
    seen.add(owner)
    if (name, desc) in meth17.get(owner, ()):
        return True
    return any(has17(s, name, desc, seen) for s in h17.get(owner, ()))


method_rows = []
unmatched = []
for owner in sorted(meth16):
    fo = renames.get(owner, owner)
    if fo not in h17 and fo not in meth17:
        continue  # class the 1.7.10 API does not have: handled as synthesized
    for name, desc in sorted(meth16[owner]):
        if name.startswith("func_") or name in ("<init>", "<clinit>"):
            continue
        d = rd(desc)
        if has17(fo, name, d):
            continue
        cands = set()
        for sid in name2ids.get(name, ()):
            n = m17.get(sid)
            if n and n != name and has17(fo, n, d):
                cands.add(n)
        if len(cands) == 1:
            method_rows.append(("M", owner, name, desc, cands.pop()))
        else:
            unmatched.append((owner, name, desc, sorted(cands)))

field164 = {}
field17 = {}
for ver, tgt in ((VER, field164), ("1.7.10", field17)):
    for line in open(os.path.join(res, "legacy", ver, "fields.txt"), encoding="utf-8"):
        p = line.split()
        if len(p) == 5 and p[0].startswith("field_"):
            tgt[p[0]] = (pk(p[1]), p[2], pkd(p[3])) if ver == VER else (p[1], p[2], p[3])
field_rows = []
field_moved = []
for sid, (owner, name, desc) in sorted(field164.items()):
    if sid not in field17:
        continue
    o7, n7, d7 = field17[sid]
    fo = renames.get(owner, owner)
    if n7 != name and (o7 == fo or fo in h17) and (VER != "1.4.7" or (rd(desc) == d7 and owner not in ("net/minecraft/util/Direction", "net/minecraft/util/Facing", "net/minecraft/world/ChunkCache") and (owner, name) != ("net/minecraft/item/Item", "itemName"))):
        # same owner, or the 1.7.10 owner is an ancestor of the forward owner (the name is resolved up the chain)
        field_rows.append(("F", owner, name, n7))
    if o7 != fo:
        field_moved.append((sid, owner, name, o7, n7))

if VER == "1.4.7":
    # the vanilla statics (Block.blockSteel, Block.stoneOvenIdle, Item.pickaxeSteel) are provided by the 1.6.4 facade under their 1.6.4 names (blocks.tsv, items.tsv);
    # the 1.4.7 names that differ (the same SRG id) are renamed to those
    f164 = {}
    for line in open(os.path.join(res, "legacy", "1.6.4", "fields.txt"), encoding="utf-8"):
        p = line.split()
        if len(p) == 5 and p[0].startswith("field_") and p[4] == "1":
            f164[p[0]] = (p[1], p[2])
    for sid, (owner, name, desc) in sorted(field164.items()):
        if owner not in ("net/minecraft/block/Block", "net/minecraft/item/Item"):
            continue
        o6, n6 = f164.get(sid, (None, None))
        if o6 == owner and n6 != name and ("F", owner, name, n6) not in field_rows:
            field_rows.append(("F", owner, name, n6))
out = os.path.join(res, "forward", VER, "member-renames.tsv")
with open(out, "w", encoding="utf-8", newline="\n") as f:
    f.write("# Generated by tools/gen_forward_164.py %s (do not edit; manual rules go to member-manual.tsv)\n" % VER)
    f.write("# M owner name desc newName | F owner name newName ; 1.6.4 names; the owner may be any subtype (looked up through the 1.6.4 hierarchy)\n")
    for r in sorted(method_rows) + sorted(field_rows):
        if VER != "1.6.4" and "\t".join(r) in BASE_ROWS:
            continue
        f.write("\t".join(r) + "\n")
print("methods renamed:", len(method_rows), "fields renamed:", len(field_rows), "-> ", out)
rep = os.path.join(root, "build", "forward%s-report.txt" % VER.replace(".", ""))
os.makedirs(os.path.dirname(rep), exist_ok=True)
with open(rep, "w", encoding="utf-8") as f:
    f.write("# 1.6.4 methods with no 1.7.10 counterpart of the same name+descriptor and no unique same-SRG rename\n")
    for o, n, d, c in unmatched:
        f.write("%s %s %s %s\n" % (o, n, d, ",".join(c)))
    f.write("# fields whose SRG id lives in another class in 1.7.10\n")
    for sid, o, n, o7, n7 in field_moved:
        f.write("%s %s.%s -> %s.%s\n" % (sid, o, n, o7, n7))
print("unmatched methods:", len(unmatched), "moved fields:", len(field_moved), "report:", rep)

# ---- heuristic pairing (reported for review, written to build/forward164-heuristic.tsv; accepted pairs are copied to member-manual.tsv by hand)
def has16(owner, name, desc, seen=None):
    seen = seen if seen is not None else set()
    if owner in seen:
        return False
    seen.add(owner)
    if (name, desc) in meth16.get(owner, ()):
        return True
    return any(has16(s, name, desc, seen) for s in h16.get(owner, ()))


matched = {(r[1], r[2], r[3]) for r in method_rows}
inv = {v: k for k, v in renames.items()}
pairs = []
for owner in sorted(meth16):
    fo = renames.get(owner, owner)
    if fo not in meth17:
        continue
    u16 = [(n, d) for n, d in sorted(meth16[owner]) if (owner, n, d) not in matched and not has17(fo, n, rd(d)) and not n.startswith("func_")]
    n17 = [(n, d) for n, d in sorted(meth17[fo]) if not n.startswith("func_") and not has16(owner, n, d) and d == rd(d) or (n, d) in meth17[fo] and not has16(owner, n, d)]
    n17 = sorted(set((n, d) for n, d in n17 if not has16(owner, n, d)))
    by16 = defaultdict(list)
    by17 = defaultdict(list)
    for n, d in u16:
        by16[rd(d)].append(n)
    for n, d in n17:
        by17[d].append(n)
    for d, names in by16.items():
        if len(names) == 1 and len(by17.get(d, ())) == 1:
            pairs.append((owner, names[0], [k for k in meth16[owner] if k[0] == names[0] and rd(k[1]) == d][0][1], by17[d][0]))


STOP = {"get", "set", "is", "has", "do", "can", "on", "render", "the"}
# unique same-descriptor pairs that are not renames (checked by hand against the two APIs)
DENY = {("net/minecraft/item/Item", "getItemDisplayName"), ("net/minecraft/entity/player/EntityPlayerMP", "addChatMessage"),
        ("net/minecraft/server/integrated/IntegratedServer", "getDifficulty"), ("net/minecraft/entity/item/EntityFallingSand", "setIsAnvil"),
        ("net/minecraft/world/World", "getLocationTensionFactor"), ("net/minecraft/client/multiplayer/ServerData", "isHidingAddress"),
        ("net/minecraft/client/renderer/ThreadDownloadImageData", "getBufferedImage"),
        # 1.5.2: EntityLiving's own members compared with EntityLivingBase's alone look like renames (the class is split in 1.7.10)
        ("net/minecraft/entity/EntityLiving", "setAttackTarget"), ("net/minecraft/entity/Entity", "unmountEntity"),
        ("net/minecraft/enchantment/EnchantmentHelper", "getEnchantmentModifierLiving")}


def tokens(n):
    return {t.lower() for t in re.findall(r"[A-Z]?[a-z0-9]+|[A-Z]+(?![a-z])", n)} - STOP


def plausible(old, new, desc):
    """A unique same-descriptor pair on one class is a rename when the descriptor says something (arguments, a class type)
    or the names share a word; a bare ()V/()Z/()I pair could as well be one method removed and another added."""
    if tokens(old) & tokens(new):
        return True
    args = desc[1:desc.index(")")]
    ret = desc[desc.index(")") + 1:]
    return bool(args) or (ret.startswith("L") and ret not in ("Ljava/lang/String;", "Ljava/lang/Object;"))


# 1.4.7: a rename onto a func_ name is a 1.7.10 method the shim has no readable name for; Item.setIconIndex, BlockLeaves.breakBlock and the sprite sheet getters are handled by the texture layer
DENY |= {("net/minecraft/item/Item", "setIconIndex"), ("net/minecraft/block/BlockLeaves", "breakBlock"), ("net/minecraft/block/BlockRedstoneRepeater", "getBlockTextureFromSide")}
accepted = [p for p in pairs if plausible(p[1], p[3], p[2]) and (p[0], p[1]) not in DENY and not p[3].startswith("func_")]

# fields: unique same-descriptor, same-owner pair of fields only one version has
f16 = defaultdict(list)
f17 = defaultdict(list)
for sid, (o, n, d) in field164.items():
    f16[o].append((sid, n, d))
for sid, (o, n, d) in field17.items():
    f17[o].append((sid, n, d))
field_pairs = []
done = {r[1] + "." + r[2] for r in field_rows}
names17 = defaultdict(set)
for o, lst in f17.items():
    for _, n, d in lst:
        names17[o].add(n)
for owner in sorted(f16):
    fo = renames.get(owner, owner)
    u16 = [(n, d) for sid, n, d in f16[owner] if owner + "." + n not in done and n not in names17.get(fo, ()) and not n.startswith("field_")]
    n16 = {n for _, n, _ in f16[owner]}
    u17 = [(n, rd(d) if False else d) for sid, n, d in f17.get(fo, ()) if n not in n16 and not n.startswith("field_")]
    by16 = defaultdict(list)
    by17 = defaultdict(list)
    for n, d in u16:
        by16[rd(d)].append(n)
    for n, d in u17:
        by17[d].append(n)
    for d, names in by16.items():
        if len(names) == 1 and len(by17.get(d, ())) == 1 and tokens(names[0]) & tokens(by17[d][0]):
            if owner not in ("net/minecraft/util/Direction", "net/minecraft/util/Facing", "net/minecraft/world/ChunkCache") and (owner, names[0]) != ("net/minecraft/item/Item", "itemName"):
                field_pairs.append(("F", owner, names[0], by17[d][0]))

out = os.path.join(res, "forward", VER, "member-heuristic.tsv")
with open(out, "w", encoding="utf-8", newline="\n") as f:
    f.write("# Generated by tools/gen_forward_164.py: a unique same-descriptor pair on one class that only one version has (a rename, judged by plausible()).\n")
    f.write("# Same format as member-renames.tsv; member-manual.tsv wins over this file, this file over member-renames.tsv.\n")
    for o, n, d, nn in sorted(accepted):
        row = "M\t%s\t%s\t%s\t%s" % (o, n, d, nn)
        if VER == "1.6.4" or row not in BASE_ROWS:
            f.write(row + "\n")
    for r in sorted(field_pairs):
        if VER == "1.6.4" or "\t".join(r) not in BASE_ROWS:
            f.write("\t".join(r) + "\n")
print("heuristic accepted:", len(accepted), "of", len(pairs), "field pairs:", len(field_pairs))
