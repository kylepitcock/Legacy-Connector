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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;

/**
 * Owner/member/descriptor-aware Forge SRG 1.20.1 -> Mojang-official 1.20.1 mapping view.
 *
 * <p>This composes the pinned MCPConfig SRG -> official-obfuscated relation with the pinned
 * Mojang ProGuard mapping. Unlike {@link SrgMojangTable}, it never reduces members to a global
 * SRG-token table: class owners and complete JVM descriptors remain part of every lookup.</p>
 *
 * <p>The class reads mapping bytes only. It does not rewrite selectors, package mapping data,
 * define Minecraft classes, or produce a runtime mapping payload.</p>
 */
public final class Forge1201SourceOfficialMappingIndex {
    private static final long MAX_MOJANG_MAPPING_BYTES = 64L * 1024 * 1024;
    private static final int MAX_MOJANG_ROWS = 4_000_000;
    private static final Pattern CLASS = Pattern.compile("^(\\S+) -> (\\S+):$");
    private static final Pattern METHOD = Pattern.compile(
            "^\\s+(?:\\d+:\\d+:)?(\\S+) (\\S+)\\((.*?)\\)(?::\\d+:\\d+)? -> (\\S+)$");
    private static final Pattern FIELD = Pattern.compile("^\\s+(\\S+) (\\S+) -> (\\S+)$");

    private Forge1201SourceOfficialMappingIndex() {}

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

    public record ClassCandidate(String srgName, String obfuscatedName, String officialName) {}

    public record ClassResolution(Status status, List<ClassCandidate> candidates) {
        public ClassResolution {
            candidates = List.copyOf(candidates);
        }
    }

    /**
     * Full identity at every hop. All owners are JVM internal names and descriptors are JVM
     * descriptors in their respective namespaces.
     */
    public record SelectorCandidate(
            String srgOwner,
            String srgName,
            String srgDescriptor,
            String obfuscatedOwner,
            String obfuscatedName,
            String obfuscatedDescriptor,
            String officialOwner,
            String officialName,
            String officialDescriptor,
            int externalDescriptorTypeCount) {}

    public record SelectorResolution(Status status, List<SelectorCandidate> candidates) {
        public SelectorResolution {
            candidates = List.copyOf(candidates);
        }
    }

    public record Summary(
            long mojangMappingBytes,
            String mojangMappingSha256,
            int mojangClassRows,
            int mojangMethodRows,
            int mojangFieldRows,
            int uniqueObfuscatedClasses,
            int ambiguousObfuscatedClasses,
            int uniqueMethodKeys,
            int ambiguousMethodKeys,
            int uniqueFieldKeys,
            int ambiguousFieldKeys,
            Forge1201SrgObfuscatedBridgeAudit.AuditResult srgToObfuscated) {}

    private record OwnerName(String owner, String name) {}
    private record MethodKey(String owner, String name, String descriptor) {}
    private record FieldKey(String owner, String name) {}
    private record MethodMapping(String officialName) {}
    private record FieldMapping(String obfuscatedDescriptor, String officialName) {}
    private record DescriptorResult(Status status, String descriptor, int externalTypeCount) {}

    private final Forge1201SrgObfuscatedBridgeAudit.Index srgToObfuscated;
    private final Map<String, List<String>> obfuscatedToOfficialClasses;
    private final Map<String, List<String>> officialToObfuscatedClasses;
    private final Map<MethodKey, List<MethodMapping>> methods;
    private final Map<FieldKey, List<FieldMapping>> fields;
    private final Set<OwnerName> methodOwnerNames;
    private final Summary summary;

