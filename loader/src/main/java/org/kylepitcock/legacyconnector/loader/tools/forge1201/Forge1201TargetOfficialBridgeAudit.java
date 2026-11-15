package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipFile;

import org.kylepitcock.legacyconnector.loader.mapping.Forge1201SrgIntermediaryBridgeResolver;
import org.kylepitcock.legacyconnector.loader.mapping.Forge1201TargetOfficialMappingIndex;

/**
 * Read-only composition of the Forge SRG -> target official-obfuscated -> target Mojang-official
 * edges, followed by structural verification against the resolved NeoForge compile classpath.
 *
 * <p>This report is evidence only. It never rewrites a selector, emits a runtime mapping table,
 * defines target classes, or claims semantic/gameplay compatibility.</p>
 */
public final class Forge1201TargetOfficialBridgeAudit {
    private Forge1201TargetOfficialBridgeAudit() {}

    public record ClassCandidateAudit(
            String targetOfficialObfuscatedName,
            String targetOfficialMappingStatus,
            String targetOfficialName,
            String targetClasspathStatus) {}

    public record ClassAudit(
            String srgName,
            String upstreamStatus,
            String sourceOfficialObfuscatedName,
            String sourceIntermediaryName,
            String targetIntermediaryName,
            String targetOfficialObfuscatedName,
            List<ClassCandidateAudit> candidates,
            String status) {
        public ClassAudit {
            candidates = List.copyOf(candidates);
        }
    }

    public record MemberCandidateAudit(
            String sourceIntermediaryOwner,
            String sourceIntermediaryId,
            String sourceIntermediaryDescriptor,
            String targetIntermediaryOwner,
            String targetIntermediaryDescriptor,
            String targetOfficialObfuscatedOwner,
            String targetOfficialObfuscatedName,
            String targetOfficialObfuscatedDescriptor,
            int upstreamExternalDescriptorTypeCount,
            String targetOfficialMappingStatus,
            String targetOfficialOwner,
            String targetOfficialName,
            String targetOfficialDescriptor,
            int targetExternalDescriptorTypeCount,
            String targetApiStatus,
            List<String> hierarchyVisited) {
        public MemberCandidateAudit {
            hierarchyVisited = List.copyOf(hierarchyVisited);
        }
    }

    public record MemberAudit(
            String srgOwner,
            String srgName,
            String srgDescriptor,
            String upstreamStatus,
            String status,
            List<MemberCandidateAudit> candidates) {
        public MemberAudit {
            candidates = List.copyOf(candidates);
        }
    }

    public record Report(
            Forge1201TargetOfficialMappingIndex.Summary targetOfficialMappingSummary,
            List<ClassAudit> classes,
            List<MemberAudit> methods,
            List<MemberAudit> fields,
            Map<String, Integer> classCounts,
            Map<String, Integer> methodCounts,
            Map<String, Integer> fieldCounts) {
        public Report {
            classes = List.copyOf(classes);
            methods = List.copyOf(methods);
            fields = List.copyOf(fields);
            classCounts = Map.copyOf(classCounts);
            methodCounts = Map.copyOf(methodCounts);
            fieldCounts = Map.copyOf(fieldCounts);
        }
    }

    public static Report audit(
            Forge1201SrgIntermediaryBridgeResolver.Index upstream,
            Forge1201TargetOfficialMappingIndex.Index targetOfficial,
            Forge1201TargetApiVerifier.TargetIndex targetApi) throws IOException {
        if (upstream == null || targetOfficial == null || targetApi == null) {
            throw new IllegalArgumentException("All three mapping and target API indexes are required");
        }

        List<ClassAudit> classes = new ArrayList<>();
        for (var selector : upstream.classSelectors()) {
            classes.add(resolveClass(selector, upstream, targetOfficial, targetApi));
        }

        List<MemberAudit> methods = new ArrayList<>();
        for (var selector : upstream.methodSelectors()) {
            methods.add(resolveMethod(selector, upstream, targetOfficial, targetApi));
        }

        List<MemberAudit> fields = new ArrayList<>();
        for (var selector : upstream.fieldSelectors()) {
            fields.add(resolveField(selector, upstream, targetOfficial, targetApi));
        }

        return new Report(
                targetOfficial.summary(),
                classes,
                methods,
                fields,
                countClasses(classes),
                countMembers(methods),
                countMembers(fields));
    }

