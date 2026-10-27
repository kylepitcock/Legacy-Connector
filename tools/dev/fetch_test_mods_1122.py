#!/usr/bin/env python3
"""Downloads the 1.12.2 test mods listed in tools/dev/test-mods-1.12.2.json from Modrinth into their test-mods folders.

usage: python3 tools/dev/fetch_test_mods_1122.py        (run from the repository root; jars already present are skipped)

The jars are only for local link checks and server tests: test-mods/ is gitignored, never commit or redistribute them.
"""
import hashlib, json, os, urllib.request

UA = {"User-Agent": "kylepitcock/legacy-connector-linkcheck"}
manifest = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "test-mods-1.12.2.json")))
for e in manifest:
    dest = os.path.join(e["folder"], e["file"])
    if os.path.exists(dest) or "url" not in e:
        continue
    os.makedirs(e["folder"], exist_ok=True)
    data = urllib.request.urlopen(urllib.request.Request(e["url"], headers=UA)).read()
    if hashlib.sha1(data).hexdigest() != e["sha1"]:
        print("hash mismatch, skipped:", e["file"])
        continue
    open(dest, "wb").write(data)
    print("downloaded", dest)
