"""Generates the 1.12.2 PacketBuffer: the 1.12 helpers (tools/packet_buffer_1122.java.head), then every abstract ByteBuf method
delegating to the wrapped buffer.

Usage: javap -cp netty-buffer-4.1.x.jar io.netty.buffer.ByteBuf > bytebuf.txt
       python tools/gen_packet_buffer.py bytebuf.txt src/main/java/legacyconnector/v1_12_2/net/minecraft/network/PacketBuffer.java
"""
import re
import sys

dump, out = sys.argv[1], sys.argv[2]
methods = []
for line in open(dump, encoding="utf-8"):
    line = line.strip()
    if not line.startswith("public abstract") or "class " in line:
        continue
    m = re.match(r"public abstract (?P<ret>[\w.<>\[\], ?]+?) (?P<name>\w+)\((?P<params>.*)\)( throws (?P<throws>[\w., ]+))?;", line)
    if not m:
        print("skip", line)
        continue
    params = [p.strip() for p in m.group("params").split(",")] if m.group("params").strip() else []
    methods.append((m.group("ret"), m.group("name"), params, m.group("throws")))

lines = []
for ret, name, params, throws in methods:
    args = ", ".join(f"{t} a{i}" for i, t in enumerate(params))
    call = ", ".join(f"a{i}" for i in range(len(params)))
    th = f" throws {throws}" if throws else ""
    body = f"buf.{name}({call})"
    if ret == "void":
        stmt = f"{body};"
    elif ret == "io.netty.buffer.ByteBuf" and name not in ("unwrap",) and not name.startswith(("read", "slice", "copy", "duplicate", "retainedSlice", "retainedDuplicate", "order", "asReadOnly")):
        # fluent setters/writers return this buffer, as the wrapped one would return itself
        stmt = f"{body};\n        return this;"
    else:
        stmt = f"return {body};"
    lines.append(f"    @Override\n    public {ret} {name}({args}){th} {{\n        {stmt}\n    }}\n")

import pathlib
head = pathlib.Path(__file__).with_name("packet_buffer_1122.java.head")
src = open(head, encoding="utf-8").read().replace("    // @@DELEGATES@@\n", "\n".join(lines))
open(out, "w", encoding="utf-8", newline="\n").write(src)
print(len(methods), "delegates")
