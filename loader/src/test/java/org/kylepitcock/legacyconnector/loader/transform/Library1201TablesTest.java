package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyModDecl;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.NativeLibraries1201;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** The library tables of the 1.20.1 layer ({@code lib-*.tsv}, {@code libraries.tsv}). */
class Library1201TablesTest implements Opcodes {
    @Test
    void libraryRowsAreReadAfterTheMinecraftOnes() {
        Mc1201Tables t = Mc1201Tables.load();
        assertEquals("org/kylepitcock/legacyconnector/v1_20_1/migrate/ArchitecturyEventBuses", t.classes.get("dev/architectury/platform/forge/EventBuses"));
        assertNotNull(t.calls.get("vazkii/patchouli/api/IVariable.asStream()Ljava/util/stream/Stream;"));
        assertTrue(t.bridges.stream().anyMatch(b -> b.owner().equals("top/theillusivec4/curios/api/type/capability/ICurioItem") && b.oldName().equals("getAttributeModifiers")));
        assertNotNull(t.fields.get("top/theillusivec4/curios/api/CuriosCapability.INVENTORY:Llegacyconnector/v1_20_1/net/minecraftforge/common/capabilities/Capability;"));
        assertNotNull(t.calls.get("net/minecraft/world/item/ItemStack.getTag()Lnet/minecraft/nbt/CompoundTag;"), "the Minecraft rows are still there");
    }

    @Test
    void curiosApiCallGoesToTheHelper() {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = "mod/Uses";
        n.superName = "java/lang/Object";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "go", "(Lnet/minecraft/world/entity/LivingEntity;)Ljava/lang/Object;", null, null);
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new MethodInsnNode(INVOKESTATIC, "top/theillusivec4/curios/api/CuriosApi", "getCuriosInventory",
                "(Lnet/minecraft/world/entity/LivingEntity;)Llegacyconnector/v1_20_1/net/minecraftforge/common/util/LazyOptional;", false));
        m.instructions.add(new InsnNode(ARETURN));
        m.maxStack = 1;
        m.maxLocals = 1;
        n.methods.add(m);
        Mc1201Migration.Counts c = Mc1201Migration.apply(n, new Mc1201Hierarchy());
        assertEquals(1, c.calls);
        MethodInsnNode call = (MethodInsnNode) m.instructions.get(1);
        assertEquals("org/kylepitcock/legacyconnector/v1_20_1/migrate/CuriosCompat", call.owner);
    }

    @Test
    void geckoLibColourFloatsArePackedIntoOneInt() {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = "mod/Render";
        n.superName = "java/lang/Object";
        String oldDesc = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "go", "(Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;)V", null, null);
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new InsnNode(ACONST_NULL));
        m.instructions.add(new InsnNode(ACONST_NULL));
        m.instructions.add(new InsnNode(ACONST_NULL));
        m.instructions.add(new InsnNode(ICONST_0));
        m.instructions.add(new InsnNode(ICONST_0));
        for (int i = 0; i < 4; i++) m.instructions.add(new InsnNode(FCONST_1));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "software/bernie/geckolib/renderer/GeoEntityRenderer", "renderCubesOfBone", oldDesc, false));
        m.instructions.add(new InsnNode(RETURN));
        m.maxStack = 10;
        m.maxLocals = 1;
        n.methods.add(m);
        Mc1201Migration.apply(n, new Mc1201Hierarchy());
        MethodInsnNode pack = null;
        MethodInsnNode call = null;
        for (var in : m.instructions) {
            if (in instanceof MethodInsnNode c) {
                if (c.name.equals("pack")) pack = c;
                else call = c;
            }
        }
        assertNotNull(pack, "the four floats are packed");
        assertEquals("(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", call.desc);
    }

    @Test
    void nativeLibraryJarIsSkippedOnlyWhenTheModernBuildIsInstalled() {
        LegacyJar jar = new LegacyJar(Path.of("architectury-9.2.14-forge.jar"));
        jar.version = LegacyVersion.V1_20_1;
        LegacyModDecl mod = new LegacyModDecl();
        mod.legacyId = "architectury";
        jar.mods.add(mod);
        assertNull(NativeLibraries1201.replacing(jar, Set.of()));
        assertEquals("architectury", NativeLibraries1201.replacing(jar, Set.of("architectury")).modernModId());
        mod.legacyId = "somecontentmod";
        assertNull(NativeLibraries1201.replacing(jar, Set.of("somecontentmod")), "only libraries of the table");
        assertTrue(NativeLibraries1201.table().containsKey("curios"));
        assertTrue(Files.exists(Path.of(".")));
    }
}
