package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The jar inspector finds @SidedProxy fields for the runtime to inject, also the default-package annotation of Minecraft 1.2.x ModLoader mods (BlockHelper). */
class SidedProxyCollectionTest {
    private static byte[] classWithProxy(String annotation) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_Sided", null, "java/lang/Object", null);
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "proxy", "Ljava/lang/Object;", null, null);
        fv.visitAnnotation(annotation, true).visitEnd();
        fv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static int collect(String annotation) throws Exception {
        LegacyJar jar = new LegacyJar(Path.of("mod.jar"));
        Method m = LegacyJarInspector.class.getDeclaredMethod("collectSidedProxies", ClassReader.class, LegacyJar.class);
        m.setAccessible(true);
        m.invoke(null, new ClassReader(classWithProxy(annotation)), jar);
        return jar.sidedProxies.size();
    }

    @Test
    void everySpellingOfTheAnnotationIsCollected() throws Exception {
        assertEquals(1, collect("Lcpw/mods/fml/common/SidedProxy;"));
        assertEquals(1, collect("Lnet/minecraftforge/fml/common/SidedProxy;"));
        assertEquals(1, collect("LSidedProxy;"));
        assertEquals(1, collect("Lnet/minecraft/src/SidedProxy;"));
        assertEquals(0, collect("Lsome/other/Annotation;"));
    }
}
