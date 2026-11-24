#!/usr/bin/env python3
"""EXPERIMENTAL (1.18.2 / 1.16.5 scoping): mechanical diff of two Minecraft versions by Mojang NAME (class, method with descriptor, field), for pairs whose SRG ids are
not comparable (1.16.5 ids were renumbered in 1.17) or whose ids change with the descriptor (1.18.2 -> 1.19.2: `RandomSource`).

usage: mc_name_diff.py <client-a.txt> <client-b.txt> [<out-dir>]

Inputs are Mojang's official client mappings (build-local files of :loader:genSrgMojang11xx, never committed). Prints counts; with <out-dir> also writes
class-diff.tsv (status: same|removed|added|moved), method-diff.tsv (owner, name, desc, status: same|sig|removed|added|moved) and field-diff.tsv.
`sig` = the owner still has a method of that name with another descriptor; `moved` = removed here, same simple name (class) or same name and descriptor (member) added in another class.
"""
import collections, importlib.util, pathlib, sys, tempfile

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("mc_member_diff", HERE / "mc_member_diff.py")
mmd = importlib.util.module_from_spec(spec); spec.loader.exec_module(mmd)


def load(client, tmp):
    p = pathlib.Path(tmp) / (pathlib.Path(client).stem + ".members")
    mmd.write_members(client, p)
    classes, methods, fields = set(), set(), set()
    for line in open(p, encoding="utf-8"):
        o, n, d, k = line.rstrip("\n").split("\t")
        if k == "class":
            classes.add(o)
        elif k == "m":
            methods.add((o, n, d))
        else:
            fields.add((o, n))
    return classes, methods, fields


def main():
    a, b = sys.argv[1], sys.argv[2]
    out = pathlib.Path(sys.argv[3]) if len(sys.argv) > 3 else None
    with tempfile.TemporaryDirectory() as tmp:
        ca, ma, fa = load(a, tmp)
        cb, mb, fb = load(b, tmp)
    simple_b = collections.defaultdict(set)
    for c in cb - ca:
        simple_b[c.rsplit("/", 1)[-1].split("$")[-1]].add(c)
    added_by_sig = collections.defaultdict(set)
    added_by_name = collections.defaultdict(set)
    for (o, n, d) in mb - ma:
        added_by_sig[(n, d)].add(o)
    names_b = collections.defaultdict(set)
    for (o, n, d) in mb:
        names_b[(o, n)].add(d)
    counts = collections.Counter()
    rows_c, rows_m, rows_f = [], [], []
    for c in sorted(ca):
        if c in cb:
            st = "same"
        elif simple_b.get(c.rsplit("/", 1)[-1].split("$")[-1]):
            st = "moved"
        else:
            st = "removed"
        counts["class " + st] += 1
        rows_c.append((c, st))
    counts["class added"] = len(cb - ca)
    for (o, n, d) in sorted(ma):
        if (o, n, d) in mb:
            st = "same"
        elif (o, n) in names_b:
            st = "sig"
        elif added_by_sig.get((n, d)) and o not in cb:
            st = "moved"
        else:
            st = "removed"
        counts["method " + st] += 1
        rows_m.append((o, n, d, st))
    counts["method added"] = len(mb - ma)
    for (o, n) in sorted(fa):
        st = "same" if (o, n) in fb else "removed"
        counts["field " + st] += 1
        rows_f.append((o, n, st))
    counts["field added"] = len(fb - fa)
    for k in sorted(counts):
        print(k, counts[k])
    if out:
        out.mkdir(parents=True, exist_ok=True)
        with open(out / "class-diff.tsv", "w", encoding="utf-8") as f:
            for r in rows_c:
                f.write("\t".join(r) + "\n")
        with open(out / "method-diff.tsv", "w", encoding="utf-8") as f:
            for r in rows_m:
                f.write("\t".join(r) + "\n")
        with open(out / "field-diff.tsv", "w", encoding="utf-8") as f:
            for r in rows_f:
                f.write("\t".join(r) + "\n")


if __name__ == "__main__":
    main()
