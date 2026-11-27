package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Carries the real bounty mob into AlienEvo's terrain sampler so its removed
 * 1.20.1 static path-type query can use the 1.21.1 Mob-aware API.
 */
final class AlienEvoBountyPathfinding1201 implements Opcodes {
    static final String TERRAIN = "net/habb/alienevo/entity/mob/bounty/BountyTerrain";
    static final String PLANNER = "net/habb/alienevo/entity/mob/bounty/BountyRoutePlanner";
    static final String MOB = "net/minecraft/world/entity/Mob";
    static final String LEVEL = "net/minecraft/world/level/Level";
    static final String MUTABLE_POS = "net/minecraft/core/BlockPos$MutableBlockPos";
    static final String BLOCK_POS = "net/minecraft/core/BlockPos";
    static final String WALK_NODE_EVALUATOR = "net/minecraft/world/level/pathfinder/WalkNodeEvaluator";
    static final String PATH_TYPE = "net/minecraft/world/level/pathfinder/PathType";
    static final String FLOOR = "L" + TERRAIN + "$Floor;";

    static final String FLOOR_DESC = "(L" + LEVEL + ";DDDIID)" + FLOOR;
    static final String FLOOR_WATER_DESC = "(L" + LEVEL + ";DDDIIDZ)" + FLOOR;
    static final String STANDABLE_DESC = "(L" + LEVEL + ";L" + MUTABLE_POS + ";DDDZ)" + FLOOR;
    static final String FLOOR_WITH_MOB_DESC = "(L" + LEVEL + ";DDDIIDL" + MOB + ";)" + FLOOR;
    static final String FLOOR_WATER_WITH_MOB_DESC = "(L" + LEVEL + ";DDDIIDZL" + MOB + ";)" + FLOOR;
    static final String STANDABLE_WITH_MOB_DESC =
            "(L" + LEVEL + ";L" + MUTABLE_POS + ";DDDZL" + MOB + ";)" + FLOOR;
    static final String LEGACY_PATH_DESC =
            "(Lnet/minecraft/world/level/BlockGetter;L" + MUTABLE_POS + ";)L" + PATH_TYPE + ";";
    static final String MODERN_PATH_DESC = "(L" + MOB + ";L" + BLOCK_POS + ";)L" + PATH_TYPE + ";";

    private AlienEvoBountyPathfinding1201() {}

    static int apply(ClassNode node) {
        if (TERRAIN.equals(node.name)) return applyTerrain(node);
        if (PLANNER.equals(node.name)) return applyPlanner(node);
        return 0;
    }

