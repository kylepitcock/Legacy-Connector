# Library candidates

Policy: where a maintained NeoForge 1.21.1 library already solves something the layer re-implements, depend on the library (like
Baubles -> Curios, Waila -> Jade, NEI -> JEI; see [Bridges](Bridges.md)). This page is a survey (Modrinth API, filter `versions:1.21.1` +
`categories:neoforge`, 2026-10-31) of the layer's use cases and what could replace the hand-rolled code. Downloads and dates come from Modrinth.
Check a licence and the nesting rules before bundling anything; "bundle" means jar-in-jar like Pehkui and the Scala libraries in `build.gradle`.

## Adopted

| Use case | Hand-rolled before | Library | Decision |
| --- | --- | --- | --- |
| Entity and player size (`Entity.setSize`, `eyeHeight`, Fisk's Heroes shrink/grow) | `EntitySizes1710` side table + `LivingEntityDimensionsMixin` + client pose-stack scale hook | [Pehkui](https://modrinth.com/mod/pehkui) `3.8.3+1.21-neoforge`, MIT, 10.8 M downloads, last build 2024-06 | Adopted, bundled jar-in-jar (no install step; a separately installed newer Pehkui wins). `EntitySizes1710` maps sizes to `HITBOX_WIDTH/HEIGHT`, `MODEL_WIDTH/HEIGHT`, `EYE_HEIGHT`; poses (sneak, swim) stay proportional. Risk: no release since 2024, but it still works on NeoForge 21.1.258 |

## Candidates per use case

| Use case | What we hand-roll today | Candidate (Modrinth slug) | 1.21.1 NeoForge version, licence, downloads, state | Recommendation |
| --- | --- | --- | --- | --- |
| Player/entity pose animation (`ModelBiped` rotation hooks, first-person arm pose, `client/FirstPersonPose`, Fisk animations) | Legacy `ModelBase`/`ModelBiped` classes in the shim plus pose hooks applied to the modern player model | `player-animation-library` (Player Animator) | 1.1.6 (2026-08), MIT, 3.5 M, active | Later: useful for Fisk-style whole-body animation layers, not for plain `ModelBiped` rotations (legacy model code sets the angles itself every frame) |
| Animated entity/item models | Legacy `ModelBase` renderers run as is (`LegacyEntityRenderer`) | `geckolib` | 4.9.3 (2026-09), MIT, 78.7 M, active | No: legacy mods ship Java models, not GeckoLib assets. Only relevant for an addon that re-skins a legacy entity |
| Camera roll/shake (Fisk `CameraHandler.orient`, `ViewportEvent.ComputeCameraAngles` in `FiskHeroesClient`) | Own proxy of the legacy GL calls onto the camera event | `handycam` | 2.1.0+1.21.1 (2026-10), LGPL-3.0-only, 35 K, active | No: its API is for new effects; our case is translating arbitrary GL matrix calls, which a shake library cannot express |
| Gravity / jump / step / reach | Vanilla attributes (`Attributes.GRAVITY`, `STEP_HEIGHT`, `ENTITY_INTERACTION_RANGE`) in the shim, `Entity.mcc$set$stepHeight` | vanilla 1.21.1 attributes; Pehkui also has `STEP_HEIGHT`, `JUMP_HEIGHT`, `MOTION`, `REACH` scale types | n/a | Already vanilla. Consider Pehkui `STEP_HEIGHT` for size-coupled step height (Fisk) if the attribute proves insufficient |
| Config screens (`client/LegacyConfigScreens`, `GuiConfig`/`ConfigElement` in the 1.12 shim) | Own screens rendering legacy `Configuration` categories | `cloth-config` (15.0.140+neoforge, LGPL-3.0-only, 189 M, last build 2024-09; the NeoForge-native Configured has no 1.21.1 NeoForge build on Modrinth) | Works but the 1.12 FML config-GUI API (`IConfigElement`, `GuiConfig`) maps poorly to its builder model | Later, optional: only if players ask for nicer screens; keep ours as the fallback |
| Dimension sky, fog, clouds (`LegacyDimensionEffects`, `LegacyFog`, `LegacyHorizon`) | Own `DimensionSpecialEffects` | vanilla `DimensionSpecialEffects` + NeoForge render-stage events | n/a | No library needed |
| Custom shaders / post effects (legacy `GL20` shader code, Fisk glow) | Legacy GL emulation in `LegacyGlTexture`/`DisplayLists` | `veil` (4.5.2, 2026-10, LGPL-3.0-only, 530 K, active) | Powerful render framework, but heavy and shaders in legacy mods are GLSL 1.20 strings, which need translation anyway | No for now; revisit when a legacy shader mod has to work |
| Particles (legacy `EffectRenderer`, `EntityFX`, 1.12 `Particle`) | Shim particle classes drawing through modern sprites | vanilla `Particle` + registry; no library found | n/a | No |
| Multipart blocks (Forge Multipart / CCL / Chicken Bones' `cb-multipart`) | Legacy mods ship their own ForgeMultipart; the layer translates it like any mod | `cb-multipart` 3.5.0.155 (2025-12), LGPL-2.1-only, 214 K | Native 1.21.1 multipart for modern mods; unrelated to the legacy API classes the old jar calls | No (a bridge would map `TMultiPart` to it: large, only if multipart mods (ProjectRed, EnderIO conduits) prove common) |
| Tinkers' Construct / Mantle (legacy `mantle`/`tconstruct` libs) | Mods using Mantle load their own jar | `hexa-tinkers-construct-unofficial-port` (not an API) | Mantle itself is not on Modrinth for 1.21.1 NeoForge in the search | No |
| Fluid tanks, item and energy handlers | `CapabilityRules`/`BuiltinAdapters` map legacy tanks onto NeoForge capabilities | NeoForge's own capabilities (`IFluidHandler`, `IItemHandler`, `IEnergyStorage`) | n/a | Already the modern API; no extra library |
| Entity data / capabilities attached to entities (our `EntityBridge`, `ExtendedEntityProperties`, 1.12 capabilities) | NeoForge data attachments | NeoForge data attachments (vanilla of NeoForge); `cardinal-components` has no NeoForge 1.21.1 build | n/a | Already done with attachments |
| Item animations / item model overrides | `LegacyItemRenderer`, `IsbrhBlockModel` | none suitable (`tiny-item-animations` is a resource-pack tweak, LPUL licence) | n/a | No |
| Key rebinding and conflicts (`client/LegacyKeys`, `LegacyKeyConflicts`) | Own mapping of LWJGL 2 key codes to vanilla `KeyMapping` | `controlling` (19.0.5, MIT, 43 M, 2025-03) is a search UI over the vanilla controls screen | Complements; no API we depend on | No |
| Worldgen helpers (biome modifiers, structure sets for mod dimensions, `worldgen/` generators) | Own chunk generators, features hooked on events | `lithostitched` (1.8.1, MIT, 26 M, 2026-10, active): data-driven worldgen modifiers | Aimed at datapack authors; our generators run legacy code per chunk, no overlap | No |
| Armor models (`LegacyArmorLayer` renders `ModelBiped` armor) | Own layer | `armor-model-api` (1.1.0+1.21.1-neoforge, MIT, 219 K, 2026-09) | Small and recent; would replace the layer's registration plumbing, not the legacy model drawing | Later: evaluate when armor rendering is next touched |
| First-person model / skin layers | `FirstPersonPose`, arm render hooks | `first-person-model`, `3dskinlayers` (custom licence) | Client features, no API to build on | No |
| Utility libraries many mods bundle (`kotlin-for-forge` 5.12.0 LGPL-2.1, `owo-lib` MIT, `puzzles-lib` MPL-2.0, `bookshelf-lib` LGPL-2.1, `lithostitched`) | Not used by the layer | Needed only when a bridged modern mod needs them | n/a | No; leave to the user's pack |

## How to add one

1. Gradle: Modrinth maven is already declared with the `maven.modrinth` content filter; add the version to `gradle.properties`.
2. Required and tiny (MIT/Apache, a few hundred KB): bundle with `jarJar(implementation(...))`, as for Pehkui.
3. Optional and big: `compileOnly`, soft-load through the bridge pattern in `LibraryBridges.java` (class presence check, no direct references in
   always-loaded classes) and say so in the README config table.
4. Never commit jars; list the licence in the PR.