    static ClassAudit resolveClass(
            Forge1201SrgObfuscatedBridgeAudit.ClassSelector selector,
            Forge1201SrgIntermediaryBridgeResolver.Index upstream,
            Forge1201TargetOfficialMappingIndex.Index targetOfficial,
            Forge1201TargetApiVerifier.TargetIndex targetApi) throws IOException {
        if (selector.srgName() == null) {
            return new ClassAudit(null, "SRG_CLASS_NAME_MISSING", selector.obfuscatedName(),
                    null, null, null, List.of(), "SRG_CLASS_NAME_MISSING");
        }

        var mapped = upstream.resolveClass(selector.srgName());
        if (mapped.status() != Forge1201SrgIntermediaryBridgeResolver.ClassStatus.MAPPED_EXACT) {
            return new ClassAudit(selector.srgName(), mapped.status().name(),
                    mapped.sourceOfficialObfuscatedClass(), mapped.sourceIntermediaryClass(),
                    mapped.targetOfficialObfuscatedClass(), null, List.of(), mapped.status().name());
        }

        var finalClass = targetOfficial.resolveClass(mapped.targetOfficialObfuscatedClass());
        List<ClassCandidateAudit> candidates = new ArrayList<>();
        for (var candidate : finalClass.candidates()) {
            String classpathStatus = targetApi.classInfo(candidate.officialName()).isPresent()
                    ? "TARGET_COMPILE_CLASS_FOUND" : "TARGET_COMPILE_CLASS_MISSING";
            candidates.add(new ClassCandidateAudit(
                    candidate.obfuscatedName(), finalClass.status().name(),
                    candidate.officialName(), classpathStatus));
        }

        String status = switch (finalClass.status()) {
            case MAPPED_EXACT -> candidates.getFirst().targetClasspathStatus();
            case OWNER_MISSING -> "TARGET_OFFICIAL_CLASS_MISSING";
            case OWNER_AMBIGUOUS -> "TARGET_OFFICIAL_CLASS_AMBIGUOUS";
            default -> finalClass.status().name();
        };
        return new ClassAudit(selector.srgName(), mapped.status().name(),
                mapped.sourceOfficialObfuscatedClass(), mapped.sourceIntermediaryClass(),
                mapped.targetOfficialObfuscatedClass(), mapped.targetOfficialObfuscatedClass(),
                candidates, status);
    }

    static MemberAudit resolveMethod(
            Forge1201SrgObfuscatedBridgeAudit.MethodSelector selector,
            Forge1201SrgIntermediaryBridgeResolver.Index upstream,
            Forge1201TargetOfficialMappingIndex.Index targetOfficial,
            Forge1201TargetApiVerifier.TargetIndex targetApi) throws IOException {
        var mapped = upstream.resolveMethodRow(selector);
        return resolveMember(
                selector.srgOwner(), selector.srgName(), selector.srgDescriptor(),
                mapped, targetOfficial, targetApi, true);
    }

    static MemberAudit resolveField(
            Forge1201SrgObfuscatedBridgeAudit.FieldSelector selector,
            Forge1201SrgIntermediaryBridgeResolver.Index upstream,
            Forge1201TargetOfficialMappingIndex.Index targetOfficial,
            Forge1201TargetApiVerifier.TargetIndex targetApi) throws IOException {
        var mapped = upstream.resolveFieldRow(selector);
        return resolveMember(
                selector.srgOwner(), selector.srgName(), selector.sourceDescriptor(),
                mapped, targetOfficial, targetApi, false);
    }

