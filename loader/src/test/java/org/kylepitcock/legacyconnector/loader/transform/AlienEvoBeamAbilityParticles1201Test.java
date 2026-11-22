package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for the AlienEvo 1.20.1 particle parser compatibility bridge. */
class AlienEvoBeamAbilityParticles1201Test implements Opcodes {
    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = AlienEvoBeamAbilityParticles1201.TARGET;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(ACC_PRIVATE | ACC_STATIC, "parseParticleSpec",
                "()Lnet/minecraft/core/particles/ParticleOptions;", null, null);
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC,
                AlienEvoBeamAbilityParticles1201.PARTICLE_ARGUMENT,
                AlienEvoBeamAbilityParticles1201.METHOD,
                AlienEvoBeamAbilityParticles1201.LEGACY_DESC,
                false));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxStack = 2;
        node.methods.add(method);
        return node;
    }

    private static MethodInsnNode particleCall(ClassNode node) {
        for (var instruction : node.methods.get(0).instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) return call;
        }
        throw new AssertionError("Particle parser call not found");
    }

    @Test
    void redirectsToRegistryAccessBridgeAndPreservesLegacyDescriptor() {
        ClassNode node = fixture();

        assertEquals(1, AlienEvoBeamAbilityParticles1201.apply(node));

        MethodInsnNode call = particleCall(node);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals(AlienEvoBeamAbilityParticles1201.BRIDGE_OWNER, call.owner);
        assertEquals(AlienEvoBeamAbilityParticles1201.BRIDGE_NAME, call.name);
        assertEquals(AlienEvoBeamAbilityParticles1201.LEGACY_DESC, call.desc);
        assertFalse(call.itf);
        assertEquals(0, AlienEvoBeamAbilityParticles1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedCallIsMissing() {
        ClassNode node = fixture();
        node.methods.get(0).instructions.clear();
        node.methods.get(0).instructions.add(new InsnNode(ACONST_NULL));
        node.methods.get(0).instructions.add(new InsnNode(ARETURN));

        assertThrows(IllegalStateException.class, () -> AlienEvoBeamAbilityParticles1201.apply(node));
    }

    @Test
    void failsClosedWhenTheParticleArgumentDescriptorChangesUnexpectedly() {
        ClassNode node = fixture();
        particleCall(node).desc = "()Lnet/minecraft/core/particles/ParticleOptions;";

        assertThrows(IllegalStateException.class, () -> AlienEvoBeamAbilityParticles1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInvocationKindChanges() {
        ClassNode node = fixture();
        particleCall(node).setOpcode(INVOKEVIRTUAL);

        assertThrows(IllegalStateException.class, () -> AlienEvoBeamAbilityParticles1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = fixture();
        node.name = "example/OtherBeamAbility";

        assertEquals(0, AlienEvoBeamAbilityParticles1201.apply(node));
        assertEquals(AlienEvoBeamAbilityParticles1201.LEGACY_DESC, particleCall(node).desc);
    }

    @Test
    void acceptsAnAlreadyBridgedCallIdempotently() {
        ClassNode node = fixture();
        MethodInsnNode call = particleCall(node);
        call.owner = AlienEvoBeamAbilityParticles1201.BRIDGE_OWNER;
        call.name = AlienEvoBeamAbilityParticles1201.BRIDGE_NAME;

        assertEquals(0, AlienEvoBeamAbilityParticles1201.apply(node));
        assertEquals(AlienEvoBeamAbilityParticles1201.LEGACY_DESC, call.desc);
    }
}
