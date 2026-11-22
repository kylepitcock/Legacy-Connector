package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.Type;

/** Read-only inventory of the production Forge 1.20.1 Access Transformer resource. */
public final class Forge1201AccessTransformerAnalyzer {
    public static final String RESOURCE_PATH = "META-INF/accesstransformer.cfg";
    private static final int MAX_FILE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_DIRECTIVES = 100_000;

    private Forge1201AccessTransformerAnalyzer() {}

    public enum ResultStatus { NOT_PRESENT, PARSED, PARTIALLY_PARSED, OVERSIZED, INVALID_UTF8 }
    public enum Visibility { PUBLIC, PROTECTED, DEFAULT, PRIVATE }
    public enum FinalChange { UNCHANGED, ADD_FINAL, REMOVE_FINAL }
    public enum TargetKind { CLASS, FIELD, METHOD, WILDCARD_FIELD, WILDCARD_METHOD, MALFORMED }
    public enum DirectiveStatus { VALID, WILDCARD_REQUIRES_REVIEW, MALFORMED }

    public record Directive(
            int lineNumber, String rawLine, String modifier, Visibility visibility,
            FinalChange finalChange, String owner, String memberName, String descriptor,
            TargetKind targetKind, String ownerNamespace, String memberNamespace,
            String descriptorNamespace, DirectiveStatus status, String diagnostic) {}

    public record Result(
            String resourcePath, ResultStatus status, long sizeBytes, String sha256,
            List<Directive> directives, int validDirectiveCount, int wildcardDirectiveCount,
            int malformedLineCount, int duplicateTargetCount, int conflictingTargetCount,
            List<String> warnings) {
        public Result {
            directives = List.copyOf(directives);
            warnings = List.copyOf(warnings);
        }
    }