    private static int applyTerrain(ClassNode node) {
        MethodNode floor = uniqueMethod(node, "floor", FLOOR_DESC, FLOOR_WITH_MOB_DESC);
        MethodNode floorWater = uniqueMethod(node, "floor", FLOOR_WATER_DESC, FLOOR_WATER_WITH_MOB_DESC);
        MethodNode standable = uniqueMethod(node, "standable", STANDABLE_DESC, STANDABLE_WITH_MOB_DESC);

        int migratedFloorCalls = countCalls(floor, TERRAIN, "floor", FLOOR_WATER_DESC)
                + countCalls(floorWater, TERRAIN, "standable", STANDABLE_DESC);
        int migratedPathCalls = countCalls(standable, WALK_NODE_EVALUATOR, "getBlockPathTypeStatic", LEGACY_PATH_DESC);
        int alreadyMigratedCalls = countCalls(floor, TERRAIN, "floor", FLOOR_WATER_WITH_MOB_DESC)
                + countCalls(floorWater, TERRAIN, "standable", STANDABLE_WITH_MOB_DESC)
                + countCalls(standable, WALK_NODE_EVALUATOR, "getPathTypeStatic", MODERN_PATH_DESC);

        if (floor.desc.equals(FLOOR_WITH_MOB_DESC)
                && floorWater.desc.equals(FLOOR_WATER_WITH_MOB_DESC)
                && standable.desc.equals(STANDABLE_WITH_MOB_DESC)
                && migratedFloorCalls == 0 && migratedPathCalls == 0 && alreadyMigratedCalls == 3) {
            return 0;
        }
        if (!floor.desc.equals(FLOOR_DESC) || !floorWater.desc.equals(FLOOR_WATER_DESC)
                || !standable.desc.equals(STANDABLE_DESC)
                || migratedFloorCalls != 2 || migratedPathCalls != 1 || alreadyMigratedCalls != 0) {
            throw new IllegalStateException("AlienEvo BountyTerrain expected the verified dev.24 floor/standable call graph; "
                    + "floor calls=" + migratedFloorCalls + ", path calls=" + migratedPathCalls
                    + ", already migrated=" + alreadyMigratedCalls);
        }

        int floorMobLocal = appendMobParameter(floor);
        int waterMobLocal = appendMobParameter(floorWater);
        int standableMobLocal = appendMobParameter(standable);

        MethodInsnNode floorDelegate = onlyCall(floor, TERRAIN, "floor", FLOOR_WATER_DESC);
        floor.instructions.insertBefore(floorDelegate, new VarInsnNode(ALOAD, floorMobLocal));
        replaceCall(floor, floorDelegate, "floor", FLOOR_WATER_WITH_MOB_DESC, INVOKESTATIC);
        floor.maxStack++;

        MethodInsnNode standableCall = onlyCall(floorWater, TERRAIN, "standable", STANDABLE_DESC);
        floorWater.instructions.insertBefore(standableCall, new VarInsnNode(ALOAD, waterMobLocal));
        replaceCall(floorWater, standableCall, "standable", STANDABLE_WITH_MOB_DESC, INVOKESTATIC);
        floorWater.maxStack++;

        MethodInsnNode pathCall = onlyCall(standable, WALK_NODE_EVALUATOR, "getBlockPathTypeStatic", LEGACY_PATH_DESC);
        AbstractInsnNode mutableCall = previousCode(pathCall);
        AbstractInsnNode mutableLoad = previousCode(mutableCall);
        AbstractInsnNode levelLoad = previousCode(mutableLoad);
        if (!(mutableCall instanceof MethodInsnNode mutable)
                || mutable.getOpcode() != INVOKEVIRTUAL
                || !MUTABLE_POS.equals(mutable.owner)
                || !("()L" + MUTABLE_POS + ";").equals(mutable.desc)
                || !(mutableLoad instanceof VarInsnNode position)
                || position.getOpcode() != ALOAD || position.var != 1
                || !(levelLoad instanceof VarInsnNode level)
                || level.getOpcode() != ALOAD || level.var != 0) {
            throw new IllegalStateException("AlienEvo BountyTerrain.standable no longer loads Level/MutableBlockPos "
                    + "in the expected order before its static path-type query");
        }
        standable.instructions.set(level, new VarInsnNode(ALOAD, standableMobLocal));
        replaceCall(standable, pathCall, "getPathTypeStatic", MODERN_PATH_DESC, INVOKESTATIC);
        return 1;
    }

    private static int applyPlanner(ClassNode node) {
        if (!hasField(node, "mob", "L" + MOB + ";")) {
            throw new IllegalStateException("AlienEvo BountyRoutePlanner no longer has its real Mob field");
        }
        MethodNode plan = uniqueMethod(node, "plan",
                "(L" + PLANNER + "$Context;)L" + PLANNER + "$Plan;", null);
        MethodNode scramble = uniqueMethod(node, "scramble",
                "(L" + PLANNER + "$Context;)Lnet/habb/alienevo/entity/mob/bounty/BountyRoute;", null);

        int oldShort = countCalls(plan, TERRAIN, "floor", FLOOR_DESC);
        int oldWater = countCalls(scramble, TERRAIN, "floor", FLOOR_WATER_DESC);
        int newShort = countCalls(plan, TERRAIN, "floor", FLOOR_WITH_MOB_DESC);
        int newWater = countCalls(scramble, TERRAIN, "floor", FLOOR_WATER_WITH_MOB_DESC);
        if (oldShort == 0 && oldWater == 0 && newShort == 1 && newWater == 1) return 0;
        if (oldShort != 1 || oldWater != 1 || newShort != 0 || newWater != 0) {
            throw new IllegalStateException("AlienEvo BountyRoutePlanner expected one plan() and one scramble() "
                    + "BountyTerrain.floor call; old short=" + oldShort + ", old water=" + oldWater
                    + ", migrated short=" + newShort + ", migrated water=" + newWater);
        }

        MethodInsnNode shortFloor = onlyCall(plan, TERRAIN, "floor", FLOOR_DESC);
        MethodInsnNode waterFloor = onlyCall(scramble, TERRAIN, "floor", FLOOR_WATER_DESC);
        appendRealMob(plan, shortFloor);
        replaceCall(plan, shortFloor, "floor", FLOOR_WITH_MOB_DESC, INVOKESTATIC);
        appendRealMob(scramble, waterFloor);
        replaceCall(scramble, waterFloor, "floor", FLOOR_WATER_WITH_MOB_DESC, INVOKESTATIC);
        return 1;
    }

