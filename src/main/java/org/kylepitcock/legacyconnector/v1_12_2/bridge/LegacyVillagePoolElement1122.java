package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.init.Biomes;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureComponent;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureVillagePieces;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry;
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
import org.kylepitcock.legacyconnector.core.worldgen.VillageAdditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A 1.12.2 village building (a Forge {@code IVillageCreationHandler}'s component) as a jigsaw pool element, the counterpart of the
 * 1.7.10 one: it joins the modern village as an extra piece beside a street, its footprint is the component's own box for the facing the
 * rotation gives, and placing it runs the component's {@code addComponentParts} (and those of every piece it adds by continuing the
 * layout) through the generation region of the chunk being generated, as 1.12's village did. A handler or component that throws is
 * logged (throttled) and skipped.
 */
public final class LegacyVillagePoolElement1122 extends StructurePoolElement {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    /** Components are planned at this height: some refuse a box below y=10 (canVillageGoDeeper). */
    private static final int PLAN_Y = 64;
    /** Pieces a component may add by continuing the layout (the pending lists), at most. */
    private static final int MAX_PIECES = 64;
    private static final java.util.concurrent.atomic.AtomicInteger REPORTED = new java.util.concurrent.atomic.AtomicInteger();

    public static final MapCodec<LegacyVillagePoolElement1122> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("handler").forGetter(e -> e.handler),
            Codec.STRING.fieldOf("village").forGetter(e -> e.village),
            BoundingBox.CODEC.listOf().optionalFieldOf("streets", List.of()).forGetter(e -> e.streets),
            BoundingBox.CODEC.listOf().optionalFieldOf("buildings", List.of()).forGetter(e -> e.buildings),
            projectionCodec()).apply(i, LegacyVillagePoolElement1122::new));

    public static StructurePoolElementType<LegacyVillagePoolElement1122> TYPE;

    private final String handler;
    private final String village;
    /** The village this piece was added to: its streets and other pieces, handed to the component as the legacy component list. */
    private final List<BoundingBox> streets;
    private final List<BoundingBox> buildings;

    public LegacyVillagePoolElement1122(String handler, String village, StructureTemplatePool.Projection projection) {
        this(handler, village, List.of(), List.of(), projection);
    }

    public LegacyVillagePoolElement1122(String handler, String village, List<BoundingBox> streets, List<BoundingBox> buildings,
            StructureTemplatePool.Projection projection) {
        super(projection);
        this.handler = handler;
        this.village = village;
        this.streets = List.copyOf(streets);
        this.buildings = List.copyOf(buildings);
    }

    public LegacyVillagePoolElement1122 inVillage(List<BoundingBox> villageStreets, List<BoundingBox> villageBuildings) {
        return new LegacyVillagePoolElement1122(handler, village, villageStreets, villageBuildings, getProjection());
    }

    /** The key a handler is saved under: its class and its component's class (one handler class may build several components). */
    public static String key(VillagerRegistry.IVillageCreationHandler h) {
        String component;
        try {
            Class<?> c = h.getComponentClass();
            component = c == null ? "" : c.getName();
        } catch (RuntimeException | LinkageError e) {
            component = "";
        }
        return component.isEmpty() ? h.getClass().getName() : h.getClass().getName() + "/" + component;
    }

    private VillagerRegistry.IVillageCreationHandler handler() {
        List<VillagerRegistry.IVillageCreationHandler> all = LegacyVillages1122.handlers();
        for (var h : all) if (key(h).equals(handler)) return h;
        if (handler.indexOf('/') < 0) for (var h : all) if (h.getClass().getName().equals(handler)) return h;
        return null;
    }

    /** The facing whose component geometry has its front (local z = 0, where doors and the street side are) on {@link VillageAdditions#front}. */
    private static EnumFacing facing(int mode) {
        return switch (mode) {
            case 0 -> EnumFacing.SOUTH;
            case 1 -> EnumFacing.WEST;
            case 2 -> EnumFacing.NORTH;
            default -> EnumFacing.EAST;
        };
    }

    private StructureVillagePieces.Start start(Random rand) {
        StructureVillagePieces.Start start = new StructureVillagePieces.Start(null, 0, rand, 0, 0, new ArrayList<>(), 0);
        switch (village) {
            case "desert" -> start.mcc$setStyle(1, Biomes.DESERT);
            case "savanna" -> start.mcc$setStyle(2, Biomes.SAVANNA);
            case "snowy" -> start.mcc$setStyle(0, Biomes.ICE_PLAINS);
            case "taiga" -> start.mcc$setStyle(3, Biomes.TAIGA);
            default -> start.mcc$setStyle(0, Biomes.PLAINS);
        }
        start.structureVillageWeightedPieceList = new ArrayList<>();
        return start;
    }

    /** A built group: the handler's components, and the whole component list they were built into (with the village's stand-ins). */
    private record Group(List<StructureComponent> parts, List<StructureComponent> list) {}

    private static final Group NONE = new Group(List.of(), List.of());

    /**
     * The handler's component built with its plot corner at (x, y, z), followed by every piece it adds by continuing the layout from its
     * own pending lists. The component list it is built into holds the village around it ({@link #standIns}). Empty when the handler
     * builds nothing.
     */
    private Group build(int x, int y, int z, int mode, long seed, boolean withVillage) {
        var h = handler();
        if (h == null) return NONE;
        try {
            Random rand = new Random(seed);
            StructureVillagePieces.PieceWeight weight = h.getVillagePieceWeight(rand, 0);
            if (weight == null) return NONE;
            StructureVillagePieces.Start start = start(rand);
            List<StructureComponent> pieces = new ArrayList<>();
            if (withVillage) pieces.addAll(standIns(start, PLAN_Y));
            int known = pieces.size();
            StructureComponent first = h.buildComponent(weight, start, pieces, rand, x, y, z, facing(mode), 1);
            if (first == null || first.getBoundingBox() == null) return NONE;
            pieces.add(first);
            first.buildComponent(start, pieces, rand);
            for (int guard = 0; guard < MAX_PIECES && (!start.pendingRoads.isEmpty() || !start.pendingHouses.isEmpty()); guard++) {
                List<StructureComponent> pending = start.pendingRoads.isEmpty() ? start.pendingHouses : start.pendingRoads;
                pending.remove(0).buildComponent(start, pieces, rand);
            }
            List<StructureComponent> parts = new ArrayList<>();
            List<StructureComponent> list = new ArrayList<>();
            for (int n = 0; n < pieces.size(); n++) {
                StructureComponent c = pieces.get(n);
                if (c == null || c.getBoundingBox() == null) continue;
                list.add(c);
                if (n >= known && parts.size() < MAX_PIECES) parts.add(c);
            }
            return new Group(parts, list);
        } catch (RuntimeException | LinkageError e) {
            if (REPORTED.incrementAndGet() <= 12) LOG.warn("1.12.2 village creation handler {} built nothing: {}", handler, e.toString());
            return NONE;
        }
    }

    /**
     * The modern village as 1.12's component list showed it to a village component (at the planning height): every street as a
     * {@code Path}, and every other piece of the village as one-block strips along its street, so that code reading the village's
     * extent from its paths also covers the modern buildings. Buildings first, the streets last.
     */
    private List<StructureComponent> standIns(StructureVillagePieces.Start start, int dy) {
        List<StructureComponent> out = new ArrayList<>();
        Random none = new Random(0);
        for (BoundingBox b : buildings) {
            BoundingBox street = nearest(b);
            if (street == null) continue;
            boolean alongX = street.getXSpan() >= street.getZSpan();
            if ((alongX ? b.getXSpan() : b.getZSpan()) < 2) continue;
            boolean streetLow = alongX ? street.minZ() + street.maxZ() < b.minZ() + b.maxZ() : street.minX() + street.maxX() < b.minX() + b.maxX();
            int from = alongX ? b.minZ() : b.minX(), to = alongX ? b.maxZ() : b.maxX();
            for (int n = 0; n <= to - from; n++) {
                int at = streetLow ? to - n : from + n;
                StructureBoundingBox strip = alongX
                        ? new StructureBoundingBox(b.minX(), b.minY() + dy, at, b.maxX(), b.maxY() + dy, at)
                        : new StructureBoundingBox(at, b.minY() + dy, b.minZ(), at, b.maxY() + dy, b.maxZ());
                out.add(new StructureVillagePieces.Path(start, 0, none, strip, alongX ? EnumFacing.WEST : EnumFacing.SOUTH));
            }
        }
        for (BoundingBox s : streets) {
            StructureBoundingBox box = new StructureBoundingBox(s.minX(), s.minY() + dy, s.minZ(), s.maxX(), s.maxY() + dy, s.maxZ());
            out.add(new StructureVillagePieces.Path(start, 0, none, box, s.getXSpan() >= s.getZSpan() ? EnumFacing.WEST : EnumFacing.SOUTH));
        }
        return out;
    }

    private BoundingBox nearest(BoundingBox b) {
        BoundingBox best = null;
        long bestGap = Long.MAX_VALUE;
        for (BoundingBox s : streets) {
            long gx = Math.max(0, Math.max(s.minX() - b.maxX(), b.minX() - s.maxX()));
            long gz = Math.max(0, Math.max(s.minZ() - b.maxZ(), b.minZ() - s.maxZ()));
            long gap = gx * gx + gz * gz;
            if (gap < bestGap) {
                bestGap = gap;
                best = s;
            }
        }
        return best;
    }

    /** Footprints of the group relative to the plot corner, per handler, village type and mode (empty: builds nothing there). */
    private static final java.util.Map<String, java.util.Optional<BoundingBox>> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

    private BoundingBox shape(int mode) {
        String k = handler + "|" + village + "|" + mode;
        var known = SHAPES.get(k);
        if (known == null) {
            BoundingBox union = null;
            for (StructureComponent c : build(0, PLAN_Y, 0, mode, handler.hashCode(), false).parts()) {
                StructureBoundingBox b = c.getBoundingBox();
                BoundingBox m = new BoundingBox(b.minX, b.minY - PLAN_Y, b.minZ, b.maxX, b.maxY - PLAN_Y, b.maxZ);
                union = union == null ? m : BoundingBox.encapsulatingBoxes(List.of(union, m)).orElse(union);
            }
            known = java.util.Optional.ofNullable(union);
            SHAPES.putIfAbsent(k, known);
        }
        return known.orElse(null);
    }

    public BoundingBox footprint(int mode) {
        return shape(mode);
    }

    public String describeFootprint() {
        BoundingBox b = shape(0);
        if (b == null) for (int m = 1; m < 4 && b == null; m++) b = shape(m);
        return b == null ? "none" : b.getXSpan() + "x" + b.getYSpan() + "x" + b.getZSpan();
    }

    /** Whether the handler builds anything (small components count too: added beside the streets they take no house's place). */
    public boolean isBuilding() {
        for (int mode = 0; mode < 4; mode++) if (shape(mode) != null) return true;
        return false;
    }

    // ------------------------------------------------------------------ pool element

    @Override
    public Vec3i getSize(StructureTemplateManager manager, Rotation rotation) {
        BoundingBox b = shape(VillageAdditions.modeOf(rotation));
        return b == null ? Vec3i.ZERO : new Vec3i(b.getXSpan(), b.getYSpan(), b.getZSpan());
    }

    @Override
    public BoundingBox getBoundingBox(StructureTemplateManager manager, BlockPos pos, Rotation rotation) {
        BoundingBox b = shape(VillageAdditions.modeOf(rotation));
        if (b == null) return new BoundingBox(pos);
        return b.moved(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager manager, BlockPos pos, Rotation rotation,
            RandomSource random) {
        int mode = VillageAdditions.modeOf(rotation);
        BoundingBox b = shape(mode);
        if (b == null) return List.of();
        Direction front = VillageAdditions.front(mode);
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
        int mode = VillageAdditions.modeOf(rotation);
        if (shape(mode) == null) return false;
        Group group = build(offset.getX(), offset.getY() + PLAN_Y, offset.getZ(), mode, handler.hashCode(), true);
        if (group.parts().isEmpty()) return false;
        StructureBoundingBox chunk = new StructureBoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        Random rand = new Random(random.nextLong());
        // planned at PLAN_Y: down to the street the jigsaw put it on (the village's stand-ins too, which components may keep)
        for (StructureComponent c : group.list()) c.getBoundingBox().offset(0, -PLAN_Y, 0);
        int ground = group.parts().get(0).getBoundingBox().minY;
        // legacy code that runs during generation reads and writes through the generation region, never the real level
        World world = WorldBridge1122.wrap(level);
        try {
            for (StructureComponent component : group.parts()) {
                if (component instanceof StructureVillagePieces.Village v) v.mcc$setGroundLevel(ground);
                if (!component.getBoundingBox().intersectsWith(chunk)) continue;
                try {
                    component.addComponentParts(world, rand, chunk);
                } catch (RuntimeException | LinkageError e) {
                    if (REPORTED.incrementAndGet() <= 12) LOG.warn("1.12.2 village building {} failed at {}", component.getClass().getName(), offset, e);
                }
            }
        } finally {
            WorldBridge1122.mcc$releaseRegion();
        }
        return true;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "LegacyVillage1122[" + handler + " in " + village + "]";
    }
}
