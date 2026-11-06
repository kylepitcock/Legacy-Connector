package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

class ForwardCallsTest {
    private static final String P = "legacyconnector/v1_7_10/";

    private static Hierarchy hierarchy() {
        return new Hierarchy(ShimIndex.empty(), null, Map.of(), LegacyVersion.V1_6_4);
    }

    /** A class with one method whose body is the given instructions. */
    private static ClassNode with(AbstractInsnNode... insns) {
        ClassNode node = new ClassNode();
        node.name = "my/Mod";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        for (AbstractInsnNode i : insns) m.instructions.add(i);
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
    void getTagListBecomesAHelperCallWithTheReceiverFirst() {
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, P + "net/minecraft/nbt/NBTTagCompound", "getTagList",
                "(Ljava/lang/String;)L" + P + "net/minecraft/nbt/NBTTagList;"));
        assertEquals(1, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        assertEquals(List.of("184 legacyconnector/v1_6_4/forward/Forward164.getTagList(L" + P + "net/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)L"
                + P + "net/minecraft/nbt/NBTTagList;"), body(node));
    }

    @Test
    void addChatMessageWrapsTheStringAndFindsTheRuleOnASuperclass() {
        // the call goes through a subclass the rule does not name
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, P + "net/minecraft/entity/player/EntityPlayer", "addChatMessage", "(Ljava/lang/String;)V"));
        assertEquals(1, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        List<String> b = body(node);
        assertEquals(List.of("187 " + P + "net/minecraft/util/ChatComponentText", "op90", "op95",
                "183 " + P + "net/minecraft/util/ChatComponentText.<init>(Ljava/lang/String;)V",
                "182 " + P + "net/minecraft/entity/player/EntityPlayer.addChatMessage(L" + P + "net/minecraft/util/IChatComponent;)V"), b);
    }

    @Test
    void usernameFieldBecomesAGetterOnTheModsOwnPlayerSubclass() {
        Hierarchy h = hierarchy();
        h.addLocal(new ClassInfo("my/Player", P + "net/minecraft/entity/player/EntityPlayer", List.of(), Opcodes.ACC_PUBLIC));
        ClassNode node = with(new FieldInsnNode(Opcodes.GETFIELD, "my/Player", "username", "Ljava/lang/String;"));
        assertEquals(1, ForwardCalls.apply(node, h, LegacyVersion.V1_6_4));
        assertEquals(List.of("182 my/Player.getCommandSenderName()Ljava/lang/String;"), body(node));
    }

    @Test
    void blockMaterialIsReadFromTheBlock() {
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEINTERFACE, P + "net/minecraft/world/IBlockAccess", "getBlockMaterial", "(III)L" + P + "net/minecraft/block/material/Material;", true));
        assertEquals(1, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        assertEquals(List.of("185 " + P + "net/minecraft/world/IBlockAccess.getBlock(III)L" + P + "net/minecraft/block/Block;",
                "182 " + P + "net/minecraft/block/Block.getMaterial()L" + P + "net/minecraft/block/material/Material;"), body(node));
    }

    @Test
    void unnamedTagConstructorsDropTheName() {
        // new NBTTagString("n", "v"): [name, value] -> [value]
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKESPECIAL, P + "net/minecraft/nbt/NBTTagString", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
                new MethodInsnNode(Opcodes.INVOKESPECIAL, P + "net/minecraft/nbt/NBTTagDouble", "<init>", "(Ljava/lang/String;D)V"),
                new MethodInsnNode(Opcodes.INVOKESPECIAL, P + "net/minecraft/nbt/NBTTagCompound", "<init>", "(Ljava/lang/String;)V"));
        assertEquals(3, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        assertEquals(List.of("op95", "op87", "183 " + P + "net/minecraft/nbt/NBTTagString.<init>(Ljava/lang/String;)V",
                "op93", "op88", "op87", "183 " + P + "net/minecraft/nbt/NBTTagDouble.<init>(D)V",
                "op87", "183 " + P + "net/minecraft/nbt/NBTTagCompound.<init>()V"), body(node));
    }

    @Test
    void otherVersionsAndUnrelatedCallsAreUntouched() {
        ClassNode node = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "my/Other", "getTagList", "(Ljava/lang/String;)V"));
        assertEquals(0, ForwardCalls.apply(node, hierarchy(), LegacyVersion.V1_6_4));
        ClassNode same = with(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, P + "net/minecraft/nbt/NBTTagCompound", "getTagList", "(Ljava/lang/String;)V"));
        assertEquals(0, ForwardCalls.apply(same, new Hierarchy(ShimIndex.empty(), null, Map.of(), LegacyVersion.V1_7_10), LegacyVersion.V1_7_10));
        assertTrue(new InsnNode(Opcodes.NOP).getOpcode() == Opcodes.NOP);
    }
}
