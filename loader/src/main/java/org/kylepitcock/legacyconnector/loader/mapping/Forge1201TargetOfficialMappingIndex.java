package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;

/**
 * Read-only index for the NeoForm 1.21.1 merged TSRG2 mapping edge:
 * official-obfuscated names on the left to Mojang-official names on the right.
 *
 * <p>TSRG2 member descriptors are expressed in the left/original namespace. Every
 * object type in a descriptor must therefore be remapped through the class table
 * before the result can be checked against the Mojang-named target compile classpath.</p>
 *
 * <p>This class does not load mapping bytes from a URL, package them, rewrite selectors,
 * or assert runtime compatibility. The caller is responsible for pinning the exact
 * mapping artifact and retaining its provenance/license evidence.</p>
 */
public final class Forge1201TargetOfficialMappingIndex {
    private static final String HEADER = "tsrg2 left right";
    private static final long MAX_MAPPING_BYTES = 64L * 1024 * 1024;
    private static final Pattern OBJECT_TYPE = Pattern.compile("L([^;]+);");

    private Forge1201TargetOfficialMappingIndex() {}

    public enum Status {
        MAPPED_EXACT,
        MAPPED_WITH_EXTERNAL_TYPES,
        OWNER_MISSING,
        OWNER_AMBIGUOUS,
        MEMBER_MISSING,
        MEMBER_AMBIGUOUS,
        DESCRIPTOR_MISMATCH,
        DESCRIPTOR_UNRESOLVED,
        DESCRIPTOR_AMBIGUOUS
    }

    public record ClassCandidate(String obfuscatedName, String officialName) {}

    public record ClassResolution(Status status, List<ClassCandidate> candidates) {
        public ClassResolution {
            candidates = List.copyOf(candidates);
        }
    }

    public record MethodCandidate(
            String obfuscatedOwner,
            String obfuscatedName,
            String obfuscatedDescriptor,
            String officialOwner,
            String officialName,
            String officialDescriptor,
            int externalDescriptorTypeCount) {}

    public record MethodResolution(Status status, List<MethodCandidate> candidates) {
        public MethodResolution {
            candidates = List.copyOf(candidates);
        }
    }

    public record FieldCandidate(
            String obfuscatedOwner,
            String obfuscatedName,
            String obfuscatedDescriptor,
            String officialOwner,
            String officialName,
            String officialDescriptor,
            int externalDescriptorTypeCount) {}

    public record FieldResolution(Status status, List<FieldCandidate> candidates) {
        public FieldResolution {
            candidates = List.copyOf(candidates);
        }
    }

    public record Summary(
            long inputBytes,
            String sha256,
            int classRows,
            int methodRows,
            int fieldRows,
            int uniqueClassKeys,
            int ambiguousClassKeys,
            int uniqueMethodKeys,
            int ambiguousMethodKeys,
            int uniqueFieldKeys,
            int ambiguousFieldKeys) {}

    private record MethodKey(String owner, String name, String descriptor) {}
    private record FieldKey(String owner, String name) {}
    private record MethodMapping(String name) {}
    private record FieldMapping(String descriptor, String name) {}
    private record DescriptorResult(Status status, String descriptor, int externalTypeCount) {}

    public static final class Index {
        private final Map<String, List<String>> classes;
        private final Map<MethodKey, List<MethodMapping>> methods;
        private final Map<FieldKey, List<FieldMapping>> fields;
        private final Summary summary;

        private Index(
                Map<String, List<String>> classes,
                Map<MethodKey, List<MethodMapping>> methods,
                Map<FieldKey, List<FieldMapping>> fields,
                Summary summary) {
            this.classes = immutableLists(classes);
            this.methods = immutableLists(methods);
            this.fields = immutableLists(fields);
            this.summary = summary;
        }

        public Summary summary() {
            return summary;
        }

        /** Resolves one left-namespace class name to its right-namespace name. */
        public ClassResolution resolveClass(String obfuscatedName) {
            List<String> mapped = classes.getOrDefault(obfuscatedName, List.of());
            if (mapped.isEmpty()) return new ClassResolution(Status.OWNER_MISSING, List.of());
            List<ClassCandidate> candidates = mapped.stream()
                    .map(name -> new ClassCandidate(obfuscatedName, name))
                    .toList();
            return new ClassResolution(
                    candidates.size() == 1 ? Status.MAPPED_EXACT : Status.OWNER_AMBIGUOUS,
                    candidates);
        }