    private Forge1201SourceOfficialMappingIndex(
            Forge1201SrgObfuscatedBridgeAudit.Index srgToObfuscated,
            Map<String, List<String>> obfuscatedToOfficialClasses,
            Map<String, List<String>> officialToObfuscatedClasses,
            Map<MethodKey, List<MethodMapping>> methods,
            Map<FieldKey, List<FieldMapping>> fields,
            Set<OwnerName> methodOwnerNames,
            Summary summary) {
        this.srgToObfuscated = srgToObfuscated;
        this.obfuscatedToOfficialClasses = immutableLists(obfuscatedToOfficialClasses);
        this.officialToObfuscatedClasses = immutableLists(officialToObfuscatedClasses);
        this.methods = immutableLists(methods);
        this.fields = immutableLists(fields);
        this.methodOwnerNames = Set.copyOf(methodOwnerNames);
        this.summary = summary;
    }

    public Summary summary() {
        return summary;
    }

    /** Resolves an SRG class owner through both pinned source mapping edges. */
    public ClassResolution resolveClass(String srgOwner) {
        Forge1201SrgObfuscatedBridgeAudit.ClassResolution first = srgToObfuscated.resolveClass(srgOwner);
        if (first.status() == Forge1201SrgObfuscatedBridgeAudit.Status.MISSING) {
            return new ClassResolution(Status.OWNER_MISSING, List.of());
        }
        if (first.status() == Forge1201SrgObfuscatedBridgeAudit.Status.AMBIGUOUS) {
            return new ClassResolution(Status.OWNER_AMBIGUOUS, List.of());
        }

        String obfuscatedName = first.obfuscatedNames().getFirst();
        List<String> officialNames = distinct(obfuscatedToOfficialClasses.getOrDefault(obfuscatedName, List.of()));
        if (officialNames.isEmpty()) return new ClassResolution(Status.OWNER_MISSING, List.of());

        List<ClassCandidate> candidates = officialNames.stream()
                .map(name -> new ClassCandidate(srgOwner, obfuscatedName, name))
                .toList();
        return new ClassResolution(
                candidates.size() == 1 ? Status.MAPPED_EXACT : Status.OWNER_AMBIGUOUS,
                candidates);
    }

    /**
     * Resolves by SRG owner, SRG token, and full SRG descriptor, then matches the exact
     * obfuscated owner/name/descriptor row in Mojang's 1.20.1 ProGuard mappings.
     */
    public SelectorResolution resolveMethod(String srgOwner, String srgName, String srgDescriptor) {
        Forge1201SrgObfuscatedBridgeAudit.Resolution first =
                srgToObfuscated.resolveMethod(srgOwner, srgName, srgDescriptor);
        Status firstStatus = fromSrgStatus(first.status());
        if (firstStatus != Status.MAPPED_EXACT && firstStatus != Status.MAPPED_WITH_EXTERNAL_TYPES) {
            return new SelectorResolution(firstStatus, List.of());
        }
        if (first.candidates().size() != 1) {
            return new SelectorResolution(Status.MEMBER_AMBIGUOUS, List.of());
        }

        Forge1201SrgObfuscatedBridgeAudit.Candidate mapped = first.candidates().getFirst();
        ClassResolution owner = resolveObfuscatedClass(srgOwner, mapped.obfuscatedOwner());
        if (owner.status() != Status.MAPPED_EXACT) {
            return new SelectorResolution(owner.status(), List.of());
        }

        MethodKey key = new MethodKey(
                mapped.obfuscatedOwner(), mapped.obfuscatedName(), mapped.obfuscatedDescriptor());
        List<MethodMapping> methodRows = distinctMethods(methods.getOrDefault(key, List.of()));
        if (methodRows.isEmpty()) {
            boolean sameName = methodOwnerNames.contains(new OwnerName(mapped.obfuscatedOwner(), mapped.obfuscatedName()));
            return new SelectorResolution(
                    sameName ? Status.DESCRIPTOR_MISMATCH : Status.MEMBER_MISSING, List.of());
        }
        if (methodRows.size() != 1) {
            return new SelectorResolution(Status.MEMBER_AMBIGUOUS, List.of());
        }

        DescriptorResult descriptor = remapObfuscatedDescriptor(mapped.obfuscatedDescriptor(), true);
        if (descriptor.status() != Status.MAPPED_EXACT
                && descriptor.status() != Status.MAPPED_WITH_EXTERNAL_TYPES) {
            return new SelectorResolution(descriptor.status(), List.of());
        }

        ClassCandidate classCandidate = owner.candidates().getFirst();
        int externalCount = Math.max(mapped.externalDescriptorTypeCount(), descriptor.externalTypeCount());
        SelectorCandidate candidate = new SelectorCandidate(
                srgOwner, srgName, srgDescriptor,
                mapped.obfuscatedOwner(), mapped.obfuscatedName(), mapped.obfuscatedDescriptor(),
                classCandidate.officialName(), methodRows.getFirst().officialName(),
                descriptor.descriptor(), externalCount);
        return new SelectorResolution(
                externalCount == 0 ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                List.of(candidate));
    }

