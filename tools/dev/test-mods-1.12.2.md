# 1.12.2 test mods (macOS machine, downloaded 2026-10-27)

201 1.12.2 Forge jars from Modrinth, used for the 1.12.2 link check whose results are the `# ---- 1.12.2` sections of the
`tools/dev/agent-tasks/task-*.txt` lists. They are not in git (`test-mods/` is ignored); `tools/dev/test-mods-1.12.2.json`
records each jar's folder, Modrinth project, version, download URL and SHA-1, and

```
python3 tools/dev/fetch_test_mods_1122.py
```

downloads the same jars on another machine. Never commit or redistribute the jars.

- `test-mods/1.12.2-content/` (120 jars): the most-downloaded 1.12.2 Forge mods on Modrinth that are required on the server, in
  content categories (technology, magic, adventure, worldgen, equipment, mobs, storage, food, decoration, transportation,
  economy). The main sample.
- `test-mods/1.12.2/` (81 jars): the most-downloaded 1.12.2 Forge mods overall, mostly client-side (HUDs, menus, minimaps).

Re-run the link check with
`./gradlew checkLegacyMod -PlegacyJar=$(ls -d $PWD/test-mods/1.12.2-content/*.jar | paste -sd, -)`.
Mods that bundle Mixin (Spartan Weaponry, ...) go in `mods/1.12.2/`, not `mods/`, for a server test.

## test-mods/1.12.2-content

