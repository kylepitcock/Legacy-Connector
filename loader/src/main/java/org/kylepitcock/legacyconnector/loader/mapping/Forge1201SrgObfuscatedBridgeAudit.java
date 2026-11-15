package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;

/**
 * Read-only, build-local Forge SRG 1.20.1 -> official-obfuscated 1.20.1 mapping edge.
 *
 * <p>The index retains owner and descriptor context. SRG method identifiers are not used as global
 * keys. Source Tiny v1 is used only to supply a unique descriptor for TSRG2 field rows, which do
 * not contain descriptors.</p>
 *
 * <p>This utility does not rewrite selectors or emit a runtime mapping table.</p>
 */
public final class Forge1201SrgObfuscatedBridgeAudit {
    private static final long MAX_TSRG_BYTES = 256L * 1024 * 1024;
    private static final long MAX_TINY_BYTES = 32L * 1024 * 1024;
    private static final int MAX_RECORDS = 2_000_000;
    private static final Pattern SRG_METHOD = Pattern.compile("^m_\\d+_$");
    private static final Pattern SRG_FIELD = Pattern.compile("^f_\\d+_$");

    private Forge1201SrgObfuscatedBridgeAudit() {}

    public enum Status {
        MAPPED_EXACT,
        MAPPED_WITH_EXTERNAL_TYPES,
        MISSING,
        AMBIGUOUS,
        OWNER_MISSING,
        OWNER_AMBIGUOUS,
        DESCRIPTOR_UNRESOLVED
    }

    public record ClassResolution(Status status, List<String> obfuscatedNames) {
        public ClassResolution {
            obfuscatedNames = List.copyOf(obfuscatedNames);
        }
    }

    public record Candidate(
            String sourceOwner,
            String sourceName,
            String sourceDescriptor,
            String obfuscatedOwner,
            String obfuscatedName,
            String obfuscatedDescriptor,
            int externalDescriptorTypeCount) {}

    public record Resolution(Status status, List<Candidate> candidates) {
        public Resolution {
            candidates = List.copyOf(candidates);
        }
    }

    public record ClassCoverage(
            int tsrgClassRows,
            int uniquelyMappedClassRows,
            int classRowsWithoutSrgName,
            int ambiguousSrgClassNames,
            int ambiguousObfuscatedClassNames) {}

    public record MethodCoverage(
            int tsrgMethodRows,
            int ownerMappedRows,
            int ownerUnmappedRows,
            int descriptorUnresolvedRows,
            int externalDescriptorRows,
            int externalDescriptorOccurrences,
            int ownerNameGroups,
            int overloadedOwnerNameGroups,
            int duplicateFullSelectorKeys) {}

    public record FieldCoverage(
            int tsrgFieldRows,
            int uniqueTinyDescriptorJoins,
            int missingTinyFieldJoins,
            int ambiguousTinyFieldJoins,
            int ownerUnmappedRows,
            int descriptorUnresolvedRows,
            int externalDescriptorRows,
            int externalDescriptorOccurrences) {}

    public record AuditResult(
            ClassCoverage classes,
            MethodCoverage methods,
            FieldCoverage fields,
            long tsrgSizeBytes,
            String tsrgSha256,
            long sourceTinySizeBytes,
            String sourceTinySha256) {}

    private record OwnerName(String owner, String name) {}
    private record TsrgClass(String obfuscatedName, String srgName) {}
    private record TsrgMethod(String obfuscatedOwner, String obfuscatedName, String obfuscatedDescriptor,
            String srgName) {}
    private record TsrgField(String obfuscatedOwner, String obfuscatedName, String srgName) {}
    private record TinyField(String obfuscatedOwner, String descriptor, String obfuscatedName) {}
    private record FieldKey(String owner, String name) {}
    private record DescriptorResult(String descriptor, int externalTypeCount, boolean resolved) {}

