package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

class MobsB173Test {
    static class Plain extends LivingEntity {Plain(World world){super(world);}public void helper(){}}
    static class CustomMovement extends Plain {CustomMovement(World world){super(world);}public void tickMovement(){}}
    static class InheritedMovement extends CustomMovement {InheritedMovement(World world){super(world);}}
    static class Interaction extends Plain {Interaction(World world){super(world);}public boolean interact(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity p){return false;}}
    static class SuperCall extends Plain {SuperCall(World world){super(world);}@Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound n){super.writeNbt(n);}}
    @Test void rejectsCustomSimulationIncludingInheritedOverrides(){
        assertEquals("CustomMovement.tickMovement",MobsB173.unsupportedBehaviour(CustomMovement.class));
        assertEquals("CustomMovement.tickMovement",MobsB173.unsupportedBehaviour(InheritedMovement.class));
    }
    @Test void allowsOriginalInteractionAndHelpersButDoesNotPretendOtherFamiliesAreCows(){
        assertNull(MobsB173.unsupportedBehaviour(Interaction.class));
        assertNull(MobsB173.unsupportedBehaviour(Plain.class));
        assertFalse(MobsB173.cowFamily(Plain.class));
        assertFalse(MobsB173.cowFamily(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class));
    }
    @Test void rejectsCallbacksThatRelyOnUnimplementedSuperclassBodies(){
        assertEquals("unsupported superclass callback writeNbt",MobsB173.unsupportedBehaviour(SuperCall.class));
    }
}