    /**
     * Resolves a field only after the source Tiny join supplied one exact obfuscated descriptor.
     * The Mojang field row's declared type is also checked against that descriptor.
     */
    public SelectorResolution resolveField(String srgOwner, String srgName) {
        Forge1201SrgObfuscatedBridgeAudit.Resolution first =
                srgToObfuscated.resolveField(srgOwner, srgName);
        Status firstStatus = fromSrgStatus(first.status());
        if (firstStatus != Status.MAPPED_EXACT && firstStatus != Status.MAPPED_WITH_EXTERNAL_TYPES) {
            return new SelectorResolution(firstStatus, List.of());
        }
        if (first.candidates().size() != 1) {
            return new SelectorResolution(Status.MEMBER_AMBIGUOUS, List.of());
        }

        Forge1201SrgObfuscatedBridgeAudit.Candidate mapped = first.candidates().getFirst();
        ClassResolution owner = resolveObfuscatedClass(srgOwner, mapped.obfuscatedOwner());
        if (owner.status() != Status.MAPPED_EXACT) {
            return new SelectorResolution(owner.status(), List.of());
        }

        FieldKey key = new FieldKey(mapped.obfuscatedOwner(), mapped.obfuscatedName());
        List<FieldMapping> fieldRows = distinctFields(fields.getOrDefault(key, List.of()));
        if (fieldRows.isEmpty()) return new SelectorResolution(Status.MEMBER_MISSING, List.of());

        List<FieldMapping> exactRows = fieldRows.stream()
                .filter(row -> row.obfuscatedDescriptor().equals(mapped.obfuscatedDescriptor()))
                .toList();
        if (exactRows.isEmpty()) return new SelectorResolution(Status.DESCRIPTOR_MISMATCH, List.of());
        Set<String> names = new LinkedHashSet<>();
        exactRows.forEach(row -> names.add(row.officialName()));
        if (names.size() != 1) return new SelectorResolution(Status.MEMBER_AMBIGUOUS, List.of());

        DescriptorResult descriptor = remapObfuscatedDescriptor(mapped.obfuscatedDescriptor(), false);
        if (descriptor.status() != Status.MAPPED_EXACT
                && descriptor.status() != Status.MAPPED_WITH_EXTERNAL_TYPES) {
            return new SelectorResolution(descriptor.status(), List.of());
        }

        ClassCandidate classCandidate = owner.candidates().getFirst();
        int externalCount = mapped.externalDescriptorTypeCount() + descriptor.externalTypeCount();
        SelectorCandidate candidate = new SelectorCandidate(
                srgOwner, srgName, mapped.sourceDescriptor(),
                mapped.obfuscatedOwner(), mapped.obfuscatedName(), mapped.obfuscatedDescriptor(),
                classCandidate.officialName(), names.iterator().next(), descriptor.descriptor(), externalCount);
        return new SelectorResolution(
                externalCount == 0 ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                List.of(candidate));
    }

