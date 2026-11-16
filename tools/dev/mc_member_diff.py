#!/usr/bin/env python3
"""EXPERIMENTAL (1.19.2 scoping, docs/wiki/Legacy-1.19.2-Estimate.md): mechanical diff of two Minecraft versions' Mojang-named class and member lists,
joined by the stable SRG ids (class ids C_nnn, method ids m_nnn_, field ids f_nnn_ are kept by MCPConfig across versions).

usage: mc_member_diff.py <joined-a.tsrg> <client-a.txt> <srg-mojang-a.tsv> <joined-b.tsrg> <client-b.txt> <srg-mojang-b.tsv> <out-dir>

Inputs are the build-local files of :loader:genSrgMojang1192 / genSrgMojang1201 (loader/build/tmp/mcpconfig*/joined.tsrg,
loader/build/mojang-mappings/*-client.txt, loader/build/generated/srg-mojang*/.../srg-mojang.tsv). Nothing is committed.
Writes <out-dir>/class-diff.tsv, member-diff.tsv (id, kind, a-owner.name+desc, b-owner.name+desc, status: same|renamed|moved|desc|removed|added)
and prints counts. Class names are slash-separated Mojang names ('net/minecraft/world/level/block/Block').
"""
import re, sys, pathlib, collections


def parse_client(path):
    """named class -> obf class"""
    out = {}
    pat = re.compile(r"^(\S+) -> (\S+):$")
    for line in open(path, encoding="utf-8"):
        m = pat.match(line.rstrip("\n"))
        if m:
            out[m.group(1).replace(".", "/")] = m.group(2).replace(".", "/")
    return out


def parse_joined(path):
    """obf class -> srg class (C_nnn); members: srg id -> (owner C class, srg member name, desc over C classes or None for fields)"""
    cls = {}
    members = {}
    ctors = []      # (owner C class, desc): constructors have no m_ id
    cur = None
    for line in open(path, encoding="utf-8"):
        line = line.rstrip("\n")
        if not line or line.startswith("tsrg2"):
            continue
        if not line.startswith("\t"):
            p = line.split(" ")
            cls[p[0]] = p[1]
            cur = p[1]
        elif not line.startswith("\t\t"):
            p = line.strip().split(" ")
            if len(p) == 3:      # obf srgname id (field)
                members[p[1]] = (cur, p[1], None)
            elif len(p) >= 4:    # obf desc srgname id (method)
                if p[0] == "<init>":
                    ctors.append((cur, p[1]))
                else:
                    members[p[2]] = (cur, p[2], p[1])
    return cls, members, ctors


def desc_names(desc, obf2srg, srg2named):
    def sub(m):
        o = m.group(1)
        s = obf2srg.get(o, o)
        return "L" + srg2named.get(s, s) + ";"
    return re.sub(r"L([^;]+);", sub, desc)


def load(joined, client, table):
    named2obf = parse_client(client)
    obf2srg, members, ctors = parse_joined(joined)
    srg2named = {}
    for n, o in named2obf.items():
        if o in obf2srg:
            srg2named[obf2srg[o]] = n
    names = {}
    for line in open(table, encoding="utf-8"):
        t = line.rstrip("\n").split("\t")
        if len(t) == 2:
            names[t[0]] = t[1]
    # obf2srg maps obf descriptor classes (obf names) to C_nnn; the table's descriptors use obf names of the same version
    mem = {}
    for sid, (owner, srgname, desc) in members.items():
        if not re.match(r"^[mf]_\d+_$", sid):
            continue   # (<clinit> and the like)
        mem[sid] = (srg2named.get(owner, owner), names.get(sid, sid), desc_names(desc, obf2srg, srg2named) if desc else None)
    for owner, desc in ctors:
        mem["<init>#%d" % len(mem)] = (srg2named.get(owner, owner), "<init>", desc_names(desc, obf2srg, srg2named))
    return {srg2named[s]: s for s in srg2named}, srg2named, mem


PRIM = {"int": "I", "long": "J", "boolean": "Z", "byte": "B", "char": "C", "short": "S", "float": "F", "double": "D", "void": "V"}


