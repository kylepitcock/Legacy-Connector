#!/usr/bin/env python3
"""Compact digest of a GitHub issue and its attachments, for language-model readers.

    python tools/dev/issue_digest.py <issue-number> [--repo kylepitcock/Legacy-Connector] [--out <dir>]

Reads the issue with `gh issue view`, downloads every user-attachments/files/<id>/<name> link of the body
and comments (60 s timeout each, files over 300 MB skipped) into --out (default: a fresh directory under the
system temp dir, never inside the repo), and prints a digest of about 150 lines instead of the raw logs:
header and form fields, comments, per-file kind, log findings (deduplicated exceptions, noisy loggers, mixin
failures), crash reports, and the per-jar summary of legacyconnector-report.txt.
Files are streamed line by line as bytes (logs can be hundreds of MB); standard library only.
--lines N sets the soft line budget (default 150): when the digest is longer, the exception groups, frames
and listed references are trimmed step by step. --full keeps everything.
"""
import argparse
import collections
import json
import os
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request

MAX_BYTES = 300 * 1024 * 1024
ATTACH_RE = re.compile(r"https://github\.com/user-attachments/files/(\d+)/([^\s)\]>\"'<]+)")
LOG_RE = re.compile(r"^\[[^\]]*\] \[([^\]/]*)/(\w+)\] \[([^\]]*)\]: ?(.*)$")
EXC_RE = re.compile(r"^((?:[A-Za-z_$][\w$]*\.)*[A-Za-z_$][\w$]*(?:Exception|Error|Throwable|Death))(?::\s?(.*))?$")
FRAME_RE = re.compile(r"^\s+at (.+)$")
CAUSE_RE = re.compile(r"^Caused by: (.*)$")
JDK_PREFIX = ("java.", "jdk.", "sun.", "javax.", "com.sun.", "org.objectweb.asm.")
FIELD_ORDER = [
    ("mod", "Mod"),
    ("legacy minecraft version", "Legacy MC"),
    ("result", "Result"),
    ("legacy connector version", "LC version"),
    ("neoforge version", "NeoForge"),
    ("other mods installed", "Other mods"),
    ("what works", "What works / not"),
]
LOG_PATTERNS = [
    ("Legacy mods loaded", re.compile(r"Legacy mods loaded", re.I), 3),
    ("failed during", re.compile(r"failed during", re.I), 5),
    ("failed to load", re.compile(r"failed to load", re.I), 5),
    ("content registered", re.compile(r"content registered", re.I), 3),
    ("SELF TEST", re.compile(r"SELF TEST"), 6),
]
# (exception groups, frames per group, crash frames, listed unresolved references); tried in order until the digest fits
PARAM_STEPS = [
    {"groups": 25, "frames": 6, "crash_frames": 12, "unresolved": 15},
    {"groups": 14, "frames": 4, "crash_frames": 10, "unresolved": 15},
    {"groups": 9, "frames": 3, "crash_frames": 8, "unresolved": 10},
    {"groups": 6, "frames": 2, "crash_frames": 6, "unresolved": 8},
    {"groups": 4, "frames": 2, "crash_frames": 5, "unresolved": 5},
]


# ---------------------------------------------------------------- helpers

def clip(text, n):
    text = " ".join(str(text).split())
    return text if len(text) <= n else text[: n - 3] + "..."


def human(n):
    for unit in ("B", "KB", "MB", "GB"):
        if n < 1024 or unit == "GB":
            return f"{n:.0f} {unit}" if unit == "B" else f"{n:.1f} {unit}"
        n /= 1024


def clean_frame(frame):
    """Strip `TRANSFORMER/neoforge@21.1/`, `java.base/` module prefixes and the `~[x.jar%23:?] {re:mixin}` tail."""
    f = frame.strip()
    f = re.sub(r"\s+~?\[.*$", "", f)
    f = re.sub(r"^[A-Z][A-Z0-9 _\-]*/", "", f)             # TRANSFORMER/, LAYER PLUGIN/, MC-BOOTSTRAP/
    f = re.sub(r"^[\w.\-]+@[\w.\-+]+/{1,2}", "", f)        # neoforge@21.1.266/
    f = re.sub(r"^(?:java|jdk)\.[\w.]+/", "", f)           # java.base/
    f = re.sub(r"^[\w.\-]+//", "", f)
    return f


