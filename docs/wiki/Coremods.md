# Coremods

Legacy coremods come in two kinds. Coremods that patch *other mods* run as they did (their transformers see the legacy mod classes before translation; see
[Architecture](Architecture.md)). This page is about the second kind, coremods that patch *Minecraft's own classes* (Fisk's Superheroes, Legends,
AppleCore, Quark, RandomThings, Betweenlands, ...). Those classes do not exist at run time (the shim replaces them), so the patch itself cannot run. The layer
replays what the patch did instead. State: experimental; it is keyed by Minecraft method, never by mod.

## How it works

1. **Record (loader, on a cache-miss translation).** Each coremod's ASM transformer is run against its own Minecraft's original classes (the official client
   jar, see `coremods.vanillaJar` and `coremods.downloadVanilla` in [Configuration](Configuration.md); without that jar nothing is recorded). The loader diffs
   every method before and after and records each static hook call the transformer inserted, with a **site key**
   (`<class>.<method><desc>#head-cancel | tail | const:<v> | replace:<call> | after:<insn> | cancel-after:<insn>`), in
   `.legacyconnector/cache/coremod-hooks-<mc>.txt`. Classes, fields and methods a transformer *adds* are recorded in `coremod-structure-<mc>.txt`.
2. **Replay (runtime).** A **place** is a generic mixin or event at the equivalent spot of 1.21.1 that is a no-op unless a hook is listed for its site key.
   When one is, the hook method of the translated mod is bound (parameters by type, to the values the place offers) and applied as cancel, notify, modify or
   replace. A helper of the mod's own at the site (`(ZZ)Z`, `(FF)F`...) combines the hook's answer with the vanilla value.
   - 1.7.10 places: `CoremodSites` (with `CoremodHooks` for the DragonAPI crafting and smelting hooks, which live under `compat/`).
   - 1.12.2 places: `CoremodSites1122` (declared with 1.12.2 types on the same lists and binding).
   - 1.6.4, 1.5.2, 1.4.7: the lists are rewritten to 1.7.10 class names by `PreSeventeenKeys` (flat `net.minecraft.src` names and the forward renames) and
     replayed by the `CoremodSites` places. Only the in-bed check of OpenBlocks is hit so far (`tools/dev/agent-tasks/coremod-sites-pre17.md`).
3. **Structural additions.** An interface that a transformer adds to a Minecraft class and that is a class of a mod jar, with the instance methods the
   transformer added, becomes a generated mixin on the shim class (`CoremodAdditionMixins` writes it into the jar before translation). Added instance
   fields live in `CoremodState`, per game object (shim chunk and world views are made per lookup, so a field on the view would be lost). Not applied:
   static members, constructors, and added methods without an interface (`tools/dev/agent-tasks/coremod-structural-additions.md` has the table).
4. **Guard.** `CoremodHookGuard` reads a hook's own leading checks (`if (entity == null) return false`, `if (!(entity instanceof X)) return false`) and skips
   the call, and the work of building legacy views for it, when the hook would return at once. It exists for hot places such as collision boxes.
5. **Check.** The hook mixins inject with `require = 0` (a pack without the mods must not fail), so a mixin whose target no longer matches is silent.
   `CoremodMixinCheck1710` and `CoremodMixinCheck1122` look for each merged handler in the target class and report the ones that are missing (client smoke
   test and the server probes below).

## Reading the log

One line at start-up (all versions together):

```
Legacy coremods put N hook(s) into their Minecraft's classes; M hook method(s) are replayed at K place(s) of the modern game, J place(s) have no equivalent yet
```

- N: static hook calls the transformers inserted (a hook counts once per version list).
- M and K: distinct hook methods bound, and the places that are active. A hook that is replayed does what it did; "applies" in the state files means the
  mixin applies and the hook is called, not that every effect was watched in play.
- J: site keys with no place. Features of those mods that depend on them do nothing; the rest of the mod loads. Set
  `-Dlegacyconnector.coremodHooks.report=full` (Gradle: `-PcoremodReport`) to list every `no place yet` site with its hook methods; without it the list is
  at debug level. "A coremod hook failed" is a warning with the stack trace (at most three per place): a hook threw and the call was skipped.

