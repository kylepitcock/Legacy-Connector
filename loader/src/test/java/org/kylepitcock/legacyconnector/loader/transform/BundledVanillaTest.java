package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/** A mod jar that carries reobfuscated copies of vanilla classes (Crafting Dead): the game's own class wins, as in Forge. */
class BundledVanillaTest {
    private static byte[] clazz(String name, String sourceFile) {
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        if (sourceFile != null) w.visitSource(sourceFile, null);
        w.visitEnd();
        return w.toByteArray();
    }

    @Test
    void dropsDefaultPackageCopiesOfVanillaClasses() {
        Map<String, byte[]> classes = new HashMap<>();
        classes.put("bbj", clazz("bbj", "ModelBiped.java"));                 // notch ModelBiped
        classes.put("bhj", clazz("bhj", "RenderPlayer.java"));               // notch RenderPlayer
        classes.put("bid", clazz("bid", "ThreadDownloadImageDataINNER1.java")); // its anonymous inner class
        classes.put("bbo", clazz("bbo", "SomethingTheModWrote.java"));       // a notch name, but not that class's source
        classes.put("mod_X", clazz("mod_X", "mod_X.java"));                  // not a notch name
        classes.put("com/x/Y", clazz("com/x/Y", "Y.java"));
        int dropped = BundledVanillaTestAccess.drop(classes);
        assertEquals(2, dropped);
        assertFalse(classes.containsKey("bbj"));
        assertFalse(classes.containsKey("bhj"));
        assertTrue(classes.containsKey("bid"));                              // no notch name for anonymous classes: kept, harmless
        assertTrue(classes.containsKey("bbo"));
        assertTrue(classes.containsKey("mod_X"));
        assertTrue(classes.containsKey("com/x/Y"));
    }

    /** The helper is package private in LegacyJarTransformer; this class lives in its package. */
    static final class BundledVanillaTestAccess {
        static int drop(Map<String, byte[]> classes) {
            return LegacyJarTransformer.dropBundledVanilla(classes, LegacyVersion.V1_6_4);
        }
    }
}
