# Version roadmap

Which legacy Minecraft versions the layer supports or will support, in order, and why. Written 2026-11-06; 1.3.2 was removed from the roadmap on 2026-11-07 (few mods; its row in the mod-count table below is kept as evidence). Back to [Home](Home.md).

| # | Version | State | Why here | Approach |
|---|---|---|---|---|
| 1 | 1.7.10 | Beta | Huge pool of mods with no modern port (Thaumcraft 4, Witchery, GTNH, the Reika mods...) | Hand-written shim over 1.21.1 |
| 2 | 1.12.2 | Beta (2026-11-07) | The largest old pool of mods with no modern port | Hand-written shim (169k lines) |
| 3 | 1.6.4 | Alpha (2026-11-07) | Closest old version to 1.7.10 (same `cpw.mods.fml` packages); cheap | Thin forward layer over the 1.7.10 shim: numeric ids, `Icon`, old packets ([estimate](Legacy-1.6.4-Estimate.md), [corpus](Legacy-1.6.4-Corpus.md)) |
| 4 | 1.5.2 | Alpha (2026-11-07) | Same numeric-id and old packet model as 1.6.4, so a small delta while that work is fresh; the FTB Ultimate/Unleashed era with mods that exist only there (Thaumcraft 3, early Railcraft/Forestry/BuildCraft, Equivalent Exchange 2) | Delta over the 1.6.4 layer |
| 5 | 1.4.7 | Alpha (2026-11-07) | Same model; the Hexxit era | Delta over 1.5.2 plus obfuscated/flat class names, ModLoader `BaseMod` mods and sprite-sheet textures ([estimate](Legacy-1.4.7-Estimate.md), [corpus](Legacy-1.4.7-Corpus.md)) |
| 6 | 1.20.1 | Alpha (2026-11-08) | Largest Forge ecosystem; most popular mods have 1.21.1 ports, so the value is the abandoned set; its pipeline is the base of the next three | In-place bytecode migration onto NeoForge 1.21.1 ([estimate](Legacy-1.20.1-Estimate.md)) |
| 7 | 1.19.2 | Planned | Big modpack base, many mods stopped here | 1.19.2 to 1.20.1 renames and diffs on top of the 1.20.1 pipeline |
| 8 | 1.18.2 | Planned | Same | Same, one step further back |
| 9 | 1.16.5 | Planned | Many abandoned mods and packs; Forge 36 still used MCP names | Same pipeline plus the MCP to Mojang names step |
| 10 | 1.2.5 | Planned | Tekkit Classic / RedPower 2 era: mods that exist nowhere else | Different layer: `ModLoader`/`BaseMod` emulation and flat names, no FML |

## Estimates (wall-clock, 2026-11-06 evening; plus or minus 50%)

