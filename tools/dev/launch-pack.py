#!/usr/bin/env python3
"""Launches a Prism Launcher instance's Minecraft client without Prism, in offline mode (Windows, pure stdlib).

Reads the instance's mmc-pack.json, merges the Prism meta version JSONs (meta/<uid>/<version>.json, or the instance's
own patches/<uid>.json), resolves the library jars in Prism's maven-layout libraries/ dir and runs the game from a
COPY of the pack (--game-dir), never from the original instance. No account data is read.

  python tools/dev/launch-pack.py --dry-run                      # print the command, check the classpath
  python tools/dev/launch-pack.py --mods-extra build/dist/legacyconnector-0.3.13.jar --control
"""
import argparse, hashlib, json, os, platform, shlex, shutil, subprocess, sys, uuid
from pathlib import Path

PRISM = Path(os.environ.get("PRISM_DIR", "C:/Users/Kyleb/AppData/Roaming/PrismLauncher"))
REPO = Path(__file__).resolve().parents[2]
DEFAULT_INSTANCE = "X:/Modded games/Prism Launcher/instances/ATM10 2026"
FALLBACK_JAVAS = ["C:/Program Files/Java/jdk-21/bin/java.exe"]


def read_cfg(path):
    """instance.cfg is an INI file; only [General] matters. Values may be wrapped in "..." (Prism escapes inner quotes)."""
    cfg = {}
    for line in Path(path).read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("["):
            if line.strip() != "[General]":
                break
            continue
        if "=" in line:
            k, v = line.split("=", 1)
            v = v.strip()
            if len(v) >= 2 and v[0] == v[-1] == '"':
                v = v[1:-1].replace('\\"', '"')
            cfg[k.strip()] = v
    return cfg


def rule_ok(rules):
    """Mojang library rules, evaluated for 64-bit Windows."""
    if not rules:
        return True
    allowed = False
    for r in rules:
        os_name = (r.get("os") or {}).get("name")
        if os_name is None or os_name == "windows":  # windows-arm64 / osx / linux never match
            allowed = r["action"] == "allow"
    return allowed


def maven_path(name):
    """group:artifact:version[:classifier][@ext] -> group/path/artifact/version/artifact-version[-classifier].ext"""
    name, _, ext = name.partition("@")
    parts = name.split(":")
    group, art, ver = parts[0], parts[1], parts[2]
    cls = "-" + parts[3] if len(parts) > 3 else ""
    return "%s/%s/%s/%s-%s%s.%s" % (group.replace(".", "/"), art, ver, art, ver, cls, ext or "jar")


def lib_path(lib):
    art = (lib.get("downloads") or {}).get("artifact") or {}
    return art.get("path") or maven_path(lib["name"])


def lib_key(name):  # group:artifact[:classifier]: a later patch's library replaces an earlier one with the same key
    p = name.split("@")[0].split(":")
    return ":".join([p[0], p[1]] + p[3:])


def load_patches(instance, pack):
    patches = []
    for comp in pack["components"]:
        uid, ver = comp["uid"], comp.get("version")
        own = instance / "patches" / (uid + ".json")
        f = own if own.exists() else PRISM / "meta" / uid / (ver + ".json")
        if not f.exists():
            sys.exit("missing version json for %s %s: %s" % (uid, ver, f))
        patches.append(json.loads(f.read_text(encoding="utf-8")))
    return patches


def merge(patches):
    m = {"libs": {}, "jvm": [], "game": None, "mainClass": None, "assetIndex": None, "mainJar": None, "maven": []}
    for p in patches:
        for l in p.get("libraries", []):
            if rule_ok(l.get("rules")):
                lk = lib_key(l["name"])
                m["libs"].pop(lk, None)  # re-insert at the end, like Prism
                m["libs"][lk] = l
        m["jvm"] += p.get("+jvmArgs", [])
        m["maven"] += p.get("mavenFiles", [])
        for k in ("mainClass", "assetIndex", "mainJar"):
            if p.get(k):
                m[k] = p[k]
        if p.get("minecraftArguments"):
            m["game"] = p["minecraftArguments"].split()
        if p.get("arguments"):  # modern format; Prism metas rarely use it
            m["game"] = [a for a in p["arguments"].get("game", []) if isinstance(a, str)] or m["game"]
            m["jvm"] += [a for a in p["arguments"].get("jvm", []) if isinstance(a, str)]
    return m


def offline_uuid(name):
    h = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
    h[6] = (h[6] & 0x0F) | 0x30
    h[8] = (h[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(h)))


