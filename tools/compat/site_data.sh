#!/bin/bash
# Generated site data (docs/data/mods.json, probe.json, reports.json, resources.json) for jobs that may not push to master.
# master is protected (pull request + review), so scheduled jobs write to the unprotected branch "site-data" instead; the site
# (docs/assets/site.js) reads that branch first and falls back to the copy on master.
#   site_data.sh pull            take a data file from origin/site-data when its last commit there is newer than on HEAD
#   site_data.sh push "<msg>"    commit the working tree's data files to site-data and push (no-op when nothing changed)
# Needs full history for "pull" (actions/checkout with fetch-depth: 0).
set -e
cd "$(dirname "$0")/../.."
FILES="mods.json probe.json reports.json resources.json"
has_branch() { git ls-remote --exit-code --heads origin site-data >/dev/null 2>&1; }
case "$1" in
  pull)
    has_branch || { echo "site-data: branch does not exist yet"; exit 0; }
    git fetch -q origin site-data
    for f in $FILES; do
      theirs=$(git log -1 --format=%ct FETCH_HEAD -- "docs/data/$f" 2>/dev/null || true)
      ours=$(git log -1 --format=%ct HEAD -- "docs/data/$f" 2>/dev/null || true)
      if [ -n "$theirs" ] && [ "${theirs:-0}" -gt "${ours:-0}" ]; then
        git show "FETCH_HEAD:docs/data/$f" > "docs/data/$f"
        echo "site-data: took docs/data/$f from site-data"
      fi
    done
    ;;
  push)
    msg="${2:-Update site data}"
    d=$(mktemp -d)
    if has_branch; then
      git fetch -q origin site-data
      git worktree add -q -B site-data "$d" FETCH_HEAD
    else
      git worktree add -q --detach "$d"
      git -C "$d" checkout -q --orphan site-data
      git -C "$d" rm -rfq . >/dev/null 2>&1 || true
    fi
    mkdir -p "$d/docs/data"
    for f in $FILES; do [ -f "docs/data/$f" ] && cp "docs/data/$f" "$d/docs/data/$f"; done
    git -C "$d" add docs/data
    # rendered store pages of a description run (out/*.md): kept on this branch too, so the newest ones have a stable place
    if [ -f out/curseforge.md ] && [ -f out/modrinth.md ]; then
      mkdir -p "$d/description"; cp out/curseforge.md out/modrinth.md "$d/description/"; git -C "$d" add description
    fi
    if git -C "$d" diff --cached --quiet; then
      echo "site-data: no change"
    else
      git -C "$d" -c user.name="github-actions[bot]" -c user.email="41898282+github-actions[bot]@users.noreply.github.com" commit -q -m "$msg"
      git -C "$d" push -q origin site-data
      echo "site-data: pushed"
    fi
    git worktree remove --force "$d"
    ;;
  *) echo "usage: $0 pull | push \"<message>\""; exit 2;;
esac