Measured on the local 1.12.2 set (AppleCore, AutoRegLib, Charm, Quark, RandomThings, server and client): 103 hooks, 53 hook methods at 63 places, 32 places
without equivalent, no hook failure (`tools/dev/agent-tasks/coremod-sites-1122.md`). Fisk's Heroes on a server: 44 hooks visible, 34 hook methods replayed,
one place without equivalent. Counts of the other sets are in `coremod-sites-1710.md` and `coremod-sites-pre17.md`.

## What cannot be replayed

Each class below is a reason for "no equivalent"; examples are real sites.

- **Hook hands back or reads a legacy object that the modern game has no counterpart of.** A `DamageSource` subclass (Quark `createPlayerDamage`; the
  modern one is a final class from a registry type), AppleCore's event objects whose fields replace constants, a `BlockPistonStructureHelper` subclass, a
  player object built at login (Betweenlands), the legacy `NBTTagList` of enchantments (modern enchantments are data components).
- **Legacy GL state and vertex buffers.** Hooks that rotate the modelview matrix (Quark `renderNausea`), pose the legacy player model, draw at the matrix of
  a vanilla renderer, or write vertex colours into the chunk mesher's buffer (coloured lights, RandomThings `renderBlock`). The modern renderers use a
  `PoseStack` and other vertex formats, and chunk meshing is usually Sodium's.
- **Post-generation edits of base game structures.** Hooks that edit a legacy village church or the ocean monument after it was built (RandomThings, Charm
  `addComponentParts` events). Modern pieces are jigsaw template pieces; the project's rule is native extension points, not fills.
- **The transformer replaces or empties a whole method**, or rewrites code inline without a static hook call. There is nothing to call (Betweenlands'
  click handlers, the NEI and Xcraft transformers of 1.6.4/1.4.7).
- **Per-packet interception.** SpaceCore's `Packet.processPacket` heads (93 sites): legacy packets travel in the layer's own payload, vanilla ones are
  modern types.
- **Engine fixes the modern game does natively.** CoFH Core's stacking, collision-box and texture-tick patches; the 1.7.10 engine problem no longer exists.
- **Pre-1.7 only methods** such as `Block.<init>` with the numeric id (an id clash reporter): the id facade has no clashes to report.
- **Data model differences** where the hook edits legacy data (the enchantment list above, the peaceful regeneration constants as event objects).

The per-site reasons are in the state files, so a site found there is not worth re-investigating without new information.

## Adding a place

1. Find the site: `./gradlew probeCoremodHooks -PlegacyJar=a.jar,b.jar [-Psites=true] [-PvanillaJar=client-<mc>.jar] [-PsrgZip=srg.zip]` lists what each
   transformer changes (added static members are marked). The `-PcoremodReport` server log lists what the running game could not place.
2. Find the equivalent spot of 1.21.1 and write a mixin there (`src/main/java/org/kylepitcock/legacyconnector/mixin`, injected with `require = 0`; its
   handler is named `legacyconnector$coremod...`). The handler asks the site (`SITE.listed()`) before doing any work, so a pack without the hook pays one
   boolean read, then calls the site's apply method with the values it has.
3. Declare the site in `CoremodSites` (1.7.10 key) or `CoremodSites1122` (1.12.2 key): `CoremodSites.site(label, Class<?>[] offered, keys...)` names the
   values the hook parameters are bound to and the keys it is listed under. `Site.returns(type)`, `Site.plain()` (keeps a `(II)I` hook from being taken for a value
   joiner) and `Site.named(constant)` (a hook parameter of the mod's own enum type gets the constant the place names) cover the usual shapes; a key ending in
   `=n` addresses the nth occurrence of a repeated instruction.
4. Add the handler to the list of `CoremodMixinCheck1710` or `CoremodMixinCheck1122` so a target that stops matching is reported.
5. Check offline, then on a server (see [Development](Development.md#coremod-probes)): every mixin applies, no "A coremod hook failed", and the effect where a
   server can drive it. Write the result into the matching state file.
6. If the list format or a key changes, bump `TRANSFORMER_VERSION` so cached lists are rewritten.

Rules: the place implements a Minecraft behaviour for any hook listed there, never a mod's behaviour; a hook that needs a mod's own classes is called, not
copied. Plugins are constructed at the first mod construct, before any vanilla `Items` exist, as FML did before Bootstrap.

## Limits

Forge's binary patches are not applied before the transformers run, so a transformer that looks for a Forge-added instruction finds nothing. Whole-class
replacements are not handled. A cached list is only as good as the jar it was recorded from: after changing the vanilla jar or the transformer, delete
`.legacyconnector/cache`.