    public static Result analyze(Path jarPath) throws IOException {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Access Transformer input must be an existing JAR: " + jarPath);
        }
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zip.getEntry(RESOURCE_PATH);
            if (entry == null || entry.isDirectory()) {
                return new Result(RESOURCE_PATH, ResultStatus.NOT_PRESENT, 0, null,
                        List.of(), 0, 0, 0, 0, 0, List.of());
            }
            if (entry.getSize() > MAX_FILE_BYTES) {
                return unavailable(ResultStatus.OVERSIZED, entry.getSize(),
                        "Access Transformer exceeds the " + MAX_FILE_BYTES + " byte inventory limit.");
            }
            byte[] bytes;
            try (InputStream input = zip.getInputStream(entry)) {
                bytes = readBounded(input, MAX_FILE_BYTES);
            }
            if (bytes == null) {
                return unavailable(ResultStatus.OVERSIZED, entry.getSize(),
                        "Access Transformer exceeded the " + MAX_FILE_BYTES + " byte inventory limit while reading.");
            }
            final String source;
            try {
                source = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException exception) {
                return new Result(RESOURCE_PATH, ResultStatus.INVALID_UTF8, bytes.length, sha256(bytes),
                        List.of(), 0, 0, 0, 0, 0,
                        List.of("Access Transformer is not valid UTF-8; no lines were interpreted."));
            }
            return parse(source, bytes.length, sha256(bytes));
        }
    }

    static Result parse(String source, long sizeBytes, String sha256) {
        if (source == null) throw new IllegalArgumentException("Access Transformer source is required.");
        List<Directive> directives = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int valid = 0, wildcard = 0, malformed = 0;
        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String line = raw;
            if (i == 0 && line.startsWith("\uFEFF")) line = line.substring(1);
            int comment = line.indexOf('#');
            if (comment >= 0) line = line.substring(0, comment);
            line = line.trim();
            if (line.isEmpty()) continue;
            if (directives.size() >= MAX_DIRECTIVES) {
                warnings.add("Directive count exceeded " + MAX_DIRECTIVES + "; remaining lines were not parsed.");
                break;
            }
            Directive directive = parseLine(i + 1, raw, line);
            directives.add(directive);
            switch (directive.status()) {
                case VALID -> valid++;
                case WILDCARD_REQUIRES_REVIEW -> wildcard++;
                case MALFORMED -> {
                    malformed++;
                    warnings.add("Line " + directive.lineNumber() + ": " + directive.diagnostic());
                }
            }
        }

        Map<String, TreeSet<String>> transforms = new TreeMap<>();
        Map<String, Integer> occurrences = new TreeMap<>();
        for (Directive directive : directives) {
            if (directive.status() == DirectiveStatus.MALFORMED) continue;
            String key = targetKey(directive);
            occurrences.merge(key, 1, Integer::sum);
            transforms.computeIfAbsent(key, ignored -> new TreeSet<>()).add(directive.modifier());
        }
        int duplicates = 0, conflicts = 0;
        for (Map.Entry<String, Integer> item : occurrences.entrySet()) {
            if (item.getValue() > 1) duplicates++;
            if (transforms.get(item.getKey()).size() > 1) conflicts++;
        }
        if (conflicts > 0) warnings.add(conflicts + " target selector(s) have conflicting Access Transformer modifiers.");
        ResultStatus status = malformed == 0 ? ResultStatus.PARSED : ResultStatus.PARTIALLY_PARSED;
        return new Result(RESOURCE_PATH, status, sizeBytes, sha256, directives,
                valid, wildcard, malformed, duplicates, conflicts, warnings);
    }

    private static Directive parseLine(int lineNumber, String raw, String line) {
        String[] tokens = line.split("\\s+");
        if (tokens.length < 2 || tokens.length > 3) {
            return malformed(lineNumber, raw, "Expected a modifier, owner, and optional member selector.");
        }
        ParsedModifier modifier = parseModifier(tokens[0]);
        if (modifier == null) return malformed(lineNumber, raw, "Unsupported access modifier '" + tokens[0] + "'.");
        String owner = tokens[1];
        if (!validOwner(owner)) return malformed(lineNumber, raw, "Invalid target owner '" + owner + "'.");

        String ownerNamespace = owner.startsWith("net.minecraft.")
                ? "MOJANG_OFFICIAL_1.20.1" : "DECLARED_OWNER_NAMESPACE_UNVERIFIED";
        if (tokens.length == 2) {
            return new Directive(lineNumber, raw, tokens[0], modifier.visibility(), modifier.finalChange(),
                    owner, null, null, TargetKind.CLASS, ownerNamespace, "NOT_APPLICABLE",
                    "NOT_APPLICABLE", DirectiveStatus.VALID, null);
        }

        String selector = tokens[2];
        int descriptorStart = selector.indexOf('(');
        String memberName = descriptorStart < 0 ? selector : selector.substring(0, descriptorStart);
        String descriptor = descriptorStart < 0 ? null : selector.substring(descriptorStart);
        if (memberName.isEmpty()) return malformed(lineNumber, raw, "Member selector has no name.");

        TargetKind kind;
        DirectiveStatus status;
        if (descriptorStart >= 0) {
            if (!"*".equals(memberName) && !validMemberName(memberName)) {
                return malformed(lineNumber, raw, "Invalid method selector name '" + memberName + "'.");
            }
            try {
                Type parsed = Type.getMethodType(descriptor);
                if (!parsed.getDescriptor().equals(descriptor)) {
                    return malformed(lineNumber, raw, "Method selector has a non-canonical JVM descriptor.");
                }
            } catch (IllegalArgumentException exception) {
                return malformed(lineNumber, raw, "Invalid method JVM descriptor '" + descriptor + "'.");
            }
            kind = "*".equals(memberName) ? TargetKind.WILDCARD_METHOD : TargetKind.METHOD;
            status = "*".equals(memberName) ? DirectiveStatus.WILDCARD_REQUIRES_REVIEW : DirectiveStatus.VALID;
        } else {
            if (!"*".equals(memberName) && !validMemberName(memberName)) {
                return malformed(lineNumber, raw, "Invalid field selector name '" + memberName + "'.");
            }
            kind = "*".equals(memberName) ? TargetKind.WILDCARD_FIELD : TargetKind.FIELD;
            status = "*".equals(memberName) ? DirectiveStatus.WILDCARD_REQUIRES_REVIEW : DirectiveStatus.VALID;
        }

        String memberNamespace = owner.startsWith("net.minecraft.")
                ? "FORGE_SRG_1.20.1" : "DECLARED_MEMBER_NAMESPACE_UNVERIFIED";
        String descriptorNamespace = descriptor == null ? "NOT_APPLICABLE"
                : owner.startsWith("net.minecraft.")
                        ? "MOJANG_OFFICIAL_1.20.1" : "DECLARED_DESCRIPTOR_NAMESPACE_UNVERIFIED";
        String diagnostic = status == DirectiveStatus.WILDCARD_REQUIRES_REVIEW
                ? "Wildcard member target is inventoried but cannot be resolved as one symbol." : null;
        return new Directive(lineNumber, raw, tokens[0], modifier.visibility(), modifier.finalChange(),
                owner, memberName, descriptor, kind, ownerNamespace, memberNamespace,
                descriptorNamespace, status, diagnostic);
    }

    private static Directive malformed(int lineNumber, String raw, String diagnostic) {
        return new Directive(lineNumber, raw, null, null, null, null, null, null,
                TargetKind.MALFORMED, null, null, null, DirectiveStatus.MALFORMED, diagnostic);
    }

    private static ParsedModifier parseModifier(String value) {
        for (Visibility visibility : Visibility.values()) {
            String base = visibility.name().toLowerCase(java.util.Locale.ROOT);
            if (value.equals(base)) return new ParsedModifier(visibility, FinalChange.UNCHANGED);
            if (value.equals(base + "+f")) return new ParsedModifier(visibility, FinalChange.ADD_FINAL);
            if (value.equals(base + "-f")) return new ParsedModifier(visibility, FinalChange.REMOVE_FINAL);
        }
        return null;
    }

    private static boolean validOwner(String owner) {
        if (owner == null || owner.isBlank() || owner.startsWith(".") || owner.endsWith(".")
                || owner.contains("/") || owner.contains("..")) return false;
        for (String component : owner.split("\\.", -1)) {
            if (!component.matches("[A-Za-z_$][A-Za-z0-9_$]*")) return false;
        }
        return true;
    }

    private static boolean validMemberName(String value) {
        return value.matches("[A-Za-z_$][A-Za-z0-9_$]*");
    }

    private static String targetKey(Directive directive) {
        return directive.targetKind() + "|" + directive.owner() + "|"
                + directive.memberName() + "|" + directive.descriptor();
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 16 * 1024));
        byte[] buffer = new byte[8192];
        int total = 0;
        for (int read; (read = input.read(buffer)) != -1;) {
            total += read;
            if (total > limit) return null;
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static Result unavailable(ResultStatus status, long size, String warning) {
        return new Result(RESOURCE_PATH, status, size, null,
                List.of(), 0, 0, 0, 0, 0, List.of(warning));
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    private record ParsedModifier(Visibility visibility, FinalChange finalChange) {}
}