        /**
         * Resolves a method by exact left owner, name, and descriptor. The descriptor is
         * converted to the right namespace only after every object type is resolved.
         */
        public MethodResolution resolveMethod(String obfuscatedOwner, String obfuscatedName,
                String obfuscatedDescriptor) {
            ClassResolution owner = resolveClass(obfuscatedOwner);
            if (owner.status() == Status.OWNER_MISSING) {
                return new MethodResolution(Status.OWNER_MISSING, List.of());
            }
            if (owner.status() == Status.OWNER_AMBIGUOUS) {
                return new MethodResolution(Status.OWNER_AMBIGUOUS, List.of());
            }

            DescriptorResult descriptor = remapDescriptor(obfuscatedDescriptor, classes, true);
            if (descriptor.status() != Status.MAPPED_EXACT
                    && descriptor.status() != Status.MAPPED_WITH_EXTERNAL_TYPES) {
                return new MethodResolution(descriptor.status(), List.of());
            }

            MethodKey key = new MethodKey(obfuscatedOwner, obfuscatedName, obfuscatedDescriptor);
            List<MethodMapping> mapped = methods.getOrDefault(key, List.of());
            if (mapped.isEmpty()) {
                boolean sameName = methods.keySet().stream()
                        .anyMatch(candidate -> candidate.owner().equals(obfuscatedOwner)
                                && candidate.name().equals(obfuscatedName));
                return new MethodResolution(
                        sameName ? Status.DESCRIPTOR_MISMATCH : Status.MEMBER_MISSING, List.of());
            }
            if (mapped.size() != 1) {
                return new MethodResolution(Status.MEMBER_AMBIGUOUS, List.of());
            }

            MethodCandidate candidate = new MethodCandidate(
                    obfuscatedOwner,
                    obfuscatedName,
                    obfuscatedDescriptor,
                    owner.candidates().getFirst().officialName(),
                    mapped.getFirst().name(),
                    descriptor.descriptor(),
                    descriptor.externalTypeCount());
            return new MethodResolution(
                    descriptor.externalTypeCount() == 0
                            ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                    List.of(candidate));
        }

        /**
         * Resolves a field by exact left owner/name. TSRG2 may omit a field descriptor;
         * in that case the caller-supplied descriptor is still converted and retained.
         */
        public FieldResolution resolveField(String obfuscatedOwner, String obfuscatedName,
                String obfuscatedDescriptor) {
            ClassResolution owner = resolveClass(obfuscatedOwner);
            if (owner.status() == Status.OWNER_MISSING) {
                return new FieldResolution(Status.OWNER_MISSING, List.of());
            }
            if (owner.status() == Status.OWNER_AMBIGUOUS) {
                return new FieldResolution(Status.OWNER_AMBIGUOUS, List.of());
            }

            DescriptorResult descriptor = remapDescriptor(obfuscatedDescriptor, classes, false);
            if (descriptor.status() != Status.MAPPED_EXACT
                    && descriptor.status() != Status.MAPPED_WITH_EXTERNAL_TYPES) {
                return new FieldResolution(descriptor.status(), List.of());
            }

            FieldKey key = new FieldKey(obfuscatedOwner, obfuscatedName);
            List<FieldMapping> rows = fields.getOrDefault(key, List.of());
            if (rows.isEmpty()) return new FieldResolution(Status.MEMBER_MISSING, List.of());

            List<FieldMapping> matches = rows.stream()
                    .filter(row -> row.descriptor() == null
                            || row.descriptor().equals(obfuscatedDescriptor))
                    .toList();
            if (matches.isEmpty()) {
                return new FieldResolution(Status.DESCRIPTOR_MISMATCH, List.of());
            }

            LinkedHashSet<String> mappedNames = new LinkedHashSet<>();
            matches.forEach(row -> mappedNames.add(row.name()));
            if (mappedNames.size() != 1) {
                return new FieldResolution(Status.MEMBER_AMBIGUOUS, List.of());
            }

            FieldCandidate candidate = new FieldCandidate(
                    obfuscatedOwner,
                    obfuscatedName,
                    obfuscatedDescriptor,
                    owner.candidates().getFirst().officialName(),
                    mappedNames.iterator().next(),
                    descriptor.descriptor(),
                    descriptor.externalTypeCount());
            return new FieldResolution(
                    descriptor.externalTypeCount() == 0
                            ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                    List.of(candidate));
        }
    }

