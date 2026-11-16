package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Every {@code TailClient.} row of {@code forge-call-rewrites.tsv} (the client events whose 1.20.1 methods 1.21.1 changed; see
 * docs/wiki/Legacy-1.20.1-Client-Events.md) must have a helper in {@link ForgeApiTailClient1201} with exactly the descriptor the loader generates:
 * the receiver typed as the owner of the rewritten call first, then the original arguments, the original return type. A missing overload is a
 * {@code NoSuchMethodError} in the game, which no offline link check of the helper class would show.
 */
class ClientEventRewriteHelpersTest {
    private static final Path TABLE = Path.of("loader/src/main/resources/legacyconnector/forward/1.20.1/forge-call-rewrites.tsv");

    @Test
    void everyTailClientRowHasAHelperWithTheGeneratedDescriptor() throws Exception {
        Set<String> available = new HashSet<>();
        for (Method m : ForgeApiTailClient1201.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
                available.add(m.getName() + java.lang.invoke.MethodType.methodType(m.getReturnType(), m.getParameterTypes()).toMethodDescriptorString());
            }
        }
        int rows = 0;
        for (String line : Files.readAllLines(TABLE, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] a = line.strip().split("\t");
            if (a.length != 2 || !a[1].startsWith("TailClient.")) continue;
            rows++;
            String key = a[0];
            int paren = key.indexOf('(');
            int dot = key.lastIndexOf('.', paren);
            String owner = key.substring(0, dot);
            String desc = key.substring(paren);
            String helper = a[1].substring("TailClient.".length());
            int close = desc.indexOf(')');
            String expected = helper + "(L" + owner + ";" + desc.substring(1);
            assertTrue(available.contains(expected), "no helper " + expected + " in ForgeApiTailClient1201 for the row " + key);
            assertFalse(owner.contains("minecraftforge"), "the owner is the NeoForge name after the renames: " + key);
            assertEquals('(', desc.charAt(0));
            assertTrue(close > 0);
        }
        assertTrue(rows >= 30, "the client rows are there: " + rows);
    }
}
