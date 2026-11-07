package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Forge 1.20.1 methods that NeoForge 1.21.1's classes lack, redirected to static helpers in {@code legacyconnector/v1_20_1/ForgeApi1201}
 * (table: {@code forward/1.20.1/forge-call-rewrites.tsv}, lines {@code owner.name(desc)ret<TAB>helper}, names as they are after the class renames).
 * An instance call gets its receiver as the first helper argument. Minecraft signature changes are {@link CallRewrites1201}'s.
 */
final class ForgeApiRewrites1201 {
    static final String HELPERS = "legacyconnector/v1_20_1/ForgeApi1201";
    static final String RESOURCE = "legacyconnector/forward/1.20.1/forge-call-rewrites.tsv";

    private static volatile Map<String, String> table;

    private ForgeApiRewrites1201() {}

    static synchronized Map<String, String> table() {
        if (table != null) return table;
        Map<String, String> map = new HashMap<>();
        try (InputStream in = ForgeApiRewrites1201.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) {
                for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (l.isBlank() || l.startsWith("#")) continue;
                    String[] a = l.strip().split("\t");
                    if (a.length == 2) map.put(a[0], a[1]);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return table = map;
    }

    /** Rewrites the calls of one class; returns how many. */
    static int apply(ClassNode node) {
        return apply(node, table());
    }

    static int apply(ClassNode node, Map<String, String> rules) {
        if (rules.isEmpty()) return 0;
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            AbstractInsnNode next;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = next) {
                next = in.getNext();
                if (!(in instanceof MethodInsnNode call)) continue;
                String helper = rules.get(call.owner + "." + call.name + call.desc);
                if (helper == null) helper = rules.get("*." + call.name + call.desc);
                if (helper == null) continue;
                if (call.getOpcode() == Opcodes.INVOKESPECIAL) {
                    // super.getCapability(...) of a legacy class whose parent is a Minecraft class has no method to call any more: the base behaviour is the helper's
                    if (call.name.equals("<init>") || !(call.owner.startsWith("net/minecraft/") || call.owner.startsWith("net/neoforged/"))) continue;
                    helper = "super" + Character.toUpperCase(helper.charAt(0)) + helper.substring(1);
                }
                String desc = call.desc;
                if (call.getOpcode() != Opcodes.INVOKESTATIC) {
                    Type[] args = Type.getArgumentTypes(call.desc);
                    Type[] withReceiver = new Type[args.length + 1];
                    withReceiver[0] = Type.getObjectType(call.owner);
                    System.arraycopy(args, 0, withReceiver, 1, args.length);
                    desc = Type.getMethodDescriptor(Type.getReturnType(call.desc), withReceiver);
                }
                m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, helperClass(helper), helper, desc, false));
                n++;
            }
        }
        return n;
    }

    /** Capability helpers live in their own class. */
    private static String helperClass(String helper) {
        return helper.toLowerCase().contains("getcapability") ? CAPS : HELPERS;
    }

    static final String CAPS = "legacyconnector/v1_20_1/Caps1201";
    static final String PROVIDER = "legacyconnector/v1_20_1/net/minecraftforge/common/capabilities/ICapabilityProvider";
    static final String GET_CAPABILITY_DESC = "(Llegacyconnector/v1_20_1/net/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)"
            + "Llegacyconnector/v1_20_1/net/minecraftforge/common/util/LazyOptional;";

    /**
     * A class that declares Forge's {@code getCapability(cap, side)} is a capability provider: it gets the facade interface so calls can reach it
     * (the method no longer overrides anything in NeoForge's block entity / entity / item classes).
     */
    static boolean markProviders(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) != 0 || node.interfaces.contains(PROVIDER)) return false;
        for (MethodNode m : node.methods) {
            if (m.name.equals("getCapability") && m.desc.equals(GET_CAPABILITY_DESC) && (m.access & Opcodes.ACC_STATIC) == 0) {
                node.interfaces.add(PROVIDER);
                return true;
            }
        }
        return false;
    }
}
