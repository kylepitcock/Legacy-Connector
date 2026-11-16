#!/usr/bin/env python3
"""Writes loader/src/main/resources/legacyconnector/detect/notch-1.2.5.txt.gz: the obfuscated (official "notch") class, field and method
names of Minecraft 1.2.5, client and server, which the loader's version detection fingerprints jars with (1.2.5 mods name the game by these).

Source: Ornithe's Calamus intermediary (CC0, https://github.com/OrnitheMC/calamus), fetched from maven.ornithemc.net: only official names and
descriptors are kept, no readable names. Run by hand when the table needs regenerating; the build never needs the network for it.

Line format (tab separated):  <side c|s>  C  <class>
                              <side>  M  <owner>  <name>  <desc>
                              <side>  F  <owner>  <name>  <desc>
Only members whose name is at most 3 characters are kept (the obfuscated ones; readable names tell nothing about the version).
"""
import gzip, io, os, sys, urllib.request, zipfile

BASE = "https://maven.ornithemc.net/releases/net/ornithemc/calamus-intermediary/1.2.5-%s/calamus-intermediary-1.2.5-%s-v2.jar"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "loader", "src", "main", "resources",
                   "legacyconnector", "detect", "notch-1.2.5.txt.gz")


def rows(side):
    data = urllib.request.urlopen(BASE % (side, side), timeout=60).read()
    text = zipfile.ZipFile(io.BytesIO(data)).read("mappings/mappings.tiny").decode().splitlines()
    owner = None
    for line in text[1:]:
        p = line.split("\t")
        if p[0] == "c":
            owner = p[1]
            yield "C", owner, None, None
        elif p[0] == "" and p[1] in ("m", "f") and owner and len(p[3]) <= 3 and not p[3].startswith("<"):
            yield p[1].upper(), owner, p[3], p[2]


def main():
    out = []
    for side, code in (("client", "c"), ("server", "s")):
        for kind, owner, name, desc in rows(side):
            out.append("\t".join([code, kind, owner] + ([name, desc] if name else [])))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with gzip.GzipFile(OUT, "wb", mtime=0) as f:
        f.write(("\n".join(out) + "\n").encode())
    print(len(out), "rows ->", os.path.normpath(OUT), os.path.getsize(OUT), "bytes")


if __name__ == "__main__":
    sys.exit(main())
