#!/usr/bin/env python3
"""Writes src/main/resources/legacyconnector/v1_7_10/field-order.txt: the declared fields of every vanilla 1.7.10 class in
declaration order (MCP names), for FML's index-based reflection (ReflectionHelper/ObfuscationReflectionHelper with a field
index: Aether swaps EntityRenderer field #7, the item renderer). The shim classes declare their fields in another order.
One line per class: `net/minecraft/Class field0 field1 ...`.
usage: tools/gen_field_order.py   (needs build/devtools/mcp/client1710.jar, srg1710.zip, mcp_stable-12-1.7.10.zip:
       tools/fetch_devtools.sh, the client jar from Mojang's version manifest)"""
import csv, io, os, struct, zipfile

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
mcp = os.path.join(root, "build", "devtools", "mcp")
out = os.path.join(root, "src", "main", "resources", "legacyconnector", "v1_7_10", "field-order.txt")

classes, fields_srg = {}, {}
with zipfile.ZipFile(os.path.join(mcp, "srg1710.zip")) as z:
    for line in z.read("joined.srg").decode().splitlines():
        p = line.split()
        if p[0] == "CL:":
            classes[p[1]] = p[2]
        elif p[0] == "FD:":
            obf_owner, obf_name = p[1].rsplit("/", 1)
            fields_srg[(obf_owner, obf_name)] = p[2].rsplit("/", 1)[1]
with zipfile.ZipFile(os.path.join(mcp, "mcp_stable-12-1.7.10.zip")) as z:
    names = {r["searge"]: r["name"] for r in csv.DictReader(io.StringIO(z.read("fields.csv").decode()))}


def field_names(data):
    """Names of the fields of a class file, in declaration order."""
    pos, pool = 10, [None]
    count = struct.unpack(">H", data[8:10])[0]
    i = 1
    while i < count:
        tag = data[pos]
        if tag == 1:
            n = struct.unpack(">H", data[pos + 1:pos + 3])[0]
            pool.append(data[pos + 3:pos + 3 + n].decode("utf-8", "replace"))
            pos += 3 + n
        else:
            pool.append(None)
            pos += {3: 5, 4: 5, 5: 9, 6: 9, 7: 3, 8: 3, 9: 5, 10: 5, 11: 5, 12: 5, 15: 4, 16: 3, 18: 5}[tag]
            if tag in (5, 6):
                pool.append(None)
                i += 1
        i += 1
    pos += 6
    pos += 2 + 2 * struct.unpack(">H", data[pos:pos + 2])[0]   # interfaces
    out = []
    pos += 2
    for _ in range(struct.unpack(">H", data[pos - 2:pos])[0]):
        name_index = struct.unpack(">H", data[pos + 2:pos + 4])[0]
        out.append(pool[name_index])
        pos += 8
        for _ in range(struct.unpack(">H", data[pos - 2:pos])[0]):
            pos += 6 + struct.unpack(">I", data[pos + 2:pos + 6])[0]
    return out


lines = []
with zipfile.ZipFile(os.path.join(mcp, "client1710.jar")) as z:
    for entry in z.namelist():
        if not entry.endswith(".class"):
            continue
        obf = entry[:-6]
        deobf = classes.get(obf, obf)
        if not deobf.startswith("net/minecraft/"):
            continue
        order = [names.get(fields_srg.get((obf, f), f), fields_srg.get((obf, f), f)) for f in field_names(z.read(entry))]
        if order:
            lines.append(deobf + " " + " ".join(order))
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", newline="\n") as f:
    f.write("\n".join(sorted(lines)) + "\n")
print(f"{len(lines)} classes -> {out}")
