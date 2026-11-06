# Version roadmap

Which legacy Minecraft versions the layer supports or will support, in order, and why. Written 2026-11-06. Back to [Home](Home.md).

| # | Version | State | Why here | Approach |
|---|---|---|---|---|
| 1 | 1.7.10 | Beta | Huge pool of mods with no modern port (Thaumcraft 4, Witchery, GTNH, the Reika mods...) | Hand-written shim over 1.21.1 |
| 2 | 1.12.2 | Alpha, beta when its top libraries and content mods are playable | The largest old pool of mods with no modern port | Hand-written shim (169k lines) |
| 3 | 1.6.4 | Started | Closest old version to 1.7.10 (same `cpw.mods.fml` packages); cheap | Thin forward layer over the 1.7.10 shim: numeric ids, `Icon`, old packets ([estimate](Legacy-1.6.4-Estimate.md), [corpus](Legacy-1.6.4-Corpus.md)) |
| 4 | 1.5.2 | Planned | Same numeric-id and old packet model as 1.6.4, so a small delta while that work is fresh; the FTB Ultimate/Unleashed era with mods that exist only there (Thaumcraft 3, early Railcraft/Forestry/BuildCraft, Equivalent Exchange 2) | Delta over the 1.6.4 layer |
| 5 | 1.4.7 | Planned | Same model; the Hexxit era | Delta over 1.5.2 plus the flat `net.minecraft.src` class names |
| 6 | 1.20.1 | Started (experimental) | Largest Forge ecosystem; most popular mods have 1.21.1 ports, so the value is the abandoned set; its pipeline is the base of the next three | In-place bytecode migration onto NeoForge 1.21.1 ([estimate](Legacy-1.20.1-Estimate.md)) |
| 7 | 1.19.2 | Planned | Big modpack base, many mods stopped here | 1.19.2 to 1.20.1 renames and diffs on top of the 1.20.1 pipeline |
| 8 | 1.18.2 | Planned | Same | Same, one step further back |
| 9 | 1.16.5 | Planned | Many abandoned mods and packs; Forge 36 still used MCP names | Same pipeline plus the MCP to Mojang names step |
| 10 | 1.2.5 | Planned | Tekkit Classic / RedPower 2 era: mods that exist nowhere else | Different layer: `ModLoader`/`BaseMod` emulation and flat names, no FML |
| 11 | 1.3.2 | On demand | Few mods | Only if a specific mod needs it |

Always in parallel: keep the layer able to follow newer NeoForge/Minecraft host versions (1.21.4 and later). That is not a legacy version but it decides
whether any of the above keeps working.

## Not planned

- **1.8 to 1.11.2:** nothing useful lives only there (decision of 2026-10-25).
- **Beta, Alpha, 1.0 to 1.1 (ModLoader/jar-mod era):** most mods patch `minecraft.jar` itself (replaced vanilla classes), which cannot be translated as a standalone jar.

## Evidence

Modrinth mod counts (Forge category, project type mod; understates old versions, whose mods live on CurseForge): 1.20.1 25,526; 1.19.2 8,197; 1.18.2 6,072;
1.16.5 4,295; 1.12.2 2,748; 1.7.10 897; 1.6.4 131; 1.5.2 74; 1.4.7 77; 1.2.5 71. Compatibility data in `docs/data/mods.json` rates 86 1.12.2 mods "works" or better and
202 1.7.10 mods (2026-11-06). Effort figures use the project's measured pace (about four weeks from the first commit to two shims and the 1.20.1 groundwork).
