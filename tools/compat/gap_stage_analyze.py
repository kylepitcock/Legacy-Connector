#!/usr/bin/env python3
"""EXPERIMENTAL (1.18.2 / 1.16.5 scoping, docs/wiki/Legacy-1.18.2-Estimate.md, Legacy-1.16.5-Estimate.md): classifies the unresolved symbols of a gap report of old Forge jars
along the chain of versions up to 1.20.1 (the version the layer has a pipeline for), stage by stage.

usage: gap_stage_analyze.py --detail <report>.detail.tsv --jars <jar-dir> --table <own srg-mojang.tsv> --forward <forward/1.20.1 dir> --out <out-dir>
           --chain 1.16.5,1.18.2,1.19.2,1.20.1 --client <ver>=<client.txt> ... [--forge <ver>=<forge-api-index.tsv> ...] [--iddiff <from>:<to>=<member-diff.tsv> ...]
           [--own <ver>] [--limit 300]

  detail.tsv   <report>.detail.tsv of ./gradlew gap1182|gap1165 (raw: own table + 1.20.1 -> 1.21.1 renames) or gap1201 -Pgap.translate=true (the layer as it is)
  --client     Mojang client.txt per version (build-local, never committed): the complete Mojang-named class/method/field lists
  --forge      tools/compat/forge_api_index.py output per version (Forge API classes and members)
  --iddiff     tools/dev/mc_member_diff.py member-diff.tsv of a transition (renames found by stable SRG ids); other transitions use a heuristic

A symbol is walked along the chain: the first transition in which it is absent decides its stage (letter = the target version's stage: with the chain 1.16.5,1.18.2,1.19.2,1.20.1
the transitions are A, B, C). Class a = a pure rename/move exists (a table row), b = signature changed or removed (needs a bridge/helper or emulation); a symbol that survives
the whole chain is "D": present unchanged in 1.20.1, the layer's own 1.20.1 -> 1.21.1 work (or a gap of the layer). F = Forge API (diffed with --forge when given),
n = not a Minecraft member (mod, library), u = member of a class that exists but is in no list (synthesised).
"""
import argparse, collections, importlib.util, pathlib, re, sys, zipfile

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("gap1192_analyze", HERE / "gap1192_analyze.py")
g19 = importlib.util.module_from_spec(spec); spec.loader.exec_module(g19)
spec = importlib.util.spec_from_file_location("mc_member_diff", HERE.parent / "dev" / "mc_member_diff.py")
mmd = importlib.util.module_from_spec(spec); spec.loader.exec_module(mmd)


class Version:
    def __init__(self, ver, client, tmp):
        self.ver = ver
        path = tmp / f"members-{ver}.tsv"
        mmd.write_members(client, path)
        self.classes, self.methods, self.fields = set(), set(), set()
        self.by_name = collections.defaultdict(set)   # (name, desc) -> owners (inherited members, other owners)
        self.by_class = collections.defaultdict(lambda: ([], []))
        self.simple = collections.defaultdict(set)
        for line in open(path, encoding="utf-8"):
            o, n, d, k = line.rstrip("\n").split("\t")
            if k == "class":
                self.classes.add(o)
                self.simple[o.rsplit("/", 1)[-1].split("$")[-1]].add(o)
            elif k == "m":
                self.methods.add((o, n, d)); self.by_name[(n, d)].add(o); self.by_class[o][0].append((n, d))
            else:
                self.fields.add((o, n)); self.by_name[(n, "")].add(o); self.by_class[o][1].append(n)


class ForgeVersion:
    def __init__(self, path):
        self.classes, self.methods, self.fields, self.by_name = {}, set(), set(), collections.defaultdict(set)
        self.simple = collections.defaultdict(set)
        for line in open(path, encoding="utf-8"):
            t = line.rstrip("\n").split("\t")
            if t[0] == "class":
                self.classes[t[1]] = (t[2], [x for x in t[3].split(",") if x])
                self.simple[t[1].rsplit("/", 1)[-1].split("$")[-1]].add(t[1])
            elif t[0] == "m":
                self.methods.add((t[1], t[2], t[3])); self.by_name[(t[2], t[3])].add(t[1])
            else:
                self.fields.add((t[1], t[2])); self.by_name[(t[2], "")].add(t[1])

    def has_member(self, owner, name, desc, method, seen=None):
        seen = seen or set()
        if owner in seen or owner not in self.classes:
            return False
        seen.add(owner)
        if (owner, name, desc) in self.methods if method else (owner, name) in self.fields:
            return True
        sup, ifs = self.classes[owner]
        return any(self.has_member(s, name, desc, method, seen) for s in [sup] + ifs if s)


