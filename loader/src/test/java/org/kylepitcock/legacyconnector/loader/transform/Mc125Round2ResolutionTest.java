package org.kylepitcock.legacyconnector.loader.transform;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.2.5 round 2 resolution fixes (tools/dev/agent-tasks/1.2.5-gaps.md): a member inherited through a class 1.2.5 does not have, a member inherited through an FML class that extends a
 * game class, the Forge interface that extends the game's IInventory, and the class that moved packages between 1.6.4 and 1.7.10.
 */
class Mc125Round2ResolutionTest {
    // 1.4.7 notch names are real: re = EntityLargeFireball, rb = EntityFireball (1.2.5 has no such class), lq = Entity, bdg = TextureFX
    private static final String TABLE = String.join("\n",
            "C\tc\tfbl\tre", "C\tc\tent\tlq", "C\tc\ttfx\tbdg",
            "F\tc\tent\to\tD\tlq\tt\tD",
            "F\tc\ttfx\tf\t[B\tbdg\timageData\t[B") + "\n";

    private static Notch125To147.Table client() throws IOException {
        return Notch125To147.parse(new BufferedReader(new StringReader(TABLE)))[0];
    }

    private static List<String> members(byte[] bytes) {
        List<String> out = new ArrayList<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String n, String d) {
                        out.add(owner + "." + n + ":" + d);
                    }
                };
            }
        }, 0);
        return out;
    }

    private static byte[] reader(String name, String superName, String fieldOwner, String field, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, name, null, superName, null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "run", "(L" + fieldOwner + ";)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.GETFIELD, fieldOwner, field, desc);
        mv.visitInsn(desc.equals("D") ? Opcodes.POP2 : Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** The ghast fireball's own posX of 1.2.5: 1.4.2 put EntityFireball between it and Entity, a class 1.2.5 has no counterpart of, so the walk used to stop there. */
    @Test
    void memberInheritedThroughAClassOf147OnlyIsFound() throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("Herobrine", reader("Herobrine", "java/lang/Object", "fbl", "o", "D"));
        Notch125To147.apply(client(), NotchNames.get("1.4.7"), classes);
        List<String> m = members(classes.get("Herobrine"));
        assertTrue(m.contains("re.t:D"), "the field of Entity is reached through the fireball and its 1.4.7-only base: " + m);
    }

    /** A mod class extends FML's FMLTextureFX, which extends the game's TextureFX: the inherited game field is named like the game's, not left under the 1.2.5 letter. */
    @Test
    void memberInheritedThroughAnFmlClassIsFound() throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("ModFx", reader("ModFx", "cpw/mods/fml/client/FMLTextureFX", "ModFx", "f", "[B"));
        Notch125To147.apply(client(), NotchNames.get("1.4.7"), classes);
        List<String> m = members(classes.get("ModFx"));
        assertTrue(m.contains("ModFx.imageData:[B"), m.toString());
    }

    /** Forge 3.x's ISidedInventory extends IInventory: a call of the game's inventory method through it gets the 1.7.10 name (Logistics Pipes). */
    @Test
    void forgeInterfaceReachesTheGameMethodRenames() {
        ForwardMembers table = ForwardMembers.forVersion(LegacyVersion.V1_4_7);
        String owner = "net/minecraft/src/forge/ISidedInventory";
        assertEquals("closeInventory", table.method(owner, "closeChest", "()V", null));
        assertEquals("openInventory", table.method(owner, "openChest", "()V", null));
        assertEquals("getInventoryName", table.method(owner, "getInvName", "()Ljava/lang/String;", null));
        assertEquals("markDirty", table.method(owner, "onInventoryChanged", "()V", null));
    }

    @Test
    void loadingScreenRendererMovedPackages() {
        assertEquals("net/minecraft/client/LoadingScreenRenderer", ForwardRenames.map(LegacyVersion.V1_6_4, "net/minecraft/client/gui/LoadingScreenRenderer"));
        assertEquals("net/minecraft/client/LoadingScreenRenderer", ForwardRenames.map(LegacyVersion.V1_4_7, "net/minecraft/src/LoadingScreenRenderer"));
    }

    /** EntityLiving.lastDamage was an int: reads and writes go through the helper that keeps the modern entity's float. */
    @Test
    void lastDamageIsAnIntThroughHelpers() {
        ForwardMembers table = ForwardMembers.forVersion(LegacyVersion.V1_4_7);
        assertTrue(table.fieldCallRules().containsKey("net/minecraft/entity/EntityLivingBase.lastDamage"));
        assertTrue(table.fieldSetRules().containsKey("net/minecraft/entity/EntityLivingBase.lastDamage"));
    }
}
