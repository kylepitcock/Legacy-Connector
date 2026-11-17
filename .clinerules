# Legacy Connector: instructions for this assistant

This repository runs unmodified legacy Forge/ModLoader mods on NeoForge 1.21.1 by translating their jars at load time.

Before proposing or making any change, read these two files in the repository root, in this order, and follow them:

1. `AGENTS.md`: how the project is developed and tested (design rules, the fix loop, test tiers, merging, issue etiquette, the report format).
2. `CLAUDE.md`: the project's full instruction file for every AI agent, whatever its vendor (roadmap order, build and test commands, per-version notes, release rules).

Rules that are never broken:

- Fix generically: implement the missing legacy API in the shim, bridges or loader. Code keyed by a mod's name goes only under a `compat/` package (`tools/dev/check_mod_specific.py` enforces it).
- Use native extension points: legacy content enters the game the way a 1.21.1 NeoForge mod would add it (placed features and biome modifiers, loot tables, data pack recipes, capabilities, extra pieces of the vanilla village). No post-generation edits, no result-overriding mixins.
- Run the mod's own callbacks; a legacy handler that throws is logged once and skipped, never crashes the game.
- Use JDK 21. Run `tools/dev/gate.sh` before calling work done; run the regression suites (`tools/dev/verify.sh`) for the versions you touched. Delete `run-*/.legacyconnector/cache` after loader or shim changes.
- Never commit or redistribute mod jars. Do not push to master, cut releases, delete branches or post on GitHub unless the maintainer asked you to.
- Say plainly what you tested and what you did not. A link check or server run does not prove client behavior.