NL = chr(10)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--detail", required=True); ap.add_argument("--jars", required=True); ap.add_argument("--table", required=True)
    ap.add_argument("--forward", required=True); ap.add_argument("--out", required=True); ap.add_argument("--chain", required=True)
    ap.add_argument("--client", action="append", default=[]); ap.add_argument("--forge", action="append", default=[]); ap.add_argument("--iddiff", action="append", default=[])
    ap.add_argument("--classrows", help="MCP -> Mojang class rows of the jars' own version (1.16.5), reversed to read leftover symbols")
    ap.add_argument("--limit", type=int, default=300)
    ap.add_argument("--md", help="also write the top symbols as a markdown table (tools/dev/agent-tasks/<ver>-symbols.md)")
    ap.add_argument("--title", default="Gap symbols")
    ap.add_argument("--intro", default="")
    a = ap.parse_args()
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    chain = a.chain.split(",")
    letters = [chr(ord("A") + i) for i in range(len(chain) - 1)]
    # stage letters are fixed by target version: 1.18.2 = A, 1.19.2 = B, 1.20.1 = C
    fixed = {"1.18.2": "A", "1.19.2": "B", "1.20.1": "C"}
    letters = [fixed[v] for v in chain[1:]]
    vers = {v: Version(v, p, out) for v, p in (x.split("=", 1) for x in a.client)}
    forge = {v: ForgeVersion(p) for v, p in (x.split("=", 1) for x in a.forge)}
    iddiff = {}
    for x in a.iddiff:
        key, p = x.split("=", 1)
        d = {}
        for line in open(p, encoding="utf-8"):
            sid, kind, A, B, st = line.rstrip("\n").split("\t")
            d[A] = (B, st)
        iddiff[key] = d

    own_table = {}
    for line in open(a.table, encoding="utf-8"):
        t = line.rstrip("\n").split("\t")
        if len(t) == 2:
            own_table[t[0]] = t[1]
    rev = {}
    for f in sorted(pathlib.Path(a.forward).glob("*.tsv")):
        for line in open(f, encoding="utf-8"):
            if line.startswith("#"):
                continue
            t = line.rstrip("\n").split("\t")
            if len(t) == 2 and "(" not in t[0] and "/" in t[0]:
                rev.setdefault(t[1], t[0])
    prefix_back = (("legacyconnector/v1_20_1/", ""), ("org/kylepitcock/legacyconnector/v1_20_1/", ""), ("net/neoforged/neoforge/", "net/minecraftforge/"), ("net/neoforged/fml/", "net/minecraftforge/fml/"),
                   ("net/neoforged/bus/", "net/minecraftforge/eventbus/"), ("net/neoforged/api/", "net/minecraftforge/api/"), ("net/neoforged/neoforgespi/", "net/minecraftforge/forgespi/"))

    def ren(n):
        """a class name as the layer wrote it -> the name it had in the old Forge/Minecraft version (class rows reversed, then the package moves reversed)"""
        base, dol, inner = n.partition("$")
        # a name that is a real class of the old version stays (many-to-one rows: StrictNBTIngredient and PartialNBTIngredient both map to Ingredient)
        if base in vers[chain[0]].classes or (chain[0] in forge and base in forge[chain[0]].classes):
            return n
        r = rev.get(base)
        if r is None:
            for new, old in prefix_back:
                if base.startswith(new):
                    r = old + base[len(new):]
                    break
        return (r or base) + dol + inner

    def unrename(kind, s):
        if kind == "class":
            return ren(s)
        owner, dot, rest = s.partition(".")
        rest = re.sub(r"L([A-Za-z0-9_/$]+);", lambda m: "L" + ren(m.group(1)) + ";", rest)
        return ren(owner) + dot + rest

    fabric = g19.FABRIC
    refs, miss = {}, collections.defaultdict(set)
    id_syms = set()   # symbols that still carried an SRG id after the layer (translated run): the 1.20.1 table does not know the id, the own-version table names it
    fabric_dropped = 0
    for line in open(a.detail, encoding="utf-8"):
        jar, kind, sym = line.rstrip("\n").split("\t", 2)
        if kind == "refs":
            refs[jar] = int(sym) if sym.isdigit() else 0
        elif kind in ("class", "method", "field"):
            if fabric.search(sym):
                fabric_dropped += 1; continue
            id_rx = r"\b(m_\d+_|f_\d+_|func_\d+_[A-Za-z]+_?|field_\d+_[A-Za-z]+_?)"
            had_id = re.search(id_rx, sym) is not None
            sym = re.sub(id_rx, lambda m: own_table.get(m.group(0), m.group(0)), sym)
            miss[jar].add((kind, sym))
            if had_id:
                id_syms.add((kind, sym))

    jarkind = {}
    for j in pathlib.Path(a.jars).glob("*.jar"):
        try:
            names = set(zipfile.ZipFile(j).namelist())
        except Exception:
            continue
        jarkind[j.name] = ("META-INF/mods.toml" in names, "fabric.mod.json" in names or "quilt.mod.json" in names)

    def step_mc(v0, v1, kind, owner, name, desc):
        """exists in v1 -> ('ok', ''); else ('a'|'b', why)"""
        V0, V1 = vers[v0], vers[v1]
        key = f"{v0}:{v1}"
        if kind == "class":
            if owner in V1.classes:
                return "ok", ""
            cand = [c for c in V1.simple.get(owner.rsplit("/", 1)[-1].split("$")[-1], ()) if c not in V0.classes]
            if cand:
                return "a", "class moved/renamed -> " + sorted(cand)[0]
            return "b", "class removed"
        if owner not in V1.classes:
            cand = [c for c in V1.simple.get(owner.rsplit("/", 1)[-1].split("$")[-1], ()) if c not in V0.classes]
            return ("b", "owner class removed" + (f" (class moved -> {sorted(cand)[0]})" if cand else ""))
        if kind == "field":
            if (owner, name) in V1.fields or V1.by_name.get((name, "")):
                return "ok", ""
            if key in iddiff:
                pass
            added = [f for f in V1.by_class[owner][1] if (owner, f) not in V0.fields]
            removed = [f for f in V0.by_class[owner][1] if (owner, f) not in V1.fields]
            if len(added) == 1 and len(removed) == 1 and removed[0] == name:
                return "a", "field renamed -> " + added[0]
            return "b", "field removed"
        if (owner, name, desc) in V1.methods or (name, desc) in V1.by_name:
            return "ok", ""
        hit = None
        if key in iddiff:
            hit = iddiff[key].get(f"{owner}.{name}{desc}")
        if hit and hit[1] in ("renamed", "moved"):
            return "a", "method %s -> %s" % (hit[1], hit[0])
        same_name = [d for (n, d) in V1.by_class[owner][0] if n == name]
        if same_name:
            return "b", "signature changed -> " + same_name[0]
        added = [(n, d) for (n, d) in V1.by_class[owner][0] if (owner, n, d) not in V0.methods]
        cand = [n for (n, d) in added if d == desc]
        if len(cand) == 1 and name != "<init>":
            return "a", "method renamed -> " + cand[0]
        return "b", "method removed"

    def step_forge(v0, v1, kind, owner, name, desc):
        F0, F1 = forge[v0], forge[v1]
        if kind == "class":
            if owner in F1.classes:
                return "ok", ""
            cand = [c for c in F1.simple.get(owner.rsplit("/", 1)[-1].split("$")[-1], ()) if c not in F0.classes]
            if cand:
                return "a", "class moved/renamed -> " + sorted(cand)[0]
            return "b", "class removed"
        if owner not in F1.classes:
            cand = [c for c in F1.simple.get(owner.rsplit("/", 1)[-1].split("$")[-1], ()) if c not in F0.classes]
            return "b", "owner class removed" + (f" (moved -> {sorted(cand)[0]})" if cand else "")
        method = kind == "method"
        if F1.has_member(owner, name, desc, method):
            return "ok", ""
        if method and any((owner, name, d) in F1.methods for (o, n, d) in [(owner, name, "")] ) :
            return "b", "signature changed"
        if method and any(n == name and o == owner for (o, n, d) in F1.methods):
            return "b", "signature changed"
        return "b", "member removed"

    def forge_patch_member(owner, name, desc, V0):
        """a member of a Minecraft class that Mojang's list does not have: added by Forge's binary patches (getRegistryName, harvestTool, isAir(pos)...) or by the layer.
        The patches are not in any jar of the index; when the descriptor names a Forge class that vanishes along the chain the symbol is a Forge-API break of that stage."""
        if owner not in V0.classes:
            return "n", "", "not a Minecraft member"
        cur = chain[0]
        for nxt, letter in zip(chain[1:], letters):
            if cur in forge and nxt in forge:
                for cn in re.findall(r"L(net/minecraftforge/[A-Za-z0-9_/$]+);", desc):
                    if cn in forge[cur].classes and cn not in forge[nxt].classes:
                        return "Fb", letter, "Forge-patched member of a Minecraft class; its descriptor uses a Forge class removed in " + nxt
            cur = nxt
        return "u", "", "member of a Minecraft class that is in no list: added by Forge's patches or by the layer (not diffable offline)"

    def split(kind, sym):
        return g19.split_sym(kind, sym)

    def classify(kind, sym):
        area = g19.area(sym)
        sym0 = unrename(kind, sym)
        owner, name, desc = split(kind, sym0)
        isforge = area == "Forge API" or owner.startswith(("net/minecraftforge/", "cpw/"))
        if isforge and not owner.startswith(("net/minecraft/", "com/mojang/")):
            if not forge or not owner.startswith(("net/minecraftforge/", "cpw/")):
                return "F", "", "Forge API (not diffed)"
            v0 = chain[0]
            if v0 not in forge:
                return "F", "", "Forge API (not diffed)"
            if owner not in forge[v0].classes:
                return "n", "", "not a Forge %s class (synthesised, shim or other)" % v0
            if kind != "class" and not forge[v0].has_member(owner, name, desc, kind == "method"):
                return "u", "", "Forge member not in Forge %s (synthesised or not public)" % v0
            cur = v0
            for nxt, letter in zip(chain[1:], letters):
                if nxt not in forge:
                    break
                res, why = step_forge(cur, nxt, kind, owner, name, desc)
                if res != "ok":
                    return "F" + res, letter, why
                cur = nxt
            return "FD", "D", "Forge API unchanged up to 1.20.1 (Forge -> NeoForge rename or layer gap)"
        if area not in ("Minecraft common", "Minecraft client"):
            return "n", "", area
        v0 = chain[0]
        V0 = vers[v0]
        if kind == "class":
            if owner not in V0.classes:
                return "n", "", "class not in Minecraft " + v0
        elif owner not in V0.classes:
            return "n", "", "class of a library (authlib, DataFixerUpper...) or not a Minecraft class of " + v0
        elif kind == "field":
            if (owner, name) not in V0.fields and not V0.by_name.get((name, "")):
                return forge_patch_member(owner, name, "", V0)
        else:
            if (owner, name, desc) not in V0.methods and not V0.by_name.get((name, desc)):
                return forge_patch_member(owner, name, desc, V0)
        cur = v0
        for nxt, letter in zip(chain[1:], letters):
            res, why = step_mc(cur, nxt, kind, owner, name, desc)
            if res != "ok":
                return res, letter, why
            cur = nxt
        if (kind, sym) in id_syms:
            return "a", "0", "SRG id absent from the 1.20.1 table (only the own-version table names it), member unchanged up to 1.20.1: a table row"
        return "D", "D", "unchanged in 1.20.1: the 1.20.1 -> 1.21.1 delta or a gap of the layer"

    sym_jars = collections.defaultdict(set)
    for jar, ss in miss.items():
        for k, s in ss:
            if g19.area(s) != "Other mod or library" and g19.area(s) != "JDK":
                sym_jars[(k, s)].add(jar)
    rows = []
    cache = {}
    for (k, s), jars in sym_jars.items():
        cl, stage, why = classify(k, s)
        cache[(k, s)] = (cl, stage, why)
        rows.append((len(jars), k, s, cl, stage, why, g19.area(s), next((n for n, rx in g19.SHAPES if rx.search(s)), "other")))
    rows.sort(key=lambda r: (-r[0], r[2]))
    with open(out / "symbols.tsv", "w", encoding="utf-8") as f:
        f.write("jars\tkind\tsymbol\tclass\tstage\twhy\tarea\tshape\n")
        for r in rows:
            f.write("\t".join(map(str, r)) + "\n")

    if a.md:
        with open(a.md, "w", encoding="utf-8") as f:
            f.write(f"# {a.title}" + NL + NL + a.intro + NL + NL)
            f.write("| # | jars | kind | class | stage | shape | symbol (own-version Mojang names; Forge symbols as the layer names them) | why |" + NL + "|---|---|---|---|---|---|---|---|" + NL)
            for i, r in enumerate(rows[:a.limit], 1):
                f.write(f"| {i} | {r[0]} | {r[1]} | {r[3]} | {r[4] or chr(45)} | {r[7]} | `{r[2]}` | {r[5]} |" + NL)

    areas = ["Minecraft common", "Minecraft client", "Forge API", "JDK", "Mixin/ASM", "Other mod or library"]
    coded = [j for j in refs if refs[j] > 0]
    stat = {}
    with open(out / "jars.tsv", "w", encoding="utf-8") as f:
        f.write("jar\tforge\tfabric\trefs\t" + "\t".join(areas) + "\tstatus\n")
        for jar in sorted(refs):
            c = collections.Counter(g19.area(s) for k, s in miss.get(jar, ()))
            core = c["Minecraft common"] + c["Minecraft client"] + c["Forge API"] + c["JDK"] + c["Mixin/ASM"]
            total = sum(c.values())
            st = "links" if total == 0 else ("links but for other mods/libraries" if core == 0 else "unresolved")
            if refs[jar] == 0:
                st = "no code"
            stat[jar] = st
            fg = jarkind.get(jar, (False, False))
            f.write(f"{jar}\t{int(fg[0])}\t{int(fg[1])}\t{refs[jar]}\t" + "\t".join(str(c[x]) for x in areas) + f"\t{st}\n")

    with open(out / "summary.txt", "w", encoding="utf-8") as f:
        def w(t=""):
            f.write(t + "\n"); print(t)
        w(f"chain {' -> '.join(chain)} (stages {', '.join(letters)}); Fabric-intermediary symbols dropped (multi-loader jars): {fabric_dropped}")
        w(f"jars with code: {len(coded)} of {len(refs)}; forge (mods.toml): {sum(1 for j in refs if jarkind.get(j, (0, 0))[0])}; fabric/quilt marker: {sum(1 for j in refs if jarkind.get(j, (0, 0))[1])}")
        cs = collections.Counter(stat.values())
        w("jar status: " + ", ".join(f"{k} {v}" for k, v in cs.most_common()))
        w("distinct Minecraft/Forge symbols: %d; occurrences (jar x symbol): %d" % (len(rows), sum(r[0] for r in rows)))
        byc = collections.defaultdict(lambda: [0, 0])
        for r in rows:
            byc[(r[3], r[4])][0] += 1; byc[(r[3], r[4])][1] += r[0]
        w("by class and stage (distinct/occurrences): " + ", ".join(f"{k[0]}{k[1]}: {v[0]}/{v[1]}" for k, v in sorted(byc.items())))
        w("")
        w("per stage, Minecraft symbols (a = rename/move row, b = changed or removed): distinct / occurrences / jars")
        per = collections.defaultdict(lambda: [0, 0, set()])
        for (k, s), jars in sym_jars.items():
            cl, stage, _ = cache[(k, s)]
            if cl in ("a", "b", "Fa", "Fb"):
                e = per[(stage, cl)]; e[0] += 1; e[1] += len(jars); e[2] |= jars
        for key in sorted(per):
            v = per[key]; w(f"  stage {key[0]} class {key[1]}: {v[0]} / {v[1]} / {len(v[2])}")
        w("")
        w("shape x (stage,class) (distinct symbols / occurrences / jars):")
        sh = collections.defaultdict(lambda: collections.defaultdict(lambda: [0, 0, set()]))
        for (k, s), jars in sym_jars.items():
            cl, stage, _ = cache[(k, s)]
            shape = next((n for n, rx in g19.SHAPES if rx.search(s)), "other")
            e = sh[shape][cl + stage]; e[0] += 1; e[1] += len(jars); e[2] |= jars
        for name in sorted(sh, key=lambda n: -sum(v[1] for v in sh[n].values())):
            w(f"  {name}: " + "; ".join(f"{c}={v[0]}/{v[1]}/{len(v[2])}" for c, v in sorted(sh[name].items())))
        w("")
        # per jar: which stages it is blocked by (a/b symbols, Minecraft and Forge), and whether anything else (D, F, FD, u, n) is left
        blockers = collections.defaultdict(lambda: collections.defaultdict(set))
        other = collections.defaultdict(set)
        for (k, s), jars in sym_jars.items():
            cl, stage, _ = cache[(k, s)]
            for j in jars:
                if cl in ("a", "b", "Fa", "Fb"):
                    blockers[j][stage].add((cl[-1], s))
                else:
                    other[j].add(cl)
        w(f"jars with no unresolved Minecraft/Forge symbol at all (pure renames + the 1.20.1 -> 1.21.1 tables already link them): {sum(1 for j in coded if not blockers[j] and not other[j])} of {len(coded)}")
        w("")
        w("link rate by stage (cumulative: the stages up to and including the one named are done; share of the jars with code):")
        w("  'no work' = jars that have no a/b symbol in those stages at all; 'tables' = jars whose a/b symbols there are all rename rows (class a);")
        w("  'full' = also nothing else left unresolved (class D, Forge-not-diffed, u, n): such jars would link on 1.21.1; 'MC only' ignores Forge API symbols")
        done = ["0"]
        for letter in letters:
            done.append(letter)
            def bl(j, only_mc=False):
                return [(c, s) for st_, ss in blockers[j].items() if st_ in done for (c, s) in ss if not (only_mc and s.startswith(("net/minecraftforge", "cpw/", "net/neoforged")))]
            nowork = sum(1 for j in coded if not bl(j))
            tables = sum(1 for j in coded if all(c == "a" for c, s in bl(j)))
            nowork_mc = sum(1 for j in coded if not bl(j, True))
            full_nowork = sum(1 for j in coded if not bl(j) and not other[j])
            full_tables = sum(1 for j in coded if all(c == "a" for c, s in bl(j)) and not other[j])
            full_all = sum(1 for j in coded if not other[j] and not any(st_ not in done for st_ in blockers[j]))
            w(f"  through stage {letter}: no work {nowork} ({100*nowork/max(1,len(coded)):.1f}%), tables only {tables} ({100*tables/max(1,len(coded)):.1f}%), MC-only no work {nowork_mc}; full link: no work {full_nowork}, tables only {full_tables}, all done {full_all} of {len(coded)}")
        w("")
        w("stage-local (only the symbols that break in exactly this transition; earlier stages assumed done):")
        for letter in letters:
            def have(j, cls, mc):
                return any(c == cls and (not mc or not s.startswith(("net/minecraftforge", "cpw/", "net/neoforged"))) for c, s in blockers[j].get(letter, ()))
            n_b = sum(1 for j in coded if have(j, "b", False)); n_a = sum(1 for j in coded if have(j, "a", False))
            n_any = sum(1 for j in coded if blockers[j].get(letter))
            n_b_mc = sum(1 for j in coded if have(j, "b", True)); n_a_mc = sum(1 for j in coded if have(j, "a", True))
            n_any_mc = sum(1 for j in coded if have(j, "a", True) or have(j, "b", True))
            w(f"  stage {letter}: jars with a-class symbols {n_a}, with b-class {n_b}, with either {n_any}; stage-local link rate (renames applied, nothing else) {len(coded) - n_b} of {len(coded)} ({100*(len(coded)-n_b)/max(1,len(coded)):.1f}%), with no a or b symbol at all {len(coded)-n_any} ({100*(len(coded)-n_any)/max(1,len(coded)):.1f}%); Minecraft symbols only: b {n_b_mc}, either {n_any_mc} (link rate after renames {100*(len(coded)-n_b_mc)/max(1,len(coded)):.1f}%)")
        w("")
        w("greedy order of shapes to fix (jars blocked by each shape of the stages, most first):")
        remaining = {j: {(st_, next((n for n, rx in g19.SHAPES if rx.search(s)), "other")) for st_, ss in blockers[j].items() for (c, s) in ss} for j in coded if blockers[j]}
        order = 0
        cleared = 0
        while remaining and order < 25:
            cnt = collections.Counter(t for tags in remaining.values() for t in tags)
            sole = collections.Counter(next(iter(tags)) for tags in remaining.values() if len(tags) == 1)
            t = (sole.most_common(1)[0][0] if sole else cnt.most_common(1)[0][0])
            order += 1
            for tags in remaining.values():
                tags.discard(t)
            done_j = [j for j, tags in remaining.items() if not tags]
            for j in done_j:
                del remaining[j]; cleared += 1
            w(f"  {order:2d}. stage {t[0]}: {t[1]}: cleared of chain blockers {cleared}")
        w("")
        areac = collections.Counter(g19.area(s) for ss in miss.values() for k, s in ss)
        w("unresolved symbol occurrences by area: " + ", ".join(f"{k} {v}" for k, v in areac.most_common()))


if __name__ == "__main__":
    main()
