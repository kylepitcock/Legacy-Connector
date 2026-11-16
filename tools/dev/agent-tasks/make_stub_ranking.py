#!/usr/bin/env python3
"""Ranks the synthesized (placeholder) members of a probe run by the number of jars using them.

usage: make_stub_ranking.py <mc> <raw.json> [<raw.json> ...] [--top 150] [--notes notes.tsv] > stub-ranking-<mc>.md

Input: `tools/compat/probe.py <dir> --mc <mc> --dry-run --no-history --json raw.json`. Members are split into server-side API and client-only API
(by class/package heuristics, see CLIENT_HINTS). `--notes` is a TSV `member-substring<TAB>reason` of members that cannot have a faithful implementation;
they are listed in their own section and do not count towards the top list.
"""
import collections, datetime, json, re, sys

CLIENT_HINTS = ("client", "renderer", ".gui.", "gui.", "GuiScreen", "Gui", "GameSettings", "RenderPlayer", "Render", "lwjgl", "model.", "ModelBase", "Tessellator",
                "audio.", "sound.", "SoundHandler", "FontRenderer", "TextureMap", "TextureManager", "IIcon", "TextureAtlasSprite", "resources.", "Minecraft.",
                "Minecraft;", "EntityPlayerSP", "EntityRenderer", "RenderGlobal", "ParticleManager", "EffectRenderer", "EntityFX", "Particle", "KeyBinding", "ScaledResolution",
                "RenderHelper", "GlStateManager", "OpenGlHelper", "Mouse", "Keyboard", "ItemRenderer", "MouseHelper", "PlayerControllerMP", "ClientRegistry", "IGuiHandler")


def side(m):
    if not m.startswith(("net.minecraft", "cpw.mods", "org.lwjgl", "io.netty", "paulscode")) :
        return "mod"   # another mod's API (NEI, Baubles ...): bridged or stubbed on purpose, not shim work
    return "client" if any(h in m for h in CLIENT_HINTS) else "server"


def clean(m):
    return re.sub(r"legacyconnector_synth\.v[0-9_a-z]+\.|legacyconnector\.v[0-9_a-z]+\.", "", m)


def main():
    a = sys.argv[1:]
    top = 150
    notes = {}
    if "--top" in a:
        i = a.index("--top"); top = int(a[i + 1]); del a[i:i + 2]
    if "--notes" in a:
        i = a.index("--notes"); f = a[i + 1]; del a[i:i + 2]
        for line in open(f, encoding="utf-8"):
            if "\t" in line:
                k, v = line.rstrip("\n").split("\t", 1)
                notes[k] = v
    mc, files = a[0], a[1:]
    use = collections.defaultdict(set)
    jars, results, total = set(), collections.Counter(), 0
    for f in files:
        for r in json.load(open(f, encoding="utf-8")):
            if r.get("mc") != mc or r["jar"] in jars:
                continue
            jars.add(r["jar"]); results[r.get("result")] += 1
            for m in r.get("stubbed") or []:
                use[clean(m)].add(r["jar"])
    ranked = sorted(use.items(), key=lambda kv: (-len(kv[1]), kv[0]))
    nofix = [(m, js) for m, js in ranked if any(k in m for k in notes)]
    live = [(m, js) for m, js in ranked if not any(k in m for k in notes)]
    print(f"# {mc}: stub ranking (synthesized members by number of jars)")
    print()
    print(f"Generated {datetime.date.today().isoformat()} by `tools/dev/agent-tasks/make_stub_ranking.py` from the offline probe of {len(jars)} jar(s) "
          f"({', '.join(f'{v} {k}' for k, v in results.most_common())}); {len(ranked)} distinct synthesized members. "
          "Regenerate after implementing; implemented members drop out. Format: `n  member  # jars`.")
    print()
    for sd in ("server", "client", "mod"):
        sel = [(m, js) for m, js in live if side(m) == sd][:top]
        print(f"## {sd}-side API (top {len(sel)} of {sum(1 for m, _ in live if side(m) == sd)})")
        print()
        print("```")
        for m, js in sel:
            names = sorted(js)
            print(f"{len(js):3d}  {m}  # {', '.join(names[:3])}{' ...' if len(names) > 3 else ''}")
        print("```")
        print()
    if nofix:
        print("## No faithful implementation possible")
        print()
        print("```")
        for m, js in nofix:
            why = next(v for k, v in notes.items() if k in m)
            print(f"{len(js):3d}  {m}  # {why}")
        print("```")


main()
