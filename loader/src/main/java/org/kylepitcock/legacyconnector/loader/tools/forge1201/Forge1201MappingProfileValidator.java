package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Strict, read-only parser and structural validator for versioned mapping profiles.
 *
 * <p>This class validates declared evidence and graph structure only. It does not load mapping
 * tables, apply a mapping, rewrite Mixin selectors, or infer identity between namespaces.</p>
 */
public final class Forge1201MappingProfileValidator {
    public static final int MAX_PROFILE_BYTES = 1024 * 1024;
    public static final int MAX_JSON_DEPTH = 64;
    public static final int MAX_CONTEXT_BINDINGS = 64;
    public static final int MAX_EDGES = 256;
    public static final int MAX_ARTIFACTS = 512;
    public static final long MAX_ARTIFACT_BYTES = 512L * 1024 * 1024;

    private static final Set<String> REQUIRED_SELECTOR_SCOPE =
            Set.of("class-names", "method-names", "field-names", "descriptors");
    private static final Set<String> SUPPORTED_SCOPE =
            Set.of("class-names", "method-names", "field-names", "descriptors", "packages");
    private static final Set<String> PROFILE_FIELDS =
            Set.of("schemaVersion", "profileId", "status", "source", "target", "artifacts",
                    "edges", "runtimeRemapping", "licenseReview");
    private static final Set<String> SOURCE_FIELDS =
            Set.of("minecraftVersion", "loader", "compileNamespaceId", "refmapContextBindings",
                    "baseRefmapBinding");
    private static final Set<String> TARGET_FIELDS =
            Set.of("minecraftVersion", "loader", "compileNamespaceId", "mappingAugmentations");
    private static final Set<String> LOADER_FIELDS = Set.of("id", "version", "versionRange");
    private static final Set<String> BINDING_FIELDS = Set.of("namespaceId", "evidence");
    private static final Set<String> EDGE_FIELDS =
            Set.of("edgeId", "from", "to", "scope", "artifactIds", "status", "reviewEvidence",
                    "identityEvidence");
    private static final Set<String> ARTIFACT_FIELDS =
            Set.of("artifactId", "provider", "version", "format", "coordinate", "url",
                    "sha256", "upstreamChecksum", "localPath", "license");
    private static final Set<String> LICENSE_FIELDS = Set.of("id", "reviewStatus", "evidence");

    private Forge1201MappingProfileValidator() {}

    public enum ProfileStatus {
        PROFILE_NOT_SUPPLIED,
        PROFILE_FILE_MISSING,
        PROFILE_FILE_LIMIT_EXCEEDED,
        PROFILE_INVALID_JSON,
        PROFILE_UNSUPPORTED_SHAPE,
        PROFILE_UNSUPPORTED_SCHEMA,
        PROFILE_SOURCE_VERSION_MISMATCH,
        PROFILE_TARGET_VERSION_MISMATCH,
        PROFILE_CONTEXT_NAMESPACE_UNBOUND,
        PROFILE_NO_COMPLETE_PATH,
        PROFILE_AMBIGUOUS_PATH,
        PROFILE_ARTIFACT_MISSING,
        PROFILE_ARTIFACT_DIGEST_MISMATCH,
        PROFILE_LICENSE_REVIEW_REQUIRED,
        PROFILE_INCOMPLETE,
        PROFILE_VALIDATED
    }

    public enum ArtifactStatus {
        VERIFIED,
        NOT_MATERIALIZED,
        MISSING,
        DIGEST_MISMATCH,
        INVALID_DECLARATION,
        LICENSE_REVIEW_REQUIRED
    }

    public enum EdgeStatus {
        VERIFIED,
        PARTIAL,
        MISSING,
        UNSUPPORTED,
        INVALID_DECLARATION,
        ARTIFACT_UNVERIFIED,
        REVIEW_EVIDENCE_MISSING,
        IDENTITY_EXPLICIT_NOT_APPLIED,
        IDENTITY_UNVERIFIED
    }

    public record ContextBindingFinding(
            String contextKey,
            String namespaceId,
            List<String> evidence,
            boolean baseTable) {
        public ContextBindingFinding {
            evidence = List.copyOf(evidence);
        }
    }

    public record ArtifactFinding(
            String artifactId,
            String provider,
            String version,
            String format,
            String identity,
            String expectedSha256,
            String actualSha256,
            String localPath,
            String licenseId,
            String licenseReviewStatus,
            ArtifactStatus status,
            List<String> evidence,
            List<String> warnings) {
        public ArtifactFinding {
            evidence = List.copyOf(evidence);
            warnings = List.copyOf(warnings);
        }
    }

