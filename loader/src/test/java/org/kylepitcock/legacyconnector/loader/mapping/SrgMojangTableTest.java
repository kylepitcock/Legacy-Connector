package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SrgMojangTableTest {
    @TempDir
    Path tempDir;

    @Test
    void joinsMemberNamesByObfuscatedOwnerAndFullMethodDescriptor() throws Exception {
        Path mojang = write("client.txt", """
                test.Type -> a:
                    void noArg() -> b
                    int withString(java.lang.String) -> c
                    int value -> d
                """);
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a
                    b ()V m_1_ 1
                    c (Ljava/lang/String;)I m_2_ 2
                    d f_3_ 3
                """);

        var generated = SrgMojangTable.generateDetailed(tsrg, mojang);

        assertEquals("noArg", generated.table().get("m_1_"));
        assertEquals("withString", generated.table().get("m_2_"));
        assertEquals("value", generated.table().get("f_3_"));
        assertEquals(3, generated.table().size());
        assertEquals(1, generated.coverage().mojangClassRows());
        assertEquals(2, generated.coverage().mojangMethodRows());
        assertEquals(1, generated.coverage().mojangFieldRows());
        assertEquals(1, generated.coverage().joinedClassRows());
        assertEquals(2, generated.coverage().joinedMethodRows());
        assertEquals(1, generated.coverage().joinedFieldRows());
        assertEquals(2, generated.coverage().matchedMethods());
        assertEquals(1, generated.coverage().matchedFields());
        assertEquals(0, generated.coverage().unmatchedMethods());
        assertEquals(0, generated.coverage().unmatchedFields());
        assertEquals(3, generated.coverage().outputRows());
    }

    @Test
    void refusesToMatchMethodsWhenTheirFullDescriptorDiffers() throws Exception {
        Path mojang = write("client.txt", """
                test.Type -> a:
                    void noArg() -> b
                    int withString(java.lang.String) -> c
                """);
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a
                    b ()V m_1_ 1
                    c (I)I m_2_ 2
                """);

        var generated = SrgMojangTable.generateDetailed(tsrg, mojang);

        assertEquals("noArg", generated.table().get("m_1_"));
        assertNull(generated.table().get("m_2_"));
        assertEquals(2, generated.coverage().joinedMethodRows());
        assertEquals(1, generated.coverage().matchedMethods());
        assertEquals(1, generated.coverage().unmatchedMethods());
    }

    @Test
    void refusesToMatchMembersFromTheWrongObfuscatedOwner() throws Exception {
        Path mojang = write("client.txt", """
                test.Type -> a:
                    void noArg() -> b
                """);
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                other/Type
                    b ()V m_1_ 1
                """);

        assertThrows(IOException.class, () -> SrgMojangTable.generateDetailed(tsrg, mojang));
    }

    @Test
    void rejectsConflictingNamesForOneGloballyUniqueSrgId() throws Exception {
        Path mojang = write("client.txt", """
                test.Type -> a:
                    void first() -> b
                    void second() -> c
                """);
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a
                    b ()V m_1_ 1
                    c ()V m_1_ 1
                """);

        assertThrows(IOException.class, () -> SrgMojangTable.generateDetailed(tsrg, mojang));
    }

    private Path write(String name, String contents) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}
