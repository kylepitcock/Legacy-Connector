package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201MappingProfileValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void validatesExplicitContextGraphAndPinnedArtifactDigest() throws Exception {
        Path profile = writeProfile(validProfile());

        var finding = Forge1201MappingProfileValidator.inspect(profile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_VALIDATED, finding.status());
        assertEquals("sample-profile", finding.profileId());
        assertEquals("minecraft:forge-srg@1.20.1", finding.contextBindings().getFirst().namespaceId());
        assertEquals(1, finding.artifacts().size());
        assertEquals(Forge1201MappingProfileValidator.ArtifactStatus.VERIFIED,
                finding.artifacts().getFirst().status());
        assertEquals(40, finding.artifacts().getFirst().actualUpstreamChecksum().length());
        assertEquals(1, finding.edges().size());
        assertEquals(Forge1201MappingProfileValidator.EdgeStatus.VERIFIED,
                finding.edges().getFirst().status());
        assertEquals(2, finding.contextPaths().size());
        assertTrue(finding.contextPaths().stream().allMatch(path -> path.completePathCount() == 1));
        assertNotNull(finding.profileSha256());
        assertTrue(finding.validationErrors().isEmpty());
    }

    @Test
    void reportsMissingCrossVersionEdgeInsteadOfAssumingIdentity() throws Exception {
        String profileJson = validProfile().replace("\"status\":\"VERIFIED\"", "\"status\":\"PARTIAL\"");
        Path profile = writeProfile(profileJson);

        var finding = Forge1201MappingProfileValidator.inspect(profile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_NO_COMPLETE_PATH, finding.status());
        assertTrue(finding.contextPaths().stream().allMatch(path -> path.completePathCount() == 0));
        assertTrue(finding.edges().stream().allMatch(edge ->
                edge.status() != Forge1201MappingProfileValidator.EdgeStatus.VERIFIED));
        assertTrue(finding.contextPaths().stream().allMatch(path -> "NO_COMPLETE_PATH".equals(path.status())));
    }

    @Test
    void rejectsArtifactDigestMismatch() throws Exception {
        String profileJson = validProfile().replace("HASH_TOKEN", "0".repeat(64));
        Path profile = writeProfile(profileJson);

        var finding = Forge1201MappingProfileValidator.inspect(profile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_ARTIFACT_DIGEST_MISMATCH,
                finding.status());
        assertEquals(Forge1201MappingProfileValidator.ArtifactStatus.DIGEST_MISMATCH,
                finding.artifacts().getFirst().status());
        assertFalse(finding.artifacts().getFirst().expectedSha256()
                .equalsIgnoreCase(finding.artifacts().getFirst().actualSha256()));
    }

    @Test
    void rejectsUpstreamChecksumMismatchAndPendingLicenseReview() throws Exception {
        String badUpstream = validProfile().replace("UPSTREAM_SHA1_TOKEN", "0".repeat(40));
        var checksumFinding = Forge1201MappingProfileValidator.inspect(writeProfile(badUpstream));
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_ARTIFACT_DIGEST_MISMATCH,
                checksumFinding.status());
        assertNotNull(checksumFinding.artifacts().getFirst().actualUpstreamChecksum());
        assertFalse(checksumFinding.artifacts().getFirst().actualUpstreamChecksum()
                .equalsIgnoreCase(checksumFinding.artifacts().getFirst().upstreamChecksum().substring(5)));

        String pendingLicense = validProfile().replace("\"reviewStatus\":\"REVIEWED\"",
                "\"reviewStatus\":\"PENDING\"");
        var licenseFinding = Forge1201MappingProfileValidator.inspect(writeProfile(pendingLicense));
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_LICENSE_REVIEW_REQUIRED,
                licenseFinding.status());
        assertEquals(Forge1201MappingProfileValidator.ArtifactStatus.LICENSE_REVIEW_REQUIRED,
                licenseFinding.artifacts().getFirst().status());
    }

    @Test
    void blocksUnmodeledRuntimeRemappingAndDisabledProfileLicenseReview() throws Exception {
        String runtimeEnabled = validProfile().replace(
                "\"status\":\"UNMODELED_UNLESS_EXPLICITLY_SUPPLIED\"",
                "\"status\":\"ENABLED\"");
        var runtimeFinding = Forge1201MappingProfileValidator.inspect(writeProfile(runtimeEnabled));
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_INCOMPLETE,
                runtimeFinding.status());

        String licenseSkipped = validProfile().replace("\"required\":true", "\"required\":false");
        var licenseFinding = Forge1201MappingProfileValidator.inspect(writeProfile(licenseSkipped));
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_INCOMPLETE,
                licenseFinding.status());
        assertTrue(licenseFinding.validationErrors().stream().anyMatch(
                value -> value.contains("must require profile-level license review")));
    }

    @Test
    void reportsSourceAndTargetVersionMismatchExplicitly() throws Exception {
        Path profile = writeProfile(validProfile().replace("\"minecraftVersion\":\"1.21.1\"",
                "\"minecraftVersion\":\"1.21.2\""));

        var finding = Forge1201MappingProfileValidator.inspect(profile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_TARGET_VERSION_MISMATCH,
                finding.status());
        assertTrue(finding.warnings().stream().anyMatch(value -> value.contains("Target Minecraft version")));
    }

    @Test
    void detectsAmbiguousCompletePathsRatherThanChoosingFirstEdge() throws Exception {
        String secondEdge = """
                {
                  "edgeId":"second-path",
                  "from":"minecraft:forge-srg@1.20.1",
                  "to":"minecraft:official-mojang@1.21.1",
                  "direction":"FORWARD",
                  "sourceMinecraftVersion":"1.20.1",
                  "targetMinecraftVersion":"1.21.1",
                  "scope":["class-names","method-names","field-names","descriptors"],
                  "artifactIds":["mapping-artifact"],
                  "status":"VERIFIED",
                  "reviewEvidence":["independent reviewed evidence"]
                }
                """;
        Path profile = writeProfile(validProfile(secondEdge));

        var finding = Forge1201MappingProfileValidator.inspect(profile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_AMBIGUOUS_PATH, finding.status());
        assertTrue(finding.contextPaths().stream().allMatch(path -> path.completePathCount() == 2));
    }

    @Test
    void rejectsEdgesWithWrongVersionContinuityOrUnsupportedDirection() throws Exception {
        String wrongVersion = validProfile().replace(
                "\"targetMinecraftVersion\":\"1.21.1\"",
                "\"targetMinecraftVersion\":\"1.20.1\"");
        var versionFinding = Forge1201MappingProfileValidator.inspect(writeProfile(wrongVersion));
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_NO_COMPLETE_PATH,
                versionFinding.status());
        assertTrue(versionFinding.contextPaths().stream().allMatch(path -> path.completePathCount() == 0));

        String reverse = validProfile().replace(
                "\"direction\":\"FORWARD\"",
                "\"direction\":\"REVERSE\"");
        var directionFinding = Forge1201MappingProfileValidator.inspect(writeProfile(reverse));
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_NO_COMPLETE_PATH,
                directionFinding.status());
        assertTrue(directionFinding.edges().stream().anyMatch(
                edge -> edge.status() == Forge1201MappingProfileValidator.EdgeStatus.DIRECTION_UNSUPPORTED));
    }

    @Test
    void rejectsDuplicateJsonMembersAndUnsupportedSchemaVersions() throws Exception {
        Path duplicate = tempDir.resolve("duplicate-profile.json");
        Files.writeString(duplicate, "{\"schemaVersion\":1,\"schemaVersion\":1}",
                StandardCharsets.UTF_8);
        var duplicateFinding = Forge1201MappingProfileValidator.inspect(duplicate);
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_INVALID_JSON,
                duplicateFinding.status());

        Path unsupported = writeProfile(validProfile().replace("\"schemaVersion\":1", "\"schemaVersion\":99"));
        var unsupportedFinding = Forge1201MappingProfileValidator.inspect(unsupported);
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_UNSUPPORTED_SCHEMA,
                unsupportedFinding.status());
    }

    @Test
    void keepsUnboundBaseTableVisibleAndDoesNotInventItsNamespace() throws Exception {
        String profileJson = validProfile().replaceAll(
                "(?s),\\s*\\x22baseRefmapBinding\\x22\\s*:\\s*\\{.*?\\}(?=\\s*\\})", "");
        Path profile = writeProfile(profileJson);

        var finding = Forge1201MappingProfileValidator.inspect(profile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_VALIDATED, finding.status());
        assertFalse(finding.contextBindings().stream().anyMatch(
                Forge1201MappingProfileValidator.ContextBindingFinding::baseTable));
        assertTrue(finding.warnings().stream().anyMatch(value -> value.contains("Base refmap mappings table")));
    }

    @Test
    void distinguishesAbsentProfileAndMissingProfileFile() throws Exception {
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_NOT_SUPPLIED,
                Forge1201MappingProfileValidator.inspect(null).status());
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_FILE_MISSING,
                Forge1201MappingProfileValidator.inspect(tempDir.resolve("missing.json")).status());
    }

    private Path writeProfile(String json) throws Exception {
        Path artifact = tempDir.resolve("mapping.bin");
        Files.write(artifact, "synthetic pinned mapping bytes".getBytes(StandardCharsets.UTF_8));
        byte[] artifactBytes = Files.readAllBytes(artifact);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(artifactBytes));
        String upstream = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(artifactBytes));
        String replaced = json.replace("HASH_TOKEN", hash).replace("UPSTREAM_SHA1_TOKEN", upstream);
        Path profile = tempDir.resolve("mapping-profile.json");
        Files.writeString(profile, replaced, StandardCharsets.UTF_8);
        return profile;
    }

    private static String validProfile() {
        return validProfile("");
    }

    private static String validProfile(String extraEdge) {
        String edgeAddition = extraEdge.isBlank() ? "" : "," + extraEdge;
        return """
                {
                  "schemaVersion":1,
                  "profileId":"sample-profile",
                  "status":"DRAFT",
                  "source":{
                    "minecraftVersion":"1.20.1",
                    "loader":{"id":"forge","versionRange":"[47,48)"},
                    "compileNamespaceId":"minecraft:official-mojang@1.20.1",
                    "refmapContextBindings":{
                      "searge":{
                        "namespaceId":"minecraft:forge-srg@1.20.1",
                        "evidence":["fixture source refmap build metadata"]
                      }
                    },
                    "baseRefmapBinding":{
                      "namespaceId":"minecraft:forge-srg@1.20.1",
                      "evidence":["fixture source refmap build metadata"]
                    }
                  },
                  "target":{
                    "minecraftVersion":"1.21.1",
                    "loader":{"id":"neoforge","version":"21.1.258"},
                    "compileNamespaceId":"minecraft:official-mojang@1.21.1",
                    "mappingAugmentations":[{
                      "provider":"Parchment",
                      "minecraftVersion":"1.21.1",
                      "version":"2024.11.17",
                      "scope":["parameter-names","javadocs"],
                      "renamesClassesOrMembers":false
                    }]
                  },
                  "artifacts":[{
                    "artifactId":"mapping-artifact",
                    "provider":"fixture-provider",
                    "version":"1.0.0",
                    "format":"tiny-v2",
                    "coordinate":"org.example:mapping:1.0.0",
                    "sha256":"HASH_TOKEN",
                    "upstreamChecksum":"sha1:UPSTREAM_SHA1_TOKEN",
                    "localPath":"mapping.bin",
                    "license":{
                      "id":"MIT",
                      "reviewStatus":"REVIEWED",
                      "evidence":["fixture license review"]
                    }
                  }],
                  "edges":[{
                    "edgeId":"source-to-target",
                    "from":"minecraft:forge-srg@1.20.1",
                    "to":"minecraft:official-mojang@1.21.1",
                    "direction":"FORWARD",
                    "sourceMinecraftVersion":"1.20.1",
                    "targetMinecraftVersion":"1.21.1",
                    "scope":["class-names","method-names","field-names","descriptors"],
                    "artifactIds":["mapping-artifact"],
                    "status":"VERIFIED",
                    "reviewEvidence":["reviewed source-to-target evidence"]
                  }%s],
                "\"status\":\"UNMODELED_UNLESS_EXPLICITLY_SUPPLIED\""
                "\"status\":\"ENABLED\"");
                    "required":true,
                    "evidence":["fixture profile-level license review"]
                  }
                }
                """.formatted(edgeAddition);
    }
}
