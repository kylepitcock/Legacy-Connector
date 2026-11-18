package org.kylepitcock.legacyconnector.core;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.problems.LegacyProblems;
import org.kylepitcock.legacyconnector.core.problems.LegacyProblems.Kind;

/** A legacy tile or entity whose NBT write throws (or yields a tag the chunk writer rejects) must not fail the chunk save. */
class NbtSaveGuardTest {
    private static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("thermalexpansion", "teleport");

    @BeforeEach
    @AfterEach
    void clear() {
        LegacyProblems.reset();
    }

    @Test
    void throwingTileWriteReturnsNullSoTheCallerSavesItsLastGoodTag() {
        CompoundTag good = new CompoundTag();
        good.putString("owner", "kyle");
        assertSame(good, NbtSaveGuard.attempt(() -> good, this, TYPE, "1, 2, 3"));
        assertNull(NbtSaveGuard.attempt(() -> {
            throw new NullPointerException("str is null");
        }, this, TYPE, "1, 2, 3"));
        assertEquals(1, LegacyProblems.count("thermalexpansion", Kind.NBT_FAILURE));
        // the same tile failing again is not a new problem
        NbtSaveGuard.attempt(() -> {
            throw new NullPointerException("str is null");
        }, this, TYPE, "1, 2, 3");
        assertEquals(1, LegacyProblems.count("thermalexpansion", Kind.NBT_FAILURE));
    }

    @Test
    void aTagTheChunkWriterRejectsCountsAsAFailure() {
        CompoundTag huge = new CompoundTag();
        huge.putString("big", "x".repeat(70000)); // writeUTF limit is 65535 bytes: the chunk save would throw
        assertNull(NbtSaveGuard.attempt(() -> huge, this, TYPE, "0, 0, 0"));
        assertEquals(1, LegacyProblems.count("thermalexpansion", Kind.NBT_FAILURE));
    }

    @Test
    void entityWriteFailureKeepsTheLastGoodKeysAndDropsTheHalfWrittenOnes() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "mod:thing"); // vanilla part, written before
        CompoundTag lastGood = NbtSaveGuard.guardInPlace(tag, null, () -> {
            tag.putInt("Health", 20);
            tag.putString("Owner", "kyle");
        }, this, TYPE, "0, 0, 0");
        assertEquals(20, lastGood.getInt("Health"));
        assertFalse(lastGood.contains("id"));

        CompoundTag second = new CompoundTag();
        second.putString("id", "mod:thing");
        CompoundTag kept = NbtSaveGuard.guardInPlace(second, lastGood, () -> {
            second.putInt("Health", 5); // half written, then the mod throws
            throw new IllegalStateException("boom");
        }, this, TYPE, "0, 0, 0");
        assertSame(lastGood, kept);
        assertEquals(20, second.getInt("Health"));
        assertEquals("kyle", second.getString("Owner"));
        assertEquals("mod:thing", second.getString("id"));
        assertEquals(1, LegacyProblems.count("thermalexpansion", Kind.NBT_FAILURE));
    }

    @Test
    void firstEntityWriteThatFailsKeepsWhatItWrote() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "mod:shot");
        CompoundTag kept = NbtSaveGuard.guardInPlace(tag, null, () -> {
            tag.putInt("inTile", 7); // written before the mod throws (a projectile without a shooter)
            throw new NullPointerException("shooter");
        }, this, TYPE, "0, 0, 0");
        assertNull(kept);
        assertEquals(7, tag.getInt("inTile"));
        assertEquals("mod:shot", tag.getString("id"));
    }
}
