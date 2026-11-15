# Forge 1.20.1 source-runtime inspection workspace

This is an isolated ForgeGradle workspace used only to produce a reproducible source-side classpath for the Universal mapping audit. It is deliberately not included in the main loader project and does not add Forge 1.20.1 to the connector's runtime or packaged JAR.

## Default source baseline

- Minecraft: 1.20.1
- Forge: 47.3.0
- ForgeGradle: 6.0.24
- Mappings: Mojang official 1.20.1 names
- Java toolchain: 21

The Forge version can be overridden deliberately with a Gradle project property. Do not silently substitute the newest Forge release: the source runtime must correspond to the mod baseline being audited.

## Export the source classpath

Run from the main repository root, using the repository's Gradle wrapper:

```powershell
.\gradlew.bat -p tools\forge1201-source-runtime exportForge1201SourceRuntimeClasspath --no-daemon --console=plain
```

Build-local outputs:

- `tools/forge1201-source-runtime/build/reports/source-runtime/forge1201-classpath.txt`
- `tools/forge1201-source-runtime/build/reports/source-runtime/forge1201-classpath-manifest.json`

The export preserves Gradle classpath order, records SHA-256 hashes for file entries, and checks that the resolved classpath contains `PistonStructureResolver`, `BlockState`, and `Block`. It reports duplicate class-resource locations rather than silently choosing one.

To inspect a different Forge patch release intentionally:

```powershell
.\gradlew.bat -p tools\forge1201-source-runtime exportForge1201SourceRuntimeClasspath -Pforge_version=47.4.28 --no-daemon --console=plain
```

Keep the resulting classpath and manifest under `build/`; do not upload the classpath's JARs, mapping inputs, or derived mapping payloads.

## Scope and limitations

This workspace exports a classpath; it does not yet classify source members or modify the combined audit report. The next source-side analyzer will map each vanilla SRG selector to source Mojang owner/member/full-descriptor identity using the pinned MCPConfig joined TSRG2 and Mojang mappings, then inspect the corresponding class bytes on this exact Forge classpath.

A patch-file scan may supplement the classfile evidence, but absence of a patch file is not proof that a member exists, and presence of a class patch is not proof that a particular member was removed. The patched compiled classpath is the authority for member existence.

No selector transformation, runtime mapping table, or loader packaging change is performed here.
