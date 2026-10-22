#!/usr/bin/env python3
"""Generates the 1.7.10 vanilla registries (init/Blocks.java, init/Items.java) and hierarchy-correct shells for the
vanilla block/item classes their fields are typed with. Hand-written shim classes are never overwritten.

usage: tools/gen_vanilla_shims.py   (needs build/devtools/legacy-1.7.10.sig, see extract_legacy_signatures.py)
"""
import os, re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SHIM = os.path.join(ROOT, "src", "main", "java", "legacyconnector", "v1_7_10")
PREFIX = "legacyconnector.v1_7_10."
classes, cur = {}, None
for line in open(os.path.join(ROOT, "build", "devtools", "legacy-1.7.10.sig")):
    p = line.split()
    if p[0] == "C":
        cur = p[1]; classes[cur] = {"super": p[2], "F": [], "M": []}
    else:
        classes[cur][p[0]].append(p[1:])

def path_of(internal):
    return os.path.join(SHIM, internal + ".java")

def exists(internal):
    return os.path.exists(path_of(internal))

def jtype(desc):
    """Descriptor -> Java source type (legacy classes relocated); None if a referenced class has no shim."""
    arr = 0
    while desc.startswith("["): arr += 1; desc = desc[1:]
    prim = {"I": "int", "Z": "boolean", "F": "float", "D": "double", "J": "long", "B": "byte", "S": "short", "C": "char"}
    if desc in prim: t = prim[desc]
    else:
        n = desc[1:-1]
        if n.startswith("java/"): t = n.replace("/", ".").replace("$", ".")
        elif exists(n.split("$")[0]) or n in generated: t = PREFIX + n.replace("/", ".").replace("$", ".")
        else: return None
    return t + "[]" * arr

def params(desc):
    inner = desc[1:desc.index(")")]
    out, i = [], 0
    while i < len(inner):
        j = i
        while inner[j] == "[": j += 1
        if inner[j] == "L": j = inner.index(";", j)
        out.append(inner[i:j + 1]); i = j + 1
    return out

generated = set()
REG_NAMES = {}
_names = os.path.join(ROOT, "build", "devtools", "legacy-1.7.10-registry-names.txt")
if os.path.exists(_names):
    for line in open(_names):
        owner, field, reg = line.split()
        REG_NAMES[(owner, field)] = reg

def plan_chain(internal, root):
    chain = []
    c = internal
    while c != root:
        if not exists(c): chain.append(c)
        c = classes[c]["super"]
    return chain

MATERIAL_GUESS = {"Leaves": "leaves", "Bush": "plants", "Flower": "plants", "Grass": "grass", "Sand": "sand", "Fire": "fire",
                  "Liquid": "water", "Portal": "portal", "Redstone": "circuits", "Chest": "wood", "Glass": "glass",
                  "Mycelium": "grass", "Piston": "piston", "Tripwire": "circuits", "Daylight": "wood", "Cauldron": "iron",
                  "Hopper": "iron", "Beacon": "glass", "DoublePlant": "vine", "TallGrass": "vine", "DeadBush": "vine"}

def material_for(simple):
    for k, v in MATERIAL_GUESS.items():
        if k in simple: return v
    return "rock"

def write_shell(internal, root):
    info = classes[internal]
    pkg = internal.rsplit("/", 1)[0].replace("/", ".")
    simple = internal.rsplit("/", 1)[1]
    sup = info["super"]
    sup_t = PREFIX + sup.replace("/", ".")
    is_block = root.endswith("Block")
    lines = ["package %s%s;" % (PREFIX, pkg), "",
             "/**",
             " * Generated shell of the 1.7.10 class {@code %s}: correct type hierarchy for {@code instanceof} checks and" % internal.replace("/", "."),
             " * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.",
             " * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.",
             " */",
             "public class %s extends %s {" % (simple, sup_t),
             "    public %s(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {" % simple,
             "        super(info);",
             "    }", ""]
    have = set()
    if is_block:
        lines += ["    protected %s(%sMaterial material) {" % (simple, PREFIX + "net.minecraft.block.material."),
                  "        super(material);", "    }", ""]
        have.add("(Lnet/minecraft/block/material/Material;)V")
    else:
        lines += ["    protected %s() {" % simple, "        super();", "    }", ""]
        have.add("()V")
    for m in info["M"]:
        if m[0] != "<init>" or m[1] in have: continue
        acc = int(m[2])
        if acc & 0x2: continue  # private
        ps = params(m[1])
        types = [jtype(p) for p in ps]
        if any(t is None for t in types): continue
        vis = "public" if acc & 0x1 else "protected"
        args = ", ".join("%s p%d" % (t, i) for i, t in enumerate(types))
        mat = next(("p%d" % i for i, p in enumerate(ps) if p == "Lnet/minecraft/block/material/Material;"), None)
        call = ("this(%s)" % (mat or PREFIX + "net.minecraft.block.material.Material." + material_for(simple))) if is_block else "this()"
        lines += ["    %s %s(%s) {" % (vis, simple, args), "        %s;" % call, "    }", ""]
        have.add(m[1])
    lines[-1:] = ["}"]
    os.makedirs(os.path.dirname(path_of(internal)), exist_ok=True)
    open(path_of(internal), "w").write("\n".join(lines) + "\n")
    print("generated", internal)

def gen_registry(owner, root, bridge):
    fields = [f for f in classes[owner]["F"] if f[1].startswith("L")]
    shells = []
    for f in fields:
        for c in plan_chain(f[1][1:-1], root):
            if c not in generated:
                generated.add(c); shells.append(c)
    # super classes first so constructors can reference them
    for c in sorted(shells, key=lambda c: len(plan_chain(c, root)) * -1, reverse=True):
        write_shell(c, root)
    simple = owner.rsplit("/", 1)[1]
    kind = "Block" if root.endswith("Block") else "Item"
    out = ["package %snet.minecraft.init;" % PREFIX, "",
           "import org.kylepitcock.legacyconnector.v1_7_10.bridge.%s;" % bridge, "",
           "/**",
           " * 1.7.10 vanilla %s instances (generated by tools/gen_vanilla_shims.py). Each is a wrapper over the modern" % kind.lower(),
           " * equivalent, with legacy metadata translated through the 1.13 flattening tables.",
           " */",
           "public class %s {" % simple]
    for name, desc, acc, srg in fields:
        t = PREFIX + desc[1:-1].replace("/", ".")
        reg = REG_NAMES.get((owner, name), name)
        out.append("    public static final %s %s = %s.vanilla(\"%s\", %s.class);" % (t, name, bridge, reg, t))
    if kind == "Item":
        out += ["", "    /** Internal: the potion item, referenced by Item.hasEffect. */",
                "    public static %snet.minecraft.item.Item mcc$potion() {" % PREFIX, "        return potionitem;", "    }"]
    out.append("}")
    p = os.path.join(SHIM, "net", "minecraft", "init", simple + ".java")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w").write("\n".join(out) + "\n")
    print("generated", owner, len(fields), "fields")

gen_registry("net/minecraft/init/Blocks", "net/minecraft/block/Block", "BlockBridge")
gen_registry("net/minecraft/init/Items", "net/minecraft/item/Item", "ItemBridge")
