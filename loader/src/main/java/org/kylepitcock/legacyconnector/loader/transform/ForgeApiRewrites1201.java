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
        return apply(node, table(), null);
    }

    /** {@code modEvents}: classes (of the jar) that extend NeoForge's Event: calls on them reach the cancel/result rules like calls on NeoForge's own events. */
    static int apply(ClassNode node, java.util.function.Predicate<String> modEvents) {
        return apply(node, table(), modEvents);
    }

    static int apply(ClassNode node, Map<String, String> rules) {
        return apply(node, rules, null);
    }

    static int apply(ClassNode node, Map<String, String> rules, java.util.function.Predicate<String> modEvents) {
        if (rules.isEmpty()) return 0;
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            AbstractInsnNode next;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = next) {
                next = in.getNext();
                if (in instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy && indy.bsmArgs != null) {
                    // a method reference (FluidStack::getTag) is a handle, not a call: an exact rule redirects it to the same helper (receiver first, as for an unbound reference)
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (!(indy.bsmArgs[i] instanceof org.objectweb.asm.Handle hd)) continue;
                        if (hd.getTag() != Opcodes.H_INVOKEVIRTUAL && hd.getTag() != Opcodes.H_INVOKEINTERFACE && hd.getTag() != Opcodes.H_INVOKESTATIC) continue;
                        String hr = rules.get(hd.getOwner() + "." + hd.getName() + hd.getDesc());
                        if (hr == null || hr.startsWith("TailClient.") || hr.startsWith("@")) continue;
                        String hOwner = hr.startsWith("Tail.") ? TAIL : helperClass(hr);
                        if (hr.startsWith("Tail.")) hr = hr.substring(5);
                        String hDesc = hd.getTag() == Opcodes.H_INVOKESTATIC ? hd.getDesc() : "(L" + hd.getOwner() + ";" + hd.getDesc().substring(1);
                        indy.bsmArgs[i] = new org.objectweb.asm.Handle(Opcodes.H_INVOKESTATIC, hOwner, hr, hDesc, false);
                        n++;
                    }
                    continue;
                }
                if (!(in instanceof MethodInsnNode call)) continue;
                // Forge's Lazy is an interface, NeoForge's a class: the call must name a Methodref, not an InterfaceMethodref
                if (call.itf && call.owner.equals("net/neoforged/neoforge/common/util/Lazy")) {
                    call.itf = false;
                    if (call.getOpcode() == Opcodes.INVOKEINTERFACE) call.setOpcode(Opcodes.INVOKEVIRTUAL);
                    n++;
                }
                String helper = rules.get(call.owner + "." + call.name + call.desc);
                boolean anyReceiver = false;
                if (helper == null) {
                    helper = rules.get("*." + call.name + call.desc);
                    anyReceiver = helper != null;
                }
                if (helper == null && call.getOpcode() == Opcodes.INVOKEVIRTUAL && !call.owner.equals("net/neoforged/bus/api/ICancellableEvent")
                        && (call.owner.startsWith("net/neoforged/") || call.owner.startsWith(FACADE_EVENTS) && call.owner.contains("Event")
                                || modEvents != null && modEvents.test(call.owner))) {
                    // NeoForge's events are only cancellable when they say so (ICancellableEvent): Forge's Event had the methods itself
                    helper = rules.get("@event." + call.name + call.desc);
                    anyReceiver = helper != null;
                }
                if (helper == null) continue;
                String helperOwner = null;
                if (helper.startsWith("TailClient.")) {   // the client-only part of the tail (client event classes must not be loaded by a dedicated server)
                    helperOwner = TAIL_CLIENT;
                    helper = helper.substring(11);
                } else if (helper.startsWith("Tail.")) {   // forge-call-rewrites.tsv value "Tail.name": the helper is in ForgeApiTail1201 (the long tail of small event/hook adapters)
                    helperOwner = TAIL;
                    helper = helper.substring(5);
                }
                if (call.getOpcode() == Opcodes.INVOKESPECIAL) {
                    // super.getCapability(...) of a legacy class whose parent is a Minecraft class has no method to call any more: the base behaviour is the helper's
                    if (call.name.equals("<init>") || !(call.owner.startsWith("net/minecraft/") || call.owner.startsWith("net/neoforged/"))) continue;
                    helper = "super" + Character.toUpperCase(helper.charAt(0)) + helper.substring(1);
                }
                String desc = call.desc;
                if (call.getOpcode() != Opcodes.INVOKESTATIC) {
                    Type[] args = Type.getArgumentTypes(call.desc);
                    Type[] withReceiver = new Type[args.length + 1];
                    withReceiver[0] = anyReceiver ? Type.getObjectType("java/lang/Object") : Type.getObjectType(call.owner);   // a "*" rule's helper takes any receiver
                    System.arraycopy(args, 0, withReceiver, 1, args.length);
                    desc = Type.getMethodDescriptor(Type.getReturnType(call.desc), withReceiver);
                }
                m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, helperOwner != null ? helperOwner : helperClass(helper), helper, desc, false));
                n++;
            }
        }
        return n;
    }

    static final String FACADE_EVENTS = "legacyconnector/v1_20_1/net/minecraftforge/";
    static final String CANCELABLE = "Lnet/minecraftforge/eventbus/api/Cancelable;";
    static final String CANCELLABLE_EVENT = "net/neoforged/bus/api/ICancellableEvent";

    /**
     * A mod event annotated {@code @Cancelable} (Forge) gets NeoForge's {@code ICancellableEvent}: the bus then honours {@code setCanceled}, which Forge's Event
     * had itself. Subclasses inherit the interface.
     */
    static boolean markCancelable(ClassNode node) {
        if (node.invisibleAnnotations == null && node.visibleAnnotations == null) return false;
        boolean annotated = false;
        for (var list : java.util.List.of(node.visibleAnnotations == null ? java.util.List.<org.objectweb.asm.tree.AnnotationNode>of() : node.visibleAnnotations,
                node.invisibleAnnotations == null ? java.util.List.<org.objectweb.asm.tree.AnnotationNode>of() : node.invisibleAnnotations)) {
            for (var a : list) if (CANCELABLE.equals(a.desc)) annotated = true;
        }
        if (!annotated || node.interfaces.contains(CANCELLABLE_EVENT)) return false;
        node.interfaces.add(CANCELLABLE_EVENT);
        return true;
    }

    /** Capability helpers live in their own class. */
    private static String helperClass(String helper) {
        return helper.toLowerCase().contains("getcapability") ? CAPS : HELPERS;
    }

    static final String TAIL = "legacyconnector/v1_20_1/ForgeApiTail1201";
    static final String TAIL_CLIENT = "legacyconnector/v1_20_1/ForgeApiTailClient1201";
    static final String CAPS = "legacyconnector/v1_20_1/Caps1201";
    static final String PROVIDER = "legacyconnector/v1_20_1/net/minecraftforge/common/capabilities/ICapabilityProvider";
    static final String GET_CAPABILITY_DESC = "(Llegacyconnector/v1_20_1/net/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)"
            + "Llegacyconnector/v1_20_1/net/minecraftforge/common/util/LazyOptional;";

    /**
     * A class that declares Forge's {@code getCapability(cap, side)} is a capability provider: it gets the facade interface so calls can reach it
     * (the method no longer overrides anything in NeoForge's block entity / entity / item classes).
     */
    static boolean markProviders(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) != 0) return false;
        boolean marked = false;
        for (MethodNode m : node.methods) {
            // Item.initCapabilities(stack, nbt) no longer overrides anything: the facade interface lets Caps1201 call it
            if (m.name.equals("initCapabilities") && m.desc.equals(INIT_CAPABILITIES_DESC) && (m.access & Opcodes.ACC_STATIC) == 0
                    && !node.interfaces.contains(INIT_CAPABILITIES)) {
                node.interfaces.add(INIT_CAPABILITIES);
                marked = true;
            }
        }
        if (node.interfaces.contains(PROVIDER)) return marked;
        for (MethodNode m : node.methods) {
            if (m.name.equals("getCapability") && m.desc.equals(GET_CAPABILITY_DESC) && (m.access & Opcodes.ACC_STATIC) == 0) {
                node.interfaces.add(PROVIDER);
                return true;
            }
        }
        return marked;
    }

    static final String INIT_CAPABILITIES = "legacyconnector/v1_20_1/Caps1201$InitCapabilities1201";
    static final String INIT_CAPABILITIES_DESC = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/nbt/CompoundTag;)L" + PROVIDER + ";";
}
