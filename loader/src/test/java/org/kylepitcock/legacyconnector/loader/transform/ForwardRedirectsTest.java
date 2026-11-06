package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class ForwardRedirectsTest {
    private static final String CHAT = "(Llegacyconnector/v1_6_4/net/minecraft/util/ChatMessageComponent;)V";

    private static ClassNode withCall(int opcode, String owner, String name, String desc) {
        ClassNode node = new ClassNode();
        node.name = "mod/Foo";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        m.instructions.add(new MethodInsnNode(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE));
        node.methods.add(m);
        return node;
    }

    private static MethodInsnNode call(ClassNode node) {
        return (MethodInsnNode) node.methods.get(0).instructions.getFirst();
    }

    @Test
    void virtualCallsOnShimTypesBecomeStaticAdapterCalls() {
        ClassNode node = withCall(Opcodes.INVOKEVIRTUAL, "legacyconnector/v1_7_10/net/minecraft/entity/player/EntityPlayer", "sendChatToPlayer", CHAT);
        assertEquals(1, ForwardRedirects.apply(node, LegacyVersion.V1_6_4));
        MethodInsnNode m = call(node);
        assertEquals(Opcodes.INVOKESTATIC, m.getOpcode());
        assertEquals("org/kylepitcock/legacyconnector/v1_6_4/bridge/Redirects164", m.owner);
        assertEquals("(Ljava/lang/Object;Llegacyconnector/v1_6_4/net/minecraft/util/ChatMessageComponent;)V", m.desc);
        assertFalse(m.itf);
    }

    @Test
    void interfaceCallsAreRedirectedToo() {
        ClassNode node = withCall(Opcodes.INVOKEINTERFACE, "legacyconnector/v1_7_10/net/minecraft/command/ICommandSender", "sendChatToPlayer", CHAT);
        assertEquals(1, ForwardRedirects.apply(node, LegacyVersion.V1_6_4));
        assertEquals(Opcodes.INVOKESTATIC, call(node).getOpcode());
    }

    @Test
    void aModsOwnMethodOfTheSameNameIsLeftAlone() {
        ClassNode node = withCall(Opcodes.INVOKEVIRTUAL, "mod/MyPlayer", "sendChatToPlayer", CHAT);
        assertEquals(0, ForwardRedirects.apply(node, LegacyVersion.V1_6_4));
        assertEquals(Opcodes.INVOKEVIRTUAL, call(node).getOpcode());
    }

    @Test
    void staticRulesMatchTheOwnerAndKeepTheDescriptor() {
        String desc = "(Llegacyconnector/v1_6_4/cpw/mods/fml/common/IPlayerTracker;)V";
        ClassNode node = withCall(Opcodes.INVOKESTATIC, "legacyconnector/v1_7_10/cpw/mods/fml/common/registry/GameRegistry", "registerPlayerTracker", desc);
        assertEquals(1, ForwardRedirects.apply(node, LegacyVersion.V1_6_4));
        MethodInsnNode m = call(node);
        assertEquals("org/kylepitcock/legacyconnector/v1_6_4/bridge/Players164", m.owner);
        assertEquals("registerPlayerTracker", m.name);
        assertEquals(desc, m.desc);
        ClassNode other = withCall(Opcodes.INVOKESTATIC, "legacyconnector/v1_7_10/cpw/mods/fml/common/registry/EntityRegistry", "registerPlayerTracker", desc);
        assertEquals(0, ForwardRedirects.apply(other, LegacyVersion.V1_6_4));
    }

    @Test
    void versionsWithoutAForwardAreUntouched() {
        ClassNode node = withCall(Opcodes.INVOKEVIRTUAL, "legacyconnector/v1_7_10/net/minecraft/entity/player/EntityPlayer", "sendChatToPlayer", CHAT);
        assertEquals(0, ForwardRedirects.apply(node, LegacyVersion.V1_7_10));
        assertTrue(ForwardRedirects.rules(LegacyVersion.V1_6_4).size() >= 3);
    }
}
