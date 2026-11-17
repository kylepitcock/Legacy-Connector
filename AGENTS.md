# Legacy Connector: guide for agents and contributors

For AI agents of any vendor and for human contributors: how this project is developed and tested. Plain rules, no tool-specific syntax. **Before changing anything, also read `CLAUDE.md` in full**: despite its name it is the project's instruction file for every agent (roadmap order, build and test commands, per-version notes, release rules). `docs/wiki/Development.md` has the long form. The same guide is mirrored for other tools in `GEMINI.md`, `.github/copilot-instructions.md`, `.cursorrules`, `.windsurfrules` and `.clinerules`; edit `AGENTS.md` and keep those pointing here.

## What the project is

Legacy Connector runs unmodified old Forge/ModLoader mods on NeoForge 1.21.1 by translating their jars when the game starts. Three parts:
- **loader**: reads a legacy jar, renames and rewrites its bytecode, caches the result.
- **shim**: hand-written copies of the old Minecraft/Forge classes (`src/main/java/legacyconnector/v<version>`) whose methods call the modern game.
- **bridges**: turn legacy registrations into modern ones (blocks, items, recipes, world generation, capabilities).

## Design rules (a change that breaks one is rejected)

1. **Fix generically.** Implement the missing legacy API so every mod that uses it works. Code keyed by a mod's name is allowed only under a `compat/` package, when no general fix exists and the mod is popular. A checker enforces this (`tools/dev/check_mod_specific.py`).
2. **Use native extension points.** Legacy content enters the game the way a 1.21.1 mod would add it: ores as placed features plus biome modifiers, village buildings as extra pieces of the vanilla village, chest loot as loot tables, recipes as data packs, containers as capabilities. No post-generation edits, no mixins that override results.
3. **Run the mod's own code.** Call the mod's original callbacks; do not re-implement a mod's behavior by hand.
4. **Never crash the host.** A legacy generator, renderer or handler that throws is logged once (throttled) and skipped.
5. **Real behavior, not empty stubs.** If a member can only be a no-op, say so and why in the commit and report.

## How a defect gets fixed

1. Read the report and its `latest.log` / `legacyconnector-report.txt`. Get the exact jar (downloading test jars is allowed; never commit or redistribute them; keep them in gitignored `test-mods-issues/<issue>/`).
2. Find the cause in the mod's bytecode (`javap`), not by guessing: which legacy member does it call, and what does the layer answer?
3. Fix the layer at that member. Look for the defect class, not the one symptom (example: one mod's `Math.abs` on a height exposed every "Y is never negative" assumption; one missing `xpCooldown` field broke every legacy XP-style orb).
4. Verify at the lowest tier that can show it (below), then one tier up if the defect is visible only there.
5. Commit on your own branch. Say what was run and what was not.

## Test tiers, cheapest first

| Tier | Command | Shows |
|---|---|---|
| Gate | `tools/dev/gate.sh` | compiles, shim index has no phantom field access, checks pass |
| Offline link check | `./gradlew checkLegacyMod -PlegacyJar=a.jar,b.jar` | every reference of the jars resolves; converted assets have their textures and models |
| Probe | `tools/compat/probe.py <dir> --mc <version>` | link result for a whole folder of jars; feeds the compatibility site |
| Regression suites | `tools/dev/verify.sh <suite ...>` | headless server self test of a named jar set, compared with `tools/dev/verify/baseline.json` |
| Scripted server check | `./gradlew runServerSubTest -PsubTestDir=<dir>` plus `tools/dev/js.sh` | a specific behavior with a fake player or a world scan |
| Real client | `tools/dev/quick-shots.sh`, `tools/dev/verify-client.sh` | rendering, GUIs, JEI, anything visible; performance mods (Sodium etc.) are on by default |

Things that bite:
- Use JDK 21.
- The translation cache (`run-*/.legacyconnector/cache`) hides loader and shim changes: delete it, and bump `TRANSFORMER_VERSION` when loader output changes.
- When several servers run on one machine give each its own ports (`VERIFY_PORT_BASE=<n>`); a clash looks like every metric being 0.
- Never compile in a checkout while a game started from it is running.
- A passing link check or server suite proves loading, not behavior. Client defects need a client.

## Merging

- One branch per task, in its own git worktree. Merge commits, no rebases, no force pushes.
- master is protected and feeds an automatic nightly build. A change goes to master only after the gate and the suites of the versions it touches pass on the merged result.
- Client-visible changes that were never run in a client are held back or labeled as untested; they are not announced as fixed.
- Generated site data is pushed by workflows to the `site-data` branch, not to master.

## Issues and reports

- Every unfixed problem gets a GitHub issue. Reply to every reporter comment.
- Each reply states: the cause in plain words, what was changed, exactly what was tested (server, client, or not at all), and which build to retest. Comments written by an AI say so.
- Close an issue only when the reporter confirmed it or nothing in it is broken; silence is not confirmation.

## How another agent can help most

- Pick one open issue or one row of the ranked gap lists in `tools/dev/agent-tasks/` (start with `BRIEF.md`); do not start a later Minecraft version before the earlier one is stable (order in `CLAUDE.md`).
- Bring evidence: the bytecode that shows the cause, the suite table, log lines, screenshots (full-colour JPEG) for client work.
- Report in this shape: branch and commits; cause; fix; what was run with results; what is unverified; how to check it in game.
- Say "not tested" plainly. An honest partial result is more useful than a confident guess.
- Do not push, release, delete branches or comment on issues unless the maintainer gave you that role.

