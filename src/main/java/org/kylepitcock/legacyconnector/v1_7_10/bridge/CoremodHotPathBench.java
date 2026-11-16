package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Development aid, not used by the game: times the game's hot paths that coremod hook places sit in (block collision shapes, fence shapes, entity block
 * collisions, entity movement, block placement, mob targeting), as nanoseconds per call. Run from a server probe script
 * ({@code tools/dev/coremod-hotpath-bench.js}) once in a pack without coremods and once with them, to see what the places cost.
 */
public final class CoremodHotPathBench {
    private CoremodHotPathBench() {}

    /** One line per path: {@code name=ns/op}. {@code scale} multiplies the iteration counts (1 = about 10 seconds). */
    public static List<String> run(ServerLevel level, int scale) {
        List<String> out = new ArrayList<>();
        BlockPos base = new BlockPos(4, 200, 4);
        level.setChunkForced(0, 0, true);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        level.setBlock(base.below(), stone, 0);
        level.setBlock(base.east(), fence, 0);
        level.setBlock(base.west(), pane, 0);
        level.setBlock(base.north(), stone, 0);
        Entity zombie = EntityType.ZOMBIE.create(level);
        Entity pig = EntityType.PIG.create(level);
        zombie.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        pig.setPos(base.getX() + 2.5, base.getY(), base.getZ() + 0.5);
        CollisionContext empty = CollisionContext.empty();
        CollisionContext withZombie = CollisionContext.of(zombie);
        BlockPos[] around = {base.below(), base.north(), base, base.above(), base.east(), base.west()};
        BlockState[] states = new BlockState[around.length];
        for (int i = 0; i < around.length; i++) states[i] = level.getBlockState(around[i]);

        int n = 2_000_000 * scale;
        long sink = 0;
        // 1. block collision shape, no entity (pathfinding, fluids, placement checks)
        sink += time(out, "collisionShape(empty context)", n, () -> {
            long s = 0;
            for (int i = 0; i < states.length; i++) s += states[i].getCollisionShape(level, around[i], empty).isEmpty() ? 1 : 2;
            return s;
        }, states.length);
        // 2. the same for a zombie (entity context), stone, fence, pane, air
        sink += time(out, "collisionShape(zombie context)", n, () -> {
            long s = 0;
            for (int i = 0; i < states.length; i++) s += states[i].getCollisionShape(level, around[i], withZombie).isEmpty() ? 1 : 2;
            return s;
        }, states.length);
        // 3. the block collisions of a region (what entity movement and free-space checks iterate)
        AABB region = zombie.getBoundingBox().inflate(1.5);
        sink += time(out, "getBlockCollisions(zombie, region)", n / 20, () -> {
            long s = 0;
            for (VoxelShape shape : level.getBlockCollisions(zombie, region)) s += shape.isEmpty() ? 0 : 1;
            return s;
        }, 1);
        // 4. entity movement with block collisions
        Vec3 down = new Vec3(0.0, -0.001, 0.0);
        sink += time(out, "Entity.move(zombie, down)", n / 20, () -> {
            zombie.move(MoverType.SELF, down);
            zombie.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            return 1;
        }, 1);
        // 5. block placement of the same state (the hook places run before the game finds nothing to change)
        sink += time(out, "Level.setBlock(same state)", n / 4, () -> level.setBlock(base.below(), stone, 0) ? 1 : 0, 1);
        // 6. mob targeting
        TargetingConditions conditions = TargetingConditions.forCombat();
        sink += time(out, "TargetingConditions.test(zombie, pig)", n / 4, () -> conditions.test((LivingEntity) zombie, (LivingEntity) pig) ? 1 : 0, 1);
        out.add("sink=" + sink);
        return out;
    }

    private static long time(List<String> out, String name, int iterations, java.util.function.LongSupplier body, int perCall) {
        long sink = 0;
        for (int i = 0; i < Math.max(1, iterations / 4); i++) sink += body.getAsLong(); // warm up
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 3; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) sink += body.getAsLong();
            best = Math.min(best, System.nanoTime() - start);
        }
        out.add(String.format("%s=%.1f ns/op", name, best / ((double) iterations * perCall)));
        return sink;
    }
}