def frame_method(frame):
    """class.method without the source location, the identity used for grouping."""
    return frame.split("(", 1)[0]


def is_jdk(frame):
    return frame.startswith(JDK_PREFIX)


def collapse(frames):
    """Collapse runs of identical consecutive frames (stack overflows) to `frame  (xN)`."""
    runs = []
    for f in frames:
        if runs and runs[-1][0] == f:
            runs[-1][1] += 1
        else:
            runs.append([f, 1])
    return fmt_runs(runs, len(runs))


def fmt_runs(runs, limit):
    return [f if n == 1 else f"{f}  (x{n})" for f, n in runs[:limit]]


def split_exc(text):
    m = EXC_RE.match(text.strip())
    return (m.group(1), m.group(2) or "") if m else None


# ---------------------------------------------------------------- issue

def fetch_issue(number, repo):
    r = subprocess.run(["gh", "issue", "view", str(number), "--repo", repo, "--json",
                        "title,body,comments,author,createdAt"],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        sys.exit("gh issue view failed: " + r.stderr.strip())
    return json.loads(r.stdout)


def form_fields(body):
    """Issue-form body: `### Heading` followed by the answer."""
    fields = {}
    parts = re.split(r"^### +(.+?)\s*$", body or "", flags=re.M)
    for i in range(1, len(parts) - 1, 2):
        fields[parts[i].strip()] = parts[i + 1].strip()
    return fields


def pick_fields(fields):
    out = []
    for key, label in FIELD_ORDER:
        for name, value in fields.items():
            if name.lower().startswith(key):
                if value in ("", "_No response_"):
                    break
                if key == "what works":
                    value = re.sub(r"<img[^>]*>|!\[[^\]]*\]\([^)]*\)", "[image]", value)
                    value = re.sub(r"\n{2,}", "\n", value)
                    out.append((label, value if len(value) <= 1200 else value[:1197] + "..."))
                else:
                    out.append((label, clip(value, 300)))
                break
    return out


def attachments(texts):
    seen, out = set(), []
    for t in texts:
        for m in ATTACH_RE.finditer(t or ""):
            url = m.group(0).rstrip(".,;")
            if url not in seen:
                seen.add(url)
                out.append((url, urllib.parse.unquote(m.group(2).rstrip(".,;"))))
    return out


def download(url, name, out_dir, used):
    safe = re.sub(r"[^\w.\-]", "_", name) or "attachment"
    base, n = safe, 1
    while safe in used:
        safe = f"{n}_{base}"
        n += 1
    used.add(safe)
    path = os.path.join(out_dir, safe)
    req = urllib.request.Request(url, headers={"User-Agent": "issue-digest"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            length = resp.headers.get("Content-Length")
            if length and int(length) > MAX_BYTES:
                return None, f"skipped (over 300 MB: {human(int(length))})"
            total = 0
            with open(path, "wb") as f:
                while True:
                    chunk = resp.read(1 << 20)
                    if not chunk:
                        break
                    total += len(chunk)
                    if total > MAX_BYTES:
                        f.close()
                        os.remove(path)
                        return None, "skipped (over 300 MB)"
                    f.write(chunk)
        return path, None
    except Exception as e:  # noqa: BLE001 - report and carry on
        if os.path.exists(path):
            os.remove(path)
        return None, f"download failed: {clip(e, 100)}"


# ---------------------------------------------------------------- file kinds

def classify(path, name):
    low = name.lower()
    with open(path, "rb") as f:
        head = f.read(8192).decode("utf-8", "ignore")
    first = head.split("\n", 1)[0].rstrip("\r")
    if "Minecraft Crash Report" in head or low.startswith("crash-"):
        return "crash report"
    if "legacyconnector-report" in low or "legacy mod report" in first:
        return "legacyconnector-report"
    if low in ("latest.log", "debug.log") or LOG_RE.match(first):
        return "latest.log"
    if low.endswith(".log"):
        return "other log"
    return "other"


def lines_of(path):
    with open(path, "rb") as f:
        for raw in f:
            yield raw.decode("utf-8", "ignore").rstrip("\r\n")


# ---------------------------------------------------------------- log analysis

class ExcGroup:
    __slots__ = ("cls", "frame", "count", "first_line", "message", "runs", "causes", "root")

    def __init__(self, cls, frame, line, message, runs, causes, root):
        self.cls, self.frame, self.count, self.first_line = cls, frame, 1, line
        self.message, self.runs, self.causes, self.root = message, runs, causes, root


class LogAnalyzer:
    MAX_GROUPS = 5000
    KEEP_RUNS = 40

    def __init__(self):
        self.version = None
        self.groups = {}
        self.dropped = 0
        self.matches = {k: [] for k, _, _ in LOG_PATTERNS}
        self.match_total = collections.Counter()
        self.match_seen = set()
        self.loggers = collections.Counter()
        self.mixin = collections.OrderedDict()
        self.lines = 0
        self.cur = None

    def _start(self, line_no, cls, msg):
        self.cur = {"cls": cls, "msg": msg, "line": line_no, "runs": [], "causes": [],
                    "confirmed": False, "in_cause": False, "suppressed": False}

    def _finish(self):
        c, self.cur = self.cur, None
        if not c or not c["confirmed"]:
            return
        runs = c["runs"]
        first = next((f for f, _ in runs if not is_jdk(f)), runs[0][0] if runs else "?")
        root = c["causes"][-1][0] if c["causes"] else ""
        key = (c["cls"], frame_method(first), root)
        g = self.groups.get(key)
        if g:
            g.count += 1
        elif len(self.groups) >= self.MAX_GROUPS:
            self.dropped += 1
        else:
            self.groups[key] = ExcGroup(c["cls"], first, c["line"], c["msg"], runs, c["causes"], root)

    def _frame(self, text):
        c = self.cur
        fr = clean_frame(text)
        if c["in_cause"]:
            cause = c["causes"][-1]
            if len(cause[2]) < 12:
                cause[2].append(fr)
            return
        runs = c["runs"]
        if runs and runs[-1][0] == fr:
            runs[-1][1] += 1
        elif len(runs) < self.KEEP_RUNS:
            runs.append([fr, 1])

    def feed(self, line_no, line):
        self.lines += 1
        m = LOG_RE.match(line)
        if m:
            if self.cur:
                self._finish()
            level, logger, text = m.group(2), m.group(3).rstrip("/"), m.group(4)
            if level in ("ERROR", "WARN"):
                self.loggers[(logger, level)] += 1
            self._scan_text(text)
            hit = split_exc(text)
            if hit:
                self._start(line_no, *hit)
            return
        c = self.cur
        if c is not None:
            fm = FRAME_RE.match(line)
            if fm:
                c["confirmed"] = True
                if not c["suppressed"]:
                    self._frame(fm.group(1))
                return
            cm = CAUSE_RE.match(line)
            if cm and c["confirmed"]:
                hit = split_exc(cm.group(1)) or (clip(cm.group(1), 80), "")
                c["causes"].append([hit[0], hit[1], []])
                c["in_cause"], c["suppressed"] = True, False
                return
            if c["confirmed"] and (line.startswith("\t") or line.startswith("Suppressed:")):
                if "Suppressed:" in line:
                    c["suppressed"] = True
                return
            self._finish()
        if line and not line[0].isspace():
            hit = split_exc(line)
            if hit:
                self._start(line_no, *hit)

    def _scan_text(self, text):
        if self.version is None:
            v = re.search(r"Legacy ?Connector[^0-9]{0,40}v?(\d+\.\d+\.\d+[\w.\-+]*)", text, re.I)
            if v:
                self.version = v.group(1)
        for name, rx, cap in LOG_PATTERNS:
            if rx.search(text):
                self.match_total[name] += 1
                body = re.sub(r"^\s*[\w.\-/|]+: (?=Legacy mod)", "", text).strip()
                key = (name, re.sub(r"\d+", "#", body)[:90])
                if key not in self.match_seen and len(self.matches[name]) < cap:
                    self.match_seen.add(key)
                    self.matches[name].append(body)
                break
        low = text.lower()
        if "mixin apply" in low and "fail" in low:
            k = re.sub(r"\d+", "#", text)[:200]
            if k in self.mixin:
                self.mixin[k][0] += 1
            elif len(self.mixin) < 200:
                self.mixin[k] = [1, text]

    def finish(self):
        if self.cur:
            self._finish()


def analyze_log(path):
    a = LogAnalyzer()
    for n, line in enumerate(lines_of(path), 1):
        a.feed(n, line)
    a.finish()
    return a


def render_log(a, p):
    out = [f"  lines: {a.lines:,}; Legacy Connector version in log: {a.version or 'not found'}"]
    for name, _, _ in LOG_PATTERNS:
        for t in a.matches[name]:
            out.append(f"  [{name}] {clip(t, 220)}")
        extra = a.match_total[name] - len(a.matches[name])
        if extra > 0:
            out.append(f"  [{name}] ... {extra} more matching line(s)")
    groups = sorted(a.groups.values(), key=lambda g: (-g.count, g.first_line))
    total = sum(g.count for g in groups)
    if not groups:
        out.append("  exceptions: none")
    else:
        out.append(f"  exceptions: {total} traces in {len(groups)} distinct groups"
                   + (f" (+{a.dropped} past the group cap)" if a.dropped else ""))
    for g in groups[: p["groups"]]:
        out.append(f"  x{g.count} {g.cls} @ line {g.first_line}: {clip(g.message, 200)}")
        for f in fmt_runs(g.runs, p["frames"]):
            out.append(f"      at {clip(f, 170)}")
        for cause in g.causes[:3]:
            head = f"{cause[0]}: {clip(cause[1], 120)}" if cause[1] else cause[0]
            top = next((f for f in cause[2] if not is_jdk(f)), cause[2][0] if cause[2] else "")
            out.append(f"      Caused by: {clip(head, 170)}" + (f"  <- {clip(top, 110)}" if top else ""))
    rest = groups[p["groups"]:]
    if rest:
        out.append(f"  ... {len(rest)} more groups: " + "; ".join(
            f"x{g.count} {g.cls.rsplit('.', 1)[-1]} ({clip(g.message, 40)})" for g in rest[:5]))
    if a.loggers:
        by = collections.Counter()
        lv = collections.defaultdict(dict)
        for (lg, level), n in a.loggers.items():
            by[lg] += n
            lv[lg][level] = n
        top = ", ".join(f"{lg or '?'} {lv[lg].get('ERROR', 0)}E/{lv[lg].get('WARN', 0)}W" for lg, _ in by.most_common(15))
        out.append("  ERROR/WARN loggers: " + top)
    for _, (n, text) in list(a.mixin.items())[:6]:
        out.append(f"  mixin failure x{n}: {clip(text, 200)}")
    return out


# ---------------------------------------------------------------- crash reports

def analyze_crash(path):
    d = {"desc": None, "head": None, "runs": [], "causes": [], "sections": collections.OrderedDict(), "mods": 0}
    state, section, in_mods, cause = "pre", None, False, None
    for raw in lines_of(path):
        line = raw.rstrip()
        if state == "pre":
            if line.startswith("Description:"):
                d["desc"] = line.split(":", 1)[1].strip()
                state = "wait"
            continue
        if state == "wait":
            if line.strip():
                d["head"] = line.strip()
                state = "trace"
            continue
        if state == "trace":
            if not line.strip():
                state = "sections"
                continue
            fm = FRAME_RE.match(line)
            if fm:
                fr = clean_frame(fm.group(1))
                if cause is not None:
                    if len(cause[2]) < 12:
                        cause[2].append(fr)
                elif d["runs"] and d["runs"][-1][0] == fr:
                    d["runs"][-1][1] += 1
                elif len(d["runs"]) < 40:
                    d["runs"].append([fr, 1])
            elif line.startswith("Caused by:"):
                hit = split_exc(line[10:].strip()) or (clip(line[10:], 80), "")
                cause = [hit[0], hit[1], []]
                d["causes"].append(cause)
            continue
        m = re.match(r"^-- (.+?) --$", line)
        if m:
            section, in_mods = m.group(1), False
            d["sections"].setdefault(section, [])
            continue
        if re.match(r"^\s*(Mod List|Loaded Mods|Mods)\s*:", line, re.I):
            in_mods = True
            continue
        if in_mods:
            if line.startswith((" ", "\t")):
                d["mods"] += 1
                continue
            in_mods = False
        if (section and line.strip() and not line.strip().startswith(("at ", "Stacktrace:", "Details:"))
                and len(d["sections"][section]) < 4):
            d["sections"][section].append(clip(line.strip(), 150))
    return d


def crash_signature(d):
    return (d["head"] or "", d["runs"][0][0] if d["runs"] else "")


def render_crash(d, p, also=()):
    out = [f"  Description: {clip(d['desc'] or '?', 200)}" + (f"   (same crash: {', '.join(also)})" if also else "")]
    out.append(f"  {clip(d['head'] or '?', 230)}")
    for f in fmt_runs(d["runs"], p["crash_frames"]):
        out.append(f"      at {clip(f, 170)}")
    for cause in d["causes"][:3]:
        head = f"{cause[0]}: {clip(cause[1], 150)}" if cause[1] else cause[0]
        out.append(f"  Caused by: {clip(head, 200)}")
        for f in cause[2][:2]:
            out.append(f"      at {clip(f, 170)}")
    for sec, lines in d["sections"].items():
        if lines and sec.lower().startswith(("head", "affected")):
            out.append(f"  -- {sec} --  " + clip(" | ".join(lines[:3]), 260))
    out.append(f"  mod list: {d['mods']} lines")
    return out


# ---------------------------------------------------------------- legacyconnector-report

JAR_RE = re.compile(r"^\S.*\.jar\s+\[")


def analyze_report(path):
    jars, cur, sec, pre = [], None, None, []
    for raw in lines_of(path):
        if not raw.strip():
            continue
        if JAR_RE.match(raw):
            cur = {"name": raw.strip(), "mods": [], "notes": [], "sections": []}
            jars.append(cur)
            sec = None
            continue
        if cur is None:
            if len(pre) < 3 and not set(raw.strip()) <= set("=-"):
                pre.append(raw.strip())
            continue
        if not raw.startswith("  "):
            cur = sec = None
            continue
        body = raw.strip()
        if raw.startswith("    ") and sec is not None and body.startswith("- "):
            sec["items"].append(body[2:])
            continue
        sec = None
        mm = re.match(r"^mod (\S+) ->", body)
        if mm:
            cur["mods"].append(mm.group(1))
        elif body.startswith("note:"):
            cur["notes"].append(body[5:].strip())
        else:
            sec = {"head": body, "items": []}
            cur["sections"].append(sec)
    return {"pre": pre, "jars": jars}


def section_counts(j):
    c = {"unresolved": 0, "synth_classes": 0, "synth_members": 0, "external": 0}
    for s in j["sections"]:
        h = s["head"]
        m = re.match(r"^(\d+) unresolved", h)
        if m:
            c["unresolved"] = int(m.group(1))
        m = re.match(r"^Synthesized (\d+) legacy API class", h)
        if m:
            c["synth_classes"] = int(m.group(1))
        m = re.match(r"^Synthesized legacy APIs.*\[(\d+)\]", h)
        if m:
            c["synth_members"] = int(m.group(1))
        m = re.match(r"^Unresolved external.*\[(\d+)\]", h)
        if m:
            c["external"] = int(m.group(1))
    return c


def render_report(d, p):
    out = []
    if d["pre"]:
        out.append("  " + clip(d["pre"][0], 120))
    jars = d["jars"]
    if not jars:
        out.append("  no jar sections found")
    for i, j in enumerate(jars[:12]):
        c = section_counts(j)
        out.append(f"  {clip(j['name'], 110 if i else 150)}")
        out.append(f"      mods({len(j['mods'])}): {clip(', '.join(j['mods']), 100 if i else 200)}; unresolved {c['unresolved']}; "
                   f"synthesized {c['synth_classes']} classes + {c['synth_members']} members; external-missing {c['external']}")
        for n in j["notes"][: (3 if i < 2 else 0)]:
            out.append(f"      note: {clip(n, 150)}")
        left = p["unresolved"]
        for s in j["sections"]:
            low = s["head"].lower()
            if low.startswith(("missing shim", "removed library")):
                out.append(f"      {clip(s['head'], 150)}")
                for it in s["items"][:left]:
                    out.append(f"        - {clip(it, 150)}")
                left -= min(left, len(s["items"]))
                if len(s["items"]) > p["unresolved"]:
                    out.append(f"        ... more not shown")
            elif i < 2 and (low.startswith("synthesized") or "external" in low):
                out.append(f"      {clip(s['head'], 110)}: " + clip("; ".join(it.split("  <-")[0] for it in s["items"][:3]), 200))
    if len(jars) > 12:
        out.append(f"  ... {len(jars) - 12} more jars: " + clip(", ".join(j["name"].split("  ")[0] for j in jars[12:]), 200))
    return out


# ---------------------------------------------------------------- main

def build(header, files_out, analyses, params):
    out = list(header) + list(files_out)
    crash_seen = {}
    for name, kind, data in analyses:
        if kind == "crash report":
            sig = crash_signature(data)
            if sig in crash_seen:
                crash_seen[sig][1].append(name)
                continue
            crash_seen[sig] = [name, [], data]
    crash_done = set()
    for name, kind, data in analyses:
        if kind == "crash report":
            sig = crash_signature(data)
            if sig in crash_done:
                continue
            crash_done.add(sig)
            first, also, d = crash_seen[sig]
            out += ["", f"== {first} (crash report)"]
            out += render_crash(d, params, also)
        elif kind in ("latest.log", "other log"):
            out += ["", f"== {name} ({kind})"]
            out += render_log(data, params) if (kind == "latest.log" or data.groups) else [f"  lines: {data.lines:,}; no exceptions"]
        elif kind == "legacyconnector-report":
            out += ["", f"== {name} ({kind})"]
            out += render_report(data, params)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("issue", type=int)
    ap.add_argument("--repo", default="kylepitcock/Legacy-Connector")
    ap.add_argument("--out", default=None, help="download directory (default: new directory in the system temp dir)")
    ap.add_argument("--lines", type=int, default=150, help="soft line budget of the digest")
    ap.add_argument("--full", action="store_true", help="do not trim to the line budget")
    args = ap.parse_args(argv)

    issue = fetch_issue(args.issue, args.repo)
    out_dir = args.out or tempfile.mkdtemp(prefix=f"issue{args.issue}_")
    os.makedirs(out_dir, exist_ok=True)

    header = []
    author = (issue.get("author") or {}).get("login", "?")
    header.append(f"#{args.issue} {issue.get('title', '')}")
    header.append(f"author {author}, {issue.get('createdAt', '')}")
    for label, value in pick_fields(form_fields(issue.get("body"))):
        if "\n" in value:
            header.append(f"{label}:")
            header.extend("    " + l for l in value.split("\n")[:14])
        else:
            header.append(f"{label}: {value}")
    comments = issue.get("comments") or []
    header.append(f"comments: {len(comments)}")
    for c in comments[:12]:
        header.append(f"  {(c.get('author') or {}).get('login', '?')} {c.get('createdAt', '')[:10]}: {clip(c.get('body', ''), 300)}")
    if len(comments) > 12:
        header.append(f"  ... {len(comments) - 12} more comments")

    links = attachments([issue.get("body")] + [c.get("body") for c in comments])
    files_out = ["", f"== attachments ({len(links)}) in {out_dir}"]
    used, analyses = set(), []
    for url, name in links:
        path, err = download(url, name, out_dir, used)
        if not path:
            files_out.append(f"  {name}  {err}")
            continue
        kind = classify(path, name)
        files_out.append(f"  {name}  {human(os.path.getsize(path))}  {kind}")
        try:
            if kind in ("latest.log", "other log"):
                analyses.append((name, kind, analyze_log(path)))
            elif kind == "crash report":
                analyses.append((name, kind, analyze_crash(path)))
            elif kind == "legacyconnector-report":
                analyses.append((name, kind, analyze_report(path)))
        except Exception as e:  # noqa: BLE001
            files_out.append(f"  analysis of {name} failed: {clip(e, 150)}")

    steps = PARAM_STEPS[:1] if args.full else PARAM_STEPS
    if args.full:
        steps = [{"groups": 5000, "frames": 40, "crash_frames": 40, "unresolved": 5000}]
    out = []
    for params in steps:
        out = build(header, files_out, analyses, params)
        if len(out) <= args.lines:
            break
    print("\n".join(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
