# Parallel legacy-API work for subagents

Every legacy API that mods reference but the shim only links to a placeholder ("Synthesized legacy APIs (link, but have
no behaviour yet)" in `checkLegacyMod` / the compatibility report) is a task. The 1.7.10 lists were generated on 2026-10-26
from the link check of the 1.7.10 test set and a 100-mod user pack, split by area so several agents can work at once,
each in its own git worktree, and sorted with the most-used APIs first:

| List | Area |
|---|---|
| `task-E.txt` | 1.7.10 entities (`net.minecraft.entity.*`, potions, damage sources, AI) |
| `task-R1.txt` | 1.7.10 rendering (`net.minecraft.client.renderer.*`, models, LWJGL) |
| `task-R2a.txt` | 1.7.10 GUI screens and FML client (`net.minecraft.client.gui`, `cpw.mods.fml.client`) |
| `task-R2b.txt` | 1.7.10 rest of the client (world client, network handler, player controller, skins, resource packs, Forge client events, settings) |
| `task-W.txt` | 1.7.10 world, chunks, biomes, world generation, villages |
| `task-I.txt` | 1.7.10 blocks, items, tile entities, inventories, crafting, enchantments, ore dictionary, fluids |
| `task-F.txt` | 1.7.10 everything else (FML, Forge, network, commands, NBT, utilities, server, stats) |
| `task-C.txt` | APIs logged at runtime as "not implemented yet" (mixes 1.7.10 and 1.12.2) |

On 2026-10-27 the area lists got a `# ---- 1.12.2` section each: 12,286 1.12.2 APIs from the link check of 201 1.12.2 jars
(120 content mods and 81 popular mods from Modrinth, listed in `tools/dev/test-mods-1.12.2.md`, downloadable with
`tools/dev/fetch_test_mods_1122.py`). Same areas, 1.12.2 package names (`net.minecraftforge.fml...` instead of
`cpw.mods.fml...`): E 3,338, R1 2,113, R2a 1,061, R2b 889, W 1,423, I 2,225, F 1,237 entries. A 1.12.2 line ends with
`# n mod(s): <jar names>; e.g. <user classes>`; implement it in `src/main/java/legacyconnector/v1_12_2/...`. The 1.12.2 client
(R1, R2a, R2b) has no rendering bridge yet, so those sections need that foundation first.
| `task-D.txt` | 1.12.2 APIs logged at runtime |

Each line is `legacy.class.member(descriptor)` (fields: `class.field:type`), followed by `# n user(s): <mod classes>`.
A descriptor class under `legacyconnector_synth/...` is itself only synthesized so far. Lines with ` -> (desc)` are
constructors that link to a nearby shim constructor today.

To refresh the lists, run `./gradlew checkLegacyMod -PlegacyJar=...` over the jars and feed the logs to a script like the
lead's `make_tasks.py` (or grep game logs for `is not implemented yet`), and drop what is already implemented.

## Rules for the agents

- Work in an isolated worktree, one list per agent. Lists are long: work from the top (most used first), commit
  every 20-40 entries so finished work survives an interruption, and skip entries that make no sense on 1.21.1 (say why).
- Never run the game, servers or clients: only one game instance may
  run on a machine, and the lead agent verifies in game after merging.
- Read `CLAUDE.md` and `README.md` first. Fix generically, never per mod.
- For each entry, check which version declares it: `loader/src/main/resources/legacyconnector/legacy/<1.7.10|1.12.2>/methods.txt`
  (MCP names, descriptors, access flags; `fields.txt` and `hierarchy.txt` alongside). Implement it in that version's shim
  (`src/main/java/legacyconnector/v1_7_10/...` or `.../v1_12_2/...`), with the real legacy behaviour mapped onto the
  modern game. The 1.21.1 sources are in the Gradle cache (NeoForge/Minecraft sources jar).
- Legacy fields that exist only as virtual fields are read through the static `mcc$get$<field>` / `mcc$set$<field>`
  accessors; `generateShimIndex` fails on "phantom field access" otherwise.
- Where a signature names a class that is only synthesized, add a minimal real shim class for it.
- Match the surrounding code: short javadoc for non-obvious mappings, the `mcc$` prefix for non-legacy helpers.
- World-generation code must read blocks through the generation region, must never throw, and must throttle its
  logging (see the gotchas in `CLAUDE.md`).
- Verify with JDK 21: `./gradlew compileJava --rerun -q`, then `./gradlew generateShimIndex -q`. Both must pass.
- Commit on the worktree branch (message style `1.7.10: <what> (<why>)` or `1.12.2: ...`, with the session trailers)
  and do not push. Report the branch, the commit, what was implemented, what was skipped and why, and any uncertainty.

## Merging (lead agent)

Merge each branch into master, rebuild (`compileJava --rerun`, `generateShimIndex`), move the translation cache aside,
run the 1.7.10 server regression (`run-subtest`, see `CLAUDE.md`) and, for client entries, a quick-shots control session.
Then push, and delete the entries that were done from these lists.
