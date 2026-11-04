package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Signature changes between Forge 1.20.1 / Minecraft 1.20.1 and NeoForge 1.21.1 that a rename cannot express, rewritten at the call site
 * (after the names are already Mojang/NeoForge):
 * <ul>
 *   <li>constructors that became factories ({@code new ResourceLocation("a:b")} to {@code ResourceLocation.parse}): {@code NEW, DUP, args, INVOKESPECIAL <init>}
 *       becomes {@code args, INVOKESTATIC factory};</li>
 *   <li>instance calls that need other semantics ({@code IEventBus.post} returned whether the event was cancelled) to a static helper in
 *       {@link #HELPERS} taking the receiver first.</li>
 * </ul>
 */
final class CallRewrites1201 {
    static final String HELPERS = "legacyconnector/v1_20_1/Compat1201";

    /** owner.&lt;init&gt;desc to {owner, factory name, factory descriptor}. */
    private static final Map<String, String[]> FACTORIES = Map.of(
            "net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;)V",
            new String[] {"net/minecraft/resources/ResourceLocation", "parse", "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"},
            "net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;Ljava/lang/String;)V",
            new String[] {"net/minecraft/resources/ResourceLocation", "fromNamespaceAndPath",
                    "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"});

    /** owner.name+desc of an instance call to {helper name, helper descriptor (receiver first)}. */
    private static final Map<String, String[]> STATICS = Map.of(
            "net/neoforged/bus/api/IEventBus.post(Lnet/neoforged/bus/api/Event;)Z",
            new String[] {"post", "(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/bus/api/Event;)Z"});

    private CallRewrites1201() {}

    static int apply(ClassNode node) {
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            Deque<TypeInsnNode> news = new ArrayDeque<>();
            AbstractInsnNode next;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = next) {
                // InsnList.set/remove clear the replaced node's links: take the successor first
                next = in.getNext();
                if (in instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW) {
                    news.push(t);
                } else if (in instanceof MethodInsnNode call) {
                    if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")) {
                        TypeInsnNode made = news.isEmpty() ? null : news.peek();
                        if (made != null && made.desc.equals(call.owner)) {
                            news.pop();
                            String[] f = FACTORIES.get(call.owner + ".<init>" + call.desc);
                            if (f != null && made.getNext() != null && made.getNext().getOpcode() == Opcodes.DUP) {
                                m.instructions.remove(made.getNext());
                                m.instructions.remove(made);
                                m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, f[0], f[1], f[2], false));
                                n++;
                            }
                        }
                    } else {
                        String[] h = STATICS.get(call.owner + "." + call.name + call.desc);
                        if (h != null && call.getOpcode() != Opcodes.INVOKESTATIC) {
                            m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPERS, h[0], h[1], false));
                            n++;
                        }
                    }
                }
            }
        }
        return n;
    }
}
