package com.example.legacyspawnprobe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Runs inside the translated Forge 1.20.1 mod classloader.
 *
 * Every case invokes the actual old Forge method descriptors; this is not a
 * target-version imitation or a unit-test mock.
 */
public final class SpawnProbe {
    private static final List<String> OBSERVED_TAGS = new ArrayList<>();
    private static final List<String> EVENTS = new ArrayList<>();
    private static int positionIndex;

    private static EntityType<SpawnProbeMob> type() {
        return LegacySpawnProbeMod.SPAWN_PROBE.get();
    }

    private SpawnProbe() {}

    public static String run(MinecraftServer server) {
        if (server == null) return "LegacySpawnProbe: FAIL no MinecraftServer binding";

        ServerLevel level = server.overworld();
        List<String> results = new ArrayList<>();

        runCase(results, "EntityType.create preserves a non-null legacy tag",
                () -> explicitCreateTag(level));
        runCase(results, "EntityType.create preserves an explicit null tag",
                () -> explicitCreateNull(level));
        runCase(results, "EntityType.spawn preserves a non-null legacy tag and Consumer order",
                () -> explicitSpawnTag(level));
        runCase(results, "EntityType.spawn(ItemStack) reconstructs the legacy root tag",
                () -> itemStackSpawnTag(level));
        runCase(results, "EntityType.spawn(ItemStack) preserves absent-tag null semantics",
                () -> itemStackSpawnNull(level));
        runCase(results, "direct virtual Mob.finalizeSpawn receives its legacy tag",
                () -> directVirtualFinalizeSpawn(level));
        runCase(results, "nested explicit-null create does not inherit its parent's tag",
                () -> nestedNullCreate(level));

        long passed = results.stream().filter(line -> line.startsWith("PASS ")).count();
        StringBuilder output = new StringBuilder("LegacySpawnProbe: ")
                .append(passed).append('/').append(results.size()).append(" passed");
        for (String result : results) output.append('\n').append(result);
        return output.toString();
    }

    static void recordFinalize(CompoundTag tag) {
        String value = tagValue(tag);
        OBSERVED_TAGS.add(value);
        EVENTS.add("finalize:" + value);
    }

    static void recordSuperReturn(CompoundTag tag) {
        EVENTS.add("super:" + tagValue(tag));
    }