    public record EdgeFinding(
            String edgeId,
            String fromNamespace,
            String toNamespace,
            List<String> scope,
            List<String> artifactIds,
            String declaredStatus,
            EdgeStatus status,
            List<String> reviewEvidence,
            List<String> identityEvidence,
            List<String> warnings) {
        public EdgeFinding {
            scope = List.copyOf(scope);
            artifactIds = List.copyOf(artifactIds);
            reviewEvidence = List.copyOf(reviewEvidence);
            identityEvidence = List.copyOf(identityEvidence);
            warnings = List.copyOf(warnings);
        }
    }

    public record ContextPathFinding(
            String contextKey,
            String sourceNamespace,
            String targetNamespace,
            int completePathCount,
            List<List<String>> candidateEdgePaths,
            String status) {
        public ContextPathFinding {
            candidateEdgePaths = candidateEdgePaths.stream().map(List::copyOf).toList();
        }
    }

    public record Finding(
            String profilePath,
            long profileSizeBytes,
            String profileSha256,
            Integer schemaVersion,
            String profileId,
            String declaredStatus,
            ProfileStatus status,
            String sourceMinecraftVersion,
            String sourceLoaderId,
            String sourceLoaderVersionRange,
            String sourceCompileNamespaceId,
            String targetMinecraftVersion,
            String targetLoaderId,
            String targetLoaderVersion,
            String targetCompileNamespaceId,
            List<ContextBindingFinding> contextBindings,
            List<ArtifactFinding> artifacts,
            List<EdgeFinding> edges,
            List<ContextPathFinding> contextPaths,
            List<String> validationErrors,
            List<String> warnings) {
        public Finding {
            contextBindings = List.copyOf(contextBindings);
            artifacts = List.copyOf(artifacts);
            edges = List.copyOf(edges);
            contextPaths = List.copyOf(contextPaths);
            validationErrors = List.copyOf(validationErrors);
            warnings = List.copyOf(warnings);
        }
    }

    public static Finding inspect(Path profilePath) throws IOException {
        if (profilePath == null) {
            return empty(ProfileStatus.PROFILE_NOT_SUPPLIED,
                    "No mapping profile was supplied. No namespace mapping is selected or inferred.");
        }

        Path absolute = profilePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            return new Finding(absolute.toString(), 0, null, null, null, null,
                    ProfileStatus.PROFILE_FILE_MISSING, null, null, null, null, null, null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("Mapping profile file does not exist or is not a regular file."));
        }

        long size = Files.size(absolute);
        if (size > MAX_PROFILE_BYTES) {
            return new Finding(absolute.toString(), size, null, null, null, null,
                    ProfileStatus.PROFILE_FILE_LIMIT_EXCEEDED, null, null, null, null, null, null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("Mapping profile exceeds the " + MAX_PROFILE_BYTES + "-byte limit."));
        }

