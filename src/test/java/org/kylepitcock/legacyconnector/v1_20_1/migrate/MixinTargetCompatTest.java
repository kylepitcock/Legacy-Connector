package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

/** Every helper the rows of {@code mixin-targets.tsv} name exists in {@link MixinTargetCompat} with the descriptor the wrapper will call. */
class MixinTargetCompatTest {
    @Test
    void helpersNamedByTheTableExist() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("loader/src/main/resources/legacyconnector/forward/1.20.1/mixin-targets.tsv"));
        int helpers = 0;
        for (String line : lines) {
            if (!line.startsWith("M\t")) continue;
            String[] f = line.split("\t", -1);
            Type[] oldArgs = Type.getArgumentTypes(f[4 - 1]);
            Type[] newArgs = Type.getArgumentTypes(f[4]);
            String[] tokens = f[5].split(",");
            for (int p = 0; p < tokens.length; p++) {
                int at = tokens[p].indexOf('@');
                if (at < 0) continue;
                String method = tokens[p].substring(tokens[p].lastIndexOf('.') + 1, at);
                String arg = tokens[p].substring(at + 1);
                String desc = arg.isEmpty() ? "()" + oldArgs[p].getDescriptor() : "(" + newArgs[Integer.parseInt(arg)].getDescriptor() + ")" + oldArgs[p].getDescriptor();
                boolean found = false;
                for (var m : MixinTargetCompat.class.getDeclaredMethods()) {
                    if (m.getName().equals(method) && Type.getMethodDescriptor(m).equals(desc)) found = true;
                }
                assertTrue(found, "helper " + method + desc + " of row " + f[2] + f[3]);
                helpers++;
            }
        }
        assertTrue(helpers > 10, "helpers checked: " + helpers);
        assertEquals(0.0F, MixinTargetCompat.colorRed(0xFF00FFFF), 0.0F);
        assertEquals(1.0F, MixinTargetCompat.colorAlpha(0xFF000000), 0.0F);
    }
}