    static void runNested(ServerLevelAccessor level, BlockPos parentPosition) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Nested spawn did not receive a ServerLevel");
        }
        EVENTS.add("nested-start");
        SpawnProbeMob nested = type().create(
                serverLevel,
                null,
                entity -> EVENTS.add("nested-consumer"),
                parentPosition.offset(4, 0, 0),
                MobSpawnType.COMMAND,
                false,
                false);
        require(nested != null, "nested EntityType.create returned null");
        nested.discard();
        EVENTS.add("nested-end");
    }

    private static void explicitCreateTag(ServerLevel level) {
        reset();
        String expected = "create-explicit";
        SpawnProbeMob mob = type().create(level, tag(expected), entity -> EVENTS.add("consumer"),
                nextPosition(level), MobSpawnType.COMMAND, false, false);
        require(mob != null, "EntityType.create returned null");
        require(OBSERVED_TAGS.equals(List.of(expected)), "observed tags " + OBSERVED_TAGS);
        require(EVENTS.equals(List.of("finalize:" + expected, "super:" + expected, "consumer")),
                "event order " + EVENTS);
        mob.discard();
    }

    private static void explicitCreateNull(ServerLevel level) {
        reset();
        SpawnProbeMob mob = type().create(level, null, entity -> EVENTS.add("consumer"),
                nextPosition(level), MobSpawnType.COMMAND, false, false);
        require(mob != null, "EntityType.create returned null");
        require(OBSERVED_TAGS.equals(List.of("<null>")), "observed tags " + OBSERVED_TAGS);
        require(EVENTS.equals(List.of("finalize:<null>", "super:<null>", "consumer")),
                "event order " + EVENTS);
        mob.discard();
    }

    private static void explicitSpawnTag(ServerLevel level) {
        reset();
        String expected = "spawn-explicit";
        SpawnProbeMob mob = type().spawn(level, tag(expected), entity -> EVENTS.add("consumer"),
                nextPosition(level), MobSpawnType.COMMAND, false, false);
        require(mob != null, "EntityType.spawn returned null");
        require(OBSERVED_TAGS.equals(List.of(expected)), "observed tags " + OBSERVED_TAGS);
        require(EVENTS.equals(List.of("finalize:" + expected, "super:" + expected, "consumer")),
                "event order " + EVENTS);
        mob.discard();
    }

    private static void itemStackSpawnTag(ServerLevel level) {
        reset();
        ItemStack stack = new ItemStack(Items.STICK);
        stack.getOrCreateTag().putString("probe", "item-stack");
        SpawnProbeMob mob = type().spawn(level, stack, null, nextPosition(level),
                MobSpawnType.COMMAND, false, false);
        require(mob != null, "EntityType.spawn(ItemStack) returned null");
        require(OBSERVED_TAGS.equals(List.of("item-stack")), "observed tags " + OBSERVED_TAGS);
        mob.discard();
    }

    private static void itemStackSpawnNull(ServerLevel level) {
        reset();
        ItemStack stack = new ItemStack(Items.STICK);
        SpawnProbeMob mob = type().spawn(level, stack, null, nextPosition(level),
                MobSpawnType.COMMAND, false, false);
        require(mob != null, "EntityType.spawn(ItemStack) returned null");
        require(OBSERVED_TAGS.equals(List.of("<null>")), "observed tags " + OBSERVED_TAGS);
        mob.discard();
    }

    private static void directVirtualFinalizeSpawn(ServerLevel level) {
        reset();
        BlockPos pos = nextPosition(level);
        Mob mob = type().create(level, null, null, pos, MobSpawnType.COMMAND, false, false);
        require(mob != null, "preparatory EntityType.create returned null");
        reset();

        CompoundTag directTag = tag("direct-virtual");
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
        mob.finalizeSpawn(level, difficulty, MobSpawnType.COMMAND, null, directTag);

        require(OBSERVED_TAGS.equals(List.of("direct-virtual")), "observed tags " + OBSERVED_TAGS);
        require(EVENTS.equals(List.of("finalize:direct-virtual", "super:direct-virtual")),
                "event order " + EVENTS);
        mob.discard();
    }

    private static void nestedNullCreate(ServerLevel level) {
        reset();
        CompoundTag parentTag = tag("nested-parent");
        parentTag.putBoolean("spawn_nested", true);
        SpawnProbeMob parent = type().create(level, parentTag, entity -> EVENTS.add("consumer"),
                nextPosition(level), MobSpawnType.COMMAND, false, false);
        require(parent != null, "parent EntityType.create returned null");
        require(OBSERVED_TAGS.equals(List.of("nested-parent", "<null>")),
                "observed tags " + OBSERVED_TAGS);
        require(EVENTS.equals(List.of(
                        "finalize:nested-parent",
                        "super:nested-parent",
                        "nested-start",
                        "finalize:<null>",
                        "super:<null>",
                        "nested-consumer",
                        "nested-end",
                        "consumer")),
                "event order " + EVENTS);
        parent.discard();
    }

    private static BlockPos nextPosition(ServerLevel level) {
        return level.getSharedSpawnPos().offset(++positionIndex * 3, 0, 0);
    }

    private static CompoundTag tag(String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("probe", value);
        return tag;
    }

    private static String tagValue(CompoundTag tag) {
        return tag == null ? "<null>" : tag.getString("probe");
    }

    private static void reset() {
        OBSERVED_TAGS.clear();
        EVENTS.clear();
    }

    private static void require(boolean condition, String detail) {
        if (!condition) throw new IllegalStateException(detail);
    }

    private static void runCase(List<String> results, String name, Runnable test) {
        try {
            test.run();
            results.add("PASS " + name);
        } catch (Throwable failure) {
            results.add("FAIL " + name + ": " + failure.getClass().getSimpleName()
                    + ": " + String.valueOf(failure.getMessage()));
        }
    }
}
