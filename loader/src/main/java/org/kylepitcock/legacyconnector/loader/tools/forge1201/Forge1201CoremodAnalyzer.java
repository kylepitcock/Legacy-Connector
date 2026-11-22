package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bounded, read-only inventory of Forge 1.20.1 JavaScript coremod declarations.
 *
 * <p>The analyzer reads only META-INF/coremods.json. It never evaluates scripts, creates
 * transformers, loads mod classes, or infers transformation behavior from script contents.</p>
 */
public final class Forge1201CoremodAnalyzer {
    public static final String RESOURCE_PATH = "META-INF/coremods.json";
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private static final int MAX_DECLARATIONS = 4096;

    private Forge1201CoremodAnalyzer() {}

    public enum ResultStatus {
        NOT_PRESENT,
        PARSED,
        PARTIALLY_PARSED,
        INVALID_JSON,
        INVALID_UTF8,
        OVERSIZED
    }

    public enum ScriptStatus {
        RESOURCE_PRESENT,
        RESOURCE_MISSING,
        RESOURCE_NOT_INDEXED,
        RESOURCE_DUPLICATE_ENTRY,
        UNSAFE_RESOURCE_PATH,
        INVALID_DECLARATION
    }

    public record CoremodDeclaration(
            String name,
            String scriptPath,
            ScriptStatus scriptStatus,
            boolean duplicateName,
            String diagnostic) {}

    public record Result(
            String resourcePath,
            ResultStatus status,
            long sizeBytes,
            String sha256,
            List<CoremodDeclaration> declarations,
            int validDeclarationCount,
            int malformedDeclarationCount,
            int duplicateNameCount,
            int missingScriptCount,
            List<String> warnings) {
        public Result {
            declarations = List.copyOf(declarations);
            warnings = List.copyOf(warnings);
        }
    }

    public static Result notPresent() {
        return new Result(RESOURCE_PATH, ResultStatus.NOT_PRESENT, 0, null,
                List.of(), 0, 0, 0, 0, List.of());
    }

    /** Analyze a nested archive resource from its already-bounded in-memory bytes. */
    static Result analyzeNested(byte[] metadataBytes, Set<String> resourcePaths, boolean resourceIndexIncomplete) {
        return analyzeNested(metadataBytes, resourcePaths, Set.of(), resourceIndexIncomplete);
    }

