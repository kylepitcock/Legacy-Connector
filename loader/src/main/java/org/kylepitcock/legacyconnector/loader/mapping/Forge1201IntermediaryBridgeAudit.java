package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Build-local, read-only audit of vanilla symbol continuity from the MCPConfig
 * 1.20.1 obfuscated namespace through Fabric Intermediary to 1.21.1 obfuscated names.
 *
 * <p>This class does not create a runtime remapping table and never rewrites a
 * selector. It joins the {@code obf} owner/member/descriptor columns in
 * MCPConfig's TSRG2 file to the 1.20.1 Tiny v1 {@code official} namespace,
 * then checks whether the resulting intermediary class/member identities exist
 * in 1.21.1 with the same owner and descriptor. Missing, moved, ambiguous, or
 * descriptor-changed symbols are reported separately.</p>
 *
 * <p>The TSRG2 {@code srg} column and SRG member identifiers are retained only
 * as diagnostic metadata. This audit does not perform the Forge SRG selector
 * -> official-obfuscated conversion needed by an actual legacy mod selector.
 * It therefore measures candidate vanilla symbol continuity, not a complete
 * Mixin selector migration path.</p>
 *
 * <p>SRG member names are not treated as globally unique symbol identities.
 * The join always includes the obfuscated owner and, for methods, the full
 * JVM descriptor. A global SRG identifier is retained only as diagnostic
 * metadata so reuse across different intermediary symbols is visible.</p>
 */
public final class Forge1201IntermediaryBridgeAudit {
    private static final long MAX_INPUT_BYTES = 64L * 1024 * 1024;
    private static final int MAX_RECORDS = 2_000_000;
    private static final Pattern SRG_METHOD = Pattern.compile("^m_\\d+_$");
    private static final Pattern SRG_FIELD = Pattern.compile("^f_\\d+_$");

    private Forge1201IntermediaryBridgeAudit() {}

    public record ClassCoverage(
            int joinedRows,
            int sourceIntermediaryMappedRows,
            int sourceUnmappedRows,
            int targetSharedClassIds,
            int sourceClassIdsAbsentFromTarget,
            int targetOnlyClassIds) {}

    public record MemberCoverage(
            int joinedRows,
            int sourceIntermediaryMappedRows,
            int sourceUnmappedRows,
            int ambiguousSourceRows,
            int sourceOwnerUnmappedRows,
            int sourceDescriptorUnresolvedRows,
            int sourceDescriptorExternalTypeRows,
            int sourceDescriptorExternalTypeOccurrences,
            int distinctSourceSrgIds,
            int sourceSrgIdsWithMultipleIntermediarySymbols,
            int targetExactOwnerMemberDescriptorRows,
            int targetSameOwnerMemberDescriptorExternalPassthroughRows,
            int targetSameOwnerMemberDescriptorChangedRows,
            int targetSameOwnerMemberDescriptorUnresolvedRows,
            int targetDescriptorExternalTypeRows,
            int targetDescriptorExternalTypeOccurrences,
            int targetOwnerChangedRows,
            int targetMemberIdMissingRows,
            int ambiguousTargetRows) {}

    public record AuditResult(
            ClassCoverage classes,
            MemberCoverage methods,
            MemberCoverage fields,
            int sourceTinyClassRows,
            int sourceTinyMethodRows,
            int sourceTinyFieldRows,
            int targetTinyClassRows,
            int targetTinyMethodRows,
            int targetTinyFieldRows) {}

    private record TsrgClass(String obfuscatedName, String srgName) {}

    private record TsrgMember(
            String ownerObfuscatedName,
            String obfuscatedName,
            String descriptor,
            String srgId) {}

    private record TsrgData(
            List<TsrgClass> classes,
            List<TsrgMember> methods,
            List<TsrgMember> fields) {}

    private record MemberKey(String owner, String name, String descriptor) {}

    private record FieldKey(String owner, String name) {}

    private record OwnerAndId(String ownerIntermediary, String intermediaryId) {}

    private record TinyMember(
            String ownerOfficial,
            String ownerIntermediary,
            String descriptorOfficial,
            String descriptorIntermediary,
            int descriptorExternalTypeCount,
            String officialName,
            String intermediaryId) {}

    private record DescriptorNormalization(String descriptor, int externalTypeCount) {}

    private static final class TinyMappings {
        final Map<String, String> officialToIntermediary = new HashMap<>();
        final Map<String, String> intermediaryToOfficial = new HashMap<>();
        final Map<MemberKey, List<TinyMember>> methodsByOfficialKey = new HashMap<>();
        final Map<FieldKey, List<TinyMember>> fieldsByOfficialKey = new HashMap<>();
        final Map<String, List<TinyMember>> methodsById = new HashMap<>();
        final Map<String, List<TinyMember>> fieldsById = new HashMap<>();
        final Map<OwnerAndId, List<TinyMember>> methodsByOwnerAndId = new HashMap<>();
        final Map<OwnerAndId, List<TinyMember>> fieldsByOwnerAndId = new HashMap<>();
        int classRows;
        int methodRows;
        int fieldRows;
    }

