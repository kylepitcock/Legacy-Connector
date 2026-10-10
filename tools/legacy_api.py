#!/usr/bin/env python3
"""Developer tool: list a legacy Minecraft class's members with the names the shim must use.

usage: tools/legacy_api.py <1.7.10|1.12.2> <internal/class/Name> [regex filter]

Reads reference data from build/devtools/mcp (see tools/fetch_devtools.sh). For 1.7.10 the Forge-shipped
names win over MCP stable_12, exactly like the loader's runtime mapping table.
"""
import csv, io, os, re, sys, zipfile

D = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "build", "devtools", "mcp")
ver, cls = sys.argv[1], sys.argv[2]
flt = re.compile(sys.argv[3]) if len(sys.argv) > 3 else None
names = {}


def load(zp, member):
    with zipfile.ZipFile(os.path.join(D, zp)) as z:
        for row in csv.reader(io.TextIOWrapper(z.open(member), "utf-8")):
            if row and row[0].startswith(("func_", "field_")):
                names[row[0]] = row[1]


def emit(s):
    if not flt or flt.search(s):
        print(s)


if ver == "1.7.10":
    load("mcp_stable-12-1.7.10.zip", "methods.csv"); load("mcp_stable-12-1.7.10.zip", "fields.csv")
    load("forge1710-userdev.jar", "conf/methods.csv"); load("forge1710-userdev.jar", "conf/fields.csv")
    for line in zipfile.ZipFile(os.path.join(D, "srg1710.zip")).read("joined.srg").decode().splitlines():
        p = line.split()
        if p[0] == "FD:" and p[2].rsplit("/", 1)[0] == cls:
            n = p[2].rsplit("/", 1)[1]
            emit("F " + names.get(n, n) + "  (" + n + ")")
        elif p[0] == "MD:" and p[3].rsplit("/", 1)[0] == cls:
            n = p[3].rsplit("/", 1)[1]
            emit("M " + names.get(n, n) + " " + p[4] + "  (" + n + ")")
else:
    load("mcp_stable-39-1.12.zip", "methods.csv"); load("mcp_stable-39-1.12.zip", "fields.csv")
    lines = zipfile.ZipFile(os.path.join(D, "srg1122.zip")).read("config/joined.tsrg").decode().splitlines()
    cmap, members, cur = {}, {}, None
    for line in lines:
        if not line.startswith("\t"):
            a = line.split(); cmap[a[0]] = a[1]; cur = a[1]; members[cur] = []
        else:
            members[cur].append(line.strip().split())
    remap = lambda d: re.sub(r"L([^;]+);", lambda m: "L" + cmap.get(m.group(1), m.group(1)) + ";", d)
    for m in members.get(cls, []):
        if len(m) == 2:
            emit("F " + names.get(m[1], m[1]) + "  (" + m[1] + ")")
        else:
            emit("M " + names.get(m[2], m[2]) + " " + remap(m[1]) + "  (" + m[2] + ")")
