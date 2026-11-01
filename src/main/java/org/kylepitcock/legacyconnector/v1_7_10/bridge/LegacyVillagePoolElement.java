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
            BoundingBox.CODEC.listOf().optionalFieldOf("streets", List.of()).forGetter(e -> e.streets),
            BoundingBox.CODEC.listOf().optionalFieldOf("buildings", List.of()).forGetter(e -> e.buildings),
            projectionCodec()).apply(i, LegacyVillagePoolElement::new));

    public static StructurePoolElementType<LegacyVillagePoolElement> TYPE;

    /** The handler: its class and the component class it builds (Witchery registers one handler class per component). */
    private final String handler;
    private final String village;
    /** The village this piece was added to: its streets and other pieces, handed to the component as 1.7.10's component list. */
    private final List<BoundingBox> streets;
    private final List<BoundingBox> buildings;

    public LegacyVillagePoolElement(String handler, String village, StructureTemplatePool.Projection projection) {
        this(handler, village, List.of(), List.of(), projection);
    }

    public LegacyVillagePoolElement(String handler, String village, List<BoundingBox> streets, List<BoundingBox> buildings,
            StructureTemplatePool.Projection projection) {
        super(projection);
        this.handler = handler;
        this.village = village;
        this.streets = List.copyOf(streets);
        this.buildings = List.copyOf(buildings);
    }

    /** This building as added to one village: the same component, knowing the village's streets and pieces. */
    public LegacyVillagePoolElement inVillage(List<BoundingBox> villageStreets, List<BoundingBox> villageBuildings) {
        return new LegacyVillagePoolElement(handler, village, villageStreets, villageBuildings, getProjection());
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

    // ------------------------------------------------------------------ the legacy component

    private VillagerRegistry.IVillageCreationHandler handler() {
        for (var h : VillagerBridge.creationHandlers()) if (key(h).equals(handler)) return h;
        // saved before the key named the component: the handler class alone
        if (handler.indexOf('/') < 0) for (var h : VillagerBridge.creationHandlers()) if (h.getClass().getName().equals(handler)) return h;
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

    /** Pieces a component may add by continuing the layout (1.7.10's pending list), at most. */
    private static final int MAX_PIECES = 64;

    /** A built group: the handler's components, and the whole component list they were built into (with the village's stand-ins). */
    private record Group(List<StructureComponent> parts, List<StructureComponent> list) {}

    /**
     * The handler's component built with its plot corner at (x, y, z), followed by every piece it adds by continuing the layout
     * from its own paths, processed as 1.7.10's village start did. The component list it is built into holds the village around it
     * as 1.7.10's did ({@link #standIns}): components may keep that list (Witchery's town wall hands it to the tile entity that later
     * walls in the village along its paths). Empty when the handler builds nothing.
     */
    private Group build(int x, int y, int z, int mode, long seed, boolean withVillage) {
        var h = handler();
        if (h == null) return new Group(List.of(), List.of());
        try {
            Random rand = new Random(seed);
            StructureVillagePieces.PieceWeight weight = h.getVillagePieceWeight(rand, 0);
            if (weight == null) return new Group(List.of(), List.of());
            StructureVillagePieces.Start start = start(rand);
            List<Object> pieces = new ArrayList<>();
            if (withVillage) pieces.addAll(standIns(start, PLAN_Y));
            int known = pieces.size();
            Object built = h.buildComponent(weight, start, pieces, rand, x, y, z, mode, 1);
            if (!(built instanceof StructureComponent first) || first.getBoundingBox() == null) return new Group(List.of(), List.of());
            pieces.add(first);
            first.buildComponent(start, pieces, rand);
            for (int guard = 0; !start.field_74932_i.isEmpty() && guard < MAX_PIECES; guard++) {
                Object next = start.field_74932_i.remove(0);
                if (next instanceof StructureComponent c) c.buildComponent(start, pieces, rand);
            }
            List<StructureComponent> parts = new ArrayList<>();
            List<StructureComponent> list = new ArrayList<>();
            for (int n = 0; n < pieces.size(); n++) {
                if (!(pieces.get(n) instanceof StructureComponent c) || c.getBoundingBox() == null) continue;
                list.add(c);
                if (n >= known && parts.size() < MAX_PIECES) parts.add(c);
            }
            return new Group(parts, list);
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("Village creation handler {} built nothing: {}", handler, e.toString());
            return new Group(List.of(), List.of());
        }
    }

    /**
     * The modern village as 1.7.10's component list showed it to a village component (at the planning height): every street as a
     * {@code Path}, and every other piece of the village too, as one-block strips along its street, so that code taking the
     * village's extent from its paths (Witchery's town wall: each path widened by 20 blocks along it and 7 across) also covers the
     * modern buildings, which reach deeper than 1.7.10's. The buildings come first and each one's strip at the street last, and the
     * streets after them, so the gates such code leaves at the far ends of a path stay at the ends of the streets.
     */
    private List<StructureComponent> standIns(StructureVillagePieces.Start start, int dy) {
        List<StructureComponent> out = new ArrayList<>();
        Random none = new Random(0);
        for (BoundingBox b : buildings) {
            BoundingBox street = nearest(b);
            if (street == null) continue;
            boolean alongX = street.getXSpan() >= street.getZSpan();
            // a strip is one block deep and runs along the street; a piece narrower than 2 along it is no building (a lamp)
            if ((alongX ? b.getXSpan() : b.getZSpan()) < 2) continue;
            boolean streetLow = alongX ? street.minZ() + street.maxZ() < b.minZ() + b.maxZ() : street.minX() + street.maxX() < b.minX() + b.maxX();
            int from = alongX ? b.minZ() : b.minX(), to = alongX ? b.maxZ() : b.maxX();
            for (int n = 0; n <= to - from; n++) {
                int at = streetLow ? to - n : from + n; // the strip at the street last
                StructureBoundingBox strip = alongX
                        ? new StructureBoundingBox(b.minX(), b.minY() + dy, at, b.maxX(), b.maxY() + dy, at)
                        : new StructureBoundingBox(at, b.minY() + dy, b.minZ(), at, b.maxY() + dy, b.maxZ());
                out.add(new StructureVillagePieces.Path(start, 0, none, strip, alongX ? 0 : 1));
            }
        }
        for (BoundingBox s : streets) {
            StructureBoundingBox box = new StructureBoundingBox(s.minX(), s.minY() + dy, s.minZ(), s.maxX(), s.maxY() + dy, s.maxZ());
            out.add(new StructureVillagePieces.Path(start, 0, none, box, s.getXSpan() >= s.getZSpan() ? 0 : 1));
        }
        return out;
    }

    /** The street nearest to a box (by the gap between them), or null when the village has none. */
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
            // not computeIfAbsent: building the component may load classes that plan other shapes
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

    /** The footprint of the building relative to its plot corner in a 1.7.10 mode (coordBaseMode), or null when it builds nothing there. */
    public BoundingBox footprint(int mode) {
        return shape(mode);
    }

    /** The footprint (x by y by z) facing north, for the log. */
    public String describeFootprint() {
        BoundingBox b = shape(0);
        if (b == null) for (int m = 1; m < 4 && b == null; m++) b = shape(m);
        return b == null ? "none" : b.getXSpan() + "x" + b.getYSpan() + "x" + b.getZSpan();
    }

    /**
     * Whether the handler builds anything. Small components count too: added beside the streets they take no house's place, and
     * some are village-wide machinery rather than buildings (Witchery's 2x2 town wall post holds the block that later walls the village).
     */
    public boolean isBuilding() {
        for (int mode = 0; mode < 4; mode++) if (shape(mode) != null) return true;
        return false;
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
        if (shape(mode) == null) return false;
        Group group = build(offset.getX(), offset.getY() + PLAN_Y, offset.getZ(), mode, handler.hashCode(), true);
        if (group.parts().isEmpty()) return false;
        StructureBoundingBox chunk = new StructureBoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        World world = World.mcc$wrap(level);
        Random rand = new Random(random.nextLong());
        // planned at PLAN_Y: down to the street the jigsaw put it on (the village's stand-ins too, which components may keep)
        for (StructureComponent c : group.list()) c.getBoundingBox().offset(0, -PLAN_Y, 0);
        int ground = group.parts().get(0).getBoundingBox().minY;
        for (StructureComponent component : group.parts()) {
            // ground level already found (no 1.7.10 averaging)
            if (component instanceof StructureVillagePieces.Village v) v.mcc$setGroundLevel(ground);
            if (!component.getBoundingBox().intersectsWith(chunk)) continue;
            try {
                component.addComponentParts(world, rand, chunk);
            } catch (RuntimeException | LinkageError e) {
                if (REPORTED.incrementAndGet() <= 12) LOG.warn("Legacy village building {} failed at {}", component.getClass().getName(), offset, e);
            }
        }
        return true;
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
