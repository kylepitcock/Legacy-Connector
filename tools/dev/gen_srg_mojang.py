#!/usr/bin/env python3
"""Builds the SRG -> Mojang member name table of one Minecraft version (1.20.1 Forge mods are SRG-named at run time, NeoForge 1.21.1 is Mojang-named).

usage: gen_srg_mojang.py <mcp_config joined.tsrg> <mojang client.txt> <out.tsv>

Chains the two files on the obfuscated class, member name and descriptor. Output lines: `m_46859_<TAB>getBlockState` and `f_46441_<TAB>...`
(SRG ids are unique, so the owner is not needed, as in McpMappings). The tsrg2 file is MCPConfig's `config/joined.tsrg` (obf srg id); the Mojang
file is the version's `client.txt` from the launcher manifest.

The output is derived from Mojang's mappings (which may not be redistributed complete and unmodified), so it is generated at build time into
build/ and never committed.
"""
import re
import sys

PRIMS = {"int": "I", "long": "J", "double": "D", "float": "F", "boolean": "Z", "byte": "B", "char": "C", "short": "S", "void": "V"}


def read_mojang(path):
    """class mojang->obf, then members keyed (obfClass, obfName, obfDesc) -> mojangName (fields: desc None)."""
    classes = {}
    raw = []  # (mojangClass, kind, mojang name, ret, args, obf name)
    cur = None
    cls_re = re.compile(r"^(\S+) -> (\S+):$")
    m_re = re.compile(r"^\s+(?:\d+:\d+:)?(\S+) (\S+)\((.*)\)(?::\d+:\d+)? -> (\S+)$")
    f_re = re.compile(r"^\s+(\S+) (\S+) -> (\S+)$")
    for line in open(path, encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        m = cls_re.match(line.rstrip("\n"))
        if m:
            cur = m.group(1)
            classes[cur] = m.group(2).replace(".", "/")
            continue
        m = m_re.match(line.rstrip("\n"))
        if m:
            raw.append((cur, "m", m.group(2), m.group(1), m.group(3), m.group(4)))
            continue
        m = f_re.match(line.rstrip("\n"))
        if m:
            raw.append((cur, "f", m.group(2), m.group(1), None, m.group(3)))

    def desc(t):
        dims = 0
        while t.endswith("[]"):
            dims += 1
            t = t[:-2]
        base = PRIMS.get(t) or "L" + classes.get(t, t.replace(".", "/")) + ";"
        return "[" * dims + base

    members = {}
    for cls, kind, name, ret, args, obf in raw:
        oc = classes[cls]
        if kind == "m":
            d = "(" + "".join(desc(a) for a in args.split(",") if a) + ")" + desc(ret)
            members[(oc, obf, d)] = name
        else:
            members[(oc, obf, None)] = name
    return members


def main():
    tsrg, mojang, out = sys.argv[1:4]
    members = read_mojang(mojang)
    table = {}
    cur = None
    missing = 0
    for line in open(tsrg, encoding="utf-8"):
        if line.startswith("tsrg2"):
            continue
        if not line.startswith("\t"):
            cur = line.split()[0]
            continue
        if line.startswith("\t\t"):
            continue  # static marker / parameters
        parts = line.strip().split()
        if len(parts) == 3:  # field: obf srg id
            srg, name = parts[1], members.get((cur, parts[0], None))
        elif len(parts) == 4:  # method: obf desc srg id
            srg, name = parts[2], members.get((cur, parts[0], parts[1]))
        else:
            continue
        if not re.match(r"^[mf]_\d+_$", srg):
            continue  # <init>, <clinit>, already-named members
        if name is None:
            missing += 1
            continue
        table[srg] = name
    with open(out, "w", encoding="utf-8") as f:
        for k in sorted(table, key=lambda s: (s[0], int(s[2:-1]))):
            f.write(f"{k}\t{table[k]}\n")
    print(f"{len(table)} members mapped, {missing} without a Mojang name")


main()