| Item | Estimate |
|---|---|
| 1.6.4 server side | 2 to 4 hours |
| 1.6.4 supported (a handful of content mods playable) | 1 to 1.5 days |
| 1.5.2 | about 1 day after 1.6.4 loads, overlapping |
| 1.4.7 | 1.5 to 2.5 days after 1.5.2 (revised 2026-11-06 after the corpus: sprite sheets, ModLoader) |
| **0.4.0 (1.6.4 + 1.5.2 + 1.4.7)** | about 4 to 5 days (range 3 to 6) |
| 1.12.2 beta | done 2026-11-07 (declared by the maintainer; rendering issues #169 to #172 are still open) |
| 1.20.1 | corpus 1 day; first mod 1 to 2 days; broad coverage 1 to 2 weeks |
| 1.19.2, 1.18.2, 1.16.5 | 1 to 2 days each after the 1.20.1 pipeline |
| 1.2.5 | 2 to 3 days (a different layer) |
| Fabric 1.20.1 via Sinytra Connector | 3 to 5 days after Forge 1.20.1 works, if the corpus justifies it |

Always in parallel: keep the layer able to follow newer NeoForge/Minecraft host versions (1.21.4 and later). That is not a legacy version but it decides
whether any of the above keeps working.

## Not planned

- **1.8 to 1.11.2:** nothing useful lives only there (decision of 2026-10-25).
- **Beta, Alpha, 1.0 to 1.1 (ModLoader/jar-mod era):** most mods patch `minecraft.jar` itself (replaced vanilla classes), which cannot be translated as a standalone jar.

## Evidence

Mod counts per Minecraft version from several sources (all fetched 2026-11-06 unless noted). **Modrinth is biased toward new versions**: it is a newer platform, so most old mods
were never uploaded there. Use the CurseForge columns for old versions; Modrinth is shown to make the bias visible.

| Version | CurseForge (site search, `version=` filter, all loaders) | CFLookup, Forge only, whole 1.x family | CurseForge via a forum census, April 2023 | Modrinth (Forge category) |
|---|---|---|---|---|
| 1.2.5 | 108 | 220 (1.2) | | 71 |
| 1.3.2 | 91 | 123 (1.3) | | |
| 1.4.7 | 175 | 409 (1.4) | | 77 |
| 1.5.2 | 404 | 605 (1.5) | | 74 |
| 1.6.4 | 851 | 1,339 (1.6) | | 131 |
| 1.7.10 | 5,229 | 6,893 (1.7) | about 4,300 | 897 |
| 1.12.2 | 10,000+ (the site caps the count) | 15,199 (1.12) | about 10,620 | 2,748 |
| 1.16.5 | | 25,806 (1.16) | about 14,440 | 4,295 |
| 1.18.2 | | 17,585 (1.18) | about 11,380 | 6,072 |
| 1.19.2 | | 30,475 (1.19) | about 9,280 | 8,197 |
| 1.20.1 | | 31,887 (1.20) | | 25,526 |

The bias in numbers: Modrinth lists about 6 times fewer mods than CurseForge for 1.5.2, 1.6.4 and 1.7.10 (74 vs 404, 131 vs 851, 897 vs 5,229) and about 4 times fewer for 1.12.2 (2,748 vs 10,000+); the gap closes only at 1.20.1 and later.

Modpacks (CurseForge, April 2023 census): 1.12.2 about 20,920, 1.16.5 about 12,180, 1.18.2 about 5,660, 1.7.10 about 4,700, 1.19.2 about 4,160. So
1.12.2 is by far the most played modded version, with 1.16.5 second.

What this changes in the order above: nothing yet, but it sharpens three points. (1) 1.16.5 is the second largest old pool (about 14,000 to 26,000 mods, depending on the
source), larger than 1.12.2 in mod count, so its slot after the 1.20.1 pipeline is about the pipeline, not the size. (2) 1.6.4 (about 850 to 1,300 mods)
is about twice 1.5.2 (about 400 to 600), which is about twice 1.4.7 (about 175 to 410), so each step down the old versions is worth about half the previous one
while costing much less; that supports doing them as cheap deltas right after 1.6.4. (3) The 1.2.5 pool (108 to 220) is the smallest of all, which fits it being last
among the planned versions even though some of its mods exist nowhere else.

Caveats: CurseForge's search count includes every loader and any project that lists the version, and caps at 10,000. CFLookup counts Forge projects for a whole 1.x family
(1.7 includes 1.7.2 to 1.7.10) and several versions overlap between mods. The forum census is a person's count from 2023. None of these is the number of *abandoned*
mods (the ones that matter here), which needs a per-mod check against NeoForge 1.21.1. Compatibility data in `docs/data/mods.json` currently rates 86 1.12.2 mods "works" or better and
202 1.7.10 mods. Effort figures use the project's measured pace (about four weeks from the first commit to two shims and the 1.20.1 groundwork).

Sources: [CurseForge search with the version filter](https://www.curseforge.com/minecraft/search?class=mc-mods&version=1.6.4), [CFLookup Minecraft mod stats](https://cflookup.com/MinecraftModStats),
[Minecraft Forum: What version have the most mods?](https://www.minecraftforum.net/forums/minecraft-java-edition/discussion/3174330-what-version-have-the-most-mods),
[Modrinth search API](https://docs.modrinth.com/api/operations/searchprojects/).