    public enum ClassStatus { MAPPED_EXACT, SOURCE_CLASS_MISSING, TARGET_CLASS_ID_MISSING }

    public record ClassResolution(
            ClassStatus status, String sourceOfficialName, String intermediaryName, String targetOfficialName) {}

    public enum SymbolStatus {
        MAPPED_EXACT, MAPPED_WITH_EXTERNAL_TYPES, SOURCE_MISSING, SOURCE_AMBIGUOUS,
        SOURCE_DESCRIPTOR_MISMATCH, SOURCE_OWNER_UNMAPPED, SOURCE_DESCRIPTOR_UNRESOLVED,
        TARGET_ID_MISSING, TARGET_OWNER_CHANGED, TARGET_AMBIGUOUS,
        TARGET_DESCRIPTOR_UNRESOLVED, TARGET_DESCRIPTOR_CHANGED
    }

    public record SymbolCandidate(
            String sourceOwnerOfficial, String sourceNameOfficial, String sourceDescriptorOfficial,
            String sourceOwnerIntermediary, String intermediaryId, String sourceDescriptorIntermediary,
            String targetOwnerOfficial, String targetNameOfficial, String targetDescriptorOfficial,
            String targetOwnerIntermediary, String targetDescriptorIntermediary,
            int externalDescriptorTypeCount) {}

    public record SymbolResolution(SymbolStatus status, List<SymbolCandidate> candidates) {
        public SymbolResolution { candidates = List.copyOf(candidates); }
    }

    /** Per-symbol, owner/member/descriptor-aware view over parsed source and target Tiny v1 mappings. */
    public static final class Index {
        private final TsrgData tsrg;
        private final TinyMappings source;
        private final TinyMappings target;

        private Index(TsrgData tsrg, TinyMappings source, TinyMappings target) {
            this.tsrg = tsrg;
            this.source = source;
            this.target = target;
        }

        public AuditResult audit() {
            return new AuditResult(
                    auditClasses(tsrg.classes(), source, target),
                    auditMembers(tsrg.methods(), true, source, target),
                    auditMembers(tsrg.fields(), false, source, target),
                    source.classRows, source.methodRows, source.fieldRows,
                    target.classRows, target.methodRows, target.fieldRows);
        }

        public ClassResolution resolveClass(String sourceOfficialName) {
            String intermediaryName = source.officialToIntermediary.get(sourceOfficialName);
            if (intermediaryName == null) {
                return new ClassResolution(ClassStatus.SOURCE_CLASS_MISSING, sourceOfficialName, null, null);
            }
            String targetOfficialName = target.intermediaryToOfficial.get(intermediaryName);
            if (targetOfficialName == null) {
                return new ClassResolution(
                        ClassStatus.TARGET_CLASS_ID_MISSING, sourceOfficialName, intermediaryName, null);
            }
            return new ClassResolution(
                    ClassStatus.MAPPED_EXACT, sourceOfficialName, intermediaryName, targetOfficialName);
        }

        public SymbolResolution resolveMethod(
                String sourceOwnerOfficial, String sourceNameOfficial, String sourceDescriptorOfficial) {
            List<TinyMember> matches = source.methodsByOfficialKey.get(new MemberKey(
                    sourceOwnerOfficial, sourceNameOfficial, sourceDescriptorOfficial));
            if (matches == null || matches.isEmpty()) {
                return new SymbolResolution(SymbolStatus.SOURCE_MISSING, List.of());
            }
            if (matches.size() != 1) {
                return new SymbolResolution(SymbolStatus.SOURCE_AMBIGUOUS, List.of());
            }
            return resolveTarget(matches.getFirst(), true);
        }

        public SymbolResolution resolveField(
                String sourceOwnerOfficial, String sourceNameOfficial, String sourceDescriptorOfficial) {
            List<TinyMember> rows = source.fieldsByOfficialKey.get(new FieldKey(sourceOwnerOfficial, sourceNameOfficial));
            if (rows == null || rows.isEmpty()) {
                return new SymbolResolution(SymbolStatus.SOURCE_MISSING, List.of());
            }
            List<TinyMember> matches = rows.stream()
                    .filter(row -> row.descriptorOfficial().equals(sourceDescriptorOfficial)).toList();
            if (matches.isEmpty()) {
                return new SymbolResolution(SymbolStatus.SOURCE_DESCRIPTOR_MISMATCH, List.of());
            }
            if (matches.size() != 1) {
                return new SymbolResolution(SymbolStatus.SOURCE_AMBIGUOUS, List.of());
            }
            return resolveTarget(matches.getFirst(), false);
        }

