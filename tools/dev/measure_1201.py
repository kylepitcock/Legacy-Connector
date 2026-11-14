#!/usr/bin/env python3
"""Per-mod run-time measurement of Forge 1.20.1 mods on the headless server (what a mod does when the game starts, not whether it links).

usage: measure_1201.py <suite> [<suite> ...] [--max-iter 8] [--out build/devtools/m1201]

FML stops the start at the first loading phase in which a mod fails, and names every mod that failed in that phase. For each suite
(tools/dev/verify/suites/<suite>.txt, see gen_suites_1201.py) the script runs tools/dev/verify.sh on a temporary copy, reads the failed mod ids and their
first exception from the log ("Loading errors encountered:"), removes those mods' jars and runs again, until the server starts (or nothing more can be
attributed). Result per mod: failed (phase of the last run it was in, first line of the cause), registered content (counts from the
"1.20.1 content registered" line) or constructed (no content registered). Written to <out>/<suite>.json and printed as a table.
The jars are copied from the read-only folders by verify.sh; nothing is written back.
"""
import json, os, pathlib, re, socket, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/dev"))
sys.path.insert(0, str(ROOT / "tools/dev/verify"))
import gen_suites_1201 as gen  # noqa: E402
import verify  # noqa: E402

BASH = next((p for p in ("C:/Program Files/Git/bin/bash.exe", "C:/Program Files/Git/usr/bin/bash.exe") if os.path.exists(p)), "bash")
SUITES = ROOT / "tools/dev/verify/suites"


def phase_of(text):
    hit = None
    for m in re.finditer(r"Failed to wait for future ([\w ]+), (\d+) errors? found", text):
        hit = m.group(1)
    if hit:
        return hit
    return "pre-loading" if "Error during pre-loading phase" in text else "start"


def content_counts(text):
    out = {}
    for m in re.finditer(r"^\s+([\w.\-]+): (\d+) blocks, (\d+) items, (\d+) entities, (\d+) block entity types", text, re.M):
        out[m.group(1)] = [int(m.group(i)) for i in range(2, 6)]
    return out


def free_port_base():
    """A base so that base+1 (server) and base+101 (rcon) are free: several workers share the machine and the default 25571 is often taken."""
    import random
    for _ in range(50):
        base = random.randrange(26000, 30000)
        try:
            for p in (base + 1, base + 101):
                with socket.socket() as sk:
                    sk.bind(("", p))
            return base
        except OSError:
            continue
    return 25570


def diagnose(text, gradle_log):
    """Why a run that names no failed mod did not start: the first line that says so (a silent no-start used to print only 'no start, no error found')."""
    if "FAILED TO BIND TO PORT" in text:
        return "server could not bind its port (another server on this machine; set VERIFY_PORT_BASE)"
    if gradle_log.exists():
        g = gradle_log.read_text(encoding="utf-8", errors="replace").splitlines()
        for i, l in enumerate(g):
            if re.match(r"> Task \S+ FAILED", l):
                return "gradle: " + l + " | " + " ".join(x.strip() for x in g[i + 1:i + 4])[:200]
            if l.startswith("FAILURE: Build failed") or l.startswith("* What went wrong"):
                return "gradle: " + " ".join(x.strip() for x in g[i + 1:i + 4])[:240]
    if not text:
        return "no server log was written (the server never started)"
    if "1.20.1 content registered" in text and "Done (" not in text:
        crash = [l for l in text.splitlines() if "/FATAL]" in l or "Encountered an unexpected exception" in l]
        return "mods loaded and registered, but the server never printed Done: " + (crash[0][:200] if crash else "log ends without an error")
    return ""


def crash_frames(name, n=4):
    """FML names no mod for an 'uncaught parallel processing error' and prints no stack: the crash report holds the first frames."""
    d = ROOT / "run-verify" / name / "crash-reports"
    reports = sorted(d.glob("crash-*-fml.txt"), key=lambda p: p.stat().st_mtime) if d.exists() else []
    if not reports:
        return ""
    lines = reports[-1].read_text(encoding="utf-8", errors="replace").splitlines()
    out = []
    for i, l in enumerate(lines):
        if l.strip().startswith("Failure message:"):
            out.append(l.strip()[:200])
            out += [x.strip()[:140] for x in lines[i + 1:i + 3]]
            break
    for l in lines:
        if l.startswith("	at "):
            out.append(l.strip()[:140])
            if len(out) >= n + 3:
                break
    return " | ".join(out)


