#!/usr/bin/env python3
"""Numeric ids of the Beta 1.7.3 vanilla blocks and items, by their (biny) static field names:

  src/main/resources/legacyconnector/b1.7.3/vanilla-ids.tsv   rows: block|item <FIELD> <id> <type>

Read from the static initialisers of Block (uu) and Item (gm) in the client jar with javap: each `new X; dup; <id> ...; invokespecial;
putstatic FIELD` takes the first int pushed after the `new` as the id (items add 256, as Item's constructor did). The runtime gives
the legacy views of vanilla content these ids (Block.STONE.id == 1, Item.IRON_INGOT.id == 265) and maps them to modern blocks/items.

usage: tools/fetch_devtools.sh; ./gradlew :loader:genBetaMappings; tools/extract_beta_vanilla_ids.py
"""
import os, re, shutil, subprocess, sys, tempfile, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
JAR = os.path.join(ROOT, "build", "devtools", "b1.7.3", "client.jar")
TABLE = os.path.join(ROOT, "loader", "build", "generated", "beta-mappings", "legacyconnector", "mappings", "b1.7.3", "official-named.tsv")
OUT = os.path.join(ROOT, "src", "main", "resources", "legacyconnector", "b1.7.3", "vanilla-ids.tsv")

classes, fields = {}, {}
for line in open(TABLE, encoding="utf-8"):
    p = line.rstrip("\n").split("\t")
    if p[0] == "C" and p[1] == "c": classes[p[2]] = p[3]
    elif p[0] == "F" and p[1] == "c": fields[(p[2], p[3])] = p[5]
official = {v: k for k, v in classes.items()}
javap = shutil.which("javap") or os.path.join(os.environ.get("JAVA_HOME", ""), "bin", "javap")

# constructors that pass their id to super() themselves (BlockCloth())
NO_ID_ARGUMENT = {"WOOL": 35}

rows = []
with tempfile.TemporaryDirectory() as tmp:
    with zipfile.ZipFile(JAR) as z:
        for kind, named in (("block", "net/minecraft/block/Block"), ("item", "net/minecraft/item/Item")):
            obf = official[named]
            path = os.path.join(tmp, obf + ".class")
            open(path, "wb").write(z.read(obf + ".class"))
            text = subprocess.run([javap, "-c", "-p", "-constants", path], capture_output=True, text=True, check=True).stdout
            clinit = text[text.index("static {};"):]
            pending = None  # (class, id)
            for l in clinit.splitlines():
                m = re.search(r"\bnew\s+#\d+\s+// class (\S+)", l)
                if m:
                    pending = [m.group(1), None]
                    continue
                if pending is not None and pending[1] is None:
                    m = re.search(r"\b(?:bipush|sipush)\s+(-?\d+)", l) or re.search(r"\biconst_(\d)\b", l)
                    if m: pending[1] = int(m.group(1))
                m = re.search(r"putstatic\s+#\d+\s+// Field (\S+):L(\S+);", l)
                if m and pending is not None and pending[1] is None and fields.get((obf, m.group(1))) in NO_ID_ARGUMENT:
                    pending[1] = NO_ID_ARGUMENT[fields[(obf, m.group(1))]]
                if m and pending is not None and pending[1] is not None:
                    owner_field = m.group(1)
                    if "." in owner_field: continue
                    name = fields.get((obf, owner_field))
                    if name and m.group(2) == obf or name and classes.get(m.group(2), "").startswith(("net/minecraft/block", "net/minecraft/item")):
                        legacy_id = pending[1] + (256 if kind == "item" else 0)
                        rows.append("%s\t%s\t%d\t%s" % (kind, name, legacy_id, classes.get(pending[0], pending[0])))
                    pending = None
os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w", encoding="utf-8", newline="\n").write("# b1.7.3 vanilla block/item ids by field (tools/extract_beta_vanilla_ids.py)\n" + "\n".join(rows) + "\n")
print("wrote", OUT, sum(1 for r in rows if r.startswith("block")), "blocks,", sum(1 for r in rows if r.startswith("item")), "items")
