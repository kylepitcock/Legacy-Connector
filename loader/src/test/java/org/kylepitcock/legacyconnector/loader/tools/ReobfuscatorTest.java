package org.kylepitcock.legacyconnector.loader.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

class ReobfuscatorTest {
    @TempDir Path temp;

    @Test void forgeDeclarationsKeepOwnersDescriptorsAndInheritedOverrides() throws Exception {
        Path forge = temp.resolve("forge.jar");
        jar(forge, "conf/packaged.srg", ("MD: a/a (Ljava/lang/String;)La; net/minecraft/block/Block/func_149663_c (Ljava/lang/String;)Lnet/minecraft/block/Block;\n"
                + "FD: b/a net/minecraft/entity/Entity/field_70165_t\n").getBytes(StandardCharsets.UTF_8));
        Path api = temp.resolve("api.jar");
        jar(api, "fixture/ApiBlock.class", type("fixture/ApiBlock", "net/minecraft/block/Block", false));
        Path input = temp.resolve("in.jar");
        jar(input, "fixture/ModBlock.class", type("fixture/ModBlock", "fixture/ApiBlock", true));
        Path output = temp.resolve("out.jar");
        Reobfuscator.main(new String[] {input.toString(), "--forge1710", forge.toString(), api.toString(), output.toString()});
        try (ZipFile zip = new ZipFile(output.toFile())) {
            ClassNode cls = new ClassNode();
            new ClassReader(zip.getInputStream(zip.getEntry("fixture/ModBlock.class"))).accept(cls, 0);
            assertTrue(cls.methods.stream().anyMatch(m -> m.name.equals("func_149663_c")));
            assertTrue(cls.methods.stream().anyMatch(m -> m.name.equals("setBlockName") && m.desc.equals("()V")));
            var calls = cls.methods.stream().filter(m -> m.name.equals("calls")).findFirst().orElseThrow().instructions;
            var methods = java.util.stream.StreamSupport.stream(calls.spliterator(), false)
                    .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast).toList();
            assertEquals(List.of("func_149663_c", "setBlockName", "setBlockName"), methods.stream().map(m -> m.name).toList());
            var fields = java.util.stream.StreamSupport.stream(calls.spliterator(), false)
                    .filter(FieldInsnNode.class::isInstance).map(FieldInsnNode.class::cast).toList();
            assertEquals(List.of("field_70165_t", "posX"), fields.stream().map(f -> f.name).toList());
        }
    }

    @Test void missingForgeDeclarationsFailInsteadOfProducingDevFixture() throws Exception {
        Path forge = temp.resolve("forge.jar");
        jar(forge, "unrelated", new byte[0]);
        assertThrows(java.io.IOException.class, () -> Reobfuscator.main(new String[] {
                "unused", "--forge1710", forge.toString(), "unused", temp.resolve("out.jar").toString()}));
    }

    private static byte[] type(String name, String parent, boolean members) {
        ClassWriter cls = new ClassWriter(0);
        cls.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, name, null, parent, null);
        if (members) {
            String desc = "(Ljava/lang/String;)Lnet/minecraft/block/Block;";
            cls.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "setBlockName", desc, null, null).visitEnd();
            cls.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "setBlockName", "()V", null, null).visitEnd();
            var method = cls.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "calls", "()V", null, null);
            method.visitCode();
            for (String owner : List.of(name, "fixture/Unrelated")) {
                method.visitInsn(Opcodes.ACONST_NULL);
                method.visitInsn(Opcodes.ACONST_NULL);
                method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "setBlockName", desc, false);
                method.visitInsn(Opcodes.POP);
            }
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, name, "setBlockName", "()V", false);
            for (String owner : List.of("net/minecraft/entity/Entity", "fixture/Unrelated")) {
                method.visitInsn(Opcodes.ACONST_NULL);
                method.visitFieldInsn(Opcodes.GETFIELD, owner, "posX", "D");
                method.visitInsn(Opcodes.POP2);
            }
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(2, 0);
            method.visitEnd();
        }
        cls.visitEnd();
        return cls.toByteArray();
    }

    private static void jar(Path path, String name, byte[] data) throws Exception {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new JarEntry(name));
            out.write(data);
            out.closeEntry();
        }
    }
}