        byte[] bytes = Files.readAllBytes(absolute);
        if (bytes.length > MAX_PROFILE_BYTES) {
            return new Finding(absolute.toString(), bytes.length, null, null, null, null,
                    ProfileStatus.PROFILE_FILE_LIMIT_EXCEEDED, null, null, null, null, null, null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("Mapping profile grew beyond the " + MAX_PROFILE_BYTES + "-byte limit while reading."));
        }
        String digest = sha256(bytes);
        final String json;
        final JsonElement root;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            validateJsonWithoutDuplicateMembers(json);
            root = JsonParser.parseString(json);
        } catch (IOException | RuntimeException exception) {
            return new Finding(absolute.toString(), bytes.length, digest, null, null, null,
                    ProfileStatus.PROFILE_INVALID_JSON, null, null, null, null, null, null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("Mapping profile is not strict UTF-8 JSON or contains duplicate object keys: "
                            + conciseMessage(exception)));
        }
        if (!root.isJsonObject()) {
            return new Finding(absolute.toString(), bytes.length, digest, null, null, null,
                    ProfileStatus.PROFILE_UNSUPPORTED_SHAPE, null, null, null, null, null, null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("Mapping profile root must be a JSON object."));
        }

        JsonObject object = root.getAsJsonObject();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        collectUnknownFields(object, PROFILE_FIELDS, "profile", warnings);

        Integer schemaVersion = integerValue(object.get("schemaVersion"));
        if (schemaVersion == null) errors.add("Required integer field 'schemaVersion' is missing or invalid.");
        String profileId = requiredString(object, "profileId", "profile", errors);
        String declaredStatus = optionalString(object, "status", "profile", warnings);

        JsonObject source = objectValue(object.get("source"));
        JsonObject target = objectValue(object.get("target"));
        if (source == null) errors.add("Required object 'source' is missing or invalid.");
        if (target == null) errors.add("Required object 'target' is missing or invalid.");

        String sourceVersion = null;
        String sourceLoaderId = null;
        String sourceLoaderRange = null;
        String sourceNamespace = null;
        String targetVersion = null;
        String targetLoaderId = null;
        String targetLoaderVersion = null;
        String targetNamespace = null;
        List<ContextBindingFinding> bindings = new ArrayList<>();

        if (source != null) {
            collectUnknownFields(source, SOURCE_FIELDS, "source", warnings);
            sourceVersion = requiredString(source, "minecraftVersion", "source", errors);
            JsonObject loader = objectValue(source.get("loader"));
            if (loader == null) errors.add("Required object 'source.loader' is missing or invalid.");
            else {
                collectUnknownFields(loader, LOADER_FIELDS, "source.loader", warnings);
                sourceLoaderId = requiredString(loader, "id", "source.loader", errors);
                sourceLoaderRange = requiredString(loader, "versionRange", "source.loader", errors);
            }
            sourceNamespace = requiredString(source, "compileNamespaceId", "source", errors);
            parseBindings(source, bindings, errors, warnings);
        }
        if (target != null) {
            collectUnknownFields(target, TARGET_FIELDS, "target", warnings);
            targetVersion = requiredString(target, "minecraftVersion", "target", errors);
            JsonObject loader = objectValue(target.get("loader"));
            if (loader == null) errors.add("Required object 'target.loader' is missing or invalid.");
            else {
                collectUnknownFields(loader, LOADER_FIELDS, "target.loader", warnings);
                targetLoaderId = requiredString(loader, "id", "target.loader", errors);
                targetLoaderVersion = requiredString(loader, "version", "target.loader", errors);
            }
            targetNamespace = requiredString(target, "compileNamespaceId", "target", errors);
            validateAugmentations(target, warnings);
        }

        List<ArtifactFinding> artifacts = parseArtifacts(
                object.get("artifacts"), absolute.getParent(), errors, warnings);
        Map<String, ArtifactFinding> artifactsById = new HashMap<>();
        for (ArtifactFinding artifact : artifacts) {
            if (artifact.artifactId() != null) {
                if (artifactsById.putIfAbsent(artifact.artifactId(), artifact) != null) {
                    errors.add("Duplicate artifactId '" + artifact.artifactId() + "'.");
                }
            }
        }
        List<EdgeFinding> edges = parseEdges(
                object.get("edges"), artifactsById, errors, warnings);
        Map<String, EdgeFinding> edgeById = new HashMap<>();
        for (EdgeFinding edge : edges) {
            if (edge.edgeId() != null && edgeById.putIfAbsent(edge.edgeId(), edge) != null) {
                errors.add("Duplicate edgeId '" + edge.edgeId() + "'.");
            }
        }

        if (schemaVersion != null && schemaVersion != 1) {
            warnings.add("Unsupported mapping profile schemaVersion " + schemaVersion + "; only version 1 is supported.");
        }
        if (sourceVersion != null && !"1.20.1".equals(sourceVersion)) {
            warnings.add("Source Minecraft version must be 1.20.1 for this preflight target.");
        }
        if (targetVersion != null && !"1.21.1".equals(targetVersion)) {
            warnings.add("Target Minecraft version must be 1.21.1 for this preflight target.");
        }
        if (sourceLoaderId != null && !"forge".equalsIgnoreCase(sourceLoaderId)) {
            warnings.add("Source loader id is not 'forge'.");
        }
        if (targetLoaderId != null && !"neoforge".equalsIgnoreCase(targetLoaderId)) {
            warnings.add("Target loader id is not 'neoforge'.");
        }
        if (targetLoaderVersion != null && !"21.1.258".equals(targetLoaderVersion)) {
            warnings.add("Target NeoForge version differs from the analyzer's pinned 21.1.258 target.");
        }

        List<ContextPathFinding> contextPaths = evaluateContextPaths(bindings, targetNamespace, edges);
        boolean sourceMismatch = sourceVersion != null && !"1.20.1".equals(sourceVersion);
        boolean targetMismatch = targetVersion != null && !"1.21.1".equals(targetVersion);
        boolean unsupportedSchema = schemaVersion != null && schemaVersion != 1;
        boolean badDigest = artifacts.stream().anyMatch(value ->
                value.status() == ArtifactStatus.DIGEST_MISMATCH);
        boolean missingArtifact = artifacts.stream().anyMatch(value ->
                value.status() == ArtifactStatus.MISSING || value.status() == ArtifactStatus.NOT_MATERIALIZED);
        boolean licenseReviewRequired = artifacts.stream().anyMatch(value ->
                value.status() == ArtifactStatus.LICENSE_REVIEW_REQUIRED);
        boolean contextUnbound = bindings.isEmpty();
        boolean ambiguous = contextPaths.stream().anyMatch(value -> value.completePathCount() > 1);
        boolean noPath = !contextPaths.isEmpty()
                && contextPaths.stream().anyMatch(value -> value.completePathCount() == 0);
        boolean incompletePath = !contextPaths.isEmpty()
                && contextPaths.stream().anyMatch(value -> !"COMPLETE_UNAMBIGUOUS_PATH".equals(value.status()));

        ProfileStatus status;
        if (unsupportedSchema) status = ProfileStatus.PROFILE_UNSUPPORTED_SCHEMA;
        else if (sourceMismatch) status = ProfileStatus.PROFILE_SOURCE_VERSION_MISMATCH;
        else if (targetMismatch) status = ProfileStatus.PROFILE_TARGET_VERSION_MISMATCH;
        else if (badDigest) status = ProfileStatus.PROFILE_ARTIFACT_DIGEST_MISMATCH;
        else if (missingArtifact) status = ProfileStatus.PROFILE_ARTIFACT_MISSING;
        else if (licenseReviewRequired) status = ProfileStatus.PROFILE_LICENSE_REVIEW_REQUIRED;
        else if (contextUnbound) status = ProfileStatus.PROFILE_CONTEXT_NAMESPACE_UNBOUND;
        else if (ambiguous) status = ProfileStatus.PROFILE_AMBIGUOUS_PATH;
        else if (noPath || incompletePath) status = ProfileStatus.PROFILE_NO_COMPLETE_PATH;
        else if (!errors.isEmpty()) status = ProfileStatus.PROFILE_INCOMPLETE;
        else status = ProfileStatus.PROFILE_VALIDATED;

        if (sourceNamespace == null || targetNamespace == null) {
            warnings.add("Source/target compile namespace identity is incomplete.");
            if (status == ProfileStatus.PROFILE_VALIDATED) status = ProfileStatus.PROFILE_INCOMPLETE;
        }
        if (bindings.stream().noneMatch(ContextBindingFinding::baseTable)) {
            warnings.add("Base refmap mappings table has no explicit namespace binding; it remains unresolved.");
        }
        if (warnings.stream().anyMatch(value -> value.contains("must be") || value.contains("differs from"))) {
            if (status == ProfileStatus.PROFILE_VALIDATED) status = ProfileStatus.PROFILE_INCOMPLETE;
        }
        if (!errors.isEmpty() && status == ProfileStatus.PROFILE_VALIDATED) {
            status = ProfileStatus.PROFILE_INCOMPLETE;
        }

        return new Finding(absolute.toString(), bytes.length, digest, schemaVersion, profileId, declaredStatus,
                status, sourceVersion, sourceLoaderId, sourceLoaderRange, sourceNamespace,
                targetVersion, targetLoaderId, targetLoaderVersion, targetNamespace,
                bindings, artifacts, edges, contextPaths, errors, warnings.stream().distinct().sorted().toList());
    }

    private static Finding empty(ProfileStatus status, String warning) {
        return new Finding(null, 0, null, null, null, null, status, null, null, null, null,
                null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(warning));
    }

    private static void parseBindings(
            JsonObject source,
            List<ContextBindingFinding> bindings,
            List<String> errors,
            List<String> warnings) {
        JsonElement element = source.get("refmapContextBindings");
        if (element == null || element.isJsonNull()) {
            errors.add("Required object 'source.refmapContextBindings' is missing.");
        } else if (!element.isJsonObject()) {
            errors.add("'source.refmapContextBindings' must be a JSON object.");
        } else {
            JsonObject object = element.getAsJsonObject();
            if (object.size() > MAX_CONTEXT_BINDINGS) {
                errors.add("Refmap context bindings exceed " + MAX_CONTEXT_BINDINGS + ".");
            } else {
                for (Map.Entry<String, JsonElement> item : object.entrySet()) {
                    String key = item.getKey();
                    JsonObject binding = objectValue(item.getValue());
                    if (key.isBlank() || key.length() > 128) {
                        errors.add("Refmap context key is empty or exceeds 128 characters.");
                        continue;
                    }
                    if (binding == null) {
                        errors.add("Refmap context binding '" + key + "' must be an object.");
                        continue;
                    }
                    collectUnknownFields(binding, BINDING_FIELDS, "source.refmapContextBindings." + key, warnings);
                    String namespace = requiredString(binding, "namespaceId",
                            "source.refmapContextBindings." + key, errors);
                    List<String> evidence = stringArray(binding.get("evidence"),
                            "source.refmapContextBindings." + key + ".evidence", errors, 128);
                    if (evidence.isEmpty()) errors.add("Context binding '" + key + "' has no provenance evidence.");
                    if (namespace != null) bindings.add(new ContextBindingFinding(key, namespace, evidence, false));
                }
            }
        }

        JsonElement baseElement = source.get("baseRefmapBinding");
        if (baseElement != null && !baseElement.isJsonNull()) {
            JsonObject base = objectValue(baseElement);
            if (base == null) errors.add("'source.baseRefmapBinding' must be an object.");
            else {
                collectUnknownFields(base, BINDING_FIELDS, "source.baseRefmapBinding", warnings);
                String namespace = requiredString(base, "namespaceId", "source.baseRefmapBinding", errors);
                List<String> evidence = stringArray(base.get("evidence"),
                        "source.baseRefmapBinding.evidence", errors, 128);
                if (evidence.isEmpty()) errors.add("Base refmap binding has no provenance evidence.");
                if (namespace != null) bindings.add(new ContextBindingFinding("<base>", namespace, evidence, true));
            }
        }
        bindings.sort(Comparator.comparing(ContextBindingFinding::contextKey));
    }

    private static List<ArtifactFinding> parseArtifacts(
            JsonElement element,
            Path profileDirectory,
            List<String> errors,
            List<String> warnings) {
        List<ArtifactFinding> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            errors.add("Required array 'artifacts' is missing.");
            return result;
        }
        if (!element.isJsonArray()) {
            errors.add("'artifacts' must be an array.");
            return result;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > MAX_ARTIFACTS) {
            errors.add("Artifact count exceeds " + MAX_ARTIFACTS + ".");
            return result;
        }
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject item = objectValue(array.get(i));
            String label = "artifacts[" + i + "]";
            if (item == null) {
                errors.add(label + " must be an object.");
                continue;
            }
            collectUnknownFields(item, ARTIFACT_FIELDS, label, warnings);
            String id = requiredString(item, "artifactId", label, errors);
            String provider = requiredString(item, "provider", label, errors);
            String version = requiredString(item, "version", label, errors);
            String format = requiredString(item, "format", label, errors);
            String coordinate = optionalString(item, "coordinate", label, warnings);
            String url = optionalString(item, "url", label, warnings);
            String identity = coordinate == null ? url : coordinate;
            if ((coordinate == null) == (url == null)) {
                errors.add(label + " must declare exactly one of 'coordinate' or 'url'.");
            }
            String expectedSha = requiredString(item, "sha256", label, errors);
            if (expectedSha != null && !expectedSha.matches("(?i)[0-9a-f]{64}")) {
                errors.add(label + ".sha256 must be exactly 64 hexadecimal characters.");
            }
            String localPath = optionalString(item, "localPath", label, warnings);
            JsonObject license = objectValue(item.get("license"));
            String licenseId = null;
            String licenseStatus = null;
            List<String> licenseEvidence = List.of();
            if (license == null) {
                errors.add(label + ".license must be an object with an id and review status.");
            } else {
                collectUnknownFields(license, LICENSE_FIELDS, label + ".license", warnings);
                licenseId = requiredString(license, "id", label + ".license", errors);
                licenseStatus = requiredString(license, "reviewStatus", label + ".license", errors);
                licenseEvidence = stringArray(license.get("evidence"), label + ".license.evidence", errors, 128);
                if (licenseEvidence.isEmpty()) errors.add(label + ".license has no review evidence.");
            }
            List<String> evidence = licenseEvidence;
            ArtifactStatus status = ArtifactStatus.INVALID_DECLARATION;
            String actualSha = null;
            if (id != null && !ids.add(id)) errors.add("Duplicate artifactId '" + id + "'.");
            boolean declarationValid = id != null && provider != null && version != null && format != null
                    && identity != null && expectedSha != null && expectedSha.matches("(?i)[0-9a-f]{64}")
                    && licenseId != null && licenseStatus != null;
            if (declarationValid) {
                if (localPath == null) {
                    status = ArtifactStatus.NOT_MATERIALIZED;
                    warnings.add(label + " has an expected digest but no localPath; artifact bytes are not verified.");
                } else {
                    try {
                        Path resolved = resolveArtifactPath(profileDirectory, localPath);
                        if (!Files.isRegularFile(resolved)) {
                            status = ArtifactStatus.MISSING;
                            warnings.add(label + " local artifact does not exist: " + localPath);
                        } else if (Files.size(resolved) > MAX_ARTIFACT_BYTES) {
                            status = ArtifactStatus.INVALID_DECLARATION;
                            warnings.add(label + " local artifact exceeds " + MAX_ARTIFACT_BYTES + " bytes.");
                        } else {
                            actualSha = sha256(resolved);
                            if (!actualSha.equalsIgnoreCase(expectedSha)) {
                                status = ArtifactStatus.DIGEST_MISMATCH;
                                warnings.add(label + " SHA-256 does not match the declared digest.");
                            } else if (!"REVIEWED".equalsIgnoreCase(licenseStatus)) {
                                status = ArtifactStatus.LICENSE_REVIEW_REQUIRED;
                                warnings.add(label + " license review status is not REVIEWED.");
                            } else {
                                status = ArtifactStatus.VERIFIED;
                            }
                        }
                    } catch (IOException | IllegalArgumentException exception) {
                        status = ArtifactStatus.INVALID_DECLARATION;
                        warnings.add(label + " localPath could not be verified: " + conciseMessage(exception));
                    }
                }
            }
            result.add(new ArtifactFinding(id, provider, version, format, identity, expectedSha, actualSha,
                    localPath, licenseId, licenseStatus, status, evidence, List.of()));
        }
        result.sort(Comparator.comparing(value -> value.artifactId() == null ? "" : value.artifactId()));
        return result;
    }

    private static Path resolveArtifactPath(Path profileDirectory, String localPath) throws IOException {
        Path supplied = Path.of(localPath);
        if (supplied.isAbsolute()) throw new IllegalArgumentException("Absolute artifact paths are not allowed.");
        Path base = profileDirectory == null ? Path.of(".").toAbsolutePath().normalize() : profileDirectory;
        Path resolved = base.resolve(supplied).normalize();
        if (!resolved.startsWith(base.normalize())) {
            throw new IllegalArgumentException("Artifact path escapes the profile directory.");
        }
        return resolved;
    }

    private static List<EdgeFinding> parseEdges(
            JsonElement element,
            Map<String, ArtifactFinding> artifacts,
            List<String> errors,
            List<String> warnings) {
        List<EdgeFinding> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            errors.add("Required array 'edges' is missing.");
            return result;
        }
        if (!element.isJsonArray()) {
            errors.add("'edges' must be an array.");
            return result;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > MAX_EDGES) {
            errors.add("Mapping edge count exceeds " + MAX_EDGES + ".");
            return result;
        }
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject item = objectValue(array.get(i));
            String label = "edges[" + i + "]";
            if (item == null) {
                errors.add(label + " must be an object.");
                continue;
            }
            collectUnknownFields(item, EDGE_FIELDS, label, warnings);
            String id = requiredString(item, "edgeId", label, errors);
            String from = requiredString(item, "from", label, errors);
            String to = requiredString(item, "to", label, errors);
            List<String> scope = stringArray(item.get("scope"), label + ".scope", errors, 16);
            List<String> artifactIds = stringArray(item.get("artifactIds"), label + ".artifactIds", errors, 64);
            String declaredStatus = requiredString(item, "status", label, errors);
            List<String> reviewEvidence = stringArray(item.get("reviewEvidence"),
                    label + ".reviewEvidence", new ArrayList<>(), 128);
            List<String> identityEvidence = stringArray(item.get("identityEvidence"),
                    label + ".identityEvidence", new ArrayList<>(), 128);

            List<String> edgeWarnings = new ArrayList<>();
            EdgeStatus status = EdgeStatus.INVALID_DECLARATION;
            if (id != null && !ids.add(id)) errors.add("Duplicate edgeId '" + id + "'.");
            if (from != null && from.equals(to)) {
                if (identityEvidence.isEmpty()) {
                    status = EdgeStatus.IDENTITY_UNVERIFIED;
                    edgeWarnings.add("Identity edge has no explicit identityEvidence; no implicit identity is accepted.");
                } else {
                    status = EdgeStatus.IDENTITY_EXPLICIT_NOT_APPLIED;
                    edgeWarnings.add("Explicit identity declaration recorded; this stage records it but does not transform selectors.");
                }
            } else if ("MISSING".equalsIgnoreCase(declaredStatus)) {
                status = EdgeStatus.MISSING;
            } else if ("UNSUPPORTED".equalsIgnoreCase(declaredStatus)) {
                status = EdgeStatus.UNSUPPORTED;
            } else if ("PARTIAL".equalsIgnoreCase(declaredStatus)
                    || "PARTIAL_UNTIL_COVERAGE_AUDIT".equalsIgnoreCase(declaredStatus)) {
                status = EdgeStatus.PARTIAL;
            } else if (!"VERIFIED".equalsIgnoreCase(declaredStatus)) {
                edgeWarnings.add("Declared edge status must be VERIFIED, PARTIAL, MISSING, or UNSUPPORTED.");
            } else if (!scope.containsAll(REQUIRED_SELECTOR_SCOPE)) {
                status = EdgeStatus.PARTIAL;
                edgeWarnings.add("Edge does not declare the full class/member/descriptor scope required for selectors.");
            } else if (reviewEvidence.isEmpty()) {
                status = EdgeStatus.REVIEW_EVIDENCE_MISSING;
                edgeWarnings.add("Edge is marked VERIFIED but has no reviewEvidence.");
            } else if (artifactIds.isEmpty()) {
                status = EdgeStatus.ARTIFACT_UNVERIFIED;
                edgeWarnings.add("VERIFIED edge references no pinned mapping artifact.");
            } else {
                boolean allVerified = true;
                for (String artifactId : artifactIds) {
                    ArtifactFinding artifact = artifacts.get(artifactId);
                    if (artifact == null || artifact.status() != ArtifactStatus.VERIFIED) {
                        allVerified = false;
                        edgeWarnings.add("Artifact '" + artifactId + "' is missing or not verified.");
                    }
                }
                status = allVerified ? EdgeStatus.VERIFIED : EdgeStatus.ARTIFACT_UNVERIFIED;
            }
            if (scope.stream().anyMatch(value -> !SUPPORTED_SCOPE.contains(value))) {
                edgeWarnings.add("Edge contains an unsupported symbol scope.");
                if (status == EdgeStatus.VERIFIED) status = EdgeStatus.PARTIAL;
            }
            result.add(new EdgeFinding(id, from, to, scope, artifactIds, declaredStatus,
                    status, reviewEvidence, identityEvidence, edgeWarnings));
        }
        result.sort(Comparator.comparing(value -> value.edgeId() == null ? "" : value.edgeId()));
        return result;
    }

    private static List<ContextPathFinding> evaluateContextPaths(
            List<ContextBindingFinding> bindings,
            String targetNamespace,
            List<EdgeFinding> edges) {
        if (targetNamespace == null) return List.of();
        List<EdgeFinding> usable = edges.stream()
                .filter(edge -> edge.status() == EdgeStatus.VERIFIED)
                .sorted(Comparator.comparing(edge -> edge.edgeId() == null ? "" : edge.edgeId()))
                .toList();
        List<ContextPathFinding> findings = new ArrayList<>();
        for (ContextBindingFinding binding : bindings) {
            List<List<String>> paths = new ArrayList<>();
            enumeratePaths(binding.namespaceId(), targetNamespace, usable,
                    new LinkedHashSet<>(Set.of(binding.namespaceId())), new ArrayList<>(), paths);
            int count = Math.min(paths.size(), 2);
            String status = count == 0 ? "NO_COMPLETE_PATH"
                    : count > 1 ? "AMBIGUOUS_PATH" : "COMPLETE_UNAMBIGUOUS_PATH";
            findings.add(new ContextPathFinding(binding.contextKey(), binding.namespaceId(),
                    targetNamespace, count, paths.subList(0, count), status));
        }
        findings.sort(Comparator.comparing(ContextPathFinding::contextKey));
        return findings;
    }

    private static void enumeratePaths(
            String current,
            String target,
            List<EdgeFinding> edges,
            Set<String> visited,
            List<String> edgePath,
            List<List<String>> results) {
        if (results.size() >= 2) return;
        for (EdgeFinding edge : edges) {
            if (!current.equals(edge.fromNamespace()) || edge.toNamespace() == null
                    || visited.contains(edge.toNamespace())) continue;
            edgePath.add(edge.edgeId());
            if (target.equals(edge.toNamespace())) {
                results.add(List.copyOf(edgePath));
            } else {
                visited.add(edge.toNamespace());
                enumeratePaths(edge.toNamespace(), target, edges, visited, edgePath, results);
                visited.remove(edge.toNamespace());
            }
            edgePath.removeLast();
            if (results.size() >= 2) return;
        }
    }

    private static void validateAugmentations(JsonObject target, List<String> warnings) {
        JsonElement element = target.get("mappingAugmentations");
        if (element == null || element.isJsonNull()) return;
        if (!element.isJsonArray()) {
            warnings.add("target.mappingAugmentations is not an array and was ignored.");
            return;
        }
        for (int i = 0; i < element.getAsJsonArray().size(); i++) {
            JsonObject augmentation = objectValue(element.getAsJsonArray().get(i));
            if (augmentation == null) warnings.add("target.mappingAugmentations[" + i + "] is not an object.");
            else if (augmentation.has("renamesClassesOrMembers")
                    && augmentation.get("renamesClassesOrMembers").isJsonPrimitive()
                    && augmentation.get("renamesClassesOrMembers").getAsBoolean()) {
                warnings.add("Target mapping augmentation " + i
                        + " claims class/member renames; it must be modeled as a directed mapping edge.");
            }
        }
    }

    private static void collectUnknownFields(
            JsonObject object, Set<String> known, String location, List<String> warnings) {
        object.keySet().stream().filter(key -> !known.contains(key)).sorted()
                .forEach(key -> warnings.add("Unknown field '" + location + "." + key + "' was retained only as a warning."));
    }

    private static String requiredString(JsonObject object, String key, String location, List<String> errors) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isBlank()) {
            errors.add("Required non-empty string '" + location + "." + key + "' is missing or invalid.");
            return null;
        }
        String value = element.getAsString().trim();
        if (value.length() > 1024) {
            errors.add("String '" + location + "." + key + "' exceeds 1024 characters.");
            return null;
        }
        return value;
    }

    private static String optionalString(JsonObject object, String key, String location, List<String> warnings) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            warnings.add("Optional string '" + location + "." + key + "' has an unsupported type.");
            return null;
        }
        String value = element.getAsString().trim();
        if (value.isEmpty()) return null;
        if (value.length() > 1024) {
            warnings.add("Optional string '" + location + "." + key + "' exceeds 1024 characters.");
            return null;
        }
        return value;
    }

    private static List<String> stringArray(
            JsonElement element, String location, List<String> errors, int maximum) {
        if (element == null || element.isJsonNull()) {
            errors.add("Required string array '" + location + "' is missing.");
            return List.of();
        }
        if (!element.isJsonArray()) {
            errors.add("'" + location + "' must be an array of strings.");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > maximum) {
            errors.add("'" + location + "' exceeds " + maximum + " entries.");
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement value : array) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                    || value.getAsString().isBlank()) {
                errors.add("'" + location + "' contains a non-string or blank value.");
                continue;
            }
            String text = value.getAsString().trim();
            if (!seen.add(text)) {
                errors.add("'" + location + "' contains duplicate value '" + text + "'.");
                continue;
            }
            result.add(text);
        }
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static Integer integerValue(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return null;
        try {
            return element.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            return null;
        }
    }

    private static JsonObject objectValue(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static void addIfPresent(Set<String> target, String value) {
        if (value != null && !value.isBlank()) target.add(value);
    }

    private static void validateJsonWithoutDuplicateMembers(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            scanJsonValue(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonParseException("Trailing data after the mapping profile JSON value.");
            }
        }
    }

    private static void scanJsonValue(JsonReader reader, int depth) throws IOException {
        if (depth > MAX_JSON_DEPTH) {
            throw new JsonParseException("Mapping profile JSON nesting exceeds " + MAX_JSON_DEPTH + " levels.");
        }
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject();
                Set<String> names = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!names.add(name)) throw new JsonParseException("Duplicate JSON object member '" + name + "'.");
                    scanJsonValue(reader, depth + 1);
                }
                reader.endObject();
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                while (reader.hasNext()) scanJsonValue(reader, depth + 1);
                reader.endArray();
            }
            case STRING, NUMBER -> reader.nextString();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> throw new JsonParseException("Unexpected JSON token: " + reader.peek());
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) message = throwable.getClass().getSimpleName();
        return message.length() <= 240 ? message : message.substring(0, 237) + "...";
    }
}
