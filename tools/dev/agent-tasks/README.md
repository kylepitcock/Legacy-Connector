# Parallel legacy-API work for subagents

The game logs every legacy API a mod calls that the shim does not implement yet ("Legacy API X is not implemented yet;
using a default"). These lists were collected from recent client and server logs (2026-10-26) and split by area so
several agents can implement them at once, each in its own git worktree:

| List | Area |
|---|---|
| `task-A.txt` | entities (`net.minecraft.entity.*`) and client/rendering (`net.minecraft.client.*`, LWJGL, OpenGlHelper shaders) |
| `task-B.txt` | world and world generation (`net.minecraft.world.*`, terrain-gen events, structure pieces) |
| `task-C.txt` | everything else (items, blocks, FML/Forge registries and config, network, commands, Waila); mixes 1.7.10 and 1.12.2 entries |

Each line is `legacy.class.method(descriptor)`. A descriptor class under `legacyconnector_synth/...` is itself only
synthesized so far.

To refresh the lists, grep the logs (client `run-screenshots/logs`, server `run-subtest/logs`, a user instance's
`logs/latest.log`) for `is not implemented yet` and drop what is already implemented.

## Rules for the agents

- Work in an isolated worktree, one list per agent. Never run the game, servers or clients: only one game instance may
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
