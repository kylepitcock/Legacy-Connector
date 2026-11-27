package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies the full dev.24 bounty path sampler migration, including propagation of its actual Mob. */
class AlienEvoBountyPathfinding1201Test implements Opcodes {
    private static ClassNode terrainFixture() {
        ClassNode node = new ClassNode();
        node.name = AlienEvoBountyPathfinding1201.TERRAIN;

        MethodNode floor = new MethodNode(ACC_STATIC, "floor",
                AlienEvoBountyPathfinding1201.FLOOR_DESC, null, null);
        floor.instructions.add(new MethodInsnNode(INVOKESTATIC, node.name, "floor",
                AlienEvoBountyPathfinding1201.FLOOR_WATER_DESC, false));
        floor.instructions.add(new InsnNode(ARETURN));
        floor.maxStack = 12;
        floor.maxLocals = 11;
        node.methods.add(floor);

        MethodNode floorWater = new MethodNode(ACC_STATIC, "floor",
                AlienEvoBountyPathfinding1201.FLOOR_WATER_DESC, null, null);
        floorWater.instructions.add(new MethodInsnNode(INVOKESTATIC, node.name, "standable",
                AlienEvoBountyPathfinding1201.STANDABLE_DESC, false));
        floorWater.instructions.add(new InsnNode(ARETURN));
        floorWater.maxStack = 12;
        floorWater.maxLocals = 12;
        node.methods.add(floorWater);

        MethodNode standable = new MethodNode(ACC_PRIVATE | ACC_STATIC, "standable",
                AlienEvoBountyPathfinding1201.STANDABLE_DESC, null, null);
        standable.instructions.add(new VarInsnNode(ALOAD, 0));
        standable.instructions.add(new VarInsnNode(ALOAD, 1));
        standable.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoBountyPathfinding1201.MUTABLE_POS, "mutable",
                "()L" + AlienEvoBountyPathfinding1201.MUTABLE_POS + ";", false));
        standable.instructions.add(new MethodInsnNode(INVOKESTATIC,
                AlienEvoBountyPathfinding1201.WALK_NODE_EVALUATOR, "getBlockPathTypeStatic",
                AlienEvoBountyPathfinding1201.LEGACY_PATH_DESC, false));
        standable.instructions.add(new InsnNode(POP));
        standable.instructions.add(new InsnNode(ACONST_NULL));
        standable.instructions.add(new InsnNode(ARETURN));
        standable.maxStack = 8;
        standable.maxLocals = 12; // local 9 onwards belongs to the original method's temporary values
        node.methods.add(standable);
        return node;
    }

    private static ClassNode plannerFixture() {
        ClassNode node = new ClassNode();
        node.name = AlienEvoBountyPathfinding1201.PLANNER;
        node.fields.add(new org.objectweb.asm.tree.FieldNode(ACC_PRIVATE | ACC_FINAL,
                "mob", "L" + AlienEvoBountyPathfinding1201.MOB + ";", null, null));

        MethodNode plan = new MethodNode(ACC_PUBLIC, "plan",
                "(L" + node.name + "$Context;)L" + node.name + "$Plan;", null, null);
        plan.instructions.add(new MethodInsnNode(INVOKESTATIC, AlienEvoBountyPathfinding1201.TERRAIN,
                "floor", AlienEvoBountyPathfinding1201.FLOOR_DESC, false));
        plan.instructions.add(new InsnNode(POP));
        plan.instructions.add(new InsnNode(ACONST_NULL));
        plan.instructions.add(new InsnNode(ARETURN));
        plan.maxStack = 10;
        plan.maxLocals = 2;
        node.methods.add(plan);

        MethodNode scramble = new MethodNode(ACC_PUBLIC, "scramble",
                "(L" + node.name + "$Context;)Lnet/habb/alienevo/entity/mob/bounty/BountyRoute;", null, null);
        scramble.instructions.add(new MethodInsnNode(INVOKESTATIC, AlienEvoBountyPathfinding1201.TERRAIN,
                "floor", AlienEvoBountyPathfinding1201.FLOOR_WATER_DESC, false));
        scramble.instructions.add(new InsnNode(POP));
        scramble.instructions.add(new InsnNode(ACONST_NULL));
        scramble.instructions.add(new InsnNode(ARETURN));
        scramble.maxStack = 10;
        scramble.maxLocals = 2;
        node.methods.add(scramble);
        return node;
    }

    private static MethodInsnNode onlyCall(MethodNode method, String owner, String name, String desc) {
        MethodInsnNode found = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call) || !owner.equals(call.owner)
                    || !name.equals(call.name) || !desc.equals(call.desc)) continue;
            if (found != null) throw new AssertionError("duplicate call");
            found = call;
        }
        return found;
    }

    @Test
    void terrainThreadsTheMobThroughBothFloorOverloadsAndUsesMobAwarePathTypes() {
        ClassNode terrain = terrainFixture();

        assertEquals(1, AlienEvoBountyPathfinding1201.apply(terrain));

        MethodNode floor = terrain.methods.get(0);
        MethodNode floorWater = terrain.methods.get(1);
        MethodNode standable = terrain.methods.get(2);
        assertEquals(AlienEvoBountyPathfinding1201.FLOOR_WITH_MOB_DESC, floor.desc);
        assertEquals(AlienEvoBountyPathfinding1201.FLOOR_WATER_WITH_MOB_DESC, floorWater.desc);
        assertEquals(AlienEvoBountyPathfinding1201.STANDABLE_WITH_MOB_DESC, standable.desc);

        MethodInsnNode floorDelegate = onlyCall(floor, terrain.name, "floor",
                AlienEvoBountyPathfinding1201.FLOOR_WATER_WITH_MOB_DESC);
        assertInstanceOf(VarInsnNode.class, floorDelegate.getPrevious());
        assertEquals(ALOAD, floorDelegate.getPrevious().getOpcode());
        assertEquals(11, ((VarInsnNode) floorDelegate.getPrevious()).var);

        MethodInsnNode standableCall = onlyCall(floorWater, terrain.name, "standable",
                AlienEvoBountyPathfinding1201.STANDABLE_WITH_MOB_DESC);
        assertInstanceOf(VarInsnNode.class, standableCall.getPrevious());
        assertEquals(12, ((VarInsnNode) standableCall.getPrevious()).var);

        MethodInsnNode modernPath = onlyCall(standable, AlienEvoBountyPathfinding1201.WALK_NODE_EVALUATOR,
                "getPathTypeStatic", AlienEvoBountyPathfinding1201.MODERN_PATH_DESC);
        assertEquals(INVOKESTATIC, modernPath.getOpcode());
        AbstractInsnNode mobLoad = modernPath.getPrevious().getPrevious().getPrevious();
        assertInstanceOf(VarInsnNode.class, mobLoad);
        assertEquals(ALOAD, mobLoad.getOpcode());
        assertEquals(9, ((VarInsnNode) mobLoad).var);
        assertEquals(0, AlienEvoBountyPathfinding1201.apply(terrain), "adapter must be idempotent");
    }

    @Test
    void plannerPassesItsActualMobToEveryTerrainQuery() {
        ClassNode planner = plannerFixture();

        assertEquals(1, AlienEvoBountyPathfinding1201.apply(planner));

        MethodNode plan = planner.methods.stream().filter(method -> "plan".equals(method.name))
                .findFirst().orElseThrow();
        MethodNode scramble = planner.methods.stream().filter(method -> "scramble".equals(method.name))
                .findFirst().orElseThrow();
        MethodInsnNode planFloor = onlyCall(plan, AlienEvoBountyPathfinding1201.TERRAIN, "floor",
                AlienEvoBountyPathfinding1201.FLOOR_WITH_MOB_DESC);
        MethodInsnNode scrambleFloor = onlyCall(scramble, AlienEvoBountyPathfinding1201.TERRAIN, "floor",
                AlienEvoBountyPathfinding1201.FLOOR_WATER_WITH_MOB_DESC);
        for (MethodInsnNode call : new MethodInsnNode[] {planFloor, scrambleFloor}) {
            assertInstanceOf(FieldInsnNode.class, call.getPrevious());
            FieldInsnNode mobField = (FieldInsnNode) call.getPrevious();
            assertEquals(GETFIELD, mobField.getOpcode());
            assertEquals(AlienEvoBountyPathfinding1201.PLANNER, mobField.owner);
            assertEquals("mob", mobField.name);
            assertEquals("L" + AlienEvoBountyPathfinding1201.MOB + ";", mobField.desc);
            assertEquals(ALOAD, mobField.getPrevious().getOpcode());
            assertEquals(0, ((VarInsnNode) mobField.getPrevious()).var);
        }
        assertEquals(0, AlienEvoBountyPathfinding1201.apply(planner), "adapter must be idempotent");
    }

    @Test
    void failsClosedIfEitherPlannerCallSiteDisappears() {
        ClassNode planner = plannerFixture();
        MethodNode scramble = planner.methods.stream().filter(method -> "scramble".equals(method.name))
                .findFirst().orElseThrow();
        scramble.instructions.remove(scramble.instructions.getFirst());

        assertThrows(IllegalStateException.class, () -> AlienEvoBountyPathfinding1201.apply(planner));
    }

    @Test
    void failsClosedIfTheTerrainPathTypeInvocationNoLongerHasItsExpectedStackShape() {
        ClassNode terrain = terrainFixture();
        MethodNode standable = terrain.methods.get(2);
        standable.instructions.set(standable.instructions.getFirst(), new InsnNode(NOP));

        assertThrows(IllegalStateException.class, () -> AlienEvoBountyPathfinding1201.apply(terrain));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode other = terrainFixture();
        other.name = "example/Unrelated";
        assertEquals(0, AlienEvoBountyPathfinding1201.apply(other));
        assertTrue(onlyCall(other.methods.get(2), AlienEvoBountyPathfinding1201.WALK_NODE_EVALUATOR,
                "getBlockPathTypeStatic", AlienEvoBountyPathfinding1201.LEGACY_PATH_DESC) != null);
        assertFalse(other.methods.get(2).desc.contains("Mob;"));
    }
}