| Mod | Version | File | Categories |
|---|---|---|---|
| [Additional Banners](https://modrinth.com/mod/additional-banners) | 1.1.77 | `AdditionalBanners-1.12.2-1.1.77.jar` | adventure, utility |
| [Additional Enchanted Miner](https://modrinth.com/mod/additional-enchanted-miner) | 12.5.7 | `AdditionalEnchantedMiner-1.12.2-12.5.7.jar` | technology |
| [Additional Lanterns](https://modrinth.com/mod/additional-lanterns) | 1.1.2-forge-mc1.12 | `additionallanterns-1.1.2-forge-mc1.12.jar` | decoration |
| [Additional Structures](https://modrinth.com/mod/additional-structures) | v.2.5.0 | `Rex's-AdditionalStructures-1.12.x(v.2.5.0).jar` | adventure, worldgen |
| [Advanced Backups](https://modrinth.com/mod/advanced-backups) | 3.7.1 | `AdvancedBackups-forge-1.12-3.7.1.jar` | management, storage, utility |
| [Advent Of Ascension](https://modrinth.com/mod/adventofascension) | 1.12.2-3.3.6 | `AoA3-3.3.6.jar` | adventure, equipment, worldgen |
| [Aether: Lost Content Addon](https://modrinth.com/mod/aether-lost-content) | 1.0.2 | `lost-aether-content-1.12.2-1.0.2.jar` | equipment, mobs, worldgen |
| [Age of Weapons - Reforged](https://modrinth.com/mod/age-of-weapons-reforged) | v.0.13.6 | `Age-of-Weapons-1.12.2-(v.0.13.6).jar` | equipment, game-mechanics, technology |
| [Akashic Tome](https://modrinth.com/mod/akashic-tome) | 1.2-12 | `AkashicTome-1.2-12.jar` | storage, utility |
| [Baubley Heart Canisters](https://modrinth.com/mod/baubley-heart-canisters) | 1.12.2-2.0.3 | `baubley-heart-canisters-1.12.2-2.0.3.jar` | adventure, equipment, utility |
| [Bedspreads](https://modrinth.com/mod/bedspreads) | 1.12.2-1.0.2.2 | `cosmeticbeds-1.12.2-1.0.2.2.jar` | decoration |
| [Better Burning](https://modrinth.com/mod/better-burning) | 0.9.2 | `BetterBurning-1.12.2-0.9.2.jar` | adventure, utility |
| [Better Furnaces Reforged](https://modrinth.com/mod/better-furnaces-reforged) | 1.12.2-1.5.5 | `BetterFurnacesReforged-1.12.2-1.5.5.jar` | technology, utility |
| [Better Than Mending](https://modrinth.com/mod/better-than-mending) | 1.1.2 | `BetterThanMending-1.1.2.jar` | equipment, utility |
| [Beyond Dimensions](https://modrinth.com/mod/beyonddimensions) | 0.1.7.5 | `beyonddimensions-0.1.7.5-1.12.2.jar` | storage, transportation, utility |
| [Biomes O' Plenty](https://modrinth.com/mod/biomes-o-plenty) | 7.0.1.2445 | `BiomesOPlenty-1.12.2-7.0.1.2445-universal.jar` | adventure, decoration, worldgen |
| [Blood Magic](https://modrinth.com/mod/blood-magic) | 1.12.2-2.4.3-105 | `BloodMagic-1.12.2-2.4.3-105.jar` | equipment, magic |
| [Born in a Barn](https://modrinth.com/mod/born-in-a-barn) | 1.2 | `Born In A Barn 1.8-1.12-1.2.jar` | game-mechanics, optimization, utility |
| [Botania](https://modrinth.com/mod/botania) | 1.10-364.4-forge | `Botania+r1.10-364.4.jar` | decoration, magic, technology |
| [Capsule](https://modrinth.com/mod/capsule) | 3.4.76 | `Capsule-1.12.2-3.4.76.jar` | adventure, game-mechanics, utility |
| [Carry On](https://modrinth.com/mod/carry-on) | 1.12.7.23 | `carryon-1.12.2-1.12.7.23.jar` | game-mechanics, storage, transportation |
| [Chococraft](https://modrinth.com/mod/chococraft) | 0.9.1.52 | `chococraft_1.12.1-0.9.1.52.jar` | adventure, food, mobs |
| [Chunk Loaders](https://modrinth.com/mod/chunk-loaders) | 1.2.9a-forge-mc1.12 | `chunkloaders-1.2.9a-forge-mc1.12.jar` | technology, utility |
| [Comforts](https://modrinth.com/mod/comforts) | 1.12.2-1.4.1.3 | `comforts-1.12.2-1.4.1.3.jar` | adventure, decoration |
| [Connected Glass](https://modrinth.com/mod/connected-glass) | 1.1.14-forge-mc1.12 | `connectedglass-1.1.14-forge-mc1.12.jar` | decoration |
| [Conquest Reforged](https://modrinth.com/mod/conquest-reforged) | 1.0.0 | `ConquestReforged-3.0.2-mc1.12.2.jar` | decoration |
| [Corail Tombstone](https://modrinth.com/mod/corail-tombstone) | forge-1.12.2-4.8.0 | `tombstone-1.12.2-4.8.0.jar` | adventure, game-mechanics, magic |
| [Culinary Construct](https://modrinth.com/mod/culinary-construct) | 1.3.4 | `culinaryconstruct-1.3.4.jar` | food |
| [Dark Utilities](https://modrinth.com/mod/dark-utilities) | 1.8.230 | `DarkUtils-1.12.2-1.8.230.jar` | adventure, equipment |
| [Dawn of Time](https://modrinth.com/mod/dawn-of-time) | 1.0.7 | `dawnoftime_builder_edition-1.12.2-1.0.7.jar` | adventure, decoration |
| [Decocraft](https://modrinth.com/mod/decocraft) | 2.6.3.7 | `Decocraft-2.6.3.7_1.12.2.jar` | decoration |
| [Draconic Evolution](https://modrinth.com/mod/draconic-evolution) | 2.3.28.354 | `Draconic-Evolution-1.12.2-2.3.28.354-universal.jar` | equipment, mobs, storage |
| [Dynmap®](https://modrinth.com/mod/dynmap) | 3.7-beta-4 | `Dynmap-3.7-beta-4-forge-1.12.2.jar` | decoration, management, utility |
| [ElevatorMod](https://modrinth.com/mod/elevatormod) | 1.12.2-1.4.2 | `ElevatorMod-1.12.2-1.4.2.jar` | transportation |
| [Ender IO](https://modrinth.com/mod/enderio) | 5.3.72 | `EnderIO-1.12.2-5.3.72.jar` | decoration, equipment, technology |
| [Ender Storage](https://modrinth.com/mod/ender-storage) | 2.4.6.137 | `EnderStorage-1.12.2-2.4.6.137-universal.jar` | storage |
| [Entangled](https://modrinth.com/mod/entangled) | 1.3.21-forge-mc1.12 | `entangled-1.3.21-forge-mc1.12.jar` | magic, storage, technology |
| [Epic Fight](https://modrinth.com/mod/epic-fight) | 2.2.8 | `EpicFightMod-1.12.2-2.2.8.jar` | equipment, library, mobs |
| [Epic Knights: Shields Armor and Weapons](https://modrinth.com/mod/epic-knights-shields-armor-and-weapons) | 6.2 | `[1.12.2]-Epic-Knights-Armor-and-Weapons-6.2v(uncomplete).jar` | equipment |
| [Extreme Reactors](https://modrinth.com/mod/extreme-reactors) | 1.12.2-0.4.5.68 | `ExtremeReactors-1.12.2-0.4.5.68.jar` | technology, worldgen |
| [FirstAid](https://modrinth.com/mod/firstaid) | 1.6.22 | `firstaid-1.6.22.jar` | adventure, game-mechanics |
| [Fossils and Archeology: Revival](https://modrinth.com/mod/fossils-and-archeology-revival) | 8.0.6 | `fossilsarcheology-8.0.6.jar` | mobs, technology, worldgen |
| [Future MC](https://modrinth.com/mod/future-mc) | 0.2.21 | `Future-MC-0.2.21.jar` | decoration, equipment |
| [Grappling Hook Mod](https://modrinth.com/mod/grappling-hook-mod) | v13-1.12.2 | `grappling_hook_mod-1.12.2-v13.jar` | equipment |
| [Ice and Fire](https://modrinth.com/mod/ice-and-fire-dragons) | 1.9.1 | `iceandfire-1.9.1-1.12.2.jar` | adventure, magic, mobs |
| [Immersive Engineering](https://modrinth.com/mod/immersiveengineering) | 0.12-98 | `ImmersiveEngineering-0.12-98.jar` | equipment, technology |
| [Immersive Vehicles](https://modrinth.com/mod/immersive-vehicles) | 24.0.0-1.12.2 | `Immersive Vehicles-1.12.2-24.0.0.jar` | decoration, technology, transportation |
| [Immersive Vehicles - Official Content Pack [OCP] - Planes & Cars](https://modrinth.com/mod/immersive-vehicles-official-content-pack) | 29 | `MTS Official Pack-1.12.2-V29.jar` | technology, transportation, utility |
| [In Control!](https://modrinth.com/mod/in-control) | 1.12-3.9.18 | `incontrol-1.12-3.9.18.jar` | management, mobs, utility |
| [Industrial Foregoing](https://modrinth.com/mod/industrial-foregoing) | 1.12.13-237 | `industrialforegoing-1.12.2-1.12.13-237.jar` | food, storage, technology |
| [Infinity With Mending](https://modrinth.com/mod/infinity-with-mending) | forge-1.12.2-1.0.0 | `infwithmend-forge-1.12.2-1.0.0.jar` | equipment, game-mechanics, utility |
| [Inventory Pets](https://modrinth.com/mod/inventory-pets) | 2.0.15 | `inventorypets-1.12-2.0.15.jar` | adventure, equipment, magic |
| [Iron Jetpacks](https://modrinth.com/mod/iron-jetpacks) | 1.1.0 | `IronJetpacks-1.12.2-1.1.0.jar` | equipment, technology, transportation |
| [Item Collectors](https://modrinth.com/mod/item-collectors) | 1.1.12-forge-mc1.12 | `itemcollectors-1.1.12-forge-mc1.12.jar` | storage, technology |
| [Joy of Painting](https://modrinth.com/mod/joy-of-painting) | 1.12.2-1.3 | `xercapaint-1.12.2-1.3.jar` | decoration, game-mechanics, social |
| [Ksyxis](https://modrinth.com/mod/ksyxis) | 1.4.6 | `Ksyxis-1.4.6.jar` | optimization, utility, worldgen |
| [Macaw's Lights and Lamps](https://modrinth.com/mod/macaws-lights-and-lamps) | 1.0.6 | `mcw-lights-1.0.6-mc1.12.2forge.jar` | decoration |
| [Macaw's Paintings](https://modrinth.com/mod/macaws-paintings) | 1.0.5 | `mcw-paintings-1.0.5-1.12.2forge.jar` | decoration |
| [Mahou Tsukai](https://modrinth.com/mod/mahou-tsukai) | 1.12.2-v1.19.54 | `mahoutsukai-1.12.2-v1.19.54.jar` | magic |
| [Mekanism](https://modrinth.com/mod/mekanism) | 9.8.3.390 | `Mekanism-1.12.2-9.8.3.390.jar` | equipment, storage, technology |
| [Mekanism Generators](https://modrinth.com/mod/mekanism-generators) | 9.8.3.390 | `MekanismGenerators-1.12.2-9.8.3.390.jar` | technology |
| [Mekanism Tools](https://modrinth.com/mod/mekanism-tools) | 9.8.3.390 | `MekanismTools-1.12.2-9.8.3.390.jar` | equipment |
| [MineTraps](https://modrinth.com/mod/minetraps) | v.1.0.4 | `MineTraps-1.12.2-(v.1.0.4).jar` | cursed, decoration, equipment |
| [More Bows: Restrung!](https://modrinth.com/mod/more-bows-restrung) | v1.0.10+1.12.2 | `MoreBows-1.0.10+1.12.2.jar` | adventure, equipment |
| [MoreAvaritia](https://modrinth.com/mod/more_avaritia) | 0.7 | `MoreAvaritiaMc1.12.2-0.7.jar` | equipment |
| [Moving Elevators](https://modrinth.com/mod/moving-elevators) | 1.4.12-forge-mc1.12 | `movingelevators-1.4.12-forge-mc1.12.jar` | decoration, technology |
| [Music Triggers](https://modrinth.com/mod/music-triggers) | 7.0.4 | `musictriggers-7.0.4.jar` | adventure, management, utility |
| [Mystical Agradditions](https://modrinth.com/mod/mystical-agradditions) | 1.3.2 | `MysticalAgradditions-1.12.2-1.3.2.jar` | equipment, magic, worldgen |
| [Mystical Agriculture](https://modrinth.com/mod/mystical-agriculture) | 1.7.5 | `MysticalAgriculture-1.12.2-1.7.5.jar` | equipment, magic, worldgen |
| [Nature's Compass](https://modrinth.com/mod/natures-compass) | 1.12.2-1.8.5-forge | `NaturesCompass-1.12.2-1.8.5-forge.jar` | adventure, equipment, utility |
| [Net Music](https://modrinth.com/mod/net-music) | 1.1.0 | `netmusic-1.12.2-1.1.0.jar` | decoration, utility |
| [Nether API](https://modrinth.com/mod/nether-api) | 1.5.4 | `Nether-API-v1.5.4-mc1.12.2.jar` | library, worldgen |
| [Nyf's Spiders](https://modrinth.com/mod/nyfs-spiders) | 3.0.5 | `nyfsspiders-Forge-1.12.2-3.0.5.jar` | mobs |
| [OpenComputers](https://modrinth.com/mod/opencomputers) | 1.8.10 | `OpenComputers-MC1.12.2-1.8.10+258943363.jar` | decoration, technology |
| [Packed Up (Backpacks)](https://modrinth.com/mod/packed-up-backpacks) | 1.1.1a-forge-mc1.12 | `packedup-1.1.1a-forge-mc1.12.jar` | storage |
| [Pixelmon](https://modrinth.com/mod/pixelmon) | 8.4.3 | `Pixelmon-1.12.2-8.4.3-universal.jar` | adventure, decoration, worldgen |
| [Prefab](https://modrinth.com/mod/prefab) | 1.3.1.7 | `prefab-1.3.1.7.jar` | decoration, worldgen |
| [Project: Vibrant Journeys](https://modrinth.com/mod/project-vibrant-journeys) | 1.12.1-1.6.4 | `ProjectVibrantJourneys-1.12.2-1.6.4.jar` | worldgen |
| [Quark](https://modrinth.com/mod/quark) | r1.6-179 | `Quark-r1.6-179.jar` | game-mechanics, technology, utility |
| [Rats](https://modrinth.com/mod/rats) | 3.2.13 | `rats-3.2.13-1.12.2.jar` | cursed, mobs, technology |
| [Realistic Torches](https://modrinth.com/mod/realistic-torches) | 1.12.2-2.1.2 | `RealisticTorches-1.12.2-2.1.2.jar` | adventure, decoration, game-mechanics |
| [Rechiseled](https://modrinth.com/mod/rechiseled) | 1.2.6a-forge-mc1.12 | `rechiseled-1.2.6a-forge-mc1.12.jar` | decoration |
| [Refined Storage](https://modrinth.com/mod/refined-storage) | 1.6.16 | `refinedstorage-1.6.16.jar` | storage, technology, transportation |
| [Refined Storage Addons](https://modrinth.com/mod/refined-storage-addons) | 0.4.5 | `refinedstorageaddons-0.4.5.jar` | storage, technology, transportation |
| [Refined Storage: Requestify](https://modrinth.com/mod/rs-requestify) | 1.0.2 | `refinedstoragerequestify-1.12.2-1.0.2-3.jar` | storage, technology |
| [Responsive Shields](https://modrinth.com/mod/responsiveshields) | 2.3 | `responsiveshields-2.3-mc1.12.x.jar` | equipment, utility |
| [RFTools (Legacy)](https://modrinth.com/mod/rftools) | 1.12-7.73 | `rftools-1.12-7.73.jar` | storage, technology, transportation |
| [Scaling Health](https://modrinth.com/mod/scaling-health) | 1.3.42 | `ScalingHealth-1.12.2-1.3.42+147.jar` | adventure, game-mechanics, worldgen |
| [Scape and Run: Parasites](https://modrinth.com/mod/scapeandrunparasites) | 1.10.9 | `SRParasites-1.10.9.jar` | mobs |
| [SecurityCraft](https://modrinth.com/mod/security-craft) | v1.10.1 | `[1.12.2] SecurityCraft v1.10.1.jar` | adventure, equipment, technology |
| [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 1.2.18 | `SereneSeasons-1.12.2-1.2.18-universal.jar` | food, game-mechanics |
| [Silent Gear](https://modrinth.com/mod/silent-gear) | 0.6.7 | `SilentGear-1.12.2-0.6.7+76.jar` | equipment |
| [Simple Hats](https://modrinth.com/mod/simple-hats) | 1.12.2-Forge-0.4.1 | `SimpleHats-0.4.1.jar` | decoration, equipment |
| [Simple Magnets](https://modrinth.com/mod/simple-magnets) | 1.1.12-forge-mc1.12 | `simplemagnets-1.1.12-forge-mc1.12.jar` | adventure, equipment, technology |
| [Simple Storage Network](https://modrinth.com/mod/simple-storage-network) | 1.12.2-2.0.1 | `SimpleStorageNetwork-1.12.2-2.0.1.jar` | storage |
| [Sit](https://modrinth.com/mod/bl4cks-sit) | v1.3 | `sit-1.12.2-v1.3.jar` | game-mechanics |
| [Slashblade Murasame](https://modrinth.com/mod/slashblade-murasame) | 1.12.2-1.0.0 | `murasame-1.0.0.jar` | adventure, equipment, worldgen |
| [SlashBladeJapaneseAddonPack](https://modrinth.com/mod/sjap) | 1.7.0-1.12.2 | `slashblade_addon-1.7.0.jar` | adventure, decoration, equipment |
| [Spartan Shields](https://modrinth.com/mod/spartan-shields) | 1.12.2-1.5.5 | `SpartanShields-1.12.2-1.5.5.jar` | equipment, game-mechanics |
| [Spartan Weaponry](https://modrinth.com/mod/spartan-weaponry) | 1.12.2-1.6.1 | `SpartanWeaponry-1.12.2-1.6.1.jar` | equipment, game-mechanics |
| [StarSource](https://modrinth.com/mod/starsource) | 1.3 | `StarSourceBlade-1.12.2-1.3.jar` | equipment |
| [Storage Drawers](https://modrinth.com/mod/storagedrawers) | 1.12-5.5.2 | `StorageDrawers-1.12.2-5.5.2.jar` | storage |
| [TerraFirmaCraft](https://modrinth.com/mod/terrafirmacraft) | 1.7.23.181 | `TerraFirmaCraft-MC1.12.2-1.7.23.181.jar` | adventure, game-mechanics, worldgen |
| [The Aether](https://modrinth.com/mod/aether) | 1.12.2-v1.5.4.1 | `aether-1.12.2-v1.5.4.1.jar` | adventure, equipment, worldgen |
| [The Hordes](https://modrinth.com/mod/the-hordes) | 1.12.2-1.6.3e | `The-Hordes-1.12.2-1.6.3e.jar` | adventure, game-mechanics, mobs |
| [The Lost Cities](https://modrinth.com/mod/the-lost-cities) | 1.12-2.0.22 | `lostcities-1.12-2.0.22.jar` | adventure, worldgen |
| [The Midnight](https://modrinth.com/mod/the-midnight) | 1.12.2-0.3.5 | `themidnight-0.3.5.jar` | adventure, mobs, worldgen |
| [Tinkers' Construct](https://modrinth.com/mod/tinkers-construct) | 2.13.0.183 | `TConstruct-1.12.2-2.13.0.183.jar` | equipment, magic, technology |
| [Tough As Nails](https://modrinth.com/mod/tough-as-nails) | 3.1.0.141 | `ToughAsNails-1.12.2-3.1.0.141-universal.jar` | equipment, food, game-mechanics |
| [Trash Cans](https://modrinth.com/mod/trash-cans) | 1.1.2-forge-mc1.12 | `trashcans-1.1.2-forge-mc1.12.jar` | decoration, storage, technology |
| [Traveler's Backpack](https://modrinth.com/mod/travelersbackpack) | 1.12.2-1.0.35 | `TravelersBackpack-1.12.2-1.0.35.jar` | adventure, equipment, storage |
| [Underlay](https://modrinth.com/mod/underlay) | 1.0.2 | `underlay-1.0.2-mc1.12.2.jar` | decoration, game-mechanics, utility |
| [Useful Slime](https://modrinth.com/mod/useful-slime) | 1.2 | `usefulslime-1.2-1.12.2.jar` | adventure, equipment, transportation |
| [Vampires Need Umbrellas](https://modrinth.com/mod/vampires-need-umbrellas) | 1.12.2-1.5 | `VampiresNeedUmbrellas-1.12.2-1.5.jar` | adventure, equipment, utility |
| [Vampirism](https://modrinth.com/mod/vampirism) | 1.12.2-1.6.2 | `Vampirism-1.12.2-1.6.2.jar` | adventure, magic |
| [Vampirism Integrations](https://modrinth.com/mod/vampirism-integrations) | 1.12.2-1.3.0 | `VampirismIntegrations-1.12.2-1.3.0.jar` | magic |
| [Wireless Redstone](https://modrinth.com/mod/wirelessredstone) | 1.12.2-1.1.6 | `WirelessRedstone-1.12.2-1.1.6.jar` | game-mechanics, technology, utility |
| [XNet](https://modrinth.com/mod/xnet) | 1.12-1.8.2 | `xnet-1.12-1.8.2.jar` | technology, transportation |
| [XP Tome](https://modrinth.com/mod/xp-tome) | v2.0.1 | `xptome-1.12.2-v2.0.1.jar` | adventure, storage, utility |
| [ZeroCore](https://modrinth.com/mod/zerocore) | 1.12.2-0.1.2.9 | `zerocore-1.12.2-0.1.2.9.jar` | library, technology, utility |

## test-mods/1.12.2

| Mod | Version | File | Categories |
|---|---|---|---|
| [3D Skin Layers](https://modrinth.com/mod/3dskinlayers) | 1.2.0-forge-1.12.2 | `3dSkinLayers-forge-mc1.12.2-1.2.0.jar` | adventure, decoration |
| [Advancement Plaques](https://modrinth.com/mod/advancement-plaques) | 1.4.10 | `AdvancementPlaques-1.12.2-1.4.10.jar` | adventure, game-mechanics, utility |
| [Ageing Spawners](https://modrinth.com/mod/ageing-spawners) | 1.0.0 | `AgeingSpawners-1.12.2-1.0.0.jar` | game-mechanics |
| [AI Improvements: Performance Tuning](https://modrinth.com/mod/ai-improvements) | 0.0.1b3 | `AIImprovements-1.12-0.0.1b3.jar` | optimization |
| [Ambient Environment](https://modrinth.com/mod/ambient-environment) | 3 | `AmbientEnvironment-1.0.2.jar` |  |
| [AmbientSounds](https://modrinth.com/mod/ambientsounds) | 3.1.5 | `AmbientSounds_v3.1.5_mc1.12.2.jar` | decoration |
| [AppleSkin](https://modrinth.com/mod/appleskin) | 1.0.14+mc1.12 | `AppleSkin-mc1.12-1.0.14.jar` | food, utility |
| [AttributeFix](https://modrinth.com/mod/attributefix) | 1.0.12 | `AttributeFix-Forge-1.12.2-1.0.12.jar` | utility |
| [AzureLib](https://modrinth.com/mod/azurelib) | 1.1.3 | `azurelib-forge-1.12.2-1.1.3.jar` | game-mechanics, library, utility |
| [Bad Wither No Cookie - Reloaded](https://modrinth.com/mod/bad-wither-no-cookie) | 3.4.18 | `badwithernocookiereloaded-1.12.2-3.4.18.jar` | game-mechanics |
| [Better Advancements](https://modrinth.com/mod/better-advancements) | 0.1.0.77 | `BetterAdvancements-1.12.2-0.1.0.77.jar` | utility |
| [Biomes O' Plenty](https://modrinth.com/mod/biomes-o-plenty) | 7.0.1.2445 | `BiomesOPlenty-1.12.2-7.0.1.2445-universal.jar` | adventure, decoration, worldgen |
| [BisectHosting Server Integration Menu](https://modrinth.com/mod/bisect-mod) | 2.4.8 | `BHMenu-Forge-1.12.2-2.4.8.jar` | library, utility |
| [Bookshelf](https://modrinth.com/mod/bookshelf-lib) | 2.3.590 | `Bookshelf-1.12.2-2.3.590.jar` | library, utility |
| [Borderless Mining](https://modrinth.com/mod/borderless-mining) | 0.0.0-1.6.0+1.12 | `FullscreenWindowed-1.12-1.6.0-1.jar` | utility |
| [Carry On](https://modrinth.com/mod/carry-on) | 1.12.7.23 | `carryon-1.12.2-1.12.7.23.jar` | game-mechanics, storage, transportation |
| [Cherished Worlds](https://modrinth.com/mod/cherished-worlds) | 1.12.2-1.0.1 | `cherishedworlds-1.12.2-1.0.1.jar` | management, utility |
| [Client Tweaks](https://modrinth.com/mod/client-tweaks) | 3.1.11+forge-1.21.2 | `ClientTweaks_1.12.2-3.1.11.jar` | game-mechanics, utility |
| [Clumps](https://modrinth.com/mod/clumps) | 3.1.2 | `Clumps-3.1.2.jar` | storage, utility |
| [Comforts](https://modrinth.com/mod/comforts) | 1.12.2-1.4.1.3 | `comforts-1.12.2-1.4.1.3.jar` | adventure, decoration |
| [Controlling](https://modrinth.com/mod/controlling) | 3.0.12.2 | `Controlling-3.0.12.2.jar` | utility |
| [Crafting Tweaks](https://modrinth.com/mod/crafting-tweaks) | 8.1.9+forge-1.12.2 | `CraftingTweaks_1.12.2-8.1.9.jar` | management, utility |
| [CraftPresence](https://modrinth.com/mod/craftpresence) | 2.7.1+1.12.2-forge | `CraftPresence-2.7.1+1.12.2-forge.jar` | library, utility |
| [Crash Assistant](https://modrinth.com/mod/crash-assistant) | 1.11.12 | `!!!CrashAssistant-forge-1.12.2-1.11.12.jar` | utility |
| [CreativeCore](https://modrinth.com/mod/creativecore) | 1.10.62 | `CreativeCore_v1.10.62_mc1.12.2.jar` | library |
| [CustomSkinLoader](https://modrinth.com/mod/customskinloader) | 15.1-Universal | `CustomSkinLoader_Universal-15.1.jar` | decoration |
| [Distant Horizons](https://modrinth.com/mod/distanthorizons) | 3.3.3-1.12.2 | `DistantHorizons-3.3.3-1.12.2-forge.jar` | optimization, utility |
| [Durability Tooltip](https://modrinth.com/mod/durability-tooltip) | 1.2.0-forge-mc1.12 | `durabilitytooltip-1.2.0-forge-mc1.12.jar` | utility |
| [Enchantment Descriptions](https://modrinth.com/mod/enchantment-descriptions) | 1.1.20 | `EnchantmentDescriptions-1.12.2-1.1.20.jar` | magic, utility |
| [Entity Culling](https://modrinth.com/mod/entityculling) | 1.6.3-1.12.2 | `entityculling-1.12.2-1.6.3.jar` | optimization |
| [Essential Mod](https://modrinth.com/mod/essential) | 1.5.1 | `Essential_1-5-1_forge_1-12-2.jar` | management, social, utility |
| [Euphoria Patches](https://modrinth.com/mod/euphoria-patches) | 1.10.5-r5.9.3-forgeLegacy | `EuphoriaPatcher-1.10.5-r5.9.3-forgeLegacy.jar` | utility |
| [FancyMenu](https://modrinth.com/mod/fancymenu) | 2.14.9-1.12-1.12.2-forge | `fancymenu_forge_2.14.9_MC_1.12-1.12.2.jar` | utility |
| [FPS Reducer](https://modrinth.com/mod/fps-reducer) | 1.12.2-1.20 | `FpsReducer-mc1.12.2-1.20.jar` | optimization |
| [Fusion (Connected Textures)](https://modrinth.com/mod/fusion-connected-textures) | 1.3.16-forge-mc1.12 | `fusion-1.3.16-forge-mc1.12.jar` | decoration |
| [Geckolib](https://modrinth.com/mod/geckolib) | 3.0.31 | `geckolib-forge-1.12.2-3.0.31.jar` | game-mechanics, library, utility |
| [GPUBooster](https://modrinth.com/mod/gputape) | 1.0.4-1.12 | `GPUTape-1.12.2-1.0.4.jar` | library, optimization, utility |
| [I18nUpdateMod](https://modrinth.com/mod/i18nupdatemod) | 3.7.0 | `I18nUpdateMod-3.7.0-all.jar` | utility |
| [InventoryHUD+](https://modrinth.com/mod/inventoryhudplus) | 3.4.4 | `InventoryHUD-1.12.2.forge-3.4.4.jar` | adventure, management, storage |
| [Iron Chests (with Netherite Chest)](https://modrinth.com/mod/iron-chests-with-netherite-chest) | 1.0.2 | `ironchest-netherite-1.12.2-1.0.2.jar` | storage |
| [Item Borders](https://modrinth.com/mod/item-borders) | 1.2.0 | `ItemBorders-1.12.2-forge-1.2.0.jar` | adventure, game-mechanics, utility |
| [Item Highlighter](https://modrinth.com/mod/item-highlighter) | 1.1.7 | `Highlighter-1.12.2-forge-1.1.7.jar` | adventure, equipment, utility |
| [JourneyMap](https://modrinth.com/mod/journeymap) | 1.12.2-6.0.10+forge | `journeymap-forge-1.12.2-6.0.10.jar` | adventure, utility |
| [Just Enough Items (JEI)](https://modrinth.com/mod/jei) | 4.24.0.1040 | `jei_1.12.2-4.24.0.1040.jar` | library, utility |
| [JustEnoughCharacters](https://modrinth.com/mod/justenoughcharacters) | 3.7.4 | `jecharacters-1.12.0-3.7.4.jar` | utility |
| [Konkrete](https://modrinth.com/mod/konkrete) | 1.6.1-1.12-1.12.2-forge | `konkrete_forge_1.6.1_MC_1.12-1.12.2.jar` | library |
| [Ksyxis](https://modrinth.com/mod/ksyxis) | 1.4.6 | `Ksyxis-1.4.6.jar` | optimization, utility, worldgen |
| [Legendary Tooltips](https://modrinth.com/mod/legendary-tooltips) | 1.1.11 | `LegendaryTooltips-1.12.2-1.1.11.jar` | adventure, equipment, utility |
| [Let Me Despawn](https://modrinth.com/mod/lmd) | 1.12.2-forge-1.0.2 | `letmedespawn-1.12.2-forge-1.0.2.jar` | optimization |
| [Litematica](https://modrinth.com/mod/litematica) | 0.0.0-dev.20191217.195055 | `litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar` | utility |
| [Lootr](https://modrinth.com/mod/lootr) | 0.6.2 | `lootr-1.12.2-0.6.2.jar` | utility |
| [MaLiLib](https://modrinth.com/mod/malilib) | 0.10.0-dev.23.nomixin | `malilib-forge-1.12.2-0.10.0-dev.23.nomixin.jar` | library, utility |
| [Nature's Compass](https://modrinth.com/mod/natures-compass) | 1.12.2-1.8.5-forge | `NaturesCompass-1.12.2-1.8.5-forge.jar` | adventure, equipment, utility |
| [Neat](https://modrinth.com/mod/neat) | 1.4-17 | `Neat+1.4-17.jar` | utility |
| [NetherPortalFix](https://modrinth.com/mod/netherportalfix) | 5.3.17+forge-1.12.2 | `NetherPortalFix_1.12.1-5.3.17.jar` | game-mechanics, social, utility |
| [Nyf's Spiders](https://modrinth.com/mod/nyfs-spiders) | 3.0.5 | `nyfsspiders-Forge-1.12.2-3.0.5.jar` | mobs |
| [Packet Fixer](https://modrinth.com/mod/packet-fixer) | 2.1.0 | `PacketFixer-forge-2.1.0-1.12.2.jar` | optimization, utility |
| [Patchouli](https://modrinth.com/mod/patchouli) | 1.0-23.6-forge | `Patchouli-1.0-23.6.jar` | library, utility |
| [Quark](https://modrinth.com/mod/quark) | r1.6-179 | `Quark-r1.6-179.jar` | game-mechanics, technology, utility |
| [Rechiseled](https://modrinth.com/mod/rechiseled) | 1.2.6a-forge-mc1.12 | `rechiseled-1.2.6a-forge-mc1.12.jar` | decoration |
| [ReplayMod](https://modrinth.com/mod/replaymod) | 1.12.2-2.6.24 | `replaymod-1.12.2-2.6.24.jar` | utility |
| [Resourcify](https://modrinth.com/mod/resourcify) | 1.6.2 | `Resourcify (1.12.2-forge)-1.6.2.jar` | management, utility |
| [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 1.2.18 | `SereneSeasons-1.12.2-1.2.18-universal.jar` | food, game-mechanics |
| [Shoulder Surfing Reloaded](https://modrinth.com/mod/shoulder-surfing-reloaded) | 1.12.2-2.9.6 | `ShoulderSurfing-1.12.2-2.9.6.jar` | adventure, utility |
| [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) | forge-1.12.2-2.6.26 | `voicechat-forge-1.12.2-2.6.26.jar` | adventure, social, utility |
| [Spartan Shields](https://modrinth.com/mod/spartan-shields) | 1.12.2-1.5.5 | `SpartanShields-1.12.2-1.5.5.jar` | equipment, game-mechanics |
| [Spartan Weaponry](https://modrinth.com/mod/spartan-weaponry) | 1.12.2-1.6.1 | `SpartanWeaponry-1.12.2-1.6.1.jar` | equipment, game-mechanics |
| [SuperMartijn642's Config Lib](https://modrinth.com/mod/supermartijn642s-config-lib) | 1.1.8-forge-mc1.12 | `supermartijn642configlib-1.1.8-forge-mc1.12.jar` | library |
| [SuperMartijn642's Core Lib](https://modrinth.com/mod/supermartijn642s-core-lib) | 1.1.25-forge-mc1.12 | `_supermartijn642corelib-1.1.25-forge-mc1.12.jar` | library |
| [The Aether](https://modrinth.com/mod/aether) | 1.12.2-v1.5.4.1 | `aether-1.12.2-v1.5.4.1.jar` | adventure, equipment, worldgen |
| [The Lost Cities](https://modrinth.com/mod/the-lost-cities) | 1.12-2.0.22 | `lostcities-1.12-2.0.22.jar` | adventure, worldgen |
| [Tips](https://modrinth.com/mod/tips) | 1.0.9 | `Tips-1.12.2-1.0.9.jar` | adventure, utility |
| [Traveler's Backpack](https://modrinth.com/mod/travelersbackpack) | 1.12.2-1.0.35 | `TravelersBackpack-1.12.2-1.0.35.jar` | adventure, equipment, storage |
| [Tweakeroo](https://modrinth.com/mod/tweakeroo) | 0.10.0-dev.20201007.231650+notrd | `tweakeroo-forge-1.12.2-0.10.0-dev.20201007.231650+notrd.jar` | utility |
| [UniLib](https://modrinth.com/mod/unilib) | 1.2.1+1.12.2-forge | `UniLib-1.2.1+1.12.2-forge.jar` | library, utility |
| [Wavey Capes](https://modrinth.com/mod/wavey-capes) | 1.2.0-forge-1.12 | `waveycapes-forge-mc1.12.2-1.2.0.jar` | decoration |
| [WorldEdit](https://modrinth.com/mod/worldedit) | 6.1.10 | `worldedit-forge-mc1.12.2-6.1.10-dist.jar` | library, management, utility |
| [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) | forge-1.12.2-26.6.0 | `xaerominimap-forge-1.12.2-26.6.0.jar` | adventure, transportation, utility |
| [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) | forge-1.12.2-1.47.0 | `xaeroworldmap-forge-1.12.2-1.47.0.jar` | adventure, transportation, utility |
| [XaeroPlus](https://modrinth.com/mod/xaeroplus) | 200 | `XaeroPlus-200+forge-1.12.2-WM1.47.0-MM26.6.0.jar` | adventure, transportation, utility |
| [Zume](https://modrinth.com/mod/zume) | 1.2.2 | `zume-1.2.2.jar` | utility |