    /**
     * Reads only the deterministic TSRG2 left/right output. Input bytes are size-bounded,
     * UTF-8 is decoded strictly, and the complete file digest is retained in the summary.
     */
    public static Index read(Path mappingFile) throws IOException {
        Objects.requireNonNull(mappingFile, "mappingFile");
        if (!Files.isRegularFile(mappingFile) || !Files.isReadable(mappingFile)) {
            throw new IOException("Target merged TSRG2 mapping is not a readable regular file: " + mappingFile);
        }
        long size = Files.size(mappingFile);
        if (size <= 0 || size > MAX_MAPPING_BYTES) {
            throw new IOException("Target merged TSRG2 mapping size is outside the supported range: " + size);
        }
        String sha256 = sha256(mappingFile);

        Map<String, List<String>> classes = new LinkedHashMap<>();
        Map<MethodKey, List<MethodMapping>> methods = new LinkedHashMap<>();
        Map<FieldKey, List<FieldMapping>> fields = new LinkedHashMap<>();
        int classRows = 0;
        int methodRows = 0;
        int fieldRows = 0;
        int lineNumber = 0;
        String currentOwner = null;

        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(mappingFile), decoder))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) continue;

                int indentation = leadingTabs(line);
                String trimmed = line.substring(indentation).trim();
                if (lineNumber == 1) {
                    if (!HEADER.equals(trimmed)) {
                        throw malformed(lineNumber, "expected header '" + HEADER + "'");
                    }
                    continue;
                }

                if (indentation == 0) {
                    String[] parts = split(trimmed);
                    if (parts.length >= 2 && parts[0].endsWith("/") && parts[1].endsWith("/")) {
                        currentOwner = null; // Package mapping; not a class row.
                        continue;
                    }
                    if (parts.length < 2 || !validInternalName(parts[0]) || !validInternalName(parts[1])) {
                        throw malformed(lineNumber, "invalid class mapping row");
                    }
                    currentOwner = parts[0];
                    add(classes, parts[0], parts[1]);
                    classRows++;
                    continue;
                }

                if (indentation > 1) continue; // Parameter rows and method metadata.

                if (currentOwner == null) {
                    throw malformed(lineNumber, "member row has no preceding class row");
                }

                String[] parts = split(trimmed);
                if (parts.length == 2) {
                    add(fields, new FieldKey(currentOwner, parts[0]),
                            new FieldMapping(null, parts[1]));
                    fieldRows++;
                    continue;
                }
                if (parts.length < 3 || parts.length > 4) {
                    throw malformed(lineNumber, "unsupported member row shape");
                }

                if (parts[1].startsWith("(")) {
                    if (!validMethodDescriptor(parts[1])) {
                        throw malformed(lineNumber, "invalid method descriptor");
                    }
                    add(methods, new MethodKey(currentOwner, parts[0], parts[1]),
                            new MethodMapping(parts[2]));
                    methodRows++;
                } else {
                    if (!validFieldDescriptor(parts[1])) {
                        throw malformed(lineNumber, "invalid field descriptor");
                    }
                    add(fields, new FieldKey(currentOwner, parts[0]),
                            new FieldMapping(parts[1], parts[2]));
                    fieldRows++;
                }
            }
        }

        if (lineNumber == 0) throw new IOException("Target merged TSRG2 mapping is empty: " + mappingFile);
        if (classRows == 0) throw new IOException("Target merged TSRG2 mapping contains no class rows: " + mappingFile);

        Summary summary = new Summary(
                size,
                sha256,
                classRows,
                methodRows,
                fieldRows,
                classes.size(),
                ambiguousKeys(classes),
                methods.size(),
                ambiguousKeys(methods),
                fields.size(),
                ambiguousKeys(fields));
        return new Index(classes, methods, fields, summary);
    }

    /** Reads and verifies the caller-pinned complete mapping-file SHA-256. */
    public static Index read(Path mappingFile, String expectedSha256) throws IOException {
        if (expectedSha256 == null || !expectedSha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Expected mapping SHA-256 must contain exactly 64 hexadecimal characters.");
        }
        Index index = read(mappingFile);
        if (!index.summary().sha256().equalsIgnoreCase(expectedSha256)) {
            throw new IOException("Target merged TSRG2 SHA-256 mismatch: expected "
                    + expectedSha256 + " but read " + index.summary().sha256());
        }
        return index;
    }

    private static DescriptorResult remapDescriptor(String descriptor,
            Map<String, List<String>> classes, boolean method) {
        if (descriptor == null || descriptor.isBlank()) {
            return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, 0);
        }
        try {
            if (method) {
                Type parsed = Type.getMethodType(descriptor);
                if (!parsed.getDescriptor().equals(descriptor)) {
                    return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, 0);
                }
            } else {
                Type parsed = Type.getType(descriptor);
                if (parsed.getSort() == Type.VOID || parsed.getSort() == Type.METHOD
                        || !parsed.getDescriptor().equals(descriptor)) {
                    return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, 0);
                }
            }
        } catch (IllegalArgumentException invalid) {
            return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, 0);
        }

        Matcher matcher = OBJECT_TYPE.matcher(descriptor);
        StringBuilder mapped = new StringBuilder(descriptor.length());
        int cursor = 0;
        int externalCount = 0;
        boolean ambiguous = false;
        boolean unresolved = false;
        while (matcher.find()) {
            mapped.append(descriptor, cursor, matcher.start());
            String obfuscatedType = matcher.group(1);
            List<String> names = classes.getOrDefault(obfuscatedType, List.of());
            if (names.size() == 1) {
                mapped.append('L').append(names.getFirst()).append(';');
            } else if (names.size() > 1) {
                mapped.append(matcher.group());
                ambiguous = true;
            } else if (obfuscatedType.indexOf('/') >= 0) {
                // Package-qualified types absent from the Minecraft map are external candidates,
                // not proof of target classpath presence.
                mapped.append(matcher.group());
                externalCount++;
            } else {
                mapped.append(matcher.group());
                unresolved = true;
            }
            cursor = matcher.end();
        }
        mapped.append(descriptor, cursor, descriptor.length());

        if (ambiguous) return new DescriptorResult(Status.DESCRIPTOR_AMBIGUOUS, mapped.toString(), externalCount);
        if (unresolved) return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, mapped.toString(), externalCount);
        return new DescriptorResult(
                externalCount == 0 ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                mapped.toString(),
                externalCount);
    }

    private static boolean validMethodDescriptor(String descriptor) {
        try {
            return descriptor.startsWith("(")
                    && Type.getMethodType(descriptor).getDescriptor().equals(descriptor);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static boolean validFieldDescriptor(String descriptor) {
        try {
            Type type = Type.getType(descriptor);
            return type.getSort() != Type.VOID
                    && type.getSort() != Type.METHOD
                    && type.getDescriptor().equals(descriptor);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static int leadingTabs(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == '\t') count++;
        return count;
    }

    private static String[] split(String line) {
        return line.trim().split("\\s+");
    }

    private static boolean validInternalName(String name) {
        return name != null && !name.isBlank()
                && name.indexOf('.') < 0
                && name.indexOf(';') < 0
                && name.indexOf('[') < 0
                && name.indexOf(' ') < 0
                && name.indexOf('\t') < 0
                && !name.startsWith("/")
                && !name.endsWith("/");
    }

    private static IOException malformed(int line, String reason) {
        return new IOException("Malformed target merged TSRG2 mapping at line " + line + ": " + reason);
    }

    private static <K, V> void add(Map<K, List<V>> map, K key, V value) {
        List<V> values = map.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!values.contains(value)) values.add(value);
    }

    private static <K, V> Map<K, List<V>> immutableLists(Map<K, List<V>> source) {
        Map<K, List<V>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static <K, V> int ambiguousKeys(Map<K, List<V>> values) {
        return (int) values.values().stream().filter(candidates -> candidates.size() > 1).count();
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