def jtype(t):
    dims = 0
    while t.endswith("[]"):
        dims += 1
        t = t[:-2]
    return "[" * dims + (PRIM[t] if t in PRIM else "L" + t.replace(".", "/") + ";")


def write_members(client, path):
    """every class, method (with descriptor, constructors as <init>) and field of the Mojang file in Mojang names, the complete set (the joined.tsrg lists only members with SRG ids)"""
    cur = None
    cpat = re.compile(r"^(\S+) -> (\S+):$")
    mpat = re.compile(r"^\s+(?:\d+:\d+:)?(\S+) (\S+)\((.*?)\)(?::\d+:\d+)? -> (\S+)$")
    fpat = re.compile(r"^\s+(\S+) (\S+) -> (\S+)$")
    with open(path, "w", encoding="utf-8") as out:
        for line in open(client, encoding="utf-8"):
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            m = cpat.match(line)
            if m:
                cur = m.group(1).replace(".", "/")
                out.write(cur + "\t\t\tclass\n")
                continue
            m = mpat.match(line)
            if m and cur:
                args = "".join(jtype(x) for x in m.group(3).split(",") if x)
                out.write(cur + "\t" + m.group(2) + "\t(" + args + ")" + jtype(m.group(1)) + "\tm\n")
                continue
            m = fpat.match(line)
            if m and cur:
                out.write(cur + "\t" + m.group(2) + "\t\tf\n")


def main():
    a_joined, a_client, a_table, b_joined, b_client, b_table, out = sys.argv[1:8]
    out = pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    a_n2s, a_s2n, a_mem = load(a_joined, a_client, a_table)
    b_n2s, b_s2n, b_mem = load(b_joined, b_client, b_table)
    for tag, client in (("a", a_client), ("b", b_client)):   # complete lists for the analysis, straight from the Mojang file (owner TAB name TAB desc-or-empty TAB marker)
        write_members(client, out / f"members-{tag}.tsv")
    a_mem = {k: v for k, v in a_mem.items() if k[0] in "mf"}
    b_mem = {k: v for k, v in b_mem.items() if k[0] in "mf"}
    c = collections.Counter()
    with open(out / "class-diff.tsv", "w", encoding="utf-8") as f:
        for s, n in sorted(a_s2n.items()):
            if s in b_s2n:
                status = "same" if b_s2n[s] == n else ("moved" if b_s2n[s].rsplit("/", 1)[0] != n.rsplit("/", 1)[0] else "renamed")
                f.write(f"{s}\t{n}\t{b_s2n[s]}\t{status}\n")
            else:
                status = "removed"
                f.write(f"{s}\t{n}\t\t{status}\n")
            c["class " + status] += 1
        for s, n in sorted(b_s2n.items()):
            if s not in a_s2n:
                f.write(f"{s}\t\t{n}\tadded\n")
                c["class added"] += 1
    with open(out / "member-diff.tsv", "w", encoding="utf-8") as f:
        for sid in sorted(a_mem, key=lambda x: (x[0], int(x[2:-1]))):
            ao, an, ad = a_mem[sid]
            if sid in b_mem:
                bo, bn, bd = b_mem[sid]
                if (ao, an, ad) == (bo, bn, bd):
                    status = "same"
                elif an != bn:
                    status = "renamed"
                elif ao != bo:
                    status = "moved"
                else:
                    status = "desc"
                f.write(f"{sid}\t{sid[0]}\t{ao}.{an}{ad or ''}\t{bo}.{bn}{bd or ''}\t{status}\n")
            else:
                status = "removed"
                f.write(f"{sid}\t{sid[0]}\t{ao}.{an}{ad or ''}\t\t{status}\n")
            c[("method " if sid[0] == "m" else "field ") + status] += 1
        for sid in b_mem:
            if sid not in a_mem:
                c[("method " if sid[0] == "m" else "field ") + "added"] += 1
    for k in sorted(c):
        print(k, c[k])


if __name__ == "__main__":
    main()
