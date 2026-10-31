# Performance

Back to [Home](Home.md).

Measured on 2026-10-25 (0.2.7 plus fixes) in a real client: NeoForge 1.21.1, a 640x360 window, render distance 8, vsync off, the
same seed and spot. Each setup ran three times, each with a fresh world:

| | No legacy mods | Thaumcraft 4 (with Baubles, bridged to Curios) |
|---|---|---|
| Heap in use after a full GC | 595, 600, 596 MB | 640, 633, 632 MB (about +38 MB) |
| Process private memory | 2831, 3334, 2844 MB | 2999, 3044, 2865 MB (same range) |
| Frames per second (30 s average) | 1038, 1181, 1494 | 960, 1463, 955 |
| Server tick time | 1.46, 1.68, 1.95 ms | 1.58, 1.82, 1.69 ms |
| Extra startup | none | about 5.5 s to load Thaumcraft (45 blocks, 153 items, 148 recipes) on every launch; translating the jar adds about 0.5 s on the first launch only (the result is cached) |

The only consistent difference is memory: Thaumcraft adds about 38 MB of heap. Frame rate and tick time vary more between
identical runs than between the two setups, so no cost shows at this resolution. The harness that takes these
measurements is part of Legacy Connector, so "no legacy mods" means Legacy Connector loaded with nothing to translate,
not a NeoForge install without it. Translation costs about 0.4 to 0.7 s per large jar (AgriCraft, Applied Energistics 2,
Botania) on first launch.
