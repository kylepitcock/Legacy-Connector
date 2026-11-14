package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Round four of the 1.20.1 link gaps: Balm and Resourceful Lib networking, the MeleeAttackGoal and ItemPredicate stand-ins. */
class Round4Libs1201Test implements Opcodes {
    private static final String BALM_NET = "net/blay09/mods/balm/api/network/BalmNetworking";
    private static final String MIGRATE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";
    private static final String PRED = "net/minecraft/advancements/critereon/ItemPredicate";

    private static ClassNode cls(String name, String sup) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = sup;
        return n;
    }

    @Test
    void balmSendGoesToTheLegacyChannelWithTheReceiverFirst() {
        ClassNode n = cls("mod/Net", "java/lang/Object");
        String desc = "(Lnet/minecraft/world/entity/player/Player;Ljava/lang/Object;)V";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "go", "(L" + BALM_NET + ";Lnet/minecraft/world/entity/player/Player;Ljava/lang/Object;)V", null, null);
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(ALOAD, 1));
        m.instructions.add(new VarInsnNode(ALOAD, 2));
        m.instructions.add(new MethodInsnNode(INVOKEINTERFACE, BALM_NET, "sendTo", desc, true));
        m.instructions.add(new InsnNode(RETURN));
        m.maxStack = 3;
        m.maxLocals = 3;
        n.methods.add(m);
        Mc1201Migration.apply(n, new Mc1201Hierarchy());
        MethodInsnNode call = null;
        for (var in : m.instructions) if (in instanceof MethodInsnNode c) call = c;
        assertNotNull(call);
        assertEquals(MIGRATE + "BalmNetworkCompat", call.owner);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals("(Ljava/lang/Object;Lnet/minecraft/world/entity/player/Player;Ljava/lang/Object;)V", call.desc);
    }

    @Test
    void balmInitializeTakesTheModLoadContextFromTheHelper() {
        Mc1201Tables t = Mc1201Tables.load();
        assertNotNull(t.calls.get("net/blay09/mods/balm/api/Balm.initialize(Ljava/lang/String;Ljava/lang/Runnable;)V"));
        assertNotNull(t.calls.get("net/blay09/mods/balm/api/client/BalmClient.initialize(Ljava/lang/String;Ljava/lang/Runnable;)V"));
    }

    @Test
    void resourcefulLibPacketClassesMapToTheStandIns() {
        Mc1201Tables t = Mc1201Tables.load();
        String old = "com/teamresourceful/resourcefullib/common/networking/";
        assertEquals(MIGRATE + "LegacyRlNetwork$LegacyRlChannel", t.classes.get(old + "NetworkChannel"));
        assertEquals(MIGRATE + "LegacyRlNetwork$LegacyRlPacket", t.classes.get(old + "base/Packet"));
        assertEquals(MIGRATE + "LegacyRlNetwork$LegacyRlPacketHandler", t.classes.get(old + "base/PacketHandler"));
        assertEquals(MIGRATE + "LegacyRlNetwork$LegacyRlPacketContext", t.classes.get(old + "base/PacketContext"));
        assertEquals(MIGRATE + "LegacyRlNetwork$LegacyRlDirection", t.classes.get(old + "base/NetworkDirection"));
    }

    @Test
    void meleeAttackGoalIsTheStandIn() {
        assertEquals(MIGRATE + "LegacyMeleeAttackGoal", Mc1201Tables.load().classes.get("net/minecraft/world/entity/ai/goal/MeleeAttackGoal"));
    }

    @Test
    void botariumIsNotListedAsNativeBecauseNoBuildExists() throws IOException {
        try (InputStream in = Round4Libs1201Test.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/libraries.tsv")) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(text.lines().anyMatch(l -> l.startsWith("botarium\t")));
            assertTrue(text.lines().anyMatch(l -> l.startsWith("balm\t")));
        }
    }

    @Test
    void itemPredicateSubclassGetsAGeneratedBase() {
        ClassNode sub = cls("mod/pred/CustomPredicate", PRED);
        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, PRED, "<init>", "()V", false));
        init.instructions.add(new InsnNode(RETURN));
        init.maxStack = 1;
        init.maxLocals = 1;
        sub.methods.add(init);
        Mc1201Migration.Counts c = Mc1201Migration.apply(sub, new Mc1201Hierarchy());
        assertEquals("mod/pred/$LegacyItemPredicate", sub.superName);
        assertEquals("mod/pred/$LegacyItemPredicate", ((MethodInsnNode) init.instructions.get(1)).owner, "the super constructor call follows the new superclass");
        assertEquals(java.util.Set.of("mod/pred/$LegacyItemPredicate"), c.itemPredicateStandIns);

        ClassNode base = new ClassNode();
        new ClassReader(ItemPredicateStandIn1201.generate("mod/pred/$LegacyItemPredicate")).accept(base, 0);
        assertEquals(PRED, base.superName);
        assertTrue(base.methods.stream().anyMatch(m -> m.name.equals("matches") && m.desc.equals("(Lnet/minecraft/world/item/ItemStack;)Z")));
        assertTrue(base.methods.stream().anyMatch(m -> m.name.equals("test") && m.desc.equals("(Lnet/minecraft/world/item/ItemStack;)Z")));
        assertTrue(base.methods.stream().anyMatch(m -> m.name.equals("serializeToJson")));
    }
}
