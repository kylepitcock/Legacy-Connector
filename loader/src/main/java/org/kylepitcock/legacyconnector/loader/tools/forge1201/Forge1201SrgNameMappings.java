package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads the source-version Forge MCPConfig SRG -> Mojang-name CSVs for the
 * read-only 1.20.1 -> 1.21.1 verifier.
 *
 * <p>This deliberately normalizes source member identifiers to the readable
 * 1.20.1 names before comparing them with the 1.21.1 target class files. It
 * never maps an old SRG identifier directly to a new-version SRG identifier.</p>
 */
final class Forge1201SrgNameMappings {
    static final String SOURCE_MAPPING_VERSION = "1.20.1-20230612.114412";

    private static final Pattern SRG_METHOD = Pattern.compile("m_[0-9]+_");
    private static final Pattern SRG_FIELD = Pattern.compile("f_[0-9]+_");

    private final Map<String, String> methods;
    private final Map<String, String> fields;

    private Forge1201SrgNameMappings(Map<String, String> methods, Map<String, String> fields) {
        this.methods = Map.copyOf(methods);
        this.fields = Map.copyOf(fields);
    }

    static Forge1201SrgNameMappings load(Path methodsCsv, Path fieldsCsv) throws IOException {
        return new Forge1201SrgNameMappings(
                readCsv(methodsCsv, SRG_METHOD, "method"),
                readCsv(fieldsCsv, SRG_FIELD, "field"));
    }

