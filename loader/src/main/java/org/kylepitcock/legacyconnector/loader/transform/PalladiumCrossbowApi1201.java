package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adapts Palladium's concrete AddonCrossbowItem call sites and its client
 * crossbow charge predicate to the Minecraft 1.21.1 component/instance API.
 */
final class PalladiumCrossbowApi1201 implements Opcodes {
    static final String ADDON_CROSSBOW = "net/threetag/palladium/item/AddonCrossbowItem";
    static final String PALLADIUM_CLIENT = "net/threetag/palladium/PalladiumClient";
    static final String CROSSBOW = "net/minecraft/world/item/CrossbowItem";
    static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    static final String LEVEL = "net/minecraft/world/level/Level";
    static final String INTERACTION_HAND = "net/minecraft/world/InteractionHand";
    static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/PalladiumCrossbowCompat";

    private static final String POWER_DESC = "(L" + ITEM_STACK + ";)F";
    private static final String OLD_PERFORM_DESC = "(L" + LEVEL + ";L" + LIVING_ENTITY
            + ";L" + INTERACTION_HAND + ";L" + ITEM_STACK + ";FF)V";
    private static final String NEW_PERFORM_DESC = "(L" + CROSSBOW + ";L" + LEVEL + ";L" + LIVING_ENTITY
            + ";L" + INTERACTION_HAND + ";L" + ITEM_STACK + ";FF)V";
    private static final String CHARGED_DESC = "(L" + ITEM_STACK + ";Z)V";
    private static final String OLD_CHARGE_DURATION_DESC = "(L" + ITEM_STACK + ";)I";
    private static final String NEW_CHARGE_DURATION_DESC = "(L" + ITEM_STACK + ";L" + LIVING_ENTITY + ";)I";

    private PalladiumCrossbowApi1201() {}

    /** Returns the number of call sites adapted in this class. */
    static int apply(ClassNode node) {
        return switch (node.name) {
            case ADDON_CROSSBOW -> adaptAddonCrossbowItem(node);
            case PALLADIUM_CLIENT -> adaptClientChargePredicate(node);
            default -> 0;
        };
    }

    private static int adaptAddonCrossbowItem(ClassNode node) {
        int changes = 0;
        boolean shootingPowerFound = false;
        boolean performShootingFound = false;
        boolean setChargedFound = false;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals(CROSSBOW)) continue;

                if (call.name.equals("getShootingPower") && call.desc.equals(POWER_DESC)
                        && call.getOpcode() == INVOKESTATIC) {
                    call.owner = HELPER;
                    call.name = "getShootingPower";
                    changes++;
                    shootingPowerFound = true;
                } else if (call.name.equals("performShooting") && call.desc.equals(OLD_PERFORM_DESC)
                        && call.getOpcode() == INVOKESTATIC) {
                    rewritePerformShooting(method, call);
                    changes++;
                    performShootingFound = true;
                } else if (call.name.equals("setCharged") && call.desc.equals(CHARGED_DESC)
                        && call.getOpcode() == INVOKESTATIC) {
                    // 1.21.1 CrossbowItem.performShooting clears CHARGED_PROJECTILES
                    // itself before dispatching the shot. The legacy follow-up
                    // setCharged(stack, false) is therefore redundant.
                    InsnList pops = new InsnList();
                    pops.add(new InsnNode(POP)); // boolean
                    pops.add(new InsnNode(POP)); // ItemStack
                    method.instructions.insertBefore(call, pops);
                    method.instructions.remove(call);
                    changes++;
                    setChargedFound = true;
                }
            }
        }

        // The configured multiplier and inaccuracy must both remain on the
        // AddonCrossbowItem firing path; no behavior is silently discarded.
        if (performShootingFound && !shootingPowerFound) {
            throw new IllegalStateException("Palladium AddonCrossbowItem lost the inspected getShootingPower(ItemStack) call");
        }
        if (shootingPowerFound != performShootingFound) {
            throw new IllegalStateException("Palladium AddonCrossbowItem has only one of its paired shooting-power/firing calls");
        }
        // setCharged is expected beside the legacy static firing call; it is
        // intentionally removed only when that path was successfully migrated.
        if (setChargedFound && !performShootingFound) {
            throw new IllegalStateException("Palladium AddonCrossbowItem.setCharged was found without its firing path");
        }
        return changes;
    }

    private static void rewritePerformShooting(MethodNode method, MethodInsnNode call) {
        Type[] arguments = Type.getArgumentTypes(call.desc);
        if (arguments.length != 6 || arguments[0].getSort() != Type.OBJECT
                || !arguments[0].getInternalName().equals(LEVEL)
                || !arguments[1].getInternalName().equals(LIVING_ENTITY)
                || !arguments[2].getInternalName().equals(INTERACTION_HAND)
                || !arguments[3].getInternalName().equals(ITEM_STACK)
                || arguments[4].getSort() != Type.FLOAT || arguments[5].getSort() != Type.FLOAT) {
            throw new IllegalStateException("Unexpected Palladium CrossbowItem.performShooting descriptor " + call.desc);
        }

        int[] locals = new int[arguments.length];
        int next = Math.max(method.maxLocals, minimumLocals(method));
        for (int i = 0; i < arguments.length; i++) {
            locals[i] = next;
            next += arguments[i].getSize();
        }

        InsnList code = new InsnList();
        for (int i = arguments.length - 1; i >= 0; i--) {
            code.add(new VarInsnNode(arguments[i].getOpcode(ISTORE), locals[i]));
        }
        // New API is an instance method on CrossbowItem with a nullable target.
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ALOAD, locals[0]));
        code.add(new VarInsnNode(ALOAD, locals[1]));
        code.add(new VarInsnNode(ALOAD, locals[2]));
        code.add(new VarInsnNode(ALOAD, locals[3]));
        code.add(new VarInsnNode(FLOAD, locals[4]));
        code.add(new VarInsnNode(FLOAD, locals[5]));
        code.add(new InsnNode(ACONST_NULL));

        method.instructions.insertBefore(call, code);
        call.setOpcode(INVOKESTATIC);
        call.owner = HELPER;
        call.name = "performShooting";
        call.desc = NEW_PERFORM_DESC;
        call.itf = false;
        method.maxLocals = next;
        method.maxStack = Math.max(method.maxStack, 7);
    }

    private static int minimumLocals(MethodNode method) {
        int required = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
        return Math.max(required, method.maxLocals);
    }

    private static int adaptClientChargePredicate(ClassNode node) {
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            // This exact lambda receives (ItemStack, ClientLevel, LivingEntity, int);
            // the old static getChargeDuration(stack) now also requires its user.
            if (!method.name.equals("lambda$itemModelPredicates$15")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != INVOKESTATIC
                        || !call.owner.equals(CROSSBOW)
                        || !call.name.equals("getChargeDuration")
                        || !call.desc.equals(OLD_CHARGE_DURATION_DESC)) continue;

                method.instructions.insertBefore(call, new VarInsnNode(ALOAD, 2));
                call.desc = NEW_CHARGE_DURATION_DESC;
                changes++;
            }
        }
        return changes;
    }
}