        private SymbolResolution resolveTarget(TinyMember sourceMember, boolean method) {
            if (sourceMember.ownerIntermediary() == null) {
                return new SymbolResolution(SymbolStatus.SOURCE_OWNER_UNMAPPED, List.of());
            }
            if (sourceMember.descriptorIntermediary() == null) {
                return new SymbolResolution(SymbolStatus.SOURCE_DESCRIPTOR_UNRESOLVED, List.of());
            }
            Map<String, List<TinyMember>> targetById = method ? target.methodsById : target.fieldsById;
            Map<OwnerAndId, List<TinyMember>> targetByOwnerAndId =
                    method ? target.methodsByOwnerAndId : target.fieldsByOwnerAndId;
            List<TinyMember> allTargetRows = targetById.get(sourceMember.intermediaryId());
            if (allTargetRows == null || allTargetRows.isEmpty()) {
                return new SymbolResolution(SymbolStatus.TARGET_ID_MISSING, List.of());
            }
            List<TinyMember> sameOwnerRows = targetByOwnerAndId.get(
                    new OwnerAndId(sourceMember.ownerIntermediary(), sourceMember.intermediaryId()));
            if (sameOwnerRows == null || sameOwnerRows.isEmpty()) {
                SymbolStatus status = allTargetRows.size() == 1
                        ? SymbolStatus.TARGET_OWNER_CHANGED : SymbolStatus.TARGET_AMBIGUOUS;
                return new SymbolResolution(status,
                        allTargetRows.stream().map(row -> candidate(sourceMember, row)).toList());
            }
            if (sameOwnerRows.size() != 1) {
                return new SymbolResolution(SymbolStatus.TARGET_AMBIGUOUS,
                        sameOwnerRows.stream().map(row -> candidate(sourceMember, row)).toList());
            }
            TinyMember targetMember = sameOwnerRows.getFirst();
            SymbolCandidate result = candidate(sourceMember, targetMember);
            if (targetMember.descriptorIntermediary() == null) {
                return new SymbolResolution(SymbolStatus.TARGET_DESCRIPTOR_UNRESOLVED, List.of(result));
            }
            if (!sourceMember.descriptorIntermediary().equals(targetMember.descriptorIntermediary())) {
                return new SymbolResolution(SymbolStatus.TARGET_DESCRIPTOR_CHANGED, List.of(result));
            }
            int externalTypes = sourceMember.descriptorExternalTypeCount()
                    + targetMember.descriptorExternalTypeCount();
            return new SymbolResolution(externalTypes > 0
                    ? SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES : SymbolStatus.MAPPED_EXACT, List.of(
                    new SymbolCandidate(
                            result.sourceOwnerOfficial(), result.sourceNameOfficial(), result.sourceDescriptorOfficial(),
                            result.sourceOwnerIntermediary(), result.intermediaryId(), result.sourceDescriptorIntermediary(),
                            result.targetOwnerOfficial(), result.targetNameOfficial(), result.targetDescriptorOfficial(),
                            result.targetOwnerIntermediary(), result.targetDescriptorIntermediary(), externalTypes)));
        }

        private static SymbolCandidate candidate(TinyMember sourceMember, TinyMember targetMember) {
            return new SymbolCandidate(
                    sourceMember.ownerOfficial(), sourceMember.officialName(), sourceMember.descriptorOfficial(),
                    sourceMember.ownerIntermediary(), sourceMember.intermediaryId(),
                    sourceMember.descriptorIntermediary(),
                    targetMember.ownerOfficial(), targetMember.officialName(), targetMember.descriptorOfficial(),
                    targetMember.ownerIntermediary(), targetMember.descriptorIntermediary(),
                    sourceMember.descriptorExternalTypeCount() + targetMember.descriptorExternalTypeCount());
        }
    }

    public static Index readIndex(Path joinedTsrg, Path sourceTiny, Path targetTiny) throws IOException {
        requireInput(joinedTsrg, "MCPConfig joined.tsrg");
        requireInput(sourceTiny, "Fabric Intermediary 1.20.1 Tiny mapping");
        requireInput(targetTiny, "Fabric Intermediary 1.21.1 Tiny mapping");
        TsrgData tsrg = readTsrg(joinedTsrg);
        Set<String> sourceMinecraftClasses = new HashSet<>();
        for (TsrgClass row : tsrg.classes()) sourceMinecraftClasses.add(row.obfuscatedName());
        return new Index(tsrg, readTiny(sourceTiny, sourceMinecraftClasses), readTiny(targetTiny, Set.of()));
    }