    /**
     * Reverse lookup for diagnostics that start with a Mojang-named source runtime identity.
     * The returned selector candidates still retain their complete SRG and obfuscated identities.
     */
    public SelectorResolution findMethodsByOfficialIdentity(
            String officialOwner, String officialName, String officialDescriptor) {
        if (!validDescriptor(officialDescriptor, true)) {
            return new SelectorResolution(Status.DESCRIPTOR_UNRESOLVED, List.of());
        }
        List<SelectorCandidate> matches = new ArrayList<>();
        for (Forge1201SrgObfuscatedBridgeAudit.MethodSelector selector : srgToObfuscated.methodSelectors()) {
            if (selector.srgOwner() == null || selector.srgName() == null || selector.srgDescriptor() == null) {
                continue;
            }
            List<String> officialOwners = distinct(
                    obfuscatedToOfficialClasses.getOrDefault(selector.obfuscatedOwner(), List.of()));
            if (officialOwners.size() != 1 || !officialOwners.getFirst().equals(officialOwner)) continue;

            MethodKey key = new MethodKey(
                    selector.obfuscatedOwner(), selector.obfuscatedName(), selector.obfuscatedDescriptor());
            List<MethodMapping> methodRows = distinctMethods(methods.getOrDefault(key, List.of()));
            if (methodRows.stream().noneMatch(row -> row.officialName().equals(officialName))) continue;

            DescriptorResult descriptor = remapObfuscatedDescriptor(selector.obfuscatedDescriptor(), true);
            if ((descriptor.status() != Status.MAPPED_EXACT
                    && descriptor.status() != Status.MAPPED_WITH_EXTERNAL_TYPES)
                    || !officialDescriptor.equals(descriptor.descriptor())) {
                continue;
            }

            Forge1201SrgObfuscatedBridgeAudit.Resolution source = srgToObfuscated.resolveMethod(
                    selector.srgOwner(), selector.srgName(), selector.srgDescriptor());
            if ((source.status() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT
                    && source.status() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES)
                    || source.candidates().size() != 1) {
                continue;
            }
            Forge1201SrgObfuscatedBridgeAudit.Candidate mapped = source.candidates().getFirst();
            int externalCount = Math.max(mapped.externalDescriptorTypeCount(), descriptor.externalTypeCount());
            matches.add(new SelectorCandidate(
                    selector.srgOwner(), selector.srgName(), selector.srgDescriptor(),
                    mapped.obfuscatedOwner(), mapped.obfuscatedName(), mapped.obfuscatedDescriptor(),
                    officialOwner, officialName, descriptor.descriptor(), externalCount));
        }
        return resolutionFromMatches(matches);
    }

    /** Reverse lookup for fields; a unique Tiny-derived descriptor is still mandatory. */
    public SelectorResolution findFieldsByOfficialIdentity(
            String officialOwner, String officialName, String officialDescriptor) {
        if (!validDescriptor(officialDescriptor, false)) {
            return new SelectorResolution(Status.DESCRIPTOR_UNRESOLVED, List.of());
        }
        List<SelectorCandidate> matches = new ArrayList<>();
        for (Forge1201SrgObfuscatedBridgeAudit.FieldSelector selector : srgToObfuscated.fieldSelectors()) {
            if (selector.srgOwner() == null || selector.srgName() == null
                    || selector.sourceDescriptor() == null || selector.obfuscatedDescriptor() == null) {
                continue;
            }
            List<String> officialOwners = distinct(
                    obfuscatedToOfficialClasses.getOrDefault(selector.obfuscatedOwner(), List.of()));
            if (officialOwners.size() != 1 || !officialOwners.getFirst().equals(officialOwner)) continue;

            FieldKey key = new FieldKey(selector.obfuscatedOwner(), selector.obfuscatedName());
            List<FieldMapping> fieldRows = distinctFields(fields.getOrDefault(key, List.of()));
            boolean nameAndDescriptorMatch = fieldRows.stream().anyMatch(row ->
                    row.officialName().equals(officialName)
                            && row.obfuscatedDescriptor().equals(selector.obfuscatedDescriptor()));
            if (!nameAndDescriptorMatch) continue;

            DescriptorResult descriptor = remapObfuscatedDescriptor(selector.obfuscatedDescriptor(), false);
            if ((descriptor.status() != Status.MAPPED_EXACT
                    && descriptor.status() != Status.MAPPED_WITH_EXTERNAL_TYPES)
                    || !officialDescriptor.equals(descriptor.descriptor())) {
                continue;
            }

            Forge1201SrgObfuscatedBridgeAudit.Resolution source =
                    srgToObfuscated.resolveField(selector.srgOwner(), selector.srgName());
            if ((source.status() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT
                    && source.status() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES)
                    || source.candidates().size() != 1) {
                continue;
            }
            Forge1201SrgObfuscatedBridgeAudit.Candidate mapped = source.candidates().getFirst();
            int externalCount = Math.max(mapped.externalDescriptorTypeCount(), descriptor.externalTypeCount());
            matches.add(new SelectorCandidate(
                    selector.srgOwner(), selector.srgName(), selector.sourceDescriptor(),
                    mapped.obfuscatedOwner(), mapped.obfuscatedName(), mapped.obfuscatedDescriptor(),
                    officialOwner, officialName, descriptor.descriptor(), externalCount));
        }
        return resolutionFromMatches(matches);
    }