    static Result analyzeNested(
            byte[] metadataBytes, Set<String> resourcePaths, Set<String> duplicateResourcePaths,
            boolean resourceIndexIncomplete) {
        if (metadataBytes == null) return notPresent();
        if (metadataBytes.length > MAX_FILE_BYTES) {
            return unavailable(ResultStatus.OVERSIZED, metadataBytes.length,
                    "Nested Coremod metadata exceeds the " + MAX_FILE_BYTES + " byte inventory limit.");
        }
        final String source;
        try {
            source = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(metadataBytes)).toString();
        } catch (CharacterCodingException exception) {
            return new Result(RESOURCE_PATH, ResultStatus.INVALID_UTF8, metadataBytes.length, sha256(metadataBytes),
                    List.of(), 0, 0, 0, 0,
                    List.of("Nested Coremod metadata is not valid UTF-8; no declarations were interpreted."));
        }
        Set<String> indexedPaths = resourcePaths == null ? Set.of() : resourcePaths;
        Set<String> duplicatedPaths = duplicateResourcePaths == null ? Set.of() : duplicateResourcePaths;
        return parse(source, metadataBytes.length, sha256(metadataBytes), indexedPaths::contains,
                duplicatedPaths::contains, resourceIndexIncomplete);
    }

    public static Result analyze(Path jarPath) throws IOException {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Coremod metadata input must be an existing JAR: " + jarPath);
        }

        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry metadata = zip.getEntry(RESOURCE_PATH);
            if (metadata == null || metadata.isDirectory()) {
                return notPresent();
            }
            if (metadata.getSize() > MAX_FILE_BYTES) {
                return unavailable(ResultStatus.OVERSIZED, metadata.getSize(),
                        "Coremod metadata exceeds the " + MAX_FILE_BYTES + " byte inventory limit.");
            }

            byte[] bytes;
            try (InputStream input = zip.getInputStream(metadata)) {
                bytes = readBounded(input, MAX_FILE_BYTES);
            }
            if (bytes == null) {
                return unavailable(ResultStatus.OVERSIZED, metadata.getSize(),
                        "Coremod metadata exceeded the " + MAX_FILE_BYTES + " byte inventory limit while reading.");
            }

            final String source;
            try {
                source = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException exception) {
                return new Result(RESOURCE_PATH, ResultStatus.INVALID_UTF8, bytes.length, sha256(bytes),
                        List.of(), 0, 0, 0, 0,
                        List.of("Coremod metadata is not valid UTF-8; no declarations were interpreted."));
            }

            return parse(source, bytes.length, sha256(bytes), path -> {
                ZipEntry script = zip.getEntry(path);
                return script != null && !script.isDirectory();
            }, ignored -> false, false);
        }
    }

    static Result parse(String source, long sizeBytes, String sha256, ZipFile zip) {
        if (zip == null) throw new IllegalArgumentException("Source archive is required.");
        return parse(source, sizeBytes, sha256, path -> {
            ZipEntry script = zip.getEntry(path);
            return script != null && !script.isDirectory();
        }, ignored -> false, false);
    }

    private static Result parse(
            String source, long sizeBytes, String sha256,
            Predicate<String> resourceExists, Predicate<String> resourceDuplicated,
            boolean resourceIndexIncomplete) {
        if (source == null) throw new IllegalArgumentException("Coremod metadata source is required.");
        if (resourceExists == null || resourceDuplicated == null)
            throw new IllegalArgumentException("Script resource lookup is required.");

        List<CoremodDeclaration> declarations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> names = new HashSet<>();
        int valid = 0;
        int malformed = 0;
        int duplicates = 0;
        int missing = 0;
        int unindexed = 0;
        boolean truncated = false;
        boolean invalidJson = false;
        String parseDiagnostic = null;

        try (JsonReader reader = new JsonReader(new StringReader(source))) {
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                return new Result(RESOURCE_PATH, ResultStatus.INVALID_JSON, sizeBytes, sha256,
                        List.of(), 0, 1, 0, 0,
                        List.of("Coremod metadata root must be a JSON object mapping names to script paths."));
            }

            reader.beginObject();
            while (reader.hasNext()) {
                if (declarations.size() >= MAX_DECLARATIONS) {
                    warnings.add("Declaration count exceeded " + MAX_DECLARATIONS
                            + "; remaining metadata entries were not inspected.");
                    truncated = true;
                    break;
                }

                String name = reader.nextName();
                boolean duplicateName = !names.add(name);
                if (duplicateName) duplicates++;

                if (reader.peek() != JsonToken.STRING) {
                    reader.skipValue();
                    malformed++;
                    declarations.add(new CoremodDeclaration(name, null, ScriptStatus.INVALID_DECLARATION,
                            duplicateName, "Coremod script value must be a JSON string."));
                    warnings.add("Coremod '" + printable(name) + "' has a non-string script path.");
                    continue;
                }

                String scriptPath = reader.nextString();
                if (name == null || name.isBlank() || containsControlCharacters(name)) {
                    malformed++;
                    declarations.add(new CoremodDeclaration(name, scriptPath, ScriptStatus.INVALID_DECLARATION,
                            duplicateName, "Coremod name is empty or contains control characters."));
                    warnings.add("A coremod declaration has an invalid name.");
                    continue;
                }
                if (!isSafeResourcePath(scriptPath)) {
                    malformed++;
                    declarations.add(new CoremodDeclaration(name, scriptPath, ScriptStatus.UNSAFE_RESOURCE_PATH,
                            duplicateName, "Script path is empty, absolute, contains traversal, or is not a safe JAR resource path."));
                    warnings.add("Coremod '" + printable(name) + "' declares an unsafe script resource path.");
                    continue;
                }

                if (resourceDuplicated.test(scriptPath)) {
                    unindexed++;
                    declarations.add(new CoremodDeclaration(name, scriptPath, ScriptStatus.RESOURCE_DUPLICATE_ENTRY,
                            duplicateName, "Multiple nested ZIP entries use the declared script resource path."));
                } else if (!resourceExists.test(scriptPath)) {
                    if (resourceIndexIncomplete) {
                        unindexed++;
                        declarations.add(new CoremodDeclaration(name, scriptPath, ScriptStatus.RESOURCE_NOT_INDEXED,
                                duplicateName, "The nested resource-path index is incomplete; script presence could not be determined."));
                    } else {
                        missing++;
                        declarations.add(new CoremodDeclaration(name, scriptPath, ScriptStatus.RESOURCE_MISSING,
                                duplicateName, "Declared JavaScript resource is not present in the archive."));
                    }
                } else {
                    valid++;
                    declarations.add(new CoremodDeclaration(name, scriptPath, ScriptStatus.RESOURCE_PRESENT,
                            duplicateName, duplicateName ? "Duplicate coremod name is preserved for review." : null));
                }
            }

            if (!truncated) {
                reader.endObject();
                if (reader.peek() != JsonToken.END_DOCUMENT) {
                    throw new IOException("Unexpected content after the coremod metadata object.");
                }
            }
        } catch (IOException | IllegalStateException exception) {
            invalidJson = true;
            parseDiagnostic = conciseMessage(exception);
        }

        declarations.sort((left, right) -> {
            int nameOrder = String.valueOf(left.name()).compareTo(String.valueOf(right.name()));
            if (nameOrder != 0) return nameOrder;
            int pathOrder = String.valueOf(left.scriptPath()).compareTo(String.valueOf(right.scriptPath()));
            if (pathOrder != 0) return pathOrder;
            return Boolean.compare(left.duplicateName(), right.duplicateName());
        });

        if (invalidJson) {
            warnings.add("Coremod metadata JSON could not be fully parsed: " + parseDiagnostic);
        }
        if (duplicates > 0) {
            warnings.add(duplicates + " duplicate coremod name occurrence(s) were retained for review.");
        }
        if (missing > 0) {
            warnings.add(missing + " declared coremod script resource(s) are missing from the archive.");
        }
        if (unindexed > 0) {
            warnings.add(unindexed + " declared coremod script resource(s) could not be classified because a resource path was duplicated or nested resource indexing was incomplete.");
        }

        ResultStatus status = invalidJson ? ResultStatus.INVALID_JSON
                : truncated || malformed > 0 || duplicates > 0 || unindexed > 0
                        ? ResultStatus.PARTIALLY_PARSED : ResultStatus.PARSED;
        return new Result(RESOURCE_PATH, status, sizeBytes, sha256, declarations,
                valid, malformed, duplicates, missing, warnings);
    }

    private static boolean isSafeResourcePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/")
                || path.indexOf('\\') >= 0 || path.indexOf(':') >= 0
                || containsControlCharacters(path)) {
            return false;
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) return false;
        }
        return true;
    }

    private static boolean containsControlCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) return true;
        }
        return false;
    }

    private static String printable(String value) {
        return value == null ? "<null>" : value.replace('\n', ' ').replace('\r', ' ');
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
                List.of(), 0, 0, 0, 0, List.of(warning));
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
