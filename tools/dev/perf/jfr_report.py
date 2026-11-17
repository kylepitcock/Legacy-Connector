#!/usr/bin/env python3
"""Aggregate a Java Flight Recorder file: where do CPU samples and allocated bytes go, per thread group.

usage: python tools/dev/perf/jfr_report.py <file.jfr> [--top N] [--jfr <path to the jfr tool>] [--from-sec S] [--to-sec S]

For each thread group (server, render, meshing, worldgen, loading, other) prints
  - top self frames (the method on top of the stack) by CPU samples (jdk.ExecutionSample, plus jdk.NativeMethodSample ignored)
  - top "first layer frame" (the first frame from the top whose class is in legacyconnector. or org.kylepitcock.legacyconnector.,
    i.e. who in the layer is on the stack) by CPU samples, and the share of all samples of that group the layer is on the stack
  - the same by allocated bytes (jdk.ObjectAllocationSample weight) plus top allocated classes
  - GC totals (jdk.GarbageCollection sum of pauses, jdk.GCHeapSummary peak), monitor-enter contention (jdk.JavaMonitorEnter) by
    monitor class and first layer frame, thread park (jdk.ThreadPark) by first layer frame.
See docs/wiki/Performance.md ("Measuring").
"""
import argparse
import collections
import json
import os
import subprocess
import sys

LAYER = ("legacyconnector.", "org.kylepitcock.legacyconnector.")

GROUPS = [
    ("server", ("Server thread",)),
    ("render", ("Render thread",)),
    ("meshing", ("Chunk Render Task", "Worker-Main", "Sodium", "chunk-build", "Chunk Mesh", "Meshing")),
    ("worldgen", ("Worker-", "ForkJoinPool", "C2ME", "worldgen", "Chunk")),
    ("loading", ("main", "modloading", "ModLoading", "Mod loading", "bootstrap")),
]


def group_of(name):
    if not name:
        return "other"
    for g, keys in GROUPS:
        for k in keys:
            if name == k or name.startswith(k):
                return g
    return "other"


def find_jfr(arg):
    if arg:
        return arg
    jh = os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-21")
    return os.path.join(jh, "bin", "jfr")


def events(jfr, path, kinds):
    out = subprocess.run([jfr, "print", "--json", "--stack-depth", "64", "--events", ",".join(kinds), path],
                         capture_output=True, text=True, encoding="utf-8", check=True).stdout
    return json.loads(out)["recording"]["events"]


def frame_name(f):
    m = f["method"]
    return m["type"]["name"] + "." + m["name"]


def is_layer(name):
    return name.startswith(LAYER)


def analyse(evs, weight_of, thread_of, top, title, unit):
    per = collections.defaultdict(lambda: {"total": 0, "self": collections.Counter(), "layer": collections.Counter(), "onstack": 0})
    for e in evs:
        v = e["values"]
        st = v.get("stackTrace")
        frames = st["frames"] if st else []
        w = weight_of(v)
        g = group_of(thread_of(v))
        for key in (g, "ALL"):
            d = per[key]
            d["total"] += w
            if frames:
                d["self"][frame_name(frames[0])] += w
            first = None
            for f in frames:
                n = frame_name(f)
                if is_layer(n):
                    first = n
                    break
            if first:
                d["layer"][first] += w
                d["onstack"] += w
            else:
                d["layer"]["(no layer frame)"] += w
    for g in sorted(per, key=lambda k: -per[k]["total"]):
        d = per[g]
        if d["total"] == 0:
            continue
        print(f"\n## {title} / {g}: {d['total']:,} {unit}; layer frame on {100.0 * d['onstack'] / d['total']:.1f}% of it")
        print("  top self frames:")
        for n, c in d["self"].most_common(top):
            print(f"    {100.0 * c / d['total']:5.1f}%  {c:>14,}  {n}")
        print("  top first layer frames:")
        for n, c in d["layer"].most_common(top):
            print(f"    {100.0 * c / d['total']:5.1f}%  {c:>14,}  {n}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--top", type=int, default=12)
    ap.add_argument("--jfr")
    a = ap.parse_args()
    jfr = find_jfr(a.jfr)
    evs = events(jfr, a.file, ["jdk.ExecutionSample", "jdk.ObjectAllocationSample", "jdk.GarbageCollection", "jdk.GCHeapSummary",
                               "jdk.JavaMonitorEnter", "jdk.ThreadPark"])
    by = collections.defaultdict(list)
    for e in evs:
        by[e["type"]].append(e)
    print(f"events: " + ", ".join(f"{k}={len(v)}" for k, v in sorted(by.items())))

    analyse(by["jdk.ExecutionSample"], lambda v: 1, lambda v: (v.get("sampledThread") or {}).get("javaName"), a.top, "CPU samples", "samples")
    analyse(by["jdk.ObjectAllocationSample"], lambda v: v.get("weight", 0), lambda v: (v.get("eventThread") or {}).get("javaName"), a.top,
            "Allocation bytes", "bytes")

    cls = collections.Counter()
    for e in by["jdk.ObjectAllocationSample"]:
        v = e["values"]
        cls[(v.get("objectClass") or {}).get("name", "?")] += v.get("weight", 0)
    if cls:
        print("\n## Top allocated classes (bytes, sampled weight)")
        for n, c in cls.most_common(a.top):
            print(f"  {c:>16,}  {n}")

    gc = by["jdk.GarbageCollection"]
    if gc:
        def ms(x):
            if isinstance(x, (int, float)):
                return x
            return 0
        pauses = [e["values"].get("sumOfPauses") for e in gc]
        print(f"\n## GC: {len(gc)} collections; pause durations as printed by jfr: first {pauses[:3]} ... last {pauses[-1:]}")
    heap = [e["values"] for e in by["jdk.GCHeapSummary"] if e["values"].get("when") == "After GC"]
    if heap:
        used = [h["heapUsed"] for h in heap]
        print(f"## Heap after GC: min {min(used) >> 20} MB, last {used[-1] >> 20} MB, max {max(used) >> 20} MB over {len(used)} collections")

    for kind, label in (("jdk.JavaMonitorEnter", "Monitor contention"), ("jdk.ThreadPark", "Thread park")):
        evs2 = by[kind]
        if not evs2:
            continue
        tot = collections.Counter()
        key = collections.Counter()
        for e in evs2:
            v = e["values"]
            d = v.get("duration")
            dur = _dur(d)
            st = v.get("stackTrace")
            first = next((frame_name(f) for f in (st["frames"] if st else []) if is_layer(frame_name(f))), "(no layer frame)")
            tot[first] += dur
            key[(v.get("monitorClass") or v.get("parkedClass") or {}).get("name", "?")] += dur
        print(f"\n## {label}: {len(evs2)} events, total {sum(tot.values()):.0f} ms")
        for n, c in tot.most_common(a.top):
            print(f"  {c:>10.0f} ms  {n}")
        print("  by monitor/parked class:")
        for n, c in key.most_common(6):
            print(f"  {c:>10.0f} ms  {n}")


def _dur(d):
    """jfr prints durations like 'PT0.001234S' in JSON; returns milliseconds."""
    if isinstance(d, (int, float)):
        return d / 1e6
    if isinstance(d, str) and d.startswith("PT") and d.endswith("S"):
        try:
            return float(d[2:-1]) * 1000.0
        except ValueError:
            return 0.0
    return 0.0


if __name__ == "__main__":
    sys.exit(main())
