#!/usr/bin/env python3
"""Builds docs/data/reports.json from the GitHub issues labelled 'compatibility' (the compatibility report form).

usage: build_reports.py [owner/repo]     needs the GitHub CLI (gh) logged in, or GH_TOKEN set.
Issue text is untrusted: only whitelisted fields are copied, lengths are capped, and the site renders them as text.
"""
import json, re, subprocess, sys, pathlib

REPO = sys.argv[1] if len(sys.argv) > 1 else "kylepitcock/Legacy-Connector"
OUT = pathlib.Path(__file__).resolve().parents[2] / "docs" / "data" / "reports.json"
STATUS = [("works", "works"), ("partially", "partial"), ("fails", "fails"), ("crashes", "crashes")]


def sections(body):
    out, key = {}, None
    for line in (body or "").splitlines():
        m = re.match(r"^###\s+(.*\S)\s*$", line)
        if m:
            key = m.group(1).strip().lower()
            out[key] = []
        elif key is not None:
            out[key].append(line)
    return {k: "\n".join(v).strip() for k, v in out.items()}


def field(sec, prefix):
    for k, v in sec.items():
        if k.startswith(prefix) and v and v != "_No response_":
            return v
    return ""


def clip(s, n):
    s = re.sub(r"\s+", " ", s).strip()
    return s if len(s) <= n else s[: n - 1] + "…"


def main():
    raw = subprocess.run(
        ["gh", "api", "--paginate", f"repos/{REPO}/issues?labels=compatibility&state=all&per_page=100"],
        check=True, capture_output=True, text=True).stdout
    # --paginate concatenates JSON arrays: "][" -> ","
    issues = json.loads(re.sub(r"\]\s*\[", ",", raw)) if raw.strip() else []
    reports = []
    for i in issues:
        if "pull_request" in i:
            continue
        sec = sections(i.get("body"))
        mod = clip(field(sec, "mod"), 80)
        status_text = field(sec, "result").lower()
        status = next((v for k, v in STATUS if status_text.startswith(k)), None)
        if not mod or not status:
            continue
        link = field(sec, "download link")
        reports.append({
            "issue": i["number"],
            "mod": mod,
            "mc": clip(field(sec, "legacy minecraft version"), 40) or "?",
            "status": status,
            "details": clip(field(sec, "what works"), 400),
            "lc": clip(field(sec, "legacy connector version"), 40),
            "neoforge": clip(field(sec, "neoforge version"), 20),
            "link": link if re.match(r"^https?://", link) else "",
            "date": i["created_at"][:10],
            "open": i["state"] == "open",
        })
    reports.sort(key=lambda r: r["issue"])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"reports": reports}, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(reports)} report(s) -> {OUT}")


main()