    public static final class Index {
        private final Map<String, List<String>> srgToObfuscatedClasses;
        private final Map<String, List<String>> obfuscatedToSrgClasses;
        private final Set<String> knownSrgClasses;
        private final Set<String> knownObfuscatedClasses;
        private final Map<OwnerName, List<TsrgMethod>> methodsBySrgOwnerAndName;
        private final Map<OwnerName, List<Candidate>> fieldsBySrgOwnerAndName;
        private final Set<OwnerName> ambiguousFields;
        private final AuditResult audit;

        private Index(
                Map<String, List<String>> srgToObfuscatedClasses,
                Map<String, List<String>> obfuscatedToSrgClasses,
                Set<String> knownSrgClasses,
                Set<String> knownObfuscatedClasses,
                Map<OwnerName, List<TsrgMethod>> methodsBySrgOwnerAndName,
                Map<OwnerName, List<Candidate>> fieldsBySrgOwnerAndName,
                Set<OwnerName> ambiguousFields,
                AuditResult audit) {
            this.srgToObfuscatedClasses = immutableLists(srgToObfuscatedClasses);
            this.obfuscatedToSrgClasses = immutableLists(obfuscatedToSrgClasses);
            this.knownSrgClasses = Set.copyOf(knownSrgClasses);
            this.knownObfuscatedClasses = Set.copyOf(knownObfuscatedClasses);
            this.methodsBySrgOwnerAndName = immutableLists(methodsBySrgOwnerAndName);
            this.fieldsBySrgOwnerAndName = immutableLists(fieldsBySrgOwnerAndName);
            this.ambiguousFields = Set.copyOf(ambiguousFields);
            this.audit = audit;
        }

        public AuditResult audit() {
            return audit;
        }

        public ClassResolution resolveClass(String srgOwner) {
            List<String> candidates = srgToObfuscatedClasses.getOrDefault(srgOwner, List.of());
            if (candidates.isEmpty()) return new ClassResolution(Status.MISSING, List.of());
            if (candidates.size() > 1) return new ClassResolution(Status.AMBIGUOUS, candidates);
            return new ClassResolution(Status.MAPPED_EXACT, candidates);
        }

        /**
         * Resolves a fully-qualified SRG method selector. The owner, SRG member name, and full
         * descriptor are all required; a repeated m_N_ token elsewhere cannot affect the result.
         */
        public Resolution resolveMethod(String srgOwner, String srgName, String srgDescriptor) {
            ClassResolution owner = resolveClass(srgOwner);
            if (owner.status() == Status.MISSING) return new Resolution(Status.OWNER_MISSING, List.of());
            if (owner.status() == Status.AMBIGUOUS) return new Resolution(Status.OWNER_AMBIGUOUS, List.of());

            DescriptorResult descriptor = normalizeDescriptor(
                    srgDescriptor, srgToObfuscatedClasses, knownSrgClasses);
            if (!descriptor.resolved()) return new Resolution(Status.DESCRIPTOR_UNRESOLVED, List.of());

            List<TsrgMethod> rows = methodsBySrgOwnerAndName.getOrDefault(
                    new OwnerName(srgOwner, srgName), List.of());
            List<Candidate> matches = new ArrayList<>();
            for (TsrgMethod row : rows) {
                if (owner.obfuscatedNames().getFirst().equals(row.obfuscatedOwner())
                        && descriptor.descriptor().equals(row.obfuscatedDescriptor())) {
                    // Preserve the exact caller-supplied SRG descriptor after the forward match.
                    Candidate candidate = new Candidate(
                            srgOwner, srgName, srgDescriptor, row.obfuscatedOwner(),
                            row.obfuscatedName(), row.obfuscatedDescriptor(), descriptor.externalTypeCount());
                    if (!matches.contains(candidate)) matches.add(candidate);
                }
            }
            if (matches.isEmpty()) return new Resolution(Status.MISSING, List.of());
            if (matches.size() > 1) return new Resolution(Status.AMBIGUOUS, matches);
            Status status = descriptor.externalTypeCount() > 0
                    ? Status.MAPPED_WITH_EXTERNAL_TYPES : Status.MAPPED_EXACT;
            return new Resolution(status, matches);
        }