    public static AuditResult analyze(Path joinedTsrg, Path sourceTiny, Path targetTiny) throws IOException {
        return readIndex(joinedTsrg, sourceTiny, targetTiny).audit();
    }

    /**
     * Usage: Forge1201IntermediaryBridgeAudit
     *        <joined.tsrg> <source-mappings.tiny> <target-mappings.tiny> <report.json>
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201IntermediaryBridgeAudit <joined.tsrg> <source-tiny> <target-tiny> <report.json>");
        }

        Path tsrg = Path.of(args[0]);
        Path sourceTiny = Path.of(args[1]);
        Path targetTiny = Path.of(args[2]);
        Path report = Path.of(args[3]);
        AuditResult result = analyze(tsrg, sourceTiny, targetTiny);

        Files.createDirectories(report.toAbsolutePath().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
            writer.write(toJson(result, tsrg, sourceTiny, targetTiny));
            writer.newLine();
        }

        System.out.printf(
                "MCPConfig obfuscated -> Fabric Intermediary 1.20.1: classes %d/%d, methods %d/%d, fields %d/%d%n",
                result.classes().sourceIntermediaryMappedRows(), result.classes().joinedRows(),
                result.methods().sourceIntermediaryMappedRows(), result.methods().joinedRows(),
                result.fields().sourceIntermediaryMappedRows(), result.fields().joinedRows());
        System.out.printf(
                "Intermediary cross-version candidates: methods exact %d, signature changed %d, owner changed %d, target ID absent %d; fields exact %d, signature changed %d, owner changed %d, target ID absent %d%n",
                result.methods().targetExactOwnerMemberDescriptorRows(),
                result.methods().targetSameOwnerMemberDescriptorChangedRows(),
                result.methods().targetOwnerChangedRows(),
                result.methods().targetMemberIdMissingRows(),
                result.fields().targetExactOwnerMemberDescriptorRows(),
                result.fields().targetSameOwnerMemberDescriptorChangedRows(),
                result.fields().targetOwnerChangedRows(),
                result.fields().targetMemberIdMissingRows());
        System.out.println("SRG method IDs reused across distinct intermediary symbols: "
                + result.methods().sourceSrgIdsWithMultipleIntermediarySymbols());
        System.out.println("Build-local Intermediary bridge audit: " + report);
        System.out.println("No selector transformation or runtime mapping table was produced.");
    }

    private static void requireInput(Path path, String label) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Missing " + label + " input: " + path);
        }
        long size = Files.size(path);
        if (size <= 0 || size > MAX_INPUT_BYTES) {
            throw new IOException(label + " input has an unsupported size: " + size);
        }
    }

    private static TsrgData readTsrg(Path path) throws IOException {
        List<TsrgClass> classes = new ArrayList<>();
        List<TsrgMember> methods = new ArrayList<>();
        List<TsrgMember> fields = new ArrayList<>();
        String currentOwner = null;
        String header;
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            header = reader.readLine();
            lineNumber++;
            if (!"tsrg2 obf srg id".equals(header)) {
                throw new IOException("Unexpected MCPConfig TSRG2 header at " + path + ": " + header);
            }

            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                if (line.isBlank()) continue;
                if (line.startsWith("\t\t")) continue;

                if (line.startsWith("\t")) {
                    String[] parts = line.trim().split("\\s+");
                    if (currentOwner == null) {
                        throw new IOException("TSRG2 member row appears before a class row at " + path + ":" + lineNumber);
                    }
                    if (parts.length == 3 && SRG_FIELD.matcher(parts[1]).matches()) {
                        fields.add(new TsrgMember(currentOwner, parts[0], null, parts[1]));
                    } else if (parts.length == 4 && SRG_METHOD.matcher(parts[2]).matches()) {
                        methods.add(new TsrgMember(currentOwner, parts[0], parts[1], parts[2]));
                    }
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                        throw new IOException("Malformed TSRG2 class row at " + path + ":" + lineNumber);
                    }
                    currentOwner = parts[0];
                    classes.add(new TsrgClass(parts[0], parts[1]));
                }

                if (classes.size() + methods.size() + fields.size() > MAX_RECORDS) {
                    throw new IOException("TSRG2 mapping exceeds the " + MAX_RECORDS + " record limit: " + path);
                }
            }
        }
        return new TsrgData(List.copyOf(classes), List.copyOf(methods), List.copyOf(fields));
    }

    private static TinyMappings readTiny(Path path, Set<String> knownMinecraftClasses) throws IOException {
        TinyMappings mappings = new TinyMappings();
        List<String[]> rawMembers = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            lineNumber++;
            if (!"v1\tofficial\tintermediary".equals(header)) {
                throw new IOException("Expected Tiny v1 official -> intermediary header in " + path + ", got: " + header);
            }

            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                if (line.isBlank()) continue;
                String[] parts = line.split("\t", -1);
                if (parts[0].equals("CLASS") && parts.length == 3) {
                    String old = mappings.officialToIntermediary.putIfAbsent(parts[1], parts[2]);
                    if (old != null && !old.equals(parts[2])) {
                        throw new IOException("Conflicting intermediary class mapping for " + parts[1] + " at "
                                + path + ":" + lineNumber);
                    }
                    String previousOfficial = mappings.intermediaryToOfficial.putIfAbsent(parts[2], parts[1]);
                    if (previousOfficial != null && !previousOfficial.equals(parts[1])) {
                        throw new IOException("Intermediary class ID maps to multiple official owners: " + parts[2]);
                    }
                    mappings.classRows++;
                } else if ((parts[0].equals("FIELD") || parts[0].equals("METHOD")) && parts.length == 5) {
                    rawMembers.add(parts);
                    if (parts[0].equals("FIELD")) mappings.fieldRows++;
                    else mappings.methodRows++;
                } else {
                    throw new IOException("Unsupported Tiny v1 row at " + path + ":" + lineNumber + ": " + line);
                }

                if (mappings.classRows + rawMembers.size() > MAX_RECORDS) {
                    throw new IOException("Tiny mappings exceed the " + MAX_RECORDS + " record limit: " + path);
                }
            }
        }

        // Resolve descriptor class names only after the complete class table is known.
        for (String[] parts : rawMembers) {
            String ownerIntermediary = mappings.officialToIntermediary.get(parts[1]);
            DescriptorNormalization normalizedDescriptor =
                    normalizeDescriptor(parts[2], mappings.officialToIntermediary, knownMinecraftClasses);
            TinyMember member = new TinyMember(
                    parts[1], ownerIntermediary, parts[2], normalizedDescriptor.descriptor(),
                    normalizedDescriptor.externalTypeCount(), parts[3], parts[4]);
            if (parts[0].equals("METHOD")) {
                add(mappings.methodsByOfficialKey,
                        new MemberKey(parts[1], parts[3], parts[2]), member);
                add(mappings.methodsById, parts[4], member);
                if (ownerIntermediary != null) {
                    add(mappings.methodsByOwnerAndId, new OwnerAndId(ownerIntermediary, parts[4]), member);
                }
            } else {
                add(mappings.fieldsByOfficialKey, new FieldKey(parts[1], parts[3]), member);
                add(mappings.fieldsById, parts[4], member);
                if (ownerIntermediary != null) {
                    add(mappings.fieldsByOwnerAndId, new OwnerAndId(ownerIntermediary, parts[4]), member);
                }
            }
        }
        return mappings;
    }

    private static DescriptorNormalization normalizeDescriptor(
            String descriptor,
            Map<String, String> officialToIntermediary,
            Set<String> knownMinecraftClasses) throws IOException {
        StringBuilder normalized = new StringBuilder(descriptor.length());
        int externalTypeCount = 0;
        for (int index = 0; index < descriptor.length();) {
            char current = descriptor.charAt(index);
            if (current != 'L') {
                normalized.append(current);
                index++;
                continue;
            }

            int end = descriptor.indexOf(';', index + 1);
            if (end < 0) throw new IOException("Malformed JVM descriptor in intermediary mapping: " + descriptor);
            String officialType = descriptor.substring(index + 1, end);
            String intermediaryType = officialToIntermediary.get(officialType);
            if (intermediaryType != null) {
                normalized.append('L').append(intermediaryType).append(';');
            } else if (knownMinecraftClasses.contains(officialType) || officialType.indexOf('/') < 0) {
                // A known source Minecraft class without a Tiny identity, or an unqualified
                // unmapped obfuscated name, is unresolved. Never pass it through as external.
                return new DescriptorNormalization(null, externalTypeCount);
            } else {
                // Package-qualified types absent from Minecraft's mapping inventory are kept
                // unchanged as external candidates. Their target classpath presence is not
                // proven here, so rows using them receive a separate non-exact classification.
                normalized.append('L').append(officialType).append(';');
                externalTypeCount++;
            }
            index = end + 1;
        }
        return new DescriptorNormalization(normalized.toString(), externalTypeCount);
    }

    private static <K, V> void add(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }

    private static ClassCoverage auditClasses(
            List<TsrgClass> sourceRows, TinyMappings source, TinyMappings target) {
        int sourceMapped = 0;
        int targetShared = 0;
        Set<String> sourceClassIds = new HashSet<>();
        for (TsrgClass row : sourceRows) {
            String id = source.officialToIntermediary.get(row.obfuscatedName());
            if (id == null) continue;
            sourceMapped++;
            sourceClassIds.add(id);
        }
        for (String id : sourceClassIds) {
            if (target.intermediaryToOfficial.containsKey(id)) targetShared++;
        }
        int targetOnly = 0;
        for (String id : target.intermediaryToOfficial.keySet()) {
            if (!source.officialToIntermediary.containsValue(id)) targetOnly++;
        }
        return new ClassCoverage(
                sourceRows.size(),
                sourceMapped,
                sourceRows.size() - sourceMapped,
                targetShared,
                sourceClassIds.size() - targetShared,
                targetOnly);
    }

    private static MemberCoverage auditMembers(
            List<TsrgMember> sourceRows,
            boolean method,
            TinyMappings source,
            TinyMappings target) {
        int sourceMapped = 0;
        int sourceUnmapped = 0;
        int ambiguousSource = 0;
        int sourceOwnerUnmapped = 0;
        int sourceDescriptorUnresolved = 0;
        int sourceDescriptorExternalTypeRows = 0;
        int sourceDescriptorExternalTypeOccurrences = 0;
        int targetExact = 0;
        int targetExternalPassthrough = 0;
        int targetDescriptorChanged = 0;
        int targetDescriptorUnresolved = 0;
        int targetDescriptorExternalTypeRows = 0;
        int targetDescriptorExternalTypeOccurrences = 0;
        int targetOwnerChanged = 0;
        int targetIdMissing = 0;
        int ambiguousTarget = 0;
        Map<String, Set<String>> intermediaryIdsBySrgId = new HashMap<>();

        for (TsrgMember row : sourceRows) {
            List<TinyMember> matches;
            if (method) {
                matches = source.methodsByOfficialKey.get(
                        new MemberKey(row.ownerObfuscatedName(), row.obfuscatedName(), row.descriptor()));
            } else {
                matches = source.fieldsByOfficialKey.get(
                        new FieldKey(row.ownerObfuscatedName(), row.obfuscatedName()));
            }

            if (matches == null || matches.isEmpty()) {
                sourceUnmapped++;
                continue;
            }
            if (matches.size() != 1) {
                ambiguousSource++;
                continue;
            }

            TinyMember sourceMember = matches.get(0);
            sourceMapped++;
            intermediaryIdsBySrgId.computeIfAbsent(row.srgId(), ignored -> new HashSet<>())
                    .add(sourceMember.intermediaryId());

            if (sourceMember.ownerIntermediary() == null) {
                sourceOwnerUnmapped++;
                continue;
            }
            if (sourceMember.descriptorIntermediary() == null) sourceDescriptorUnresolved++;
            if (sourceMember.descriptorExternalTypeCount() > 0) {
                sourceDescriptorExternalTypeRows++;
                sourceDescriptorExternalTypeOccurrences += sourceMember.descriptorExternalTypeCount();
            }

            Map<String, List<TinyMember>> targetById = method ? target.methodsById : target.fieldsById;
            Map<OwnerAndId, List<TinyMember>> targetByOwnerAndId =
                    method ? target.methodsByOwnerAndId : target.fieldsByOwnerAndId;
            List<TinyMember> allTargetRows = targetById.get(sourceMember.intermediaryId());
            if (allTargetRows == null || allTargetRows.isEmpty()) {
                targetIdMissing++;
                continue;
            }

            List<TinyMember> sameOwnerRows = targetByOwnerAndId.get(
                    new OwnerAndId(sourceMember.ownerIntermediary(), sourceMember.intermediaryId()));
            if (sameOwnerRows == null || sameOwnerRows.isEmpty()) {
                targetOwnerChanged++;
                continue;
            }
            if (sameOwnerRows.size() != 1) {
                ambiguousTarget++;
                continue;
            }

            TinyMember targetMember = sameOwnerRows.get(0);
            boolean hasExternalDescriptorTypes = sourceMember.descriptorExternalTypeCount() > 0
                    || targetMember.descriptorExternalTypeCount() > 0;
            if (targetMember.descriptorExternalTypeCount() > 0) {
                targetDescriptorExternalTypeRows++;
                targetDescriptorExternalTypeOccurrences += targetMember.descriptorExternalTypeCount();
            }
            if (sourceMember.descriptorIntermediary() == null
                    || targetMember.descriptorIntermediary() == null) {
                targetDescriptorUnresolved++;
            } else if (sourceMember.descriptorIntermediary().equals(targetMember.descriptorIntermediary())) {
                if (hasExternalDescriptorTypes) targetExternalPassthrough++;
                else targetExact++;
            } else {
                targetDescriptorChanged++;
            }
        }

        int multiSymbolSrgIds = 0;
        for (Set<String> ids : intermediaryIdsBySrgId.values()) {
            if (ids.size() > 1) multiSymbolSrgIds++;
        }

        return new MemberCoverage(
                sourceRows.size(),
                sourceMapped,
                sourceUnmapped,
                ambiguousSource,
                sourceOwnerUnmapped,
                sourceDescriptorUnresolved,
                sourceDescriptorExternalTypeRows,
                sourceDescriptorExternalTypeOccurrences,
                intermediaryIdsBySrgId.size(),
                multiSymbolSrgIds,
                targetExact,
                targetExternalPassthrough,
                targetDescriptorChanged,
                targetDescriptorUnresolved,
                targetDescriptorExternalTypeRows,
                targetDescriptorExternalTypeOccurrences,
                targetOwnerChanged,
                targetIdMissing,
                ambiguousTarget);
    }

    private static String toJson(AuditResult result, Path tsrg, Path sourceTiny, Path targetTiny) throws IOException {
        StringBuilder json = new StringBuilder(4096);
        json.append("{\n");
        field(json, "schemaVersion", "1", true);
        field(json, "sourceMinecraftVersion", quote("1.20.1"), true);
        field(json, "targetMinecraftVersion", quote("1.21.1"), true);
        field(json, "sourceNamespace", quote("minecraft:official-obfuscated@1.20.1"), true);
        field(json, "sourceSrgMetadataNamespace", quote("minecraft:forge-srg@1.20.1"), true);
        field(json, "identityNamespace", quote("minecraft:fabric-intermediary"), true);
        field(json, "targetNamespace", quote("minecraft:official-obfuscated@1.21.1"), true);
        field(json, "completeForgeSrgSelectorPath", "false", true);
        field(json, "status", quote("PARTIAL"), true);
        field(json, "selectorTransformationPerformed", "false", true);
        field(json, "runtimeMappingTableProduced", "false", true);
        json.append("  \"artifacts\": [\n");
        artifact(json, "mcpconfig-joined-tsrg-1201", "MCPConfig", "1.20.1-20230612.114412",
                "config/joined.tsrg", tsrg);
        json.append(",\n");
        artifact(json, "fabric-intermediary-1201", "FabricMC/intermediary", "1.20.1",
                "mappings/mappings.tiny (Tiny v1 official -> intermediary)", sourceTiny);
        json.append(",\n");
        artifact(json, "fabric-intermediary-1211", "FabricMC/intermediary", "1.21.1",
                "mappings/mappings.tiny (Tiny v1 official -> intermediary)", targetTiny);
        json.append("\n  ],\n");
        json.append("  \"coverage\": {\n");
        json.append("    \"classes\": {\n");
        appendClassCoverage(json, result.classes());
        json.append("\n    },\n");
        json.append("    \"methods\": {\n");
        appendMemberCoverage(json, result.methods());
        json.append("\n    },\n");
        json.append("    \"fields\": {\n");
        appendMemberCoverage(json, result.fields());
        json.append("\n    }\n");
        json.append("  },\n");
        json.append("  \"tinyRowCounts\": {\n");
        field(json, "sourceClasses", Integer.toString(result.sourceTinyClassRows()), true);
        field(json, "sourceMethods", Integer.toString(result.sourceTinyMethodRows()), true);
        field(json, "sourceFields", Integer.toString(result.sourceTinyFieldRows()), true);
        field(json, "targetClasses", Integer.toString(result.targetTinyClassRows()), true);
        field(json, "targetMethods", Integer.toString(result.targetTinyMethodRows()), true);
        field(json, "targetFields", Integer.toString(result.targetTinyFieldRows()), false);
        json.append("\n  },\n");
        json.append("  \"interpretation\": {\n");
        field(json, "sourceJoin",
                quote("Methods join by the MCPConfig obfuscated owner + obfuscated member name + full JVM descriptor; fields join by obfuscated owner + obfuscated member name."),
                true);
        field(json, "tsrgNamespaces",
                quote("MCPConfig joined.tsrg header is obf/srg/id. This audit consumes its obf columns and uses SRG names/IDs only as diagnostic metadata."),
                true);
        field(json, "selectorPath",
                quote("Forge SRG owner/member/descriptor selectors are not converted to the obfuscated input namespace by this audit. A complete SRG selector edge is still required."),
                true);
        field(json, "identity",
                quote("A shared intermediary ID is a candidate identity link. Only unchanged owner + member ID + fully mapped descriptor is counted as structurally exact."),
                true);
        field(json, "srgIdentifiers",
                quote("SRG identifiers are diagnostic member names, not global semantic symbol keys; the report counts IDs reused across distinct intermediary symbols."),
                true);
        field(json, "descriptorCoverage",
                quote("Minecraft classes are translated through the version-specific Intermediary class table. Package-qualified descriptor types absent from that inventory are preserved as external candidates and counted separately; their target classpath presence is not verified here. Known source Minecraft classes missing a Tiny identity, and unqualified unmapped obfuscated types, remain unresolved. External-pass-through matches are not counted as fully mapped exact rows."),
                true);
        field(json, "limits",
                quote("This report ends at the target official-obfuscated namespace. Converting target names to NeoForge's Mojang-named runtime is a separate stage. Stable identity does not prove binary or gameplay compatibility."),
                false);
        json.append("\n  }\n");
        json.append("}\n");
        return json.toString();
    }

    private static void artifact(
            StringBuilder json, String artifactId, String provider, String version, String entry, Path path)
            throws IOException {
        json.append("    {\n");
        field(json, "artifactId", quote(artifactId), true);
        field(json, "provider", quote(provider), true);
        field(json, "version", quote(version), true);
        field(json, "entry", quote(entry), true);
        field(json, "fileName", quote(path.getFileName().toString()), true);
        field(json, "sizeBytes", Long.toString(Files.size(path)), true);
        field(json, "sha256", quote(sha256(path)), false);
        json.append("    }");
    }

    private static void appendClassCoverage(StringBuilder json, ClassCoverage coverage) {
        field(json, "joinedRows", Integer.toString(coverage.joinedRows()), true);
        field(json, "sourceIntermediaryMappedRows", Integer.toString(coverage.sourceIntermediaryMappedRows()), true);
        field(json, "sourceUnmappedRows", Integer.toString(coverage.sourceUnmappedRows()), true);
        field(json, "targetSharedClassIds", Integer.toString(coverage.targetSharedClassIds()), true);
        field(json, "sourceClassIdsAbsentFromTarget", Integer.toString(coverage.sourceClassIdsAbsentFromTarget()), true);
        field(json, "targetOnlyClassIds", Integer.toString(coverage.targetOnlyClassIds()), false);
    }

    private static void appendMemberCoverage(StringBuilder json, MemberCoverage coverage) {
        field(json, "joinedRows", Integer.toString(coverage.joinedRows()), true);
        field(json, "sourceIntermediaryMappedRows", Integer.toString(coverage.sourceIntermediaryMappedRows()), true);
        field(json, "sourceUnmappedRows", Integer.toString(coverage.sourceUnmappedRows()), true);
        field(json, "ambiguousSourceRows", Integer.toString(coverage.ambiguousSourceRows()), true);
        field(json, "sourceOwnerUnmappedRows", Integer.toString(coverage.sourceOwnerUnmappedRows()), true);
        field(json, "sourceDescriptorUnresolvedRows", Integer.toString(coverage.sourceDescriptorUnresolvedRows()), true);
        field(json, "sourceDescriptorExternalTypeRows", Integer.toString(coverage.sourceDescriptorExternalTypeRows()), true);
        field(json, "sourceDescriptorExternalTypeOccurrences",
                Integer.toString(coverage.sourceDescriptorExternalTypeOccurrences()), true);
        field(json, "distinctSourceSrgIds", Integer.toString(coverage.distinctSourceSrgIds()), true);
        field(json, "sourceSrgIdsWithMultipleIntermediarySymbols",
                Integer.toString(coverage.sourceSrgIdsWithMultipleIntermediarySymbols()), true);
        field(json, "targetExactOwnerMemberDescriptorRows",
                Integer.toString(coverage.targetExactOwnerMemberDescriptorRows()), true);
        field(json, "targetSameOwnerMemberDescriptorExternalPassthroughRows",
                Integer.toString(coverage.targetSameOwnerMemberDescriptorExternalPassthroughRows()), true);
        field(json, "targetSameOwnerMemberDescriptorChangedRows",
                Integer.toString(coverage.targetSameOwnerMemberDescriptorChangedRows()), true);
        field(json, "targetSameOwnerMemberDescriptorUnresolvedRows",
                Integer.toString(coverage.targetSameOwnerMemberDescriptorUnresolvedRows()), true);
        field(json, "targetDescriptorExternalTypeRows",
                Integer.toString(coverage.targetDescriptorExternalTypeRows()), true);
        field(json, "targetDescriptorExternalTypeOccurrences",
                Integer.toString(coverage.targetDescriptorExternalTypeOccurrences()), true);
        field(json, "targetOwnerChangedRows", Integer.toString(coverage.targetOwnerChangedRows()), true);
        field(json, "targetMemberIdMissingRows", Integer.toString(coverage.targetMemberIdMissingRows()), true);
        field(json, "ambiguousTargetRows", Integer.toString(coverage.ambiguousTargetRows()), false);
    }

    private static void field(StringBuilder json, String name, String value, boolean comma) {
        json.append("    \"").append(escape(name)).append("\": ").append(value);
        if (comma) json.append(',');
        json.append('\n');
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static String sha256(Path file) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest.digest()) hex.append(String.format("%02x", value & 0xff));
        return hex.toString();
    }
}
