#!/usr/bin/env python3
"""Builds the legacy class hierarchy the loader ships for API synthesis:
loader/src/main/resources/legacyconnector/legacy/<ver>/hierarchy.txt

Each line: <internal name> <super|-> <iface,iface|-> <access flags>
Covers every vanilla class (from build/devtools/legacy-<ver>.sig) plus every Forge/FML class, parsed from the
sources inside the Forge userdev jar. Interfaces Forge patches onto vanilla classes (IShearable, IPlantable, ...)
are merged in from the userdev patch sets. Only names and supertypes are recorded, never code.

usage: tools/extract_legacy_hierarchy.py 1.7.10
"""
import io, os, re, sys, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DEV = os.path.join(ROOT, "build", "devtools")
ver = sys.argv[1] if len(sys.argv) > 1 else "1.7.10"
USERDEV = {"1.7.10": os.path.join(DEV, "mcp", "forge1710-userdev.jar"),
           "1.12.2": os.path.join(DEV, "mcp", "forge1122-sources.jar")}[ver]
# Forge 1.12.2 patches these vanilla types to be registry entries (binary patches only, so listed here).
EXTRA_1122 = {
    "net/minecraft/block/Block": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
    "net/minecraft/item/Item": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
    "net/minecraft/enchantment/Enchantment": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
    "net/minecraft/potion/Potion": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
    "net/minecraft/potion/PotionType": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
    "net/minecraft/util/SoundEvent": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
    "net/minecraft/world/biome/Biome": ("net/minecraftforge/registries/IForgeRegistryEntry$Impl", []),
}
EXTRA_1122_IFACES = {
    "net/minecraft/item/crafting/IRecipe": ["net/minecraftforge/registries/IForgeRegistryEntry"],
}
SRC_PREFIX = "src/main/java/" if ver == "1.7.10" else ""

ACC_PUBLIC, ACC_FINAL, ACC_INTERFACE, ACC_ABSTRACT, ACC_ANNOTATION, ACC_ENUM = 0x1, 0x10, 0x200, 0x400, 0x2000, 0x4000

classes = {}  # name -> [super, [ifaces], access]
for line in open(os.path.join(DEV, "legacy-" + ver + ".sig")):
    p = line.split()
    if p[0] == "C":
        classes[p[1]] = [p[2], [] if p[3] == "-" else p[3].split(","), int(p[4])]

# ------------------------------------------------------------------ Java source parsing (Forge / FML)
TOKEN = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*|\S")


def strip(src):
    src = re.sub(r"/\*.*?\*/", " ", src, flags=re.S)
    src = re.sub(r"//[^\n]*", " ", src)
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    return re.sub(r"'(?:\\.|[^'\\])+'", "' '", src)


def parse(path, src):
    toks = TOKEN.findall(strip(src))
    pkg, imports, out = "", [], []
    i, depth, stack = 0, 0, []  # stack: (name, depth at which body opened)
    pending = None
    while i < len(toks):
        t = toks[i]
        if t == "package" and depth == 0:
            j = i + 1
            while toks[j] != ";": j += 1
            pkg = "".join(toks[i + 1:j]).replace(".", "/"); i = j
        elif t == "import" and depth == 0:
            j = i + 1
            while toks[j] != ";": j += 1
            imp = "".join(toks[i + 1:j])
            if not imp.startswith("static"): imports.append(imp)
            i = j
        elif t in ("class", "interface", "enum") and (i == 0 or toks[i - 1] != ".") and i + 1 < len(toks) and re.match(r"[A-Za-z_]", toks[i + 1]):
            kind, name = t, toks[i + 1]
            annotation = i > 0 and toks[i - 1] == "@"
            mods, k = set(), i - 1
            while k >= 0 and toks[k] in ("public", "private", "protected", "static", "final", "abstract", "strictfp", "@"):
                mods.add(toks[k]); k -= 1
            j, sup, ifaces = i + 2, None, []
            if j < len(toks) and toks[j] == "<":
                d = 0
                while True:
                    d += toks[j] == "<"; d -= toks[j] == ">"; j += 1
                    if d == 0: break
            cur = None
            while toks[j] != "{":
                if toks[j] in ("extends", "implements"): cur = toks[j]; j += 1; continue
                if toks[j] == "<":
                    d = 0
                    while True:
                        d += toks[j] == "<"; d -= toks[j] == ">"; j += 1
                        if d == 0: break
                    continue
                if toks[j] == ",": j += 1; continue
                q = [toks[j]]; j += 1
                while toks[j] == "." : q.append(toks[j + 1]); j += 2
                ref = ".".join(q)
                if cur == "extends" and kind == "class": sup = ref
                else: ifaces.append(ref)
                j += 0
            outer = stack[-1][0] if stack else None
            internal = (outer + "$" + name) if outer else ((pkg + "/" if pkg else "") + name)
            acc = 0
            if "public" in mods: acc |= ACC_PUBLIC
            if "final" in mods: acc |= ACC_FINAL
            if "abstract" in mods: acc |= ACC_ABSTRACT
            if kind == "interface": acc |= ACC_INTERFACE | ACC_ABSTRACT
            if annotation: acc |= ACC_ANNOTATION
            if kind == "enum": acc |= ACC_ENUM | ACC_FINAL
            default_super = "java/lang/Enum" if kind == "enum" else ("java/lang/Object")
            out.append((internal, sup, ifaces, acc, default_super, pkg, list(imports), [s[0] for s in stack]))
            depth += 1
            stack.append((internal, depth))
            i = j + 1
            continue
        elif t == "{":
            depth += 1
        elif t == "}":
            if stack and stack[-1][1] == depth: stack.pop()
            depth -= 1
        i += 1
    return out