    private static Map<String, String> readCsv(Path path, Pattern expectedId, String kind) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Missing Forge 1.20.1 " + kind + " mapping CSV: " + path);
        }

        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header != null && header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }
            if (header == null || !header.trim().equals("searge,name,side,desc")) {
                throw new IOException("Unexpected " + kind + " mapping CSV header in " + path
                        + ": " + header);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;

                // The first two columns are unquoted identifiers. Limit the split so
                // commas inside the free-form description column do not affect parsing.
                int firstComma = line.indexOf(',');
                int secondComma = firstComma < 0 ? -1 : line.indexOf(',', firstComma + 1);
                if (firstComma <= 0 || secondComma <= firstComma + 1) {
                    throw new IOException("Malformed " + kind + " mapping row at "
                            + path + ":" + lineNumber);
                }

                String srgName = line.substring(0, firstComma).trim();
                String readableName = line.substring(firstComma + 1, secondComma).trim();
                if (!expectedId.matcher(srgName).matches() || readableName.isEmpty()) {
                    throw new IOException("Invalid " + kind + " mapping row at "
                            + path + ":" + lineNumber + " (" + srgName + ")");
                }

                String previous = result.putIfAbsent(srgName, readableName);
                if (previous != null && !previous.equals(readableName)) {
                    throw new IOException("Conflicting " + kind + " mappings for "
                            + srgName + " in " + path + ": " + previous + " vs " + readableName);
                }
            }
        }

        if (result.isEmpty()) {
            throw new IOException("No " + kind + " SRG mappings found in " + path);
        }
        return result;
    }

    Normalization normalize(Forge1201ApiUsageAnalyzer.Analysis source) {
        if (source == null) throw new IllegalArgumentException("Source API analysis is required");

        List<String> methodMappings = new ArrayList<>();
        List<String> fieldMappings = new ArrayList<>();
        List<String> unmappedMethods = new ArrayList<>();
        List<String> unmappedFields = new ArrayList<>();

        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> normalizedMethods =
                source.methodReferences().stream()
                        .map(reference -> normalizeMethod(reference, methodMappings, unmappedMethods))
                        .toList();
        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> normalizedFields =
                source.fieldReferences().stream()
                        .map(reference -> normalizeField(reference, fieldMappings, unmappedFields))
                        .toList();

        List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> normalizedCallSites =
                source.methodCallSites().stream()
                        .map(callSite -> {
                            if (!isMinecraftOwner(callSite.targetOwner())
                                    || !SRG_METHOD.matcher(callSite.targetName()).matches()) {
                                return callSite;
                            }
                            String mapped = methods.get(callSite.targetName());
                            return mapped == null ? callSite : new Forge1201ApiUsageAnalyzer.ApiMethodCallSite(
                                    callSite.callerClass(), callSite.callerMethod(), callSite.callerDescriptor(),
                                    callSite.targetOwner(), mapped, callSite.targetDescriptor(),
                                    callSite.opcode(), callSite.count());
                        })
                        .toList();

        List<Forge1201ApiUsageAnalyzer.ApiFieldCallSite> normalizedFieldCallSites =
                source.fieldCallSites().stream()
                        .map(callSite -> {
                            if (!isMinecraftOwner(callSite.targetOwner())
                                    || !SRG_FIELD.matcher(callSite.targetName()).matches()) {
                                return callSite;
                            }
                            String mapped = fields.get(callSite.targetName());
                            return mapped == null ? callSite : new Forge1201ApiUsageAnalyzer.ApiFieldCallSite(
                                    callSite.callerClass(), callSite.callerMethod(), callSite.callerDescriptor(),
                                    callSite.targetOwner(), mapped, callSite.targetDescriptor(),
                                    callSite.opcode(), callSite.count());
                        })
                        .toList();

        Forge1201ApiUsageAnalyzer.Analysis normalized = new Forge1201ApiUsageAnalyzer.Analysis(
                source.metadata(),
                source.classCount(),
                source.modEntrypoints(),
                source.mixinConfigs(),
                source.coreModClass(),
                source.packageReferences(),
                source.referencedApiClasses(),
                normalizedCallSites,
                normalizedFieldCallSites,
                normalizedMethods,
                normalizedFields);

        methodMappings.sort(String::compareTo);
        fieldMappings.sort(String::compareTo);
        unmappedMethods.sort(String::compareTo);
        unmappedFields.sort(String::compareTo);
        return new Normalization(normalized, methodMappings, fieldMappings, unmappedMethods, unmappedFields);
    }

    private Forge1201ApiUsageAnalyzer.ApiMemberReference normalizeMethod(
            Forge1201ApiUsageAnalyzer.ApiMemberReference reference,
            List<String> applied,
            List<String> missing) {
        if (!isMinecraftOwner(reference.owner()) || !SRG_METHOD.matcher(reference.name()).matches()) {
            return reference;
        }
        String mapped = methods.get(reference.name());
        if (mapped == null) {
            missing.add(reference.display());
            return reference;
        }
        if (!mapped.equals(reference.name())) {
            applied.add(reference.owner().replace('/', '.') + "#" + reference.name()
                    + reference.descriptor() + " -> " + mapped + reference.descriptor());
        }
        return new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                reference.owner(), mapped, reference.descriptor(), reference.count());
    }

    private Forge1201ApiUsageAnalyzer.ApiMemberReference normalizeField(
            Forge1201ApiUsageAnalyzer.ApiMemberReference reference,
            List<String> applied,
            List<String> missing) {
        if (!isMinecraftOwner(reference.owner()) || !SRG_FIELD.matcher(reference.name()).matches()) {
            return reference;
        }
        String mapped = fields.get(reference.name());
        if (mapped == null) {
            missing.add(reference.display());
            return reference;
        }
        if (!mapped.equals(reference.name())) {
            applied.add(reference.owner().replace('/', '.') + "#" + reference.name()
                    + " " + reference.descriptor() + " -> " + mapped);
        }
        return new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                reference.owner(), mapped, reference.descriptor(), reference.count());
    }

    private static boolean isMinecraftOwner(String owner) {
        return owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/");
    }

    record Normalization(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            List<String> methodMappings,
            List<String> fieldMappings,
            List<String> unmappedMethods,
            List<String> unmappedFields) {
        Normalization {
            methodMappings = List.copyOf(methodMappings);
            fieldMappings = List.copyOf(fieldMappings);
            unmappedMethods = List.copyOf(unmappedMethods);
            unmappedFields = List.copyOf(unmappedFields);
        }
    }
}