        /**
         * Resolves a field by SRG owner and name. The descriptor is accepted only when the
         * source Tiny inventory has one owner/name candidate.
         */
        public Resolution resolveField(String srgOwner, String srgName) {
            ClassResolution owner = resolveClass(srgOwner);
            if (owner.status() == Status.MISSING) return new Resolution(Status.OWNER_MISSING, List.of());
            if (owner.status() == Status.AMBIGUOUS) return new Resolution(Status.OWNER_AMBIGUOUS, List.of());

            OwnerName key = new OwnerName(srgOwner, srgName);
            List<Candidate> rows = fieldsBySrgOwnerAndName.getOrDefault(key, List.of());
            if (rows.isEmpty() && ambiguousFields.contains(key)) {
                return new Resolution(Status.AMBIGUOUS, List.of());
            }
            List<Candidate> matches = rows.stream()
                    .filter(row -> owner.obfuscatedNames().getFirst().equals(row.obfuscatedOwner()))
                    .distinct().toList();
            if (matches.isEmpty()) return new Resolution(Status.MISSING, List.of());
            if (matches.size() > 1) return new Resolution(Status.AMBIGUOUS, matches);
            Candidate match = matches.getFirst();
            if (match.obfuscatedDescriptor() == null || match.sourceDescriptor() == null) {
                return new Resolution(Status.DESCRIPTOR_UNRESOLVED, matches);
            }
            return new Resolution(match.externalDescriptorTypeCount() > 0
                    ? Status.MAPPED_WITH_EXTERNAL_TYPES : Status.MAPPED_EXACT, matches);
        }

        private static <K, V> Map<K, List<V>> immutableLists(Map<K, List<V>> source) {
            Map<K, List<V>> copy = new HashMap<>();
            source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            return Map.copyOf(copy);
        }
    }