    private static void appendRealMob(MethodNode method, MethodInsnNode call) {
        InsnList load = new InsnList();
        load.add(new VarInsnNode(ALOAD, 0));
        load.add(new FieldInsnNode(GETFIELD, PLANNER, "mob", "L" + MOB + ";"));
        method.instructions.insertBefore(call, load);
        method.maxStack++;
    }

    /** Shifts old locals out of the new appended parameter slot and returns that parameter's local index. */
    private static int appendMobParameter(MethodNode method) {
        int mobLocal = parameterSlots(method.desc);
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof VarInsnNode variable && variable.var >= mobLocal) variable.var++;
            else if (instruction instanceof IincInsnNode increment && increment.var >= mobLocal) increment.var++;
        }
        if (method.localVariables != null) {
            method.localVariables.forEach(variable -> {
                if (variable.index >= mobLocal) variable.index++;
            });
        }
        shiftLocalAnnotations(method.visibleLocalVariableAnnotations, mobLocal);
        shiftLocalAnnotations(method.invisibleLocalVariableAnnotations, mobLocal);

        Type[] oldArguments = Type.getArgumentTypes(method.desc);
        Type[] newArguments = java.util.Arrays.copyOf(oldArguments, oldArguments.length + 1);
        newArguments[oldArguments.length] = Type.getObjectType(MOB);
        method.desc = Type.getMethodDescriptor(Type.getReturnType(method.desc), newArguments);
        method.signature = null;
        method.maxLocals++;
        return mobLocal;
    }

    private static void shiftLocalAnnotations(List<LocalVariableAnnotationNode> annotations, int from) {
        if (annotations == null) return;
        for (LocalVariableAnnotationNode annotation : annotations) {
            for (int i = 0; i < annotation.index.size(); i++) {
                int index = annotation.index.get(i);
                if (index >= from) annotation.index.set(i, index + 1);
            }
        }
    }

    private static int parameterSlots(String descriptor) {
        int slots = 0;
        for (Type argument : Type.getArgumentTypes(descriptor)) slots += argument.getSize();
        return slots;
    }

    private static MethodNode uniqueMethod(ClassNode node, String name, String oldDescriptor, String newDescriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name)) continue;
            if (!method.desc.equals(oldDescriptor) && (newDescriptor == null || !method.desc.equals(newDescriptor))) continue;
            if (found != null) throw new IllegalStateException("Duplicate AlienEvo " + node.name + "." + name + method.desc);
            found = method;
        }
        if (found == null) {
            throw new IllegalStateException("AlienEvo " + node.name + " is missing expected " + name + oldDescriptor
                    + (newDescriptor == null ? "" : " (or migrated " + newDescriptor + ")"));
        }
        return found;
    }

    private static boolean hasField(ClassNode node, String name, String descriptor) {
        return node.fields.stream().anyMatch(field -> name.equals(field.name) && descriptor.equals(field.desc));
    }

    private static int countCalls(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && owner.equals(call.owner)
                    && name.equals(call.name) && descriptor.equals(call.desc)) count++;
        }
        return count;
    }

    private static MethodInsnNode onlyCall(MethodNode method, String owner, String name, String descriptor) {
        MethodInsnNode found = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call) || !owner.equals(call.owner)
                    || !name.equals(call.name) || !descriptor.equals(call.desc)) continue;
            if (found != null) throw new IllegalStateException("Duplicate call to " + owner + "." + name + descriptor);
            found = call;
        }
        if (found == null) throw new IllegalStateException("Missing call to " + owner + "." + name + descriptor);
        return found;
    }

    private static void replaceCall(MethodNode method, MethodInsnNode oldCall, String name, String descriptor, int opcode) {
        method.instructions.set(oldCall, new MethodInsnNode(opcode, oldCall.owner, name, descriptor, oldCall.itf));
    }

    private static AbstractInsnNode previousCode(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }
}