    private static MemberAudit resolveMember(
            String srgOwner,
            String srgName,
            String srgDescriptor,
            Forge1201SrgIntermediaryBridgeResolver.SymbolResolution upstream,
            Forge1201TargetOfficialMappingIndex.Index targetOfficial,
            Forge1201TargetApiVerifier.TargetIndex targetApi,
            boolean method) throws IOException {
        List<MemberCandidateAudit> candidates = new ArrayList<>();
        for (var candidate : upstream.candidates()) {
            String targetMappingStatus;
            String targetOwner = null;
            String targetName = null;
            String targetDescriptor = null;
            int targetExternalCount = 0;
            String apiStatus = "NOT_CHECKED";
            List<String> hierarchy = List.of();

            if (method) {
                var resolution = targetOfficial.resolveMethod(
                        candidate.targetOfficialObfuscatedOwner(),
                        candidate.targetOfficialObfuscatedName(),
                        candidate.targetOfficialObfuscatedDescriptor());
                targetMappingStatus = resolution.status().name();
                if (resolution.candidates().size() == 1
                        && isTargetMapped(resolution.status())) {
                    var mapped = resolution.candidates().getFirst();
                    targetOwner = mapped.officialOwner();
                    targetName = mapped.officialName();
                    targetDescriptor = mapped.officialDescriptor();
                    targetExternalCount = mapped.externalDescriptorTypeCount();
                    var lookup = lookupMethod(targetApi, targetOwner, targetName, targetDescriptor);
                    apiStatus = lookup.status();
                    hierarchy = lookup.hierarchyVisited();
                }
            } else {
                var resolution = targetOfficial.resolveField(
                        candidate.targetOfficialObfuscatedOwner(),
                        candidate.targetOfficialObfuscatedName(),
                        candidate.targetOfficialObfuscatedDescriptor());
                targetMappingStatus = resolution.status().name();
                if (resolution.candidates().size() == 1
                        && isTargetMapped(resolution.status())) {
                    var mapped = resolution.candidates().getFirst();
                    targetOwner = mapped.officialOwner();
                    targetName = mapped.officialName();
                    targetDescriptor = mapped.officialDescriptor();
                    targetExternalCount = mapped.externalDescriptorTypeCount();
                    var lookup = lookupField(targetApi, targetOwner, targetName, targetDescriptor);
                    apiStatus = lookup.status();
                    hierarchy = lookup.hierarchyVisited();
                }
            }

            candidates.add(new MemberCandidateAudit(
                    candidate.sourceIntermediaryOwner(),
                    candidate.sourceIntermediaryId(),
                    candidate.sourceIntermediaryDescriptor(),
                    candidate.targetIntermediaryOwner(),
                    candidate.targetIntermediaryDescriptor(),
                    candidate.targetOfficialObfuscatedOwner(),
                    candidate.targetOfficialObfuscatedName(),
                    candidate.targetOfficialObfuscatedDescriptor(),
                    candidate.externalDescriptorTypeCount(),
                    targetMappingStatus,
                    targetOwner,
                    targetName,
                    targetDescriptor,
                    targetExternalCount,
                    apiStatus,
                    hierarchy));
        }

        String status = upstream.status().name();
        if (isUpstreamMapped(upstream.status())) {
            if (candidates.size() != 1) {
                status = "UPSTREAM_CANDIDATE_MULTIPLICITY_UNEXPECTED";
            } else {
                status = candidates.getFirst().targetApiStatus();
                if ("NOT_CHECKED".equals(status)) {
                    status = "TARGET_OFFICIAL_MAPPING_" + candidates.getFirst().targetOfficialMappingStatus();
                }
                if (isTargetApiFound(status)
                        && (upstream.status()
                                == Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES
                            || candidates.getFirst().upstreamExternalDescriptorTypeCount() > 0
                            || candidates.getFirst().targetExternalDescriptorTypeCount() > 0)) {
                    status = "TARGET_API_MEMBER_FOUND_WITH_EXTERNAL_TYPES";
                }
            }
        }

        return new MemberAudit(srgOwner, srgName, srgDescriptor, upstream.status().name(), status, candidates);
    }

    private static Forge1201TargetApiVerifier.MemberLookup lookupMethod(
            Forge1201TargetApiVerifier.TargetIndex targetApi,
            String owner, String name, String descriptor) throws IOException {
        if (targetApi.classInfo(owner).isEmpty()) {
            return new Forge1201TargetApiVerifier.MemberLookup(
                    Forge1201TargetApiVerifier.LookupStatus.MISSING, List.of());
        }
        return targetApi.lookupMethod(owner, name, descriptor);
    }

    private static Forge1201TargetApiVerifier.MemberLookup lookupField(
            Forge1201TargetApiVerifier.TargetIndex targetApi,
            String owner, String name, String descriptor) throws IOException {
        if (targetApi.classInfo(owner).isEmpty()) {
            return new Forge1201TargetApiVerifier.MemberLookup(
                    Forge1201TargetApiVerifier.LookupStatus.MISSING, List.of());
        }
        return targetApi.lookupField(owner, name, descriptor);
    }

    private static boolean isUpstreamMapped(
            Forge1201SrgIntermediaryBridgeResolver.SymbolStatus status) {
        return status == Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.MAPPED_EXACT
                || status == Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES;
    }

