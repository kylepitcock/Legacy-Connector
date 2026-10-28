package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.LoggerFactory;

/**
 * The vanilla 1.7.10 village buildings mods add to their own village layouts ({@code StructureVillagePieces.House1.func_74898_a}
 * and the other factories), built from the modern village's building of the same use: the library, the smithy, the farms,
 * the temple... (the desert ones in deserts). The plot is sized from the template and the building turned so that its
 * entrance faces the path the plot lies on, as 1.7.10 oriented its houses; it stands on the generator's ground height
 * at the plot (the same in every chunk it crosses) on a foundation down to the terrain.
 */
public final class VanillaVillageBuildings {
    private static final Map<String, String[]> TEMPLATES = Map.of(
            "library", new String[] {"plains/houses/plains_library_1", "desert/houses/desert_library_1"},
            "smithy", new String[] {"plains/houses/plains_weaponsmith_1", "desert/houses/desert_weaponsmith_1"},
            "big_house", new String[] {"plains/houses/plains_medium_house_1", "desert/houses/desert_medium_house_1"},
            "garden_house", new String[] {"plains/houses/plains_small_house_1", "desert/houses/desert_small_house_1"},
            "hut", new String[] {"plains/houses/plains_small_house_2", "desert/houses/desert_small_house_2"},
            "hall", new String[] {"plains/houses/plains_butcher_shop_1", "desert/houses/desert_butcher_shop_1"},
            "large_field", new String[] {"plains/houses/plains_large_farm_1", "desert/houses/desert_large_farm_1"},
            "field", new String[] {"plains/houses/plains_farm_1", "desert/houses/desert_farm_1"},
            "church", new String[] {"plains/houses/plains_temple_3", "desert/houses/desert_temple_1"});
    private static final ResourceLocation ENTRANCE = ResourceLocation.withDefaultNamespace("building_entrance");
    private static final Map<String, Optional<Building>> CACHE = new ConcurrentHashMap<>();

    /** A template with its entrance: the jigsaw block the street connects to, and the direction it faces. */
    private record Building(StructureTemplate template, Direction entrance, int entranceY) {}

    private VanillaVillageBuildings() {}

    private static Building building(String kind, boolean desert) {
        String[] names = TEMPLATES.get(kind);
        if (names == null) return null;
        String name = names[desert ? 1 : 0];
        return CACHE.computeIfAbsent(name, n -> {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return Optional.empty();
            return server.getStructureManager().get(ResourceLocation.withDefaultNamespace("village/" + n)).map(t -> {
                Direction entrance = Direction.NORTH;
                int y = 0;
                for (var info : t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)) {
                    if (info.nbt() != null && ENTRANCE.toString().equals(info.nbt().getString("name"))) {
                        entrance = JigsawBlock.getFrontFacing(info.state());
                        y = info.pos().getY();
                        break;
                    }
                }
                return new Building(t, entrance.getAxis().isHorizontal() ? entrance : Direction.NORTH, y);
            });
        }).orElse(null);
    }

    /** The side of the plot the path is on, for a legacy facing (the plot extends away from it). */
    private static Direction pathSide(int facing) {
        return switch (facing & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    private static Rotation rotation(Building b, int facing) {
        Direction want = pathSide(facing);
        for (Rotation r : Rotation.values()) if (r.rotate(b.entrance()) == want) return r;
        return Rotation.NONE;
    }

    /** Width (along the path), height and depth (away from it) of the building's plot, or null when there is no such building. */
    public static int[] size(String kind, boolean desert, int facing) {
        Building b = building(kind, desert);
        if (b == null) return null;
        Vec3i s = b.template().getSize();
        Rotation r = rotation(b, facing);
        boolean turned = r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90;
        int sx = turned ? s.getZ() : s.getX(), sz = turned ? s.getX() : s.getZ();
        boolean alongX = (facing & 1) == 0; // facings 0 and 2: the path runs along x
        return new int[] {alongX ? sx : sz, s.getY(), alongX ? sz : sx};
    }

    /** Builds the part of the building inside {@code clip}. */
    public static void place(String kind, boolean desert, int facing, StructureBoundingBox plot, World world, Random rand, StructureBoundingBox clip) {
        Building b = building(kind, desert);
        if (b == null || !(world.mcc$level() instanceof ServerLevelAccessor level)) return;
        try {
            int ground = groundLevel(level, plot);
            Rotation r = rotation(b, facing);
            BoundingBox clipBox = new BoundingBox(clip.minX, clip.minY, clip.minZ, clip.maxX, clip.maxY, clip.maxZ);
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(r).setBoundingBox(clipBox)
                    .addProcessor(JigsawReplacementProcessor.INSTANCE);
            BoundingBox at0 = b.template().getBoundingBox(settings, BlockPos.ZERO);
            int baseY = ground - 1 - b.entranceY();
            BlockPos origin = new BlockPos(plot.minX - at0.minX(), baseY, plot.minZ - at0.minZ());
            b.template().placeInWorld(level, origin, origin, settings, RandomSource.create(rand.nextLong()), 2);
            foundation(level, plot, clip, baseY, desert);
        } catch (RuntimeException | LinkageError e) {
            LoggerFactory.getLogger("LegacyConnector/Villages").warn("Vanilla village building {} failed: {}", kind, e.toString());
        }
    }

    /** The terrain height the world generator gives the plot (from the noise, so every chunk the plot crosses agrees). */
    private static int groundLevel(ServerLevelAccessor level, StructureBoundingBox plot) {
        ServerLevel server = level instanceof WorldGenRegion region ? region.getLevel() : level.getLevel();
        var source = server.getChunkSource();
        var generator = source.getGenerator();
        int total = 0, n = 0;
        int[][] points = {{plot.minX, plot.minZ}, {plot.maxX, plot.minZ}, {plot.minX, plot.maxZ}, {plot.maxX, plot.maxZ},
                {(plot.minX + plot.maxX) / 2, (plot.minZ + plot.maxZ) / 2}};
        for (int[] p : points) {
            total += generator.getBaseHeight(p[0], p[1], Heightmap.Types.WORLD_SURFACE_WG, server, source.randomState());
            n++;
        }
        return total / n;
    }

    /** Fills the ground under the building's lowest layer down to the terrain, as 1.7.10's houses did. */
    private static void foundation(ServerLevelAccessor level, StructureBoundingBox plot, StructureBoundingBox clip, int baseY, boolean desert) {
        BlockState fill = desert ? Blocks.SANDSTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(plot.minX, clip.minX); x <= Math.min(plot.maxX, clip.maxX); x++) {
            for (int z = Math.max(plot.minZ, clip.minZ); z <= Math.min(plot.maxZ, clip.maxZ); z++) {
                if (level.getBlockState(pos.set(x, baseY, z)).isAir()) continue;
                for (int y = baseY - 1, depth = 0; depth < 12 && y > level.getMinBuildHeight(); y--, depth++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.isAir() && !state.liquid() && !state.canBeReplaced()) break;
                    level.setBlock(pos, fill, 2);
                }
            }
        }
    }
}
