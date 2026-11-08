package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;

final class Forge1201TranslatedJarVerifierTest {
    private static final String RESOURCE_LOCATION =
            "net/minecraft/resources/ResourceLocation";
    private static final String RESOURCE_LOCATION_FACTORY =
            "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;";

    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsOnlyDirectCallSitesForUnmappedMethodReferences() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n"
                        + "m_12345_,mappedMethod,0,\n");
        Files.writeString(fieldsCsv,
                "searge,name,side,desc\n"
                        + "f_12345_,MAPPED_FIELD,0,\n");

        Forge1201SrgNameMappings mappings =
                Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        Forge1201ApiUsageAnalyzer.Analysis analysis = new Forge1201ApiUsageAnalyzer.Analysis(
                null,
                3,
                List.of(),
                List.of(),
                null,
                Map.of(),
                List.of(RESOURCE_LOCATION),
                List.of(
                        methodCallSite("sample/FirstCaller", "first", "m_999999_"),
                        methodCallSite("sample/SecondCaller", "second", "m_999999_"),
                        methodCallSite("sample/MappedCaller", "mapped", "m_12345_")),
                List.of(),
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                RESOURCE_LOCATION, "m_999999_", RESOURCE_LOCATION_FACTORY, 2),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                RESOURCE_LOCATION, "m_12345_", RESOURCE_LOCATION_FACTORY, 1)),
                List.of());

        Forge1201SrgNameMappings.Normalization normalization = mappings.normalize(analysis);
        List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> callSites =
                Forge1201TranslatedJarVerifier.findCallSitesForUnmappedMethods(normalization);

        assertEquals(1, normalization.unmappedMethods().size());
        assertEquals(2, callSites.size());
        assertTrue(callSites.stream().anyMatch(value ->
                value.callerClass().equals("sample/FirstCaller") && value.callerMethod().equals("first")));
        assertTrue(callSites.stream().anyMatch(value ->
                value.callerClass().equals("sample/SecondCaller") && value.callerMethod().equals("second")));
        assertTrue(callSites.stream().noneMatch(value -> value.callerClass().equals("sample/MappedCaller")));
    }

    private static Forge1201ApiUsageAnalyzer.ApiMethodCallSite methodCallSite(
            String callerClass, String callerMethod, String targetName) {
        return new Forge1201ApiUsageAnalyzer.ApiMethodCallSite(
                callerClass,
                callerMethod,
                "()V",
                RESOURCE_LOCATION,
                targetName,
                RESOURCE_LOCATION_FACTORY,
                Opcodes.INVOKESTATIC,
                1);
    }
}
