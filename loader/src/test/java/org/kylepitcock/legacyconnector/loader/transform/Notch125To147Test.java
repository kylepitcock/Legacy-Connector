package org.kylepitcock.legacyconnector.loader.transform;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 1.2.5 to 1.4.7 name rewrite on synthetic {@code mod_*} classes assembled in the test: a ModLoader mod registers a block, an item, a recipe and a name
 * with the capitalised API; every call must end up on the 1.4.7 shim's ModLoader, and game references on 1.4.7's obfuscation.
 */
class Notch125To147Test {
    // 1.2.5 "aaa" is Block, "bbb" an Item, "ccc" an ItemStack, "ddd" a World; 1.4.7 calls them x1, x2, x3, x4 (synthetic table rows)
    private static final String TABLE = String.join("\n",
            "C\tc\taaa\tx1", "C\tc\tbbb\tx2", "C\tc\tccc\tx3", "C\tc\tddd\tx4",
            "M\tc\taaa\ta\t(I)Laaa;\tx1\tb\t(I)Lx1;",
            "M\tc\tbbb\ta\t(Laaa;)V\tx2\tc\t(Lx1;)V",
            "M\tc\tddd\tb\t(III)Laaa;\tx4\td\t(III)Lx1;",
            "F\tc\taaa\tz\tLaaa;\tx1\ty\tLx1;") + "\n";

    private static Notch125To147.Table client() throws IOException {
        return Notch125To147.parse(new BufferedReader(new StringReader(TABLE)))[0];
    }

    /** {@code public class mod_Test extends BaseMod} (default package): Load() registers through the capitalised API. */
    private static byte[] modClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_Test", null, "BaseMod", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "BaseMod", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor load = cw.visitMethod(Opcodes.ACC_PUBLIC, "Load", "()V", null, null); // Beta-era capitalised hook
        load.visitCode();
        // block: aaa.a(1), registered and named; item recipe; smelting
        load.visitLdcInsn(1);
        load.visitMethodInsn(Opcodes.INVOKESTATIC, "aaa", "a", "(I)Laaa;", false);
        load.visitMethodInsn(Opcodes.INVOKESTATIC, "ModLoader", "RegisterBlock", "(Laaa;)V", false);
        load.visitInsn(Opcodes.ACONST_NULL);
        load.visitLdcInsn("Test Block");
        load.visitMethodInsn(Opcodes.INVOKESTATIC, "ModLoader", "AddName", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
        load.visitInsn(Opcodes.ACONST_NULL);
        load.visitInsn(Opcodes.ACONST_NULL);
        load.visitMethodInsn(Opcodes.INVOKESTATIC, "ModLoader", "AddRecipe", "(Lccc;[Ljava/lang/Object;)V", false);
        load.visitInsn(Opcodes.ACONST_NULL);
        load.visitInsn(Opcodes.ACONST_NULL);
        load.visitMethodInsn(Opcodes.INVOKESTATIC, "ModLoader", "AddShapelessRecipe", "(Lccc;[Ljava/lang/Object;)V", false);
        load.visitFieldInsn(Opcodes.GETSTATIC, "aaa", "z", "Laaa;");
        load.visitInsn(Opcodes.POP);
        load.visitInsn(Opcodes.RETURN);
        load.visitMaxs(0, 0);
        load.visitEnd();
        MethodVisitor version = cw.visitMethod(Opcodes.ACC_PUBLIC, "Version", "()Ljava/lang/String;", null, null);
        version.visitCode();
        version.visitLdcInsn("1");
        version.visitInsn(Opcodes.ARETURN);
        version.visitMaxs(0, 0);
        version.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** {@code class mod_Test$Item extends bbb} overriding {@code a(aaa)} and calling the inherited {@code this.a(aaa)}. */
    private static byte[] subclass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "TestItem", null, "bbb", null);
        MethodVisitor m = cw.visitMethod(Opcodes.ACC_PUBLIC, "a", "(Laaa;)V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ALOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, "bbb", "a", "(Laaa;)V", false);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        MethodVisitor call = cw.visitMethod(Opcodes.ACC_PUBLIC, "go", "()V", null, null);
        call.visitCode();
        call.visitVarInsn(Opcodes.ALOAD, 0);
        call.visitInsn(Opcodes.ACONST_NULL);
        call.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "TestItem", "a", "(Laaa;)V", false); // owner is the mod class: resolved up to bbb
        call.visitInsn(Opcodes.RETURN);
        call.visitMaxs(0, 0);
        call.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Every member insn of a class as {@code owner.name desc}, plus its declared methods as {@code decl name desc}. */
    private static List<String> members(byte[] bytes) {
        List<String> out = new ArrayList<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                out.add("decl " + name + desc);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                        out.add(owner + "." + n + d);
                    }