    public static Index read(Path joinedTsrg, Path sourceTiny) throws IOException {
        requireInput(joinedTsrg, "MCPConfig joined.tsrg", MAX_TSRG_BYTES);
        requireInput(sourceTiny, "Fabric Intermediary 1.20.1 Tiny mapping", MAX_TINY_BYTES);

        List<TsrgClass> classes = new ArrayList<>();
        List<TsrgMethod> methods = new ArrayList<>();
        List<TsrgField> fields = new ArrayList<>();
        Set<String> knownObfuscated = new HashSet<>();
        readTsrg(joinedTsrg, classes, methods, fields, knownObfuscated);

        Map<String, List<String>> srgToObfuscated = new HashMap<>();
        Map<String, List<String>> obfuscatedToSrg = new HashMap<>();
        Set<String> knownSrg = new HashSet<>();
        int classRowsMapped = 0;
        int classRowsWithoutSrgName = 0;
        for (TsrgClass row : classes) {
            if (row.srgName() == null) {
                classRowsWithoutSrgName++;
                continue;
            }
            knownSrg.add(row.srgName());
            add(srgToObfuscated, row.srgName(), row.obfuscatedName());
            add(obfuscatedToSrg, row.obfuscatedName(), row.srgName());
        }
        for (TsrgClass row : classes) {
            List<String> targets = row.srgName() == null
                    ? List.of() : distinct(srgToObfuscated.getOrDefault(row.srgName(), List.of()));
            if (targets.size() == 1) classRowsMapped++;
        }
        srgToObfuscated.replaceAll((key, value) -> distinct(value));
        obfuscatedToSrg.replaceAll((key, value) -> distinct(value));

        Map<OwnerName, List<TsrgMethod>> methodsByOwnerName = new HashMap<>();
        Map<OwnerName, List<Candidate>> fieldsByOwnerName = new HashMap<>();
        Map<FieldKey, List<TinyField>> tinyFields = readTinyFields(sourceTiny);

        int ownerMappedMethodRows = 0;
        int ownerUnmappedMethodRows = 0;
        int methodDescriptorUnresolvedRows = 0;
        int methodExternalRows = 0;
        int methodExternalOccurrences = 0;
        Map<OwnerName, Set<String>> methodDescriptorsByOwnerName = new HashMap<>();
        Map<String, Set<String>> methodTargetsByFullKey = new HashMap<>();

        for (TsrgMethod row : methods) {
            List<String> owners = distinct(obfuscatedToSrg.getOrDefault(row.obfuscatedOwner(), List.of()));
            if (owners.size() != 1) {
                ownerUnmappedMethodRows++;
                continue;
            }
            ownerMappedMethodRows++;
            String srgOwner = owners.getFirst();
            OwnerName ownerName = new OwnerName(srgOwner, row.srgName());
            add(methodsByOwnerName, ownerName, row);
            DescriptorResult reverseDescriptor = normalizeDescriptor(
                    row.obfuscatedDescriptor(), obfuscatedToSrg, knownObfuscated);
            if (!reverseDescriptor.resolved()) {
                methodDescriptorUnresolvedRows++;
            } else {
                methodDescriptorsByOwnerName.computeIfAbsent(ownerName, ignored -> new LinkedHashSet<>())
                        .add(reverseDescriptor.descriptor());
                if (reverseDescriptor.externalTypeCount() > 0) {
                    methodExternalRows++;
                    methodExternalOccurrences += reverseDescriptor.externalTypeCount();
                }
            }
            String fullKey = srgOwner + "|" + row.srgName() + "|" + row.obfuscatedDescriptor();
            methodTargetsByFullKey.computeIfAbsent(fullKey, ignored -> new HashSet<>())
                    .add(row.obfuscatedName());
        }

        int overloadedGroups = (int) methodDescriptorsByOwnerName.values().stream()
                .filter(values -> values.size() > 1).count();
        int duplicateFullKeys = (int) methodTargetsByFullKey.values().stream()
                .filter(values -> values.size() > 1).count();

        int ownerUnmappedFieldRows = 0;
        int missingTinyFieldJoins = 0;
        int ambiguousTinyFieldJoins = 0;
        int uniqueTinyFieldJoins = 0;
        int fieldDescriptorUnresolvedRows = 0;
        int fieldExternalRows = 0;
        int fieldExternalOccurrences = 0;
        Set<OwnerName> ambiguousFieldKeys = new HashSet<>();
        for (TsrgField row : fields) {
            List<String> owners = distinct(obfuscatedToSrg.getOrDefault(row.obfuscatedOwner(), List.of()));
            if (owners.size() != 1) {
                ownerUnmappedFieldRows++;
                continue;
            }
            String srgOwner = owners.getFirst();
            List<TinyField> tinyMatches = distinctTinyFields(tinyFields.getOrDefault(
                    new FieldKey(row.obfuscatedOwner(), row.obfuscatedName()), List.of()));
            if (tinyMatches.isEmpty()) {
                missingTinyFieldJoins++;
                continue;
            }
            if (tinyMatches.size() != 1) {
                ambiguousTinyFieldJoins++;
                ambiguousFieldKeys.add(new OwnerName(srgOwner, row.srgName()));
                continue;
            }
            uniqueTinyFieldJoins++;
            TinyField tiny = tinyMatches.getFirst();
            DescriptorResult sourceDescriptor = normalizeDescriptor(
                    tiny.descriptor(), obfuscatedToSrg, knownObfuscated);
            if (!sourceDescriptor.resolved()) {
                fieldDescriptorUnresolvedRows++;
            }
            if (sourceDescriptor.externalTypeCount() > 0) {
                fieldExternalRows++;
                fieldExternalOccurrences += sourceDescriptor.externalTypeCount();
            }
            Candidate candidate = new Candidate(
                    srgOwner, row.srgName(), sourceDescriptor.descriptor(), row.obfuscatedOwner(),
                    row.obfuscatedName(), tiny.descriptor(), sourceDescriptor.externalTypeCount());
            add(fieldsByOwnerName, new OwnerName(srgOwner, row.srgName()), candidate);
        }

        int ambiguousSrgClasses = (int) srgToObfuscated.values().stream().filter(values -> values.size() > 1).count();
        int ambiguousObfuscatedClasses = (int) obfuscatedToSrg.values().stream().filter(values -> values.size() > 1).count();
        int ownerNameGroups = methodsByOwnerName.size();

        AuditResult result = new AuditResult(
                new ClassCoverage(classes.size(), classRowsMapped, classRowsWithoutSrgName,
                        ambiguousSrgClasses, ambiguousObfuscatedClasses),
                new MethodCoverage(methods.size(), ownerMappedMethodRows, ownerUnmappedMethodRows,
                        methodDescriptorUnresolvedRows, methodExternalRows, methodExternalOccurrences,
                        ownerNameGroups, overloadedGroups, duplicateFullKeys),
                new FieldCoverage(fields.size(), uniqueTinyFieldJoins, missingTinyFieldJoins,
                        ambiguousTinyFieldJoins, ownerUnmappedFieldRows, fieldDescriptorUnresolvedRows,
                        fieldExternalRows, fieldExternalOccurrences),
                Files.size(joinedTsrg), sha256(joinedTsrg),
                Files.size(sourceTiny), sha256(sourceTiny));

        return new Index(srgToObfuscated, obfuscatedToSrg, knownSrg, knownObfuscated,
                methodsByOwnerName, fieldsByOwnerName, ambiguousFieldKeys, result);
    }