def find_java(cfg, override):
    cands = [override, cfg.get("JavaPath")] + FALLBACK_JAVAS
    cands += sorted(str(p) for p in (PRISM / "java").glob("java-runtime-*/bin/java.exe"))
    for c in cands:
        if not c:
            continue
        p = Path(c)
        if p.name.lower() == "javaw.exe":  # keep a console so output is capturable
            p = p.with_name("java.exe")
        if p.exists():
            return p
    sys.exit("no java found; pass --java")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--instance", default=DEFAULT_INSTANCE, help="Prism instance dir (read only)")
    ap.add_argument("--game-dir", default=str(REPO / "run-atm10"), help="game directory to run in (a copy of the pack)")
    ap.add_argument("--mods-extra", action="append", default=[], help="jar copied into <game-dir>/mods (repeatable)")
    ap.add_argument("--width", type=int, default=854)
    ap.add_argument("--height", type=int, default=480)
    ap.add_argument("--mem", type=int, help="max heap in MB (default: the instance's MaxMemAlloc)")
    ap.add_argument("--java", help="java executable (default: the instance's JavaPath)")
    ap.add_argument("--username", default="Player")
    ap.add_argument("--extra-jvm", action="append", default=[], help="extra JVM argument(s), e.g. \"-Dfoo=1 -Dbar=2\"")
    ap.add_argument("--control", action="store_true",
                    help="remote-control session (tools/dev/ctl.sh, js.sh) with CONTROL=<game-dir>/control")
    ap.add_argument("--world-type", default="minecraft:normal", help="with --control: world type of the test world")
    ap.add_argument("--render-distance", type=int, default=8, help="with --control")
    ap.add_argument("--dry-run", action="store_true", help="print the command and check the classpath, do not start")
    a = ap.parse_args()

    instance, game_dir = Path(a.instance), Path(a.game_dir).resolve()
    pack = json.loads((instance / "mmc-pack.json").read_text(encoding="utf-8"))
    cfg = read_cfg(instance / "instance.cfg")
    m = merge(load_patches(instance, pack))
    if not m["mainClass"] or not m["game"] or not m["assetIndex"]:
        sys.exit("incomplete version data (mainClass/arguments/assetIndex)")

    # ---- classpath: libraries in patch order, then the Minecraft client jar
    libs = PRISM / "libraries"
    cp, missing = [], []
    skip_arch = ("-x86", "-arm64", "-arm32")  # lwjgl natives for other architectures carry a windows rule too
    for l in m["libs"].values():
        n = l["name"].split("@")[0]
        if n.endswith(skip_arch) or "natives-windows-x86" in n or "natives-windows-arm" in n:
            continue
        f = libs / lib_path(l)
        (cp if f.exists() else missing).append(f)
    mc_jar = libs / maven_path(m["mainJar"]["name"] if m["mainJar"] else "com.mojang:minecraft:1.21.1:client")
    (cp if mc_jar.exists() else missing).append(mc_jar)
    installer = None
    for mf in m["maven"]:
        if mf["name"].endswith(":installer"):
            installer = libs / maven_path(mf["name"])
            if not installer.exists():
                missing.append(installer)

    # ---- JVM command line
    mem = a.mem or int(cfg.get("MaxMemAlloc", 8192))
    java = find_java(cfg, a.java)
    cmd = [str(java), "-Xms%dm" % min(int(cfg.get("MinMemAlloc", 1024)), mem), "-Xmx%dm" % mem]
    if cfg.get("OverrideJavaArgs") == "true" and cfg.get("JvmArgs"):
        cmd += shlex.split(cfg["JvmArgs"], posix=True)
    cmd += m["jvm"]
    cmd += ["-Dminecraft.launcher.brand=prism", "-Dminecraft.launcher.version=offline",
            "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8"]
    if installer:  # ForgeWrapper (NeoForge/Forge) finds the installer, the libraries and the vanilla jar through these
        cmd += ["-Dforgewrapper.librariesDir=" + str(libs), "-Dforgewrapper.installer=" + str(installer),
                "-Dforgewrapper.minecraft=" + str(mc_jar)]
    if a.control:
        cmd += ["-Dlegacyconnector.screenshotTest=true", "-Dlegacyconnector.shotScenario=control",
                "-Dlegacyconnector.shotWorldType=" + a.world_type,
                "-Dlegacyconnector.shotRenderDistance=%d" % a.render_distance]
    for e in a.extra_jvm:
        cmd += shlex.split(e)
    cmd += ["-cp", os.pathsep.join(str(p) for p in cp), m["mainClass"]]

    # ---- game arguments (offline auth: no token is ever read)
    ai = m["assetIndex"]
    subst = {
        "auth_player_name": a.username, "auth_uuid": offline_uuid(a.username).replace("-", ""), "auth_access_token": "0",
        "auth_session": "0", "user_type": "legacy", "user_properties": "{}", "version_name": pack_mc_version(pack),
        "version_type": "release", "game_directory": str(game_dir), "assets_root": str(PRISM / "assets"),
        "game_assets": str(PRISM / "assets"), "assets_index_name": ai["id"], "auth_xuid": "0", "clientid": "0",
    }
    for g in m["game"]:
        for k, v in subst.items():
            g = g.replace("${%s}" % k, v)
        cmd.append(g)
    cmd += ["--width", str(a.width), "--height", str(a.height)]

    print("java      :", java)
    print("main class:", m["mainClass"])
    print("classpath : %d jars found, %d missing" % (len(cp), len(missing)))
    for f in missing:
        print("  MISSING:", f)
    print("game dir  :", game_dir)
    print()
    print(subprocess.list2cmdline(cmd))
    if a.dry_run:
        return 1 if missing else 0
    if missing:
        sys.exit("refusing to start: missing libraries (open the instance once in Prism so it downloads them)")

    # ---- prepare the game dir (never the original instance)
    if game_dir.resolve() == (instance / "minecraft").resolve() or instance.resolve() in game_dir.resolve().parents:
        sys.exit("--game-dir must not be inside the original instance")
    (game_dir / "mods").mkdir(parents=True, exist_ok=True)
    for j in a.mods_extra:
        shutil.copy2(j, game_dir / "mods" / Path(j).name)
        print("copied", j, "->", game_dir / "mods")
    if a.control:
        print("control dir:", game_dir / "control", "(CONTROL=%s tools/dev/ctl.sh ...)" % (game_dir / "control").relative_to(REPO)
              if REPO in game_dir.parents else "")
    return subprocess.call(cmd, cwd=game_dir)


def pack_mc_version(pack):
    for c in pack["components"]:
        if c["uid"] == "net.minecraft":
            return c.get("version", "1.21.1")
    return "1.21.1"


if __name__ == "__main__":
    sys.exit(main())
