#!/usr/bin/env python3
"""Derives rows of mc-fields.tsv / mc-members.tsv candidates from a gap1201 report: for every missing Minecraft static field
(owner.name:desc) the field of the same name in the 1.21.1 jar is looked up; a Holder-typed one becomes a `value` row (the loader reads the
holder and unwraps it), a renamed one is listed for manual curation.

usage: gen_mc_fields_1201.py <gap-report.txt> <neoforge-merged.jar>   (prints rows to stdout; review before pasting into mc-fields.tsv)
"""
import re, struct, sys, zipfile


def read_fields(data):
    pos = 8
    n = struct.unpack_from(">H", data, pos)[0]
    pos += 2
    utf = {}
    i = 1
    while i < n:
        tag = data[pos]
        if tag == 1:
            ln = struct.unpack_from(">H", data, pos + 1)[0]
            utf[i] = data[pos + 3:pos + 3 + ln].decode("utf-8", "replace")
            pos += 3 + ln
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            pos += 3
        elif tag == 15:
            pos += 4
        i += 1
    pos += 6
    ni = struct.unpack_from(">H", data, pos)[0]
    pos += 2 + 2 * ni
    nf = struct.unpack_from(">H", data, pos)[0]
    pos += 2
    fields = {}
    for _ in range(nf):
        acc, name, desc, na = struct.unpack_from(">HHHH", data, pos)
        pos += 8
        fields[utf[name]] = (utf[desc], acc)
        for _ in range(na):
            ln = struct.unpack_from(">I", data, pos + 2)[0]
            pos += 6 + ln
    return fields


def main():
    report, jar = sys.argv[1], sys.argv[2]
    z = zipfile.ZipFile(jar)
    cache = {}
    in_fields = False
    for line in open(report, encoding="utf-8"):
        if line.startswith("== Missing fields"):
            in_fields = True
            continue
        if line.startswith("== ") and in_fields:
            break
        m = re.match(r"\s*(\d+)\s+((?:net/minecraft|com/mojang)/\S+)\.(\w+):(\S+)$", line) if in_fields else None
        if not m:
            continue
        count, owner, name, desc = int(m.group(1)), m.group(2), m.group(3), m.group(4)
        if owner not in cache:
            try:
                cache[owner] = read_fields(z.read(owner + ".class"))
            except KeyError:
                cache[owner] = None
        f = cache[owner]
        if f is None:
            print(f"# {count:3d} class gone: {owner}.{name}:{desc}")
        elif name in f:
            nd = f[name][0]
            if nd in ("Lnet/minecraft/core/Holder;", "Lnet/minecraft/core/Holder$Reference;"):
                print(f"{owner}\t{name}\t{desc}\t-\t{nd}\tvalue")
            else:
                print(f"# {count:3d} type changed {owner}.{name}: {desc} -> {nd}")
        else:
            print(f"# {count:3d} gone: {owner}.{name}:{desc}")


main()