                    @Override
                    public void visitFieldInsn(int op, String owner, String n, String d) {
                        out.add(owner + "." + n + ":" + d);
                    }
                };
            }
        }, 0);
        return out;
    }

    private static Map<String, byte[]> jar() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("mod_Test", modClass());
        classes.put("TestItem", subclass());
        return classes;
    }

    @Test
    void registrationsReachTheLowerCaseModLoaderFacade() throws IOException {
        Map<String, byte[]> classes = jar();
        Notch125To147.apply(client(), NotchNames.get("1.4.7"), classes);
        List<String> m = members(classes.get("mod_Test"));
        assertTrue(m.contains("ModLoader.registerBlock(Lx1;)V"), m.toString());
        assertTrue(m.contains("ModLoader.addName(Ljava/lang/Object;Ljava/lang/String;)V"), m.toString());
        assertTrue(m.contains("ModLoader.addRecipe(Lx3;[Ljava/lang/Object;)V"), m.toString());
        assertTrue(m.contains("ModLoader.addShapelessRecipe(Lx3;[Ljava/lang/Object;)V"), m.toString());
        assertTrue(m.contains("x1.b(I)Lx1;"), "game method resolved through the table: " + m);
        assertTrue(m.contains("x1.y:Lx1;"), "game field: " + m);
        assertTrue(m.contains("decl load()V"), "capitalised hook renamed: " + m);
        assertTrue(m.contains("decl getVersion()Ljava/lang/String;"), m.toString());
        assertEquals("BaseMod", new ClassReader(classes.get("mod_Test")).getSuperName(), "BaseMod stays the ModLoader class of the 1.4.7 chain");
    }

    @Test
    void subclassOverrideAndInheritedCallFollowTheGameMethod() throws IOException {
        Map<String, byte[]> classes = jar();
        Notch125To147.apply(client(), NotchNames.get("1.4.7"), classes);
        ClassReader r = new ClassReader(classes.get("TestItem"));
        assertEquals("x2", r.getSuperName());
        List<String> m = members(classes.get("TestItem"));
        assertTrue(m.contains("decl c(Lx1;)V"), "the override is renamed with the game method: " + m);
        assertTrue(m.contains("x2.c(Lx1;)V"), "super call: " + m);
        assertTrue(m.contains("TestItem.c(Lx1;)V"), "call through the mod class resolves up to the game declaration: " + m);
    }

    @Test
    void copiesOfGameClassesAreDroppedAndReported() throws IOException {
        Map<String, byte[]> classes = jar();
        classes.put("aaa", modClass()); // a jar that ships its own (modified) Block
        List<String> notes = Notch125To147.apply(client(), NotchNames.get("1.4.7"), classes);
        assertFalse(classes.containsKey("aaa"));
        assertTrue(notes.stream().anyMatch(n -> n.startsWith("Dropped 1 modified Minecraft class")), notes.toString());
    }

    /** Every capitalised static the rewrite promises to rename has its lower-case form in the 1.4.7 shim's ModLoader. */
    @Test
    void renamedStaticsExistInTheShimModLoader() throws IOException {
        Path src = Path.of("..", "src", "main", "java", "legacyconnector", "v1_6_4", "net", "minecraft", "src", "ModLoader.java");
        assertTrue(Files.exists(src), "run from the loader module: " + src.toAbsolutePath());
        Set<String> shim = new HashSet<>();
        Matcher mm = Pattern.compile("public static [^=;(]*?\\b([a-zA-Z$]+)\\(").matcher(Files.readString(src));
        while (mm.find()) shim.add(mm.group(1));
        for (String name : Notch125To147.MODLOADER_STATICS) {
            String lower = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            assertTrue(shim.contains(lower), name + " -> " + lower + " is not a static of the 1.4.7 shim ModLoader");
        }
    }
}