    private static SelectorResolution resolutionFromMatches(List<SelectorCandidate> candidates) {
        List<SelectorCandidate> unique = candidates.stream().distinct()
                .sorted(java.util.Comparator
                        .comparing(SelectorCandidate::srgOwner)
                        .thenComparing(SelectorCandidate::srgName)
                        .thenComparing(SelectorCandidate::srgDescriptor))
                .toList();
        if (unique.isEmpty()) return new SelectorResolution(Status.MEMBER_MISSING, List.of());
        if (unique.size() > 1) return new SelectorResolution(Status.MEMBER_AMBIGUOUS, unique);
        return new SelectorResolution(
                unique.getFirst().externalDescriptorTypeCount() == 0
                        ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                unique);
    }

    private ClassResolution resolveObfuscatedClass(String srgOwner, String obfuscatedOwner) {
        List<String> officialNames = distinct(obfuscatedToOfficialClasses.getOrDefault(obfuscatedOwner, List.of()));
        if (officialNames.isEmpty()) return new ClassResolution(Status.OWNER_MISSING, List.of());
        List<ClassCandidate> candidates = officialNames.stream()
                .map(name -> new ClassCandidate(srgOwner, obfuscatedOwner, name))
                .toList();
        return new ClassResolution(
                candidates.size() == 1 ? Status.MAPPED_EXACT : Status.OWNER_AMBIGUOUS,
                candidates);
    }

    private DescriptorResult remapObfuscatedDescriptor(String descriptor, boolean method) {
        if (!validDescriptor(descriptor, method)) {
            return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, 0);
        }

