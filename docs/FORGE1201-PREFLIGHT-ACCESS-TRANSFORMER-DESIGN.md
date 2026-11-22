# Forge 1.20.1 Access Transformer Preflight Inventory

## Purpose

Add a deterministic, read-only inventory for the Access Transformer (AT) resource that Forge 1.20.1 actually discovers in a production mod JAR. AT directives affect access flags and can be as compatibility-critical as bytecode member references. They must be reported as independent source metadata, not silently treated as ordinary method/field references.

## Discovery boundary

Forge's production discovery convention is the exact outer-JAR path:

`META-INF/accesstransformer.cfg`

The preflight inventory deliberately does not infer alternate development-only source paths from a built JAR. The current slice also does not search inside nested JARs; those remain an explicit limitation, not an implicit "no AT found" result for the dependency closure.

The analyzer reads the exact ZIP entry and never applies the directives to any class.

## Parsed grammar

The parser inventories:
- visibility directives: `public`, `protected`, `default`, `private`;
- final-flag changes: optional `+f` or `-f` suffix on the visibility modifier;
- class targets (owner only);
- field targets (owner + member name);
- method targets (owner + member name + complete JVM method descriptor);
- wildcard field/method targets, which are retained but marked as requiring manual/symbol-level review;
- comments beginning with `#`, blank lines, malformed lines, duplicate targets and incompatible transformations.

Every non-comment directive retains its original line number and raw source line. Input is bounded to 4 MiB and 100,000 directives. UTF-8 is decoded strictly; oversized and invalid-encoding resources are explicit result statuses.

## Namespace evidence

A Minecraft AT directive is not one homogeneous namespace selector:
- Minecraft class owners are written in Mojang-official class names.
- Minecraft method and field names must use Forge SRG names.
- Object types embedded in JVM method descriptors use the source official class-name namespace.

The report therefore records owner, member and descriptor namespace hints separately. Non-Minecraft owners are marked as declared-but-unverified; the analyzer does not guess third-party member namespaces. This separation is crucial for the future AT migration path: class/descriptor names need the source/target class mapping edge, while Minecraft member tokens need the Forge SRG member mapping edge.

## Output and interpretation

The unified preflight JSON gains `accessTransformers` (schema 1.11). The result contains:
- resource path, parse status, byte count and SHA-256;
- directives with source line, modifier, visibility/final semantics, target kind, selector parts and namespace hints;
- counts of valid, wildcard and malformed directives;
- duplicate target and incompatible-change counts;
- deterministic warnings.

A duplicate target is not automatically a conflict. For example, `public` plus `public-f` on the same field can be compositional (widen access and remove final). Conflicts are reported only when visibility changes disagree or the same target is both forced final and forced non-final.

## Explicit non-goals

This slice does not:
- apply AT directives;
- validate that a class/member exists in Forge 47.3.0 or NeoForge 21.1.1;
- rewrite owners, SRG member names or descriptors;
- prove that an AT remains behaviorally valid after a member moves, changes descriptor, or changes inheritance;
- inspect nested-JAR AT resources or arbitrary development-only build paths.

The output is evidence to feed the Phase 2 namespace engine and later Phase 4 AT migration. It is not a conversion result or compatibility approval.

## Targeted validation

From a clean, isolated worktree of this feature branch:

```powershell
.\gradlew.bat :loader:test --tests org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201AccessTransformerAnalyzerTest --tests org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201PreflightAnalyzerTest --no-daemon --console=plain
```

Do not run `:loader:clean` for this validation; preserve existing local build reports and generated target/source classpath evidence.
