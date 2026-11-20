package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for the bounded AlienEvo campground biome lookup. */
class AlienEvoWorldGen1201Test implements Opcodes {
    private static ClassNode campgroundFeature() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = AlienEvoWorldGen1201.FEATURE;
        node.superName = "java/lang/Object";

        MethodNode place = new MethodNode(ACC_PUBLIC, "place", "()Z", null, null);
        place.instructions.add(new org.objectweb.asm.tree.VarInsnNode(ALOAD, 1));
        place.instructions.add(new org.objectweb.asm.tree.VarInsnNode(ALOAD, 2));
        place.instructions.add(new MethodInsnNode(INVOKEINTERFACE,
                AlienEvoWorldGen1201.WORLD_GEN_LEVEL, "getBiome",
                AlienEvoWorldGen1201.LEGACY_GET_BIOME, true));
        place.instructions.add(new InsnNode(POP));
        place.instructions.add(new InsnNode(ICONST_1));
        place.instructions.add(new InsnNode(IRETURN));
        place.maxLocals = 3;
        place.maxStack = 2;
        node.methods.add(place);
        return node;
    }

    @Test
    void onlyCampgroundBiomeLookupIsRedirectedToExactQuartSampler() {
        ClassNode node = campgroundFeature();

        assertEquals(1, AlienEvoWorldGen1201.apply(node));

        MethodNode place = node.methods.get(0);
        assertTrue(place.instructions.stream().anyMatch(instruction ->
                instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKESTATIC
                        && call.owner.equals(AlienEvoWorldGen1201.HELPER)
                        && call.name.equals("getBiomeForFeature")
                        && call.desc.equals(AlienEvoWorldGen1201.COMPAT_GET_BIOME)
                        && !call.itf));
        assertFalse(place.instructions.stream().anyMatch(instruction ->
                instruction instanceof MethodInsnNode call
                        && call.owner.equals(AlienEvoWorldGen1201.WORLD_GEN_LEVEL)
                        && call.name.equals("getBiome")
                        && call.desc.equals(AlienEvoWorldGen1201.LEGACY_GET_BIOME)));
    }

    @Test
    void adaptationIsIdempotent() {
        ClassNode node = campgroundFeature();

        assertEquals(1, AlienEvoWorldGen1201.apply(node));
        assertEquals(0, AlienEvoWorldGen1201.apply(node));
    }

    @Test
    void unrelatedClassesAreUntouched() {
        ClassNode node = campgroundFeature();
        node.name = "net/habb/alienevo/worldgen/OtherFeature";

        assertEquals(0, AlienEvoWorldGen1201.apply(node));
        assertTrue(node.methods.get(0).instructions.stream().anyMatch(instruction ->
                instruction instanceof MethodInsnNode call
                        && call.name.equals("getBiome")));
    }

    @Test
    void unexpectedCampgroundBytecodeFailsClosed() {
        ClassNode node = campgroundFeature();
        node.methods.get(0).instructions.clear();

        assertThrows(IllegalStateException.class, () -> AlienEvoWorldGen1201.apply(node));
    }
}
