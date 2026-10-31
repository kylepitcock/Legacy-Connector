package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureComponent;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A legacy village building (a Forge {@code IVillageCreationHandler}'s component) as a jigsaw pool element, so it joins the
 * modern village like any modern mod's house: it is one of the choices of the village's house pool, attaches to a street with
 * a {@code minecraft:building_entrance} connector on its front, and the jigsaw generator keeps it clear of the other pieces.
 * Its footprint is the component's own box for the facing the rotation gives (1.7.10's coordBaseMode); placing it runs the
 * component's {@code addComponentParts} in the chunk being generated, as 1.7.10's village did.
 */
public final class LegacyVillagePoolElement extends StructurePoolElement {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    /** Components are planned at this height: some refuse a box below y=10 (1.7.10's canVillageGoDeeper checks). */
    private static final int PLAN_Y = 64;
    /** The first placements are logged (diagnosing buildings that come out empty). */
    private static final java.util.concurrent.atomic.AtomicInteger REPORTED = new java.util.concurrent.atomic.AtomicInteger();

    public static final MapCodec<LegacyVillagePoolElement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("handler").forGetter(e -> e.handler),
            Codec.STRING.fieldOf("village").forGetter(e -> e.village),
            projectionCodec()).apply(i, LegacyVillagePoolElement::new));

    public static StructurePoolElementType<LegacyVillagePoolElement> TYPE;

    private final String handler;
    private final String village;

    public LegacyVillagePoolElement(String handler, String village, StructureTemplatePool.Projection projection) {
        super(projection);
        this.handler = handler;
        this.village = village;
    }

    // ------------------------------------------------------------------ the legacy component

    private VillagerRegistry.IVillageCreationHandler handler() {
        for (var h : VillagerBridge.creationHandlers()) if (h.getClass().getName().equals(handler)) return h;
        return null;
    }

    /** 1.7.10 coordBaseMode for a rotation; any one-to-one mapping works, the geometry is computed per mode. */
    private static int mode(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
    }

    /** The direction the building's front (the side at the street) faces in a mode. */
    private static Direction front(int mode) {
        return switch (mode) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    private StructureVillagePieces.Start start(Random rand) {
        StructureVillagePieces.Start start = new StructureVillagePieces.Start(null, 0, rand, 0, 0, new ArrayList(), 0);
        start.inDesert = village.equals("desert");
        start.biome = switch (village) {
            case "desert" -> BiomeGenBase.desert;
            case "savanna" -> BiomeGenBase.savanna;
            case "snowy" -> BiomeGenBase.icePlains;
            case "taiga" -> BiomeGenBase.taiga;
            default -> BiomeGenBase.plains;
        };
        start.structureVillageWeightedPieceList = new ArrayList<>();
        return start;
    }

    /** The component built with its plot corner at (x, y, z), or null when the handler builds nothing. */
    private StructureComponent build(int x, int y, int z, int mode, long seed) {
        var h = handler();
        if (h == null) return null;
        try {
            Random rand = new Random(seed);
            StructureVillagePieces.PieceWeight weight = h.getVillagePieceWeight(rand, 0);
            if (weight == null) return null;
            Object built = h.buildComponent(weight, start(rand), new ArrayList<>(), rand, x, y, z, mode, 1);
            return built instanceof StructureComponent c && c.getBoundingBox() != null ? c : null;
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("Village creation handler {} built nothing: {}", handler, e.toString());
            return null;
        }
    }

    /** The footprint relative to the plot corner, for a mode; null without a component. Fixed per element and mode. */
    private final BoundingBox[] shapes = new BoundingBox[4];
    private final boolean[] shaped = new boolean[4];

    private BoundingBox shape(int mode) {
        if (!shaped[mode]) {
            StructureComponent c = build(0, PLAN_Y, 0, mode, handler.hashCode());
            StructureBoundingBox b = c == null ? null : c.getBoundingBox();
            shapes[mode] = b == null ? null : new BoundingBox(b.minX, b.minY - PLAN_Y, b.minZ, b.maxX, b.maxY - PLAN_Y, b.maxZ);
            shaped[mode] = true;
        }
        return shapes[mode];
    }

    // ------------------------------------------------------------------ pool element

    @Override
    public Vec3i getSize(StructureTemplateManager manager, Rotation rotation) {
        BoundingBox b = shape(mode(rotation));
        return b == null ? Vec3i.ZERO : new Vec3i(b.getXSpan(), b.getYSpan(), b.getZSpan());
    }

    @Override
    public BoundingBox getBoundingBox(StructureTemplateManager manager, BlockPos pos, Rotation rotation) {
        BoundingBox b = shape(mode(rotation));
        if (b == null) return new BoundingBox(pos);
        return b.moved(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager manager, BlockPos pos, Rotation rotation,
            RandomSource random) {
        int mode = mode(rotation);
        BoundingBox b = shape(mode);
        if (b == null) return List.of();
        Direction front = front(mode);
        int midX = (b.minX() + b.maxX()) / 2, midZ = (b.minZ() + b.maxZ()) / 2;
        BlockPos at = switch (front) {
            case NORTH -> new BlockPos(midX, b.minY(), b.minZ());
            case SOUTH -> new BlockPos(midX, b.minY(), b.maxZ());
            case EAST -> new BlockPos(b.maxX(), b.minY(), midZ);
            default -> new BlockPos(b.minX(), b.minY(), midZ);
        };
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", "minecraft:building_entrance");
        nbt.putString("target", "minecraft:empty");
        nbt.putString("pool", "minecraft:empty");
        nbt.putString("joint", "aligned");
        nbt.putString("final_state", "minecraft:air");
        var state = Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, FrontAndTop.fromFrontAndTop(front, Direction.UP));
        return List.of(new StructureTemplate.StructureBlockInfo(at.offset(pos), state, nbt));
    }

    @Override
    public boolean place(StructureTemplateManager manager, WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            BlockPos offset, BlockPos pivot, Rotation rotation, BoundingBox box, RandomSource random, LiquidSettings liquidSettings, boolean keepJigsaws) {
        int mode = mode(rotation);
        StructureComponent component = build(offset.getX(), offset.getY() + PLAN_Y, offset.getZ(), mode, handler.hashCode());
        if (component == null) {
            if (REPORTED.incrementAndGet() <= 12) LOG.info("Legacy village building of {} at {}: the handler built no component", handler, offset);
            return false;
        }
        // planned at PLAN_Y: down to the street the jigsaw put it on, ground level already found (no 1.7.10 averaging)
        component.getBoundingBox().offset(0, -PLAN_Y, 0);
        if (component instanceof StructureVillagePieces.Village v) v.mcc$setGroundLevel(component.getBoundingBox().minY);
        try {
            StructureBoundingBox chunk = new StructureBoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
            boolean done = component.addComponentParts(World.mcc$wrap(level), new Random(random.nextLong()), chunk);
            if (REPORTED.incrementAndGet() <= 12) {
                StructureBoundingBox b = component.getBoundingBox();
                LOG.info("Legacy village building {} at {} (box {},{},{} to {},{},{}, chunk box {}): addComponentParts returned {}",
                        component.getClass().getName(), offset, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, box, done);
            }
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Legacy village building {} failed at {}", component.getClass().getName(), offset, e);
            return false;
        }
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "LegacyVillage[" + handler + " in " + village + "]";
    }
}
