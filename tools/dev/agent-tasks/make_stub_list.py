#!/usr/bin/env python3
"""Turns raw probe records (tools/compat/probe.py --json raw.json) into a work list of legacy APIs that only link to a placeholder.

usage: make_stub_list.py <mc-version> <raw.json> [<raw.json> ...] > <list>.md

"stubbed" entries are APIs the layer synthesized (they link but do nothing yet); "missing" entries could not be linked at all.
Both are grouped by class, most used first, with the jars that use them, in the format of the other lists in this folder.
"""
import collections, datetime, json, sys


def main():
    mc, files = sys.argv[1], sys.argv[2:]
    use = {"missing": collections.defaultdict(set), "stubbed": collections.defaultdict(set)}
    jars, results = set(), collections.Counter()
    for f in files:
        for r in json.load(open(f, encoding="utf-8")):
            if r.get("mc") != mc or r["jar"] in jars:
                continue
            jars.add(r["jar"])
            results[r.get("result")] += 1
            for kind in use:
                for m in r.get(kind) or []:
                    use[kind][m].add(r["jar"])
    print(f"# {mc}: legacy APIs without a real implementation")
    print()
    print(f"Generated {datetime.date.today().isoformat()} by `tools/dev/agent-tasks/make_stub_list.py` from the offline probe of {len(jars)} archive(s) "
          f"({', '.join(f'{v} {k}' for k, v in results.most_common())}). Regenerate after implementing entries; do not edit by hand.")
    print()
    print("Each line: `member` then `# n archive(s): names`. Names are the layer's own (`legacyconnector.v...` prefix is the shim package).")
    print("Rules for implementing them: `tools/dev/agent-tasks/README.md`, and for Beta the contributor rules in `CLAUDE.md`.")
    for kind, title in (("missing", "Could not be linked (the mod fails when it reaches them)"), ("stubbed", "Synthesized placeholders (link, but do nothing yet)")):
        entries = use[kind]
        print()
        print(f"## {title}: {len(entries)}")
        by_class = collections.defaultdict(list)
        for m, js in entries.items():
            head = m.split("(")[0].split(":")[0]
            by_class[head.rsplit(".", 1)[0] if "." in head else head].append((m, js))
        for cls, items in sorted(by_class.items(), key=lambda kv: -len(set().union(*[js for _, js in kv[1]]))):
            users = set().union(*[js for _, js in items])
            print()
            print(f"### {cls} ({len(items)} member(s), {len(users)} archive(s))")
            print()
            print("```")
            for m, js in sorted(items, key=lambda x: (-len(x[1]), x[0])):
                names = sorted(js)
                print(f"{m}  # {len(js)} archive(s): {', '.join(names[:4])}{' ...' if len(names) > 4 else ''}")
            print("```")


if __name__ == "__main__":
    main()
