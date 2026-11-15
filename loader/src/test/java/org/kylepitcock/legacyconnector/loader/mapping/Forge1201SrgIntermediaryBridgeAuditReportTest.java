package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201SrgIntermediaryBridgeAuditReportTest {
    @TempDir Path tempDir;

    @Test
    void emitsDeterministicPerSymbolReportForEverySourceRow() throws Exception {
        Path tsrg=write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                \tx f_20_ 3
                b net/minecraft/Component 4
                """);
        Path source=write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                METHOD\ta\t(I)V\tm\tmethod_1
                FIELD\ta\tLb;\tx\tfield_1
                """);
        Path target=write("target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_1
                CLASS\td\tnet/minecraft/class_2
                METHOD\tc\t(I)V\tz\tmethod_1
                FIELD\tc\tLd;\ty\tfield_1
                """);
        Path first=tempDir.resolve("first.json"),second=tempDir.resolve("second.json");
        String[] args={tsrg.toString(),source.toString(),target.toString(),first.toString()};
        Forge1201SrgIntermediaryBridgeAuditReport.main(args);
        args[3]=second.toString();
        Forge1201SrgIntermediaryBridgeAuditReport.main(args);
        String raw=Files.readString(first,StandardCharsets.UTF_8);
        assertEquals(raw,Files.readString(second,StandardCharsets.UTF_8));
        JsonObject json=JsonParser.parseString(raw).getAsJsonObject();
        assertEquals("PARTIAL",json.get("status").getAsString());
        assertFalse(json.get("completeForgeSrgSelectorPath").getAsBoolean());
        assertFalse(json.get("selectorTransformationPerformed").getAsBoolean());
        assertFalse(json.get("runtimeMappingTableProduced").getAsBoolean());
        JsonObject coverage=json.getAsJsonObject("coverage");
        assertEquals(2,coverage.getAsJsonObject("classes").get("total").getAsInt());
        assertEquals(1,coverage.getAsJsonObject("methods").get("total").getAsInt());
        assertEquals(1,coverage.getAsJsonObject("fields").get("total").getAsInt());
        assertEquals(1,coverage.getAsJsonObject("methods").getAsJsonObject("byStatus").get("MAPPED_EXACT").getAsInt());
        assertEquals(1,coverage.getAsJsonObject("fields").getAsJsonObject("byStatus").get("MAPPED_EXACT").getAsInt());
        JsonObject symbols=json.getAsJsonObject("symbols");
        assertEquals(2,symbols.getAsJsonArray("classes").size());
        assertEquals(1,symbols.getAsJsonArray("methods").size());
        assertEquals(1,symbols.getAsJsonArray("fields").size());
        assertTrue(symbols.getAsJsonArray("methods").get(0).getAsJsonObject()
                .getAsJsonArray("candidates").get(0).getAsJsonObject()
                .get("targetOfficialObfuscatedName").getAsString().equals("z"));
    }

    private Path write(String name,String contents)throws Exception{
        Path p=tempDir.resolve(name);Files.writeString(p,contents,StandardCharsets.UTF_8);return p;
    }
}
