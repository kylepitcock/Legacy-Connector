# Cache and Redistribution

Legacy Connector translates the mod jars you put in `mods/` into `.legacyconnector/cache/` the first time they are loaded. Those
translated jars are machine-made derivatives of somebody else's work, made for your own use. They are not meant to be passed on as
"updated versions" of the mods. Two measures make that evident and pointless; neither costs startup time or can stop a normal setup.

## Provenance watermark

Every translated jar carries:

- manifest attributes `LegacyConnector-Source` (original file name), `LegacyConnector-Original-Sha256`,
  `LegacyConnector-Translator-Version`, `LegacyConnector-Legacy-Version` and `LegacyConnector-Notice`;
- the resource `META-INF/legacyconnector/provenance.json`: the notice (a machine-translated derivative of the original mod, made locally
  for the user's own use, needs Legacy Connector, not for redistribution), the project URL, translator and transformer version, a
  timestamp, the original's file name and SHA-256, and the original mod ids and versions.

Nothing in the game reads these; they are only for people who find such a jar. The original SHA-256 is the same hash the cache key
uses, computed once more when a jar is translated (a few milliseconds).

## Per-installation signature

On first use Legacy Connector creates a random 256-bit key in the game folder, `.legacyconnector/installation.key`. Nothing is written
outside the instance (0.3.11 kept the key in the user's data folder, which store malware scanners flag; CurseForge held that release for
manual review). A translated jar taken out of the cache on its own does not verify on another installation; copying a whole instance
copies its key, so its cache stays valid. If the key cannot be written, a key for the current run is used and the jars are translated
again next start.

When a jar is translated it gets a sidecar `<jar>.sig`: an HMAC-SHA256 over the jar's size, a content digest computed once when the
jar was written, the digest of its last 64 KiB (the zip directory), the original's SHA-256 and the translator version. On every start the
loader checks each cache entry against its sidecar using only the sidecar, the file size and those last 64 KiB: the jar is never hashed
again. An entry without a valid signature is deleted and translated again from the original, with one INFO line
(`cache entry invalid for this installation, re-translating`). The loader also logs how long the check took (a few milliseconds).

A translated jar found in `mods/` (carrying the watermark) is not loaded: it is claimed and a warning names it. Put the original
mod there instead.

### What this means for you

- Copying a whole instance (or its `.legacyconnector/cache`) to **another machine** translates everything once there. The originals in
  `mods/` are all that has to be copied.
- **Moving** an instance on the same machine, or renaming its folder, changes nothing: the key is per user, not per path.
- Deleting the key, or reinstalling the OS user, translates once again. Nothing breaks.
- Dedicated servers, CI and the `run-*` test folders create the key on demand; nothing has to exist beforehand. All instances of one
  user share the key, so they also share (and may copy between each other) valid cache entries.
- A cache folder that cannot be modified (read-only) keeps working: an entry that fails verification but cannot be deleted is used as it is.
- `-Dlegacyconnector.cacheSignatures=false` switches signing and checking off (development, measuring).

The signature is a deterrent and an integrity marker, not copy protection: anyone can still translate the original themselves, which is
the intended way.

## Not done: private file extension for cache entries

Storing translated jars as `<hash>.lcjar` was evaluated and rejected. The runtime recognises translated jars by their `.jar` suffix
(`ModernizedTextures.isTranslated`), the `.links.txt`/`.synth.txt`/`.sig` sidecars and the split-package jars derive their names from the jar
name, `tools/compat/libraries.py` and the dev scripts read the cache, and FML derives module names from file names for jars without an
`Automatic-Module-Name`. The gain (a file that is slightly less droppable into `mods/`) does not justify the risk; the watermark plus the
refusal to load derivatives from `mods/` already covers that case.