parsed = []
with zipfile.ZipFile(USERDEV) as z:
    for n in z.namelist():
        if n.startswith(SRC_PREFIX) and n.endswith(".java") and not n.startswith("META-INF"):
            parsed += parse(n, z.read(n).decode("utf-8", "replace"))
    patch_sets = [z.read(n) for n in ("forgepatches.zip", "fmlpatches.zip") if n in z.namelist()]

known = set(classes) | {p[0] for p in parsed}
JAVA_LANG = {"Object", "Enum", "Exception", "RuntimeException", "Error", "Throwable", "Thread", "Runnable", "Comparable",
             "Iterable", "Cloneable", "AutoCloseable", "Number", "ClassLoader", "IllegalArgumentException",
             "IllegalStateException", "SecurityManager"}


def nested(path):
    """a/b/Outer/Inner -> a/b/Outer$Inner when only the nested spelling is a known class."""
    parts = path.split("/")
    for k in range(len(parts), 0, -1):
        cand = "/".join(parts[:k]) + "".join("$" + p for p in parts[k:])
        if cand in known: return cand
    return path


def resolve(ref, pkg, imports, outers):
    head, _, rest = ref.partition(".")
    cand = None
    for o in reversed(outers):  # nested types of enclosing classes, innermost first
        for c in (o + "$" + head, o.rsplit("$", 1)[0] + "$" + head if "$" in o else None):
            if c and c in known: cand = c; break
        if cand: break
    if not cand:
        for imp in imports:
            if imp.endswith("." + head) or imp == head:
                cand = nested(imp.replace(".", "/")); break
    if not cand and (pkg + "/" + head) in known: cand = pkg + "/" + head
    if not cand:
        for imp in imports:
            if imp.endswith(".*") and imp[:-2].replace(".", "/") + "/" + head in known:
                cand = imp[:-2].replace(".", "/") + "/" + head; break
    if not cand and head in JAVA_LANG: cand = "java/lang/" + head
    if not cand and "." in ref and ref[0].islower():  # fully qualified
        parts = ref.split(".")
        for k in range(len(parts), 0, -1):
            base = "/".join(parts[:k])
            if base in known or k == len(parts):
                return base + "".join("$" + p for p in parts[k:])
    if not cand: return None
    for part in rest.split(".") if rest else []: cand += "$" + part
    return cand


for internal, sup, ifaces, acc, default_super, pkg, imports, outers in parsed:
    if internal in classes: continue  # vanilla data from the real jar wins
    s = resolve(sup, pkg, imports, outers + [internal]) if sup else default_super
    if acc & ACC_INTERFACE: s = "java/lang/Object"
    if acc & ACC_ANNOTATION: ifaces = ["java.lang.annotation.Annotation"]
    res = []
    for f in ifaces:
        r = resolve(f, pkg, imports, outers + [internal]) if f != "java.lang.annotation.Annotation" else "java/lang/annotation/Annotation"
        if r: res.append(r)
    classes[internal] = [s or "java/lang/Object", res, acc]

# ------------------------------------------------------------------ interfaces Forge patches onto vanilla classes
DECL = re.compile(r"^\+\s*(?:public\s+|abstract\s+|final\s+)*(?:class|interface)\s+(\w+).*?\bimplements\s+([\w.,\s]+)")
added = 0
for blob in patch_sets:
    with zipfile.ZipFile(io.BytesIO(blob)) as pz:
        for n in pz.namelist():
            text = pz.read(n).decode("utf-8", "replace")
            m = re.search(r"\+\+\+ \S*?(net/minecraft/\S+?)\.java", text)
            if not m: continue
            owner = m.group(1)
            imports = re.findall(r"^[+ ]import\s+([\w.]+);", text, flags=re.M)
            for line in text.splitlines():
                d = DECL.match(line)
                if not d or owner not in classes: continue
                target = owner if d.group(1) == owner.rsplit("/", 1)[1] else owner + "$" + d.group(1)
                if target not in classes: continue
                for f in re.split(r"[,\s]+", d.group(2).strip()):
                    if not f: continue
                    r = resolve(f, owner.rsplit("/", 1)[0], imports, [owner])
                    if r and r not in classes[target][1]:
                        classes[target][1].append(r); added += 1

if ver == "1.12.2":
    for owner, (sup, ifs) in EXTRA_1122.items():
        if owner in classes: classes[owner][0] = sup
    for owner, ifs in EXTRA_1122_IFACES.items():
        if owner in classes:
            for i in ifs:
                if i not in classes[owner][1]: classes[owner][1].append(i)

out_dir = os.path.join(ROOT, "loader", "src", "main", "resources", "legacyconnector", "legacy", ver)
os.makedirs(out_dir, exist_ok=True)
with open(os.path.join(out_dir, "hierarchy.txt"), "w") as f:
    for name in sorted(classes):
        s, ifs, acc = classes[name]
        f.write("%s %s %s %d\n" % (name, s or "-", ",".join(ifs) or "-", acc))
print("wrote %d classes (%d from Forge sources, %d patched-in interfaces)" % (len(classes), len(parsed), added))
