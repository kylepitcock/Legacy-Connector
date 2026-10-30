package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Vanilla 1.12 structure pieces as the modern game's own pieces (their successors): a legacy desert pyramid, igloo, monument,
 * End city or mansion piece holds the modern pieces built for it, builds them chunk by chunk in the legacy generation box, and saves
 * them with the legacy piece (the modern pieces' NBT under "mcc:pieces").
 */
public final class ModernPieces1122 {
    private ModernPieces1122() {}

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Structures");
    private static int failures;

    private static net.minecraft.server.MinecraftServer server() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /** The server's structure templates (null before a server runs). */
    public static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates() {
        var server = server();
        return server == null ? null : server.getStructureManager();
    }

    public static net.minecraft.util.RandomSource random(Random rand) {
        return net.minecraft.util.RandomSource.create(rand == null ? 0L : rand.nextLong());
    }

    public static net.minecraft.world.level.block.Rotation rotation(legacyconnector.v1_12_2.net.minecraft.util.Rotation rotation) {
        if (rotation == null) return net.minecraft.world.level.block.Rotation.NONE;
        return switch (rotation) {
            case CLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
    }

    /** The legacy box covering the pieces (an empty box at the origin when there are none). */
    public static StructureBoundingBox box(List<? extends StructurePiece> pieces) {
        StructureBoundingBox out = null;
        for (StructurePiece p : pieces) {
            var b = p.getBoundingBox();
            var legacy = new StructureBoundingBox(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
            if (out == null) out = legacy;
            else out.expandTo(legacy);
        }
        return out == null ? new StructureBoundingBox(0, 0, 0, 0, 0, 0) : out;
    }

    /** The facing of the first piece, as a legacy facing (null when it has none). */
    public static legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing(List<? extends StructurePiece> pieces) {
        if (pieces.isEmpty() || pieces.get(0).getOrientation() == null) return null;
        return legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(pieces.get(0).getOrientation());
    }

    /** Builds the pieces inside the legacy generation box; false when the world cannot take modern pieces (a detached world). */
    public static boolean place(List<? extends StructurePiece> pieces, World world, Random rand, StructureBoundingBox sbb) {
        var level = ModernFeatures1122.level(world);
        if (level == null || !(world.mcc$level() instanceof net.minecraft.server.level.ServerLevel server) || sbb == null) return false;
        try {
            var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(sbb.minX, sbb.minY, sbb.minZ, sbb.maxX, sbb.maxY, sbb.maxZ);
            var manager = level instanceof net.minecraft.server.level.WorldGenRegion region ? server.structureManager().forWorldGenRegion(region)
                    : server.structureManager();
            var generator = server.getChunkSource().getGenerator();
            var chunk = new net.minecraft.world.level.ChunkPos(box.getCenter());
            var random = random(rand);
            for (StructurePiece p : pieces) {
                if (p.getBoundingBox().intersects(box)) p.postProcess(level, manager, generator, random, box, chunk, box.getCenter());
            }
            return true;
        } catch (RuntimeException e) {
            if (failures++ < 3) LOG.warn("Structure pieces failed in {}: {}", sbb, e.toString());
            return true;
        }
    }

    /** Moves the pieces as the legacy piece moved. */
    public static void offset(List<? extends StructurePiece> pieces, int x, int y, int z) {
        for (StructurePiece p : pieces) p.move(x, y, z);
    }

    public static void save(List<? extends StructurePiece> pieces, NBTTagCompound tag) {
        var server = server();
        if (server == null || pieces.isEmpty()) return;
        try {
            var context = net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(server.overworld());
            NBTTagList list = new NBTTagList();
            for (StructurePiece p : pieces) list.appendTag(new NBTTagCompound(p.createTag(context)));
            tag.setTag("mcc:pieces", list);
        } catch (RuntimeException e) {
            LOG.warn("Could not save structure pieces: {}", e.toString());
        }
    }

    public static List<StructurePiece> load(NBTTagCompound tag) {
        List<StructurePiece> out = new ArrayList<>();
        var server = server();
        if (server == null || tag == null || !tag.hasKey("mcc:pieces", 9)) return out;
        try {
            var context = net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(server.overworld());
            NBTTagList list = tag.getTagList("mcc:pieces", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                var modern = list.getCompoundTagAt(i).mcc$modern();
                var type = net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_PIECE.get(net.minecraft.resources.ResourceLocation.parse(modern.getString("id")));
                if (type != null) out.add(type.load(context, modern));
            }
        } catch (RuntimeException e) {
            LOG.warn("Could not read structure pieces: {}", e.toString());
        }
        return out;
    }

    /** Collects pieces built by a modern piece generator. */
    public static final class Collector implements net.minecraft.world.level.levelgen.structure.StructurePieceAccessor {
        public final List<StructurePiece> pieces = new ArrayList<>();

        @Override
        public void addPiece(StructurePiece piece) {
            pieces.add(piece);
        }

        @Override
        public StructurePiece findCollisionPiece(net.minecraft.world.level.levelgen.structure.BoundingBox box) {
            for (StructurePiece p : pieces) if (p.getBoundingBox().intersects(box)) return p;
            return null;
        }
    }
}
