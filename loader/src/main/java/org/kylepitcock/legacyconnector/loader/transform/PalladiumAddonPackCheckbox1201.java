package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayDeque;
import java.util.Deque;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Replaces AddonPackLogScreen's legacy CheckboxButton subclass with native
 * 1.21.1 Checkbox builder instances. The old nested class is not needed once
 * all three construction sites have been redirected and is omitted entirely.
 */
final class PalladiumAddonPackCheckbox1201 implements Opcodes {
    static final String SCREEN = "net/threetag/palladium/client/screen/AddonPackLogScreen";
    static final String LEGACY_CHECKBOX = SCREEN + "$CheckboxButton";
    static final String DEV_LOG_LIST = SCREEN + "$DevLogList";
    static final String COMPONENT = "net/minecraft/network/chat/Component";
    static final String CHECKBOX = "net/minecraft/client/gui/components/Checkbox";
    static final String HELPER = "legacyconnector/v1_20_1/PalladiumAddonPackCheckboxCompat";
    static final String REFRESH_BRIDGE = "legacyconnector$refreshAddonPackLogList";

    private static final String LEGACY_CTOR =
            "(L" + SCREEN + ";IIIIL" + COMPONENT + ";ZLjava/util/function/Consumer;)V";
    private static final String FACTORY_DESC =
            "(Lnet/minecraft/client/gui/screens/Screen;IIIIL" + COMPONENT
                    + ";ZLjava/util/function/Consumer;Ljava/lang/Runnable;)L" + CHECKBOX + ";";
    private static final String LAMBDA_METAFACTORY_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                    + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

    private PalladiumAddonPackCheckbox1201() {}

    static boolean dropLegacyClass(String internalName) {
        return LEGACY_CHECKBOX.equals(internalName);
    }

    /** Returns rewritten constructor sites plus the one synthetic screen bridge. */
    static int apply(ClassNode node) {
        if (!SCREEN.equals(node.name)) return 0;

        int legacyCalls = 0;
        int helperCalls = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals(LEGACY_CHECKBOX) && call.name.equals("<init>")) {
                    if (call.getOpcode() != INVOKESPECIAL || !call.desc.equals(LEGACY_CTOR)) {
                        throw new IllegalStateException("Palladium AddonPackLogScreen has an unexpected CheckboxButton constructor: "
                                + call.desc);
                    }
                    legacyCalls++;
                } else if (call.owner.equals(HELPER) && call.name.equals("create")) {
                    if (call.getOpcode() != INVOKESTATIC || !call.desc.equals(FACTORY_DESC)) {
                        throw new IllegalStateException("Palladium AddonPackLogScreen has an unexpected adapted Checkbox factory call: "
                                + call.desc);
                    }
                    helperCalls++;
                }
            }
        }

        if (legacyCalls == 0 && helperCalls == 3) {
            ensureRefreshBridge(node);
            return 0;
        }
        if (legacyCalls != 3 || helperCalls != 0) {
            throw new IllegalStateException("Palladium AddonPackLogScreen expected three legacy CheckboxButton constructors; found "
                    + legacyCalls + " legacy and " + helperCalls + " adapted");
        }

        int rewrites = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            Deque<TypeInsnNode> allocations = new ArrayDeque<>();
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof TypeInsnNode type && type.getOpcode() == NEW
                        && LEGACY_CHECKBOX.equals(type.desc)) {
                    allocations.push(type);
                    continue;
                }
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != INVOKESPECIAL
                        || !LEGACY_CHECKBOX.equals(call.owner)
                        || !call.name.equals("<init>")) continue;

                if (!call.desc.equals(LEGACY_CTOR) || allocations.isEmpty()) {
                    throw new IllegalStateException("Palladium AddonPackLogScreen CheckboxButton allocation/constructor pair changed");
                }
                TypeInsnNode allocation = allocations.pop();
                AbstractInsnNode duplicate = nextRealInstruction(allocation);
                if (duplicate == null || duplicate.getOpcode() != DUP) {
                    throw new IllegalStateException("Palladium AddonPackLogScreen CheckboxButton allocation is missing its DUP");
                }

                if ((method.access & ACC_STATIC) != 0) {
                    throw new IllegalStateException("Palladium AddonPackLogScreen Checkbox construction unexpectedly moved into a static method");
                }
                // The builder callback also needs the old outer-screen list refresh.
                // Capture this in a Runnable backed by the synthetic bridge added below.
                InsnList refresh = new InsnList();
                refresh.add(new org.objectweb.asm.tree.VarInsnNode(ALOAD, 0));
                refresh.add(new org.objectweb.asm.tree.InvokeDynamicInsnNode(
                        "run", "(L" + node.name + ";)Ljava/lang/Runnable;",
                        new org.objectweb.asm.Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory",
                                "metafactory", LAMBDA_METAFACTORY_DESC, false),
                        org.objectweb.asm.Type.getMethodType("()V"),
                        new org.objectweb.asm.Handle(H_INVOKEVIRTUAL, node.name, REFRESH_BRIDGE, "()V", false),
                        org.objectweb.asm.Type.getMethodType("()V")));
                method.instructions.insertBefore(call, refresh);

                // Replace the uninitialized allocation pair with a static factory
                // consuming the original arguments plus the captured refresh callback.
                method.instructions.remove(duplicate);
                method.instructions.remove(allocation);
                method.instructions.set(call, new MethodInsnNode(INVOKESTATIC,
                        HELPER, "create", FACTORY_DESC, false));
                rewrites++;
            }
            if (!allocations.isEmpty()) {
                throw new IllegalStateException("Palladium AddonPackLogScreen has an unmatched CheckboxButton allocation");
            }
        }
        if (rewrites != 3) {
            throw new IllegalStateException("Palladium AddonPackLogScreen expected three CheckboxButton allocation rewrites; changed "
                    + rewrites);
        }
        ensureRefreshBridge(node);
        return rewrites + 1;
    }

    private static AbstractInsnNode nextRealInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }

    private static void ensureRefreshBridge(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(REFRESH_BRIDGE) && method.desc.equals("()V")) return;
        }
        boolean hasList = false;
        for (FieldNode field : node.fields) {
            if (field.name.equals("list") && field.desc.equals("L" + DEV_LOG_LIST + ";")) {
                hasList = true;
                break;
            }
        }
        if (!hasList) {
            throw new IllegalStateException("Palladium AddonPackLogScreen list field was not found for Checkbox callback refresh");
        }

        MethodNode bridge = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, REFRESH_BRIDGE, "()V", null, null);
        bridge.instructions.add(new org.objectweb.asm.tree.VarInsnNode(ALOAD, 0));
        bridge.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(GETFIELD, node.name,
                "list", "L" + DEV_LOG_LIST + ";"));
        bridge.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, DEV_LOG_LIST, "refreshList", "()V", false));
        bridge.instructions.add(new InsnNode(RETURN));
        bridge.maxLocals = 1;
        bridge.maxStack = 1;
        node.methods.add(bridge);
    }
}
