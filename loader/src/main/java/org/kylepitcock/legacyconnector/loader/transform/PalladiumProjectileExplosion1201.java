package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Palladium 4.5.10 creates an Explosion, then builds the old six-argument
 * ClientboundExplodePacket from its block list and per-player knockback.
 * Minecraft 1.21.1 added block interaction, particle and sound metadata to
 * that packet. Re-route the constructor to a helper that reads those exact
 * values from Palladium's existing Explosion object.
 */
final class PalladiumProjectileExplosion1201 implements Opcodes {
    static final String CUSTOM_PROJECTILE = "net/threetag/palladium/entity/CustomProjectile";
    static final String EXPLOSION = "net/minecraft/world/level/Explosion";
    static final String PACKET = "net/minecraft/network/protocol/game/ClientboundExplodePacket";
    static final String EXPLODE_METHOD_DESC =
            "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/Explosion;";
    static final String OLD_CONSTRUCTOR_DESC = "(DDDFLjava/util/List;Lnet/minecraft/world/phys/Vec3;)V";
    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/PalladiumProjectileCompat";
    static final String HELPER_DESC =
            "(DDDFLjava/util/List;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Explosion;)Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;";
    private static final String HELPER_NAME = "createExplodePacket";
    private static final String EXPLOSION_DESC = "L" + EXPLOSION + ";";

    private PalladiumProjectileExplosion1201() {}

    static int apply(ClassNode node) {
        if (!CUSTOM_PROJECTILE.equals(node.name)) return 0;

        MethodNode target = null;
        int targetMethods = 0;
        for (MethodNode method : node.methods) {
            if ("explode".equals(method.name) && EXPLODE_METHOD_DESC.equals(method.desc)) {
                target = method;
                targetMethods++;
            }
        }
        if (targetMethods != 1 || target == null) {
            throw new IllegalStateException(
                    "Palladium CustomProjectile explosion method shape changed: expected one explode method, found "
                            + targetMethods);
        }

        int legacySites = 0;
        int adaptedSites = 0;
        for (AbstractInsnNode instruction = target.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.getOpcode() == INVOKESPECIAL
                    && PACKET.equals(call.owner)
                    && "<init>".equals(call.name)
                    && OLD_CONSTRUCTOR_DESC.equals(call.desc)) {
                legacySites++;
                int explosionSlot = explosionLocalSlot(target, call);
                TypeInsnNode allocation = matchingNew(target, call);
                if (allocation == null) {
                    throw new IllegalStateException(
                            "Palladium CustomProjectile explosion packet constructor has no matching NEW instruction");
                }
                AbstractInsnNode duplicate = nextOpcode(allocation);
                if (duplicate == null || duplicate.getOpcode() != DUP) {
                    throw new IllegalStateException(
                            "Palladium CustomProjectile explosion packet NEW is not followed by DUP");
                }

                target.instructions.remove(duplicate);
                target.instructions.remove(allocation);
                target.instructions.insertBefore(call, new VarInsnNode(ALOAD, explosionSlot));
                target.instructions.set(call,
                        new MethodInsnNode(INVOKESTATIC, HELPER, HELPER_NAME, HELPER_DESC, false));
                target.maxStack++;
            } else if (call.getOpcode() == INVOKESTATIC
                    && HELPER.equals(call.owner)
                    && HELPER_NAME.equals(call.name)
                    && HELPER_DESC.equals(call.desc)) {
                adaptedSites++;
            }
        }

        if (legacySites + adaptedSites != 1) {
            throw new IllegalStateException(
                    "Palladium CustomProjectile explosion packet site count changed: expected one legacy or adapted call, found "
                            + (legacySites + adaptedSites));
        }
        return legacySites;
    }

    private static int explosionLocalSlot(MethodNode method, MethodInsnNode packetCall) {
        if (method.localVariables == null) {
            throw new IllegalStateException(
                    "Palladium CustomProjectile explosion method has no local-variable table for its Explosion");
        }
        int callIndex = method.instructions.indexOf(packetCall);
        int slot = -1;
        int matches = 0;
        for (var local : method.localVariables) {
            if (!"explosion".equals(local.name) || !EXPLOSION_DESC.equals(local.desc)) continue;
            int start = method.instructions.indexOf(local.start);
            int end = method.instructions.indexOf(local.end);
            if (start <= callIndex && callIndex < end) {
                slot = local.index;
                matches++;
            }
        }
        if (matches != 1 || slot < 0) {
            throw new IllegalStateException(
                    "Palladium CustomProjectile expected one live Explosion local at the packet call, found "
                            + matches);
        }
        return slot;
    }

    private static TypeInsnNode matchingNew(MethodNode method, MethodInsnNode constructor) {
        int nestedConstructors = 0;
        for (AbstractInsnNode instruction = constructor.getPrevious();
                instruction != null; instruction = instruction.getPrevious()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESPECIAL
                    && "<init>".equals(call.name)) {
                nestedConstructors++;
            } else if (instruction.getOpcode() == NEW) {
                if (nestedConstructors == 0) {
                    TypeInsnNode allocation = (TypeInsnNode) instruction;
                    return PACKET.equals(allocation.desc) ? allocation : null;
                }
                nestedConstructors--;
            }
        }
        return null;
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }
}