        StringBuilder mapped = new StringBuilder(descriptor.length());
        int externalTypes = 0;
        for (int index = 0; index < descriptor.length();) {
            char current = descriptor.charAt(index);
            if (current != 'L') {
                mapped.append(current);
                index++;
                continue;
            }
            int end = descriptor.indexOf(';', index + 1);
            if (end <= index + 1) {
                return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, externalTypes);
            }
            String obfuscatedType = descriptor.substring(index + 1, end);
            List<String> officialTypes = distinct(
                    obfuscatedToOfficialClasses.getOrDefault(obfuscatedType, List.of()));
            if (officialTypes.size() == 1) {
                mapped.append('L').append(officialTypes.getFirst()).append(';');
            } else if (officialTypes.size() > 1) {
                return new DescriptorResult(Status.DESCRIPTOR_AMBIGUOUS, null, externalTypes);
            } else if (srgToObfuscated.knowsObfuscatedClass(obfuscatedType)
                    || obfuscatedType.startsWith("net/minecraft/")
                    || obfuscatedType.indexOf('/') < 0) {
                return new DescriptorResult(Status.DESCRIPTOR_UNRESOLVED, null, externalTypes);
            } else {
                mapped.append('L').append(obfuscatedType).append(';');
                externalTypes++;
            }
            index = end + 1;
        }
        return new DescriptorResult(
                externalTypes == 0 ? Status.MAPPED_EXACT : Status.MAPPED_WITH_EXTERNAL_TYPES,
                mapped.toString(), externalTypes);
    }

    /**
     * Reads the exact MCPConfig/Tiny source bridge and Mojang 1.20.1 client ProGuard mapping.
     * The caller is responsible for verifying/pinning the Mojang mapping digest.
     */
    public static Forge1201SourceOfficialMappingIndex read(
            Path joinedTsrg, Path sourceTiny, Path mojangClientTxt) throws IOException {
        Objects.requireNonNull(mojangClientTxt, "mojangClientTxt");
        Forge1201SrgObfuscatedBridgeAudit.Index source =
                Forge1201SrgObfuscatedBridgeAudit.read(joinedTsrg, sourceTiny);
        ParsedMojang parsed = readMojang(mojangClientTxt, source);

        Summary summary = new Summary(
                parsed.sizeBytes(),
                parsed.sha256(),
                parsed.classRows(),
                parsed.methodRows(),
                parsed.fieldRows(),
                parsed.obfuscatedToOfficial().size(),
                ambiguousKeys(parsed.obfuscatedToOfficial()),
                parsed.methods().size(),
                ambiguousKeys(parsed.methods()),
                parsed.fields().size(),
                ambiguousKeys(parsed.fields()),
                source.audit());
        return new Forge1201SourceOfficialMappingIndex(
                source,
                parsed.obfuscatedToOfficial(),
                parsed.officialToObfuscated(),
                parsed.methods(),
                parsed.fields(),
                parsed.methodOwnerNames(),
                summary);
    }

    private record ParsedMojang(
            Map<String, List<String>> obfuscatedToOfficial,
            Map<String, List<String>> officialToObfuscated,
            Map<MethodKey, List<MethodMapping>> methods,
            Map<FieldKey, List<FieldMapping>> fields,
            Set<OwnerName> methodOwnerNames,
            long sizeBytes,
            String sha256,
            int classRows,
            int methodRows,
            int fieldRows) {}

    private static ParsedMojang readMojang(Path mappingFile,
            Forge1201SrgObfuscatedBridgeAudit.Index source) throws IOException {
        if (!Files.isRegularFile(mappingFile) || !Files.isReadable(mappingFile)) {
            throw new IOException("Mojang 1.20.1 client mappings are not a readable regular file: " + mappingFile);
        }
        long size = Files.size(mappingFile);
        if (size <= 0 || size > MAX_MOJANG_MAPPING_BYTES) {
            throw new IOException("Mojang 1.20.1 client mapping size is outside the supported range: " + size);
        }

        Map<String, List<String>> obfuscatedToOfficial = new HashMap<>();
        Map<String, List<String>> officialToObfuscated = new HashMap<>();
        Map<MethodKey, List<MethodMapping>> methods = new HashMap<>();
        Map<FieldKey, List<FieldMapping>> fields = new HashMap<>();
        Set<OwnerName> methodOwnerNames = new LinkedHashSet<>();
        int classRows = 0;
        int methodRows = 0;
        int fieldRows = 0;
        int records = 0;
        String currentOfficialOwner = null;
        String currentObfuscatedOwner = null;

        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(mappingFile), decoder))) {
            int lineNumber = 0;
            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) continue;
                int indentation = 0;
                while (indentation < line.length() && Character.isWhitespace(line.charAt(indentation))) {
                    indentation++;
                }
                // ProGuard parameter/local-variable rows are indented more deeply than class members.
                if (indentation > 4) continue;

                Matcher matcher = CLASS.matcher(line);
                if (matcher.matches()) {
                    currentOfficialOwner = matcher.group(1).replace('.', '/');
                    currentObfuscatedOwner = matcher.group(2).replace('.', '/');
                    if (!validInternalName(currentOfficialOwner) || !validInternalName(currentObfuscatedOwner)) {
                        throw malformed(mappingFile, lineNumber, "invalid class mapping row");
                    }
                    add(obfuscatedToOfficial, currentObfuscatedOwner, currentOfficialOwner);
                    add(officialToObfuscated, currentOfficialOwner, currentObfuscatedOwner);
                    classRows++;
                    records++;
                    if (records > MAX_MOJANG_ROWS) {
                        throw new IOException("Mojang mappings exceed the " + MAX_MOJANG_ROWS
                                + " class/member row limit: " + mappingFile);
                    }
                    continue;
                }

                if (currentOfficialOwner == null || currentObfuscatedOwner == null) {
                    throw malformed(mappingFile, lineNumber, "member row appears before a class row");
                }

                matcher = METHOD.matcher(line);
                if (matcher.matches()) {
                    String returnType = matcher.group(1);
                    String officialName = matcher.group(2);
                    String arguments = matcher.group(3);
                    String obfuscatedName = matcher.group(4);
                    String obfuscatedDescriptor = proguardMethodDescriptor(returnType, arguments, officialToObfuscated);
                    if (!validDescriptor(obfuscatedDescriptor, true)) {
                        throw malformed(mappingFile, lineNumber, "invalid method descriptor");
                    }
                    add(methods,
                            new MethodKey(currentObfuscatedOwner, obfuscatedName, obfuscatedDescriptor),
                            new MethodMapping(officialName));
                    methodOwnerNames.add(new OwnerName(currentObfuscatedOwner, obfuscatedName));
                    methodRows++;
                    records++;
                    if (records > MAX_MOJANG_ROWS) {
                        throw new IOException("Mojang mappings exceed the " + MAX_MOJANG_ROWS
                                + " class/member row limit: " + mappingFile);
                    }
                    continue;
                }

                matcher = FIELD.matcher(line);
                if (matcher.matches()) {
                    String fieldType = matcher.group(1);
                    String officialName = matcher.group(2);
                    String obfuscatedName = matcher.group(3);
                    String obfuscatedDescriptor = proguardTypeDescriptor(fieldType, officialToObfuscated);
                    if (!validDescriptor(obfuscatedDescriptor, false)) {
                        throw malformed(mappingFile, lineNumber, "invalid field descriptor");
                    }
                    add(fields, new FieldKey(currentObfuscatedOwner, obfuscatedName),
                            new FieldMapping(obfuscatedDescriptor, officialName));
                    fieldRows++;
                    records++;
                }

                if (records > MAX_MOJANG_ROWS) {
                    throw new IOException("Mojang mappings exceed the " + MAX_MOJANG_ROWS
                            + " class/member row limit: " + mappingFile);
                }
            }
        }

        if (classRows == 0) throw new IOException("Mojang mapping contains no class rows: " + mappingFile);
        return new ParsedMojang(
                immutableLists(obfuscatedToOfficial),
                immutableLists(officialToObfuscated),
                immutableLists(methods),
                immutableLists(fields),
                Set.copyOf(methodOwnerNames),
                size,
                sha256(mappingFile),
                classRows,
                methodRows,
                fieldRows);
    }

    private static String proguardMethodDescriptor(
            String returnType, String arguments, Map<String, List<String>> officialToObfuscated) throws IOException {
        StringBuilder descriptor = new StringBuilder("(");
        if (!arguments.isBlank()) {
            for (String argument : arguments.split(",")) {
                descriptor.append(proguardTypeDescriptor(argument.trim(), officialToObfuscated));
            }
        }
        return descriptor.append(')')
                .append(proguardTypeDescriptor(returnType, officialToObfuscated))
                .toString();
    }

    private static String proguardTypeDescriptor(
            String input, Map<String, List<String>> officialToObfuscated) throws IOException {
        String type = input.trim();
        int dimensions = 0;
        while (type.endsWith("[]")) {
            dimensions++;
            type = type.substring(0, type.length() - 2).trim();
        }
        String base = switch (type) {
            case "int" -> "I";
            case "long" -> "J";
            case "double" -> "D";
            case "float" -> "F";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "void" -> "V";
            default -> {
                List<String> mapped = distinct(officialToObfuscated.getOrDefault(
                        type.replace('.', '/'), List.of()));
                if (mapped.size() > 1) {
                    throw new IOException("Ambiguous Mojang official class in member descriptor: " + type);
                }
                if (mapped.isEmpty() && type.startsWith("net.minecraft.")) {
                    throw new IOException("Unmapped Minecraft type in Mojang member descriptor: " + type);
                }
                String internalName = mapped.isEmpty() ? type.replace('.', '/') : mapped.getFirst();
                if (!validInternalName(internalName)) {
                    throw new IOException("Invalid type in Mojang member descriptor: " + type);
                }
                yield "L" + internalName + ";";
            }
        };
        if (dimensions > 0 && base.equals("V")) {
            throw new IOException("void array in Mojang member descriptor: " + input);
        }
        return "[".repeat(dimensions) + base;
    }

    private static Status fromSrgStatus(Forge1201SrgObfuscatedBridgeAudit.Status status) {
        return switch (status) {
            case MAPPED_EXACT -> Status.MAPPED_EXACT;
            case MAPPED_WITH_EXTERNAL_TYPES -> Status.MAPPED_WITH_EXTERNAL_TYPES;
            case OWNER_MISSING -> Status.OWNER_MISSING;
            case OWNER_AMBIGUOUS -> Status.OWNER_AMBIGUOUS;
            case DESCRIPTOR_UNRESOLVED -> Status.DESCRIPTOR_UNRESOLVED;
            case AMBIGUOUS -> Status.MEMBER_AMBIGUOUS;
            case MISSING -> Status.MEMBER_MISSING;
        };
    }

    private static boolean validDescriptor(String descriptor, boolean method) {
        try {
            if (method) {
                return Type.getMethodType(descriptor).getDescriptor().equals(descriptor);
            }
            Type type = Type.getType(descriptor);
            return type.getSort() != Type.METHOD && type.getSort() != Type.VOID
                    && type.getDescriptor().equals(descriptor);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException exception) {
            return false;
        }
    }

    private static boolean validInternalName(String name) {
        return name != null && !name.isBlank() && !name.startsWith("/") && !name.endsWith("/")
                && name.indexOf('.') < 0 && name.indexOf(';') < 0 && name.indexOf('[') < 0;
    }

    private static IOException malformed(Path path, int line, String reason) {
        return new IOException("Malformed Mojang ProGuard mapping at " + path + ":" + line + ": " + reason);
    }

    private static <K, V> Map<K, List<V>> immutableLists(Map<K, List<V>> source) {
        Map<K, List<V>> copy = new HashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static <K, V> int ambiguousKeys(Map<K, List<V>> source) {
        return (int) source.values().stream()
                .filter(values -> new LinkedHashSet<>(values).size() > 1)
                .count();
    }

    private static List<MethodMapping> distinctMethods(List<MethodMapping> rows) {
        Map<String, MethodMapping> values = new HashMap<>();
        for (MethodMapping row : rows) values.putIfAbsent(row.officialName(), row);
        return List.copyOf(values.values());
    }

    private static List<FieldMapping> distinctFields(List<FieldMapping> rows) {
        Map<String, FieldMapping> values = new HashMap<>();
        for (FieldMapping row : rows) {
            values.putIfAbsent(row.obfuscatedDescriptor() + "|" + row.officialName(), row);
        }
        return List.copyOf(values.values());
    }

    private static List<String> distinct(List<String> values) {
        return values.stream().distinct().sorted().toList();
    }

    private static <K, V> void add(Map<K, List<V>> map, K key, V value) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
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
}
