package legacyconnector.v1_7_10.net.minecraft.world.gen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The block-id view of a chunk array that 1.6.4 generators (recursiveGenerate with a byte[]) work on: conversion and write-back of what they changed. */
class MapGenBaseIdsTest {
    private static final Map<String, Integer> IDS = Map.of("stone", 1, "dirt", 3, "modded", 300);

    @Test
    void idsAreBytesAndUnrepresentableBlocksReadAsAir() {
        String[] chunk = {"stone", null, "dirt", "modded"};
        assertArrayEquals(new byte[] {1, 0, 3, 0}, MapGenBase.toIds(chunk, IDS::get));
    }

    @Test
    void onlyChangedEntriesAreWrittenBack() {
        String[] chunk = {"stone", "modded", "dirt", null};
        byte[] before = MapGenBase.toIds(chunk, IDS::get);
        byte[] after = before.clone();
        after[0] = 3;      // stone -> dirt
        after[3] = 1;      // air -> stone
        MapGenBase.copyBack(before, after, chunk, id -> id == 0 ? null : id == 1 ? "stone" : id == 3 ? "dirt" : "?");
        // entry 1 (a block above id 255, read as 0) was not touched by the generator and keeps its block
        assertArrayEquals(new String[] {"dirt", "modded", "dirt", "stone"}, chunk);
    }
}