def run(name, env):
    subprocess.run([BASH, "tools/dev/verify.sh", name], cwd=ROOT, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    log = ROOT / "run-verify" / name / "logs/latest.log"
    return log.read_text(encoding="utf-8", errors="replace") if log.exists() else ""


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    max_iter = int(sys.argv[sys.argv.index("--max-iter") + 1]) if "--max-iter" in sys.argv else 8
    out = pathlib.Path(sys.argv[sys.argv.index("--out") + 1]) if "--out" in sys.argv else ROOT / "build/devtools/m1201"
    args = [a for a in args if a not in (str(max_iter), str(out))]
    out.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, JAVA_HOME=os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-21"))
    env.setdefault("VERIFY_PORT_BASE", str(free_port_base()))
    atm9 = {p.name: gen.info(p) for p in pathlib.Path(gen.ATM9).glob("*.jar")}
    id_to_jar = {i: n for n, v in atm9.items() if v for i in v["ids"]}
    for suite in args:
        lines = [l for l in (SUITES / (suite + ".txt")).read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
        wanted = [l.split(None, 1)[1] for l in lines if l.startswith("1.20.1 ")]
        wanted_ids = {i: n for n in wanted for i in (atm9.get(n) or {"ids": []})["ids"]}
        result = {"suite": suite, "mods": {}, "iterations": []}
        dropped = set()
        tmp = "zz-m-" + suite
        text = ""
        for it in range(max_iter):
            keep = [l for l in lines if not (l.startswith("1.20.1 ") and l.split(None, 1)[1] in dropped)]
            (SUITES / (tmp + ".txt")).write_text("\n".join(keep) + "\n", encoding="utf-8")
            text = run(tmp, env)
            failed = verify.failed_mods_1201(text)
            result["iterations"].append({"n": it + 1, "phase": phase_of(text) if failed else "ok", "failed": failed, "jars": len(keep)})
            print("%s iteration %d: %d failed (%s)" % (suite, it + 1, len(failed), ", ".join(sorted(failed)) or "-"), flush=True)
            if not failed:
                break
            new = set()
            for mid, cause in failed.items():
                jar = id_to_jar.get(mid)
                if jar and jar not in dropped:
                    new.add(jar)
                    result["mods"][mid] = {"status": "failed", "phase": phase_of(text), "cause": cause, "jar": jar, "iteration": it + 1}
            if not new:
                result["stuck"] = "no jar to remove for " + ", ".join(sorted(failed)) + " :: " + crash_frames(tmp)
                break
            # mods that need a dropped mod would only fail with "requires": drop them in the same step (recorded as cascade)
            gone = {i for n in dropped | new for i in (atm9.get(n) or {"ids": []})["ids"]}
            for n in wanted:
                if n not in dropped | new and atm9.get(n) and atm9[n]["deps"] & gone:
                    new.add(n)
                    for i in atm9[n]["ids"]:
                        result["mods"].setdefault(i, {"status": "failed", "phase": "dependency", "cause": "needs " + ", ".join(sorted(atm9[n]["deps"] & gone)), "jar": n, "iteration": it + 1, "cascade": True})
            dropped |= new
        else:
            result["stuck"] = "max iterations"
        if not verify.failed_mods_1201(text) and "Done (" not in text:
            # a start that died outside the mod loader (translation, mixin config...): keep the first fatal line
            fatal = [l for l in text.splitlines() if "FATAL" in l or "Exception in thread" in l]
            gl = ROOT / "build/devtools" / ("verify-%s-gradle.log" % tmp)
            if gl.exists():
                fatal += [l for l in gl.read_text(encoding="utf-8", errors="replace").splitlines() if l.startswith("Exception in thread")]
            why = diagnose(text, ROOT / "build/devtools" / ("verify-%s-gradle.log" % tmp))
            result["stuck"] = why or (fatal[0][:300] if fatal else "no start, no error found")
        counts = content_counts(text)
        for mid, n in counts.items():
            if mid not in result["mods"]:
                result["mods"][mid] = {"status": "registered" if sum(n) else "constructed", "blocks": n[0], "items": n[1], "entities": n[2], "block_entities": n[3]}
        result["done"] = "Done (" in text
        errs = {}
        for l in text.splitlines():
            if "/ERROR]" in l:
                key = re.sub(r"^\[[^\]]*\] \[[^\]]*\] ", "", l)[:160]
                errs[key] = errs.get(key, 0) + 1
        result["errors"] = sorted(errs.items(), key=lambda kv: -kv[1])[:15]
        (out / (suite + ".json")).write_text(json.dumps(result, indent=1), encoding="utf-8")
        (SUITES / (tmp + ".txt")).unlink(missing_ok=True)
        ok = sum(1 for v in result["mods"].values() if v["status"] != "failed")
        print("%s: %d mods, %d started, %d failed, done=%s %s" % (suite, len(result["mods"]), ok, len(result["mods"]) - ok, result["done"], result.get("stuck", "")))


if __name__ == "__main__":
    main()