    private static boolean isTargetMapped(Forge1201TargetOfficialMappingIndex.Status status) {
        return status == Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT
                || status == Forge1201TargetOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES;
    }

    private static boolean isTargetApiFound(String status) {
        return Forge1201TargetApiVerifier.LookupStatus.FOUND.name().equals(status);
    }

    private static Map<String, Integer> countClasses(List<ClassAudit> values) {
        Map<String, Integer> counts = new TreeMap<>();
        values.forEach(value -> counts.merge(value.status(), 1, Integer::sum));
        return Map.copyOf(counts);
    }

    private static Map<String, Integer> countMembers(List<MemberAudit> values) {
        Map<String, Integer> counts = new TreeMap<>();
        values.forEach(value -> counts.merge(value.status(), 1, Integer::sum));
        return Map.copyOf(counts);
    }

    public static void writeReport(
            Path output,
            Report report,
            Path joinedTsrg,
            Path sourceTiny,
            Path targetTiny,
            Path targetOfficialTsrg,
            Path classpathList,
            List<Path> targetClasspath) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter out = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            out.write("{\n  \"schemaVersion\": 1,\n");
            property(out, "status", "READ_ONLY_STRUCTURAL_AUDIT", true);
            property(out, "sourceNamespace", "minecraft:forge-srg@1.20.1", true);
            property(out, "sourceOfficialNamespace", "minecraft:official-obfuscated@1.20.1", true);
            property(out, "sourceIntermediaryNamespace", "minecraft:fabric-intermediary@1.20.1", true);
            property(out, "targetIntermediaryNamespace", "minecraft:fabric-intermediary@1.21.1", true);
            property(out, "targetOfficialObfuscatedNamespace", "minecraft:official-obfuscated@1.21.1", true);
            property(out, "targetCompileNamespace", "minecraft:mojang-official@1.21.1", true);
            out.write("  \"selectorTransformationPerformed\": false,\n");
            out.write("  \"runtimeMappingTableProduced\": false,\n");
            out.write("  \"targetClassesDefinedOrInitialized\": false,\n");
            out.write("  \"artifacts\": {\n");
            artifact(out, "mcpconfigJoinedTsrg", joinedTsrg, true);
            artifact(out, "fabricIntermediary1201Tiny", sourceTiny, true);
            artifact(out, "fabricIntermediary1211Tiny", targetTiny, true);
            artifact(out, "targetOfficialMergedTsrg2", targetOfficialTsrg, true);
            artifact(out, "targetClasspathList", classpathList, false);
            out.write("  },\n");
            out.write("  \"targetCompileClasspath\": [\n");
            for (int i = 0; i < targetClasspath.size(); i++) {
                Path entry = targetClasspath.get(i);
                out.write("    {\"index\":" + i + ",\"path\":" + quote(entry.toString())
                        + ",\"sizeBytes\":" + Files.size(entry)
                        + ",\"sha256\":" + quote(sha256(entry)) + "}");
                out.write(i + 1 == targetClasspath.size() ? "\n" : ",\n");
            }
            out.write("  ],\n");
            out.write("  \"targetOfficialMappingSummary\": {"
                    + "\"inputBytes\":" + report.targetOfficialMappingSummary().inputBytes()
                    + ",\"sha256\":" + quote(report.targetOfficialMappingSummary().sha256())
                    + ",\"classRows\":" + report.targetOfficialMappingSummary().classRows()
                    + ",\"methodRows\":" + report.targetOfficialMappingSummary().methodRows()
                    + ",\"fieldRows\":" + report.targetOfficialMappingSummary().fieldRows()
                    + ",\"ambiguousClassKeys\":" + report.targetOfficialMappingSummary().ambiguousClassKeys()
                    + ",\"ambiguousMethodKeys\":" + report.targetOfficialMappingSummary().ambiguousMethodKeys()
                    + ",\"ambiguousFieldKeys\":" + report.targetOfficialMappingSummary().ambiguousFieldKeys()
                    + "},\n");
            out.write("  \"coverage\": {\n");
            writeCounts(out, "classes", report.classes().size(), report.classCounts(), true);
            writeCounts(out, "methods", report.methods().size(), report.methodCounts(), true);
            writeCounts(out, "fields", report.fields().size(), report.fieldCounts(), false);
            out.write("  },\n  \"symbols\": {\n    \"classes\": [\n");
            for (int i = 0; i < report.classes().size(); i++) {
                writeClass(out, report.classes().get(i));
                out.write(i + 1 == report.classes().size() ? "\n" : ",\n");
            }
            out.write("    ],\n    \"methods\": [\n");
            for (int i = 0; i < report.methods().size(); i++) {
                writeMember(out, report.methods().get(i));
                out.write(i + 1 == report.methods().size() ? "\n" : ",\n");
            }
            out.write("    ],\n    \"fields\": [\n");
            for (int i = 0; i < report.fields().size(); i++) {
                writeMember(out, report.fields().get(i));
                out.write(i + 1 == report.fields().size() ? "\n" : ",\n");
            }
            out.write("    ]\n  },\n");
            out.write("  \"limitations\": [\n");
            line(out, "    \"Read-only structural evidence; no bytecode or selector was transformed.\",");
            line(out, "    \"Ambiguous upstream target candidates remain ambiguous even when candidate checks differ.\",");
            line(out, "    \"External descriptor types remain explicitly non-exact.\",");
            line(out, "    \"Target hierarchy lookups reuse one Forge1201TargetApiVerifier.TargetIndex per audit.\",");
            line(out, "    \"A structural match does not prove binary, semantic, or gameplay compatibility.\"");
            out.write("  ]\n}\n");
        }
    }

    private static void writeClass(BufferedWriter out, ClassAudit value) throws IOException {
        out.write("      {\"srgName\":" + quote(value.srgName())
                + ",\"upstreamStatus\":" + quote(value.upstreamStatus())
                + ",\"sourceOfficialObfuscatedName\":" + quote(value.sourceOfficialObfuscatedName())
                + ",\"sourceIntermediaryName\":" + quote(value.sourceIntermediaryName())
                + ",\"targetIntermediaryName\":" + quote(value.targetIntermediaryName())
                + ",\"targetOfficialObfuscatedName\":" + quote(value.targetOfficialObfuscatedName())
                + ",\"status\":" + quote(value.status()) + ",\"candidates\":[");
        for (int i = 0; i < value.candidates().size(); i++) {
            var candidate = value.candidates().get(i);
            if (i > 0) out.write(",");
            out.write("{\"targetOfficialObfuscatedName\":" + quote(candidate.targetOfficialObfuscatedName())
                    + ",\"targetOfficialMappingStatus\":" + quote(candidate.targetOfficialMappingStatus())
                    + ",\"targetOfficialName\":" + quote(candidate.targetOfficialName())
                    + ",\"targetClasspathStatus\":" + quote(candidate.targetClasspathStatus()) + "}");
        }
        out.write("]}");
    }

    private static void writeMember(BufferedWriter out, MemberAudit value) throws IOException {
        out.write("      {\"srg\":{\"owner\":" + quote(value.srgOwner())
                + ",\"name\":" + quote(value.srgName())
                + ",\"descriptor\":" + quote(value.srgDescriptor())
                + "},\"upstreamStatus\":" + quote(value.upstreamStatus())
                + ",\"status\":" + quote(value.status()) + ",\"candidates\":[");
        for (int i = 0; i < value.candidates().size(); i++) {
            var candidate = value.candidates().get(i);
            if (i > 0) out.write(",");
            out.write("{\"sourceIntermediaryOwner\":" + quote(candidate.sourceIntermediaryOwner())
                    + ",\"sourceIntermediaryId\":" + quote(candidate.sourceIntermediaryId())
                    + ",\"sourceIntermediaryDescriptor\":" + quote(candidate.sourceIntermediaryDescriptor())
                    + ",\"targetIntermediaryOwner\":" + quote(candidate.targetIntermediaryOwner())
                    + ",\"targetIntermediaryDescriptor\":" + quote(candidate.targetIntermediaryDescriptor())
                    + ",\"targetOfficialObfuscatedOwner\":" + quote(candidate.targetOfficialObfuscatedOwner())
                    + ",\"targetOfficialObfuscatedName\":" + quote(candidate.targetOfficialObfuscatedName())
                    + ",\"targetOfficialObfuscatedDescriptor\":" + quote(candidate.targetOfficialObfuscatedDescriptor())
                    + ",\"upstreamExternalDescriptorTypeCount\":" + candidate.upstreamExternalDescriptorTypeCount()
                    + ",\"targetOfficialMappingStatus\":" + quote(candidate.targetOfficialMappingStatus())
                    + ",\"targetOfficialOwner\":" + quote(candidate.targetOfficialOwner())
                    + ",\"targetOfficialName\":" + quote(candidate.targetOfficialName())
                    + ",\"targetOfficialDescriptor\":" + quote(candidate.targetOfficialDescriptor())
                    + ",\"targetExternalDescriptorTypeCount\":" + candidate.targetExternalDescriptorTypeCount()
                    + ",\"targetApiStatus\":" + quote(candidate.targetApiStatus())
                    + ",\"hierarchyVisited\":" + stringArray(candidate.hierarchyVisited()) + "}");
        }
        out.write("]}");
    }

    private static void writeCounts(
            BufferedWriter out, String name, int total, Map<String, Integer> counts, boolean comma)
            throws IOException {
        out.write("    " + quote(name) + ":{\"total\":" + total + ",\"byStatus\":{");
        boolean first = true;
        for (var entry : new TreeMap<>(counts).entrySet()) {
            if (!first) out.write(",");
            first = false;
            out.write(quote(entry.getKey()) + ":" + entry.getValue());
        }
        out.write("}}" + (comma ? "," : "") + "\n");
    }

    private static void artifact(BufferedWriter out, String name, Path path, boolean comma) throws IOException {
        out.write("    " + quote(name) + ":{\"path\":" + quote(path.toAbsolutePath().normalize().toString())
                + ",\"sizeBytes\":" + Files.size(path) + ",\"sha256\":" + quote(sha256(path))
                + "}" + (comma ? "," : "") + "\n");
    }

    private static void property(BufferedWriter out, String name, String value, boolean comma) throws IOException {
        out.write("  " + quote(name) + ":" + quote(value) + (comma ? "," : "") + "\n");
    }

    private static void line(BufferedWriter out, String value) throws IOException {
        out.write(value + "\n");
    }

    private static String stringArray(List<String> values) {
        return "[" + values.stream().map(Forge1201TargetOfficialBridgeAudit::quote)
                .reduce((left, right) -> left + "," + right).orElse("") + "]";
    }

    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
                }
            }
        }
        return result.append('"').toString();
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 7) {
            throw new IllegalArgumentException("Usage: Forge1201TargetOfficialBridgeAudit <joined.tsrg> "
                    + "<source-intermediary.tiny> <target-intermediary.tiny> <target-official.tsrg> "
                    + "<target-official-sha256> <target-classpath.txt> <report.json>");
        }

        Path joinedTsrg = Path.of(args[0]).toAbsolutePath().normalize();
        Path sourceTiny = Path.of(args[1]).toAbsolutePath().normalize();
        Path targetTiny = Path.of(args[2]).toAbsolutePath().normalize();
        Path targetOfficialTsrg = Path.of(args[3]).toAbsolutePath().normalize();
        Path classpathList = Path.of(args[5]).toAbsolutePath().normalize();
        Path reportFile = Path.of(args[6]).toAbsolutePath().normalize();

        var upstream = Forge1201SrgIntermediaryBridgeResolver.readIndex(joinedTsrg, sourceTiny, targetTiny);
        var targetOfficial = Forge1201TargetOfficialMappingIndex.read(targetOfficialTsrg, args[4]);
        List<Path> targetClasspath = Forge1201TargetApiInventory.readClasspathFile(classpathList);
        URL[] urls = new URL[targetClasspath.size()];
        for (int i = 0; i < targetClasspath.size(); i++) {
            urls[i] = targetClasspath.get(i).toUri().toURL();
        }

        try (URLClassLoader targetLoader =
                     new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            Forge1201TargetApiVerifier.TargetIndex targetApi =
                    Forge1201TargetApiVerifier.createIndex(internalName ->
                            targetLoader.getResourceAsStream(internalName + ".class"));
            Report report = audit(upstream, targetOfficial, targetApi);
            writeReport(reportFile, report, joinedTsrg, sourceTiny, targetTiny,
                    targetOfficialTsrg, classpathList, targetClasspath);
        }

        System.out.printf("Forge SRG -> 1.21.1 Mojang-official -> target compile API audit: "
                        + "classes %d, methods %d, fields %d; report %s%n",
                upstream.classSelectors().size(), upstream.methodSelectors().size(),
                upstream.fieldSelectors().size(), reportFile);
        System.out.println("Read-only audit only: no selector transformation or runtime mapping table was produced.");
    }
}
