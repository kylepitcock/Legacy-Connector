# Cache and Redistribution

Legacy Connector translates the mod jars you put in `mods/` into `.legacyconnector/cache/` the first time they are loaded. Those
translated jars are machine-made derivatives of somebody else's work, made for your own use. They are not meant to be passed on as
"updated versions" of the mods. The watermark and the refusal to load translated jars from `mods/` make that evident and pointless; neither costs startup time or can stop a normal setup.

## Provenance watermark

Every translated jar carries:

- manifest attributes `LegacyConnector-Source` (original file name), `LegacyConnector-Original-Sha256`,
  `LegacyConnector-Translator-Version`, `LegacyConnector-Legacy-Version` and `LegacyConnector-Notice`;
- the resource `META-INF/legacyconnector/provenance.json`: the notice (a machine-translated derivative of the original mod, made locally
  for the user's own use, needs Legacy Connector, not for redistribution), the project URL, translator and transformer version, a
  timestamp, the original's file name and SHA-256, and the original mod ids and versions.

Nothing in the game reads these; they are only for people who find such a jar. The original SHA-256 is the same hash the cache key
uses, computed once more when a jar is translated (a few milliseconds).

## No per-installation signature

0.3.11 and 0.3.12 also signed every cache entry with a random per-installation key (HMAC-SHA256, `.legacyconnector/installation.key`), so a
translated jar taken out of the cache did not verify elsewhere. 0.3.13 removed it: a random key file plus cryptography over jar files is
what store malware scanners flag, and every release with it went to manual review on CurseForge and Modrinth. The watermark above already
marks a passed-on jar as a local derivative, and the refusal below makes it useless in `mods/`. Leftover `installation.key` and `.sig`
files of those versions are ignored.

A translated jar found in `mods/` (carrying the watermark) is not loaded: it is claimed and a warning names it. Put the original
mod there instead.


## Not done: private file extension for cache entries

Storing translated jars as `<hash>.lcjar` was evaluated and rejected. The runtime recognises translated jars by their `.jar` suffix
(`ModernizedTextures.isTranslated`), the `.links.txt`/`.synth.txt`/`.sig` sidecars and the split-package jars derive their names from the jar
name, `tools/compat/libraries.py` and the dev scripts read the cache, and FML derives module names from file names for jars without an
`Automatic-Module-Name`. The gain (a file that is slightly less droppable into `mods/`) does not justify the risk; the watermark plus the
refusal to load derivatives from `mods/` already covers that case.