    public static AuditResult analyze(Path joinedTsrg, Path sourceTiny) throws IOException {
        return read(joinedTsrg, sourceTiny).audit();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201SrgObfuscatedBridgeAudit <joined.tsrg> <source-tiny> <report.json>");
        }
        Index index = read(Path.of(args[0]), Path.of(args[1]));
        Path report = Path.of(args[2]);
        Files.createDirectories(report.toAbsolutePath().getParent());
        Files.writeString(report, toJson(index.audit()), StandardCharsets.UTF_8);
        AuditResult result = index.audit();
        System.out.printf(
                "Forge SRG -> official-obfuscated 1.20.1: classes mapped %d/%d (ambiguous SRG aliases %d); "
                        + "methods owner-indexed %d/%d (descriptor unresolved %d, external %d); "
                        + "fields Tiny-joined uniquely %d/%d (missing %d, ambiguous %d)%n",
                result.classes().uniquelyMappedClassRows(), result.classes().tsrgClassRows(),
                result.classes().ambiguousSrgClassNames(),
                result.methods().ownerMappedRows(), result.methods().tsrgMethodRows(),
                result.methods().descriptorUnresolvedRows(), result.methods().externalDescriptorRows(),
                result.fields().uniqueTinyDescriptorJoins(), result.fields().tsrgFieldRows(),
                result.fields().missingTinyFieldJoins(), result.fields().ambiguousTinyFieldJoins());
        System.out.println("Build-local report: " + report);
        System.out.println("No selector transformation or runtime mapping table was produced.");
    }

    private static void readTsrg(
            Path path,
            List<TsrgClass> classes,
            List<TsrgMethod> methods,
            List<TsrgField> fields,
            Set<String> knownObfuscatedClasses) throws IOException {
        String currentOwner = null;
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            lineNumber++;
            if (!"tsrg2 obf srg id".equals(header)) {
                throw new IOException("Unexpected MCPConfig TSRG2 header at " + path + ": " + header);
            }
            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("\t\t")) continue;
                if (line.startsWith("\t")) {
                    if (currentOwner == null) {
                        throw new IOException("TSRG2 member row appears before a class row at " + path + ":" + lineNumber);
                    }
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 3 && SRG_FIELD.matcher(parts[1]).matches()) {
                        fields.add(new TsrgField(currentOwner, parts[0], parts[1]));
                    } else if (parts.length == 4 && SRG_METHOD.matcher(parts[2]).matches()) {
                        methods.add(new TsrgMethod(currentOwner, parts[0], parts[1], parts[2]));
                    }
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 0 || parts[0].isBlank()) {
                        throw new IOException("Malformed TSRG2 class row at " + path + ":" + lineNumber);
                    }
                    currentOwner = parts[0];
                    knownObfuscatedClasses.add(currentOwner);
                    String srgName = parts.length >= 2 && !parts[1].equals("-") ? parts[1] : null;
                    classes.add(new TsrgClass(currentOwner, srgName));
                }
                if (classes.size() + methods.size() + fields.size() > MAX_RECORDS) {
                    throw new IOException("TSRG2 mapping exceeds the " + MAX_RECORDS + " record limit: " + path);
                }
            }
        }
    }

    private static Map<FieldKey, List<TinyField>> readTinyFields(Path path) throws IOException {
        Map<FieldKey, List<TinyField>> fields = new HashMap<>();
        int records = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!"v1\tofficial\tintermediary".equals(header)) {
                throw new IOException("Expected Tiny v1 official -> intermediary header in " + path + ", got: " + header);
            }
            int lineNumber = 1;
            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                if (line.isBlank()) continue;
                String[] parts = line.split("\t", -1);
                if (parts[0].equals("CLASS") && parts.length == 3) {
                    records++;
                } else if (parts[0].equals("METHOD") && parts.length == 5) {
                    records++;
                } else if (parts[0].equals("FIELD") && parts.length == 5) {
                    records++;
                    add(fields, new FieldKey(parts[1], parts[3]),
                            new TinyField(parts[1], parts[2], parts[3]));
                } else {
                    throw new IOException("Unsupported Tiny v1 row at " + path + ":" + lineNumber + ": " + line);
                }
                if (records > MAX_RECORDS) {
                    throw new IOException("Tiny mappings exceed the " + MAX_RECORDS + " record limit: " + path);
                }
            }
        }
        return fields;
    }

    private static DescriptorResult normalizeDescriptor(
            String descriptor,
            Map<String, List<String>> classNames,
            Set<String> knownClasses) {
        if (descriptor == null || descriptor.isBlank() || !isValidDescriptor(descriptor)) {
            return new DescriptorResult(null, 0, false);
        }
        StringBuilder normalized = new StringBuilder(descriptor.length());
        int externalTypes = 0;
        for (int index = 0; index < descriptor.length();) {
            char current = descriptor.charAt(index);
            if (current != 'L') {
                normalized.append(current);
                index++;
                continue;
            }
            int end = descriptor.indexOf(';', index + 1);
            if (end <= index + 1) return new DescriptorResult(null, externalTypes, false);
            String type = descriptor.substring(index + 1, end);
            List<String> targets = distinct(classNames.getOrDefault(type, List.of()));
            if (targets.size() == 1) {
                normalized.append('L').append(targets.getFirst()).append(';');
            } else if (targets.size() > 1 || knownClasses.contains(type)
                    || type.startsWith("net/minecraft/") || type.indexOf('/') < 0) {
                return new DescriptorResult(null, externalTypes, false);
            } else {
                normalized.append('L').append(type).append(';');
                externalTypes++;
            }
            index = end + 1;
        }
        return new DescriptorResult(normalized.toString(), externalTypes, true);
    }

    private static boolean isValidDescriptor(String descriptor) {
        try {
            if (descriptor.startsWith("(")) {
                return Type.getMethodType(descriptor).getDescriptor().equals(descriptor);
            }
            Type type = Type.getType(descriptor);
            return type.getSort() != Type.METHOD && type.getSort() != Type.VOID
                    && type.getDescriptor().equals(descriptor);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException exception) {
            return false;
        }
    }

    private static List<TinyField> distinctTinyFields(List<TinyField> source) {
        Map<String, TinyField> distinct = new TreeMap<>();
        for (TinyField field : source) {
            distinct.putIfAbsent(field.descriptor() + "|" + field.obfuscatedName(), field);
        }
        return List.copyOf(distinct.values());
    }

    private static List<String> distinct(List<String> values) {
        return values.stream().distinct().sorted().toList();
    }

    private static <K, V> void add(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }

    private static void requireInput(Path path, String label, long maximumBytes) throws IOException {
        if (path == null || !Files.isRegularFile(path)) throw new IOException("Missing " + label + ": " + path);
        long size = Files.size(path);
        if (size <= 0 || size > maximumBytes) {
            throw new IOException(label + " has unsupported size " + size + " bytes (maximum " + maximumBytes + ").");
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String toJson(AuditResult result) {
        StringBuilder json = new StringBuilder(4096);
        json.append("{\n");
        json.append("  \"schemaVersion\": 1,\n");
        json.append("  \"status\": \"PARTIAL\",\n");
        json.append("  \"sourceNamespace\": \"minecraft:forge-srg@1.20.1\",\n");
        json.append("  \"targetNamespace\": \"minecraft:official-obfuscated@1.20.1\",\n");
        json.append("  \"selectorTransformationPerformed\": false,\n");
        json.append("  \"runtimeMappingTableProduced\": false,\n");
        json.append("  \"artifacts\": {\n");
        json.append("    \"mcpconfigJoinedTsrg\": {\"sizeBytes\": ").append(result.tsrgSizeBytes())
                .append(", \"sha256\": \"").append(result.tsrgSha256()).append("\"},\n");
        json.append("    \"fabricIntermediary1201Tiny\": {\"sizeBytes\": ").append(result.sourceTinySizeBytes())
                .append(", \"sha256\": \"").append(result.sourceTinySha256()).append("\"}\n");
        json.append("  },\n");
        json.append("  \"coverage\": {\n");
        ClassCoverage c = result.classes();
        json.append("    \"classes\": {")
                .append("\"tsrgClassRows\": ").append(c.tsrgClassRows()).append(", ")
                .append("\"uniquelyMappedClassRows\": ").append(c.uniquelyMappedClassRows()).append(", ")
                .append("\"classRowsWithoutSrgName\": ").append(c.classRowsWithoutSrgName()).append(", ")
                .append("\"ambiguousSrgClassNames\": ").append(c.ambiguousSrgClassNames()).append(", ")
                .append("\"ambiguousObfuscatedClassNames\": ").append(c.ambiguousObfuscatedClassNames())
                .append("},\n");
        MethodCoverage m = result.methods();
        json.append("    \"methods\": {")
                .append("\"tsrgMethodRows\": ").append(m.tsrgMethodRows()).append(", ")
                .append("\"ownerMappedRows\": ").append(m.ownerMappedRows()).append(", ")
                .append("\"ownerUnmappedRows\": ").append(m.ownerUnmappedRows()).append(", ")
                .append("\"descriptorUnresolvedRows\": ").append(m.descriptorUnresolvedRows()).append(", ")
                .append("\"externalDescriptorRows\": ").append(m.externalDescriptorRows()).append(", ")
                .append("\"externalDescriptorOccurrences\": ").append(m.externalDescriptorOccurrences()).append(", ")
                .append("\"ownerNameGroups\": ").append(m.ownerNameGroups()).append(", ")
                .append("\"overloadedOwnerNameGroups\": ").append(m.overloadedOwnerNameGroups()).append(", ")
                .append("\"duplicateFullSelectorKeys\": ").append(m.duplicateFullSelectorKeys())
                .append("},\n");
        FieldCoverage f = result.fields();
        json.append("    \"fields\": {")
                .append("\"tsrgFieldRows\": ").append(f.tsrgFieldRows()).append(", ")
                .append("\"uniqueTinyDescriptorJoins\": ").append(f.uniqueTinyDescriptorJoins()).append(", ")
                .append("\"missingTinyFieldJoins\": ").append(f.missingTinyFieldJoins()).append(", ")
                .append("\"ambiguousTinyFieldJoins\": ").append(f.ambiguousTinyFieldJoins()).append(", ")
                .append("\"ownerUnmappedRows\": ").append(f.ownerUnmappedRows()).append(", ")
                .append("\"descriptorUnresolvedRows\": ").append(f.descriptorUnresolvedRows()).append(", ")
                .append("\"externalDescriptorRows\": ").append(f.externalDescriptorRows()).append(", ")
                .append("\"externalDescriptorOccurrences\": ").append(f.externalDescriptorOccurrences())
                .append("}\n");
        json.append("  },\n");
        json.append("  \"limitations\": [")
                .append("\"This is a read-only feasibility audit; no selector is transformed.\", ")
                .append("\"External descriptor passthrough does not prove target classpath presence.\", ")
                .append("\"The cross-version Intermediary and target runtime edges are separate stages.\"]\n");
        json.append("}\n");
        return json.toString();
    }
}
