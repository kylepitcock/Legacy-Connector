package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** An access transformer line for a member 1.21.1 no longer has is dropped; lines for members it still has stay. */
class AccessTransformerDropTest {
    @Test
    void membersOf121Stay() {
        assertNull(Forge1201Transformer.removedMember("public net.minecraft.world.level.block.ButtonBlock type"));
        assertNull(Forge1201Transformer.removedMember("public-f net.minecraft.advancements.critereon.ItemPredicate"));
        assertNull(Forge1201Transformer.removedMember("public net.minecraft.world.level.block.Blocks leaves(Lnet/minecraft/world/level/block/SoundType;)Lnet/minecraft/world/level/block/Block; # comment"));
    }

    @Test
    void removedMemberIsReported() {
        assertEquals("net.minecraft.server.level.WorldGenRegion.writeRadiusCutoff",
                Forge1201Transformer.removedMember("public net.minecraft.server.level.WorldGenRegion writeRadiusCutoff #writeRadiusCutoff"));
    }

    @Test
    void unknownClassesAndWildcardsStay() {
        assertNull(Forge1201Transformer.removedMember("public com.example.Other missing"));
        assertNull(Forge1201Transformer.removedMember("public net.minecraft.server.level.WorldGenRegion *"));
    }
}
