package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Forward rules that replaced synthesized (empty) stubs of the 1.6.4 layer: after the rewrite the call, field or constructor names a 1.7.10 shim
 * member, or a static helper that has behaviour, instead of an empty generated member.
 */
class ForwardStubRulesTest {
    private static final String P = "legacyconnector/v1_7_10/";

    private static Hierarchy hierarchy() {
        return new Hierarchy(ShimIndex.empty(), null, Map.of(), LegacyVersion.V1_6_4);
    }

    private static ClassNode with(AbstractInsnNode... insns) {
        ClassNode node = new ClassNode();
        node.name = "my/Mod";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        for (AbstractInsnNode i : insns) m.instructions.add(i);
        m.maxStack = 8;
        node.methods.add(m);
        return node;
    }

    private static List<String> body(ClassNode node) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = node.methods.get(0).instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode m) out.add(m.getOpcode() + " " + m.owner + "." + m.name + m.desc);
            else if (i instanceof FieldInsnNode f) out.add(f.getOpcode() + " " + f.owner + "." + f.name + ":" + f.desc);
            else if (i instanceof TypeInsnNode t) out.add(t.getOpcode() + " " + t.desc);
            else out.add("op" + i.getOpcode());
        }
        return out;
    }

    @Test
    void thornsStaticsCallTheBehaviourHelper() {
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKESTATIC, P + "net/minecraft/enchantment/EnchantmentThorns", "func_92096_a",
                "(L" + P + "net/minecraft/entity/Entity;L" + P + "net/minecraft/entity/EntityLivingBase;Ljava/util/Random;)V"));
        assertEquals(1, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        assertEquals(List.of("184 legacyconnector/v1_6_4/forward/Thorns164.attack(L" + P + "net/minecraft/entity/Entity;L" + P
                + "net/minecraft/entity/EntityLivingBase;Ljava/util/Random;)V"), body(node));
        ClassNode hit = with(new MethodInsnNode(Opcodes.INVOKESTATIC, P + "net/minecraft/enchantment/EnchantmentThorns", "func_92094_a", "(ILjava/util/Random;)Z"));
        assertEquals(1, ForwardCalls.apply(hit, hierarchy(), LegacyVersion.V1_6_4));
        assertEquals(List.of("184 legacyconnector/v1_6_4/forward/Thorns164.shouldHit(ILjava/util/Random;)Z"), body(hit));
    }

    @Test
    void chatLinesAreWrappedIntoComponents() {
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, P + "net/minecraft/client/gui/GuiNewChat", "printChatMessage", "(Ljava/lang/String;)V"));
        assertEquals(1, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        List<String> b = body(node);
        assertEquals("187 " + P + "net/minecraft/util/ChatComponentText", b.get(0));
        assertEquals("182 " + P + "net/minecraft/client/gui/GuiNewChat.printChatMessage(L" + P + "net/minecraft/util/IChatComponent;)V", b.get(b.size() - 1));
    }

    @Test
    void chunkManagerHellKeepsBiomeAndRainfall() {
        // new WorldChunkManagerHell(biome, temperature, rainfall): 1.7.10 has no temperature
        String hell = P + "net/minecraft/world/biome/WorldChunkManagerHell";
        String biome = "L" + P + "net/minecraft/world/biome/BiomeGenBase;";
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKESPECIAL, hell, "<init>", "(" + biome + "FF)V"));
        assertEquals(1, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        List<String> b = body(node);
        assertEquals("183 " + hell + ".<init>(" + biome + "F)V", b.get(b.size() - 1));
        // the temperature (argument 1) is spilled away, never passed on: exactly one float is pushed back
        int floatLoads = 0;
        for (AbstractInsnNode i = node.methods.get(0).instructions.getFirst(); i != null; i = i.getNext()) if (i.getOpcode() == Opcodes.FLOAD) floatLoads++;
        assertEquals(1, floatLoads);
    }

    @Test
    void renamedMembersPointAtTheShimNames() {
        var t = org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers.forVersion(LegacyVersion.V1_6_4);
        assertEquals("hovered", t.field("net/minecraft/client/gui/GuiButton", "field_82253_i", null));
        assertEquals("getBackgroundIcon", t.method("net/minecraft/item/ItemArmor", "func_94602_b", "(I)L" + "net/minecraft/util/Icon;", null));
        assertEquals("getOwner", t.method("net/minecraft/entity/passive/EntityWolf", "func_130012_q", "()Lnet/minecraft/entity/EntityLivingBase;", null));
        assertEquals("renderToolTip", t.method("net/minecraft/client/gui/inventory/GuiContainer", "drawItemStackTooltip", "(Lnet/minecraft/item/ItemStack;II)V", null));
    }

    @Test
    void scatteredFeatureMovedToTheStructurePackage() {
        assertEquals("net/minecraft/world/gen/structure/MapGenScatteredFeature",
                ForwardRenames.map(LegacyVersion.V1_6_4, "net/minecraft/world/gen/feature/MapGenScatteredFeature"));
    }
}
