package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.ticket.AABBTicket;
import legacyconnector.v1_12_2.net.minecraftforge.common.ticket.ITicketManager;
import legacyconnector.v1_12_2.net.minecraftforge.common.ticket.SimpleTicket;

/**
 * Regions machines keep farmland wet in (sprinklers): each legacy ticket is registered with NeoForge's farmland water manager through a
 * modern ticket that asks it, so the modern farmland block stays moist where the legacy ticket matches.
 */
public class FarmlandWaterManager {
    private FarmlandWaterManager() {
    }

    /** The modern ticket standing for a legacy one. */
    private static final class ModernTicket extends net.neoforged.neoforge.common.ticket.SimpleTicket<net.minecraft.world.phys.Vec3> {
        private final SimpleTicket<Vec3d> legacy;

        ModernTicket(SimpleTicket<Vec3d> legacy) {
            this.legacy = legacy;
        }

        @Override
        public boolean matches(net.minecraft.world.phys.Vec3 toMatch) {
            return legacy.matches(Vec3d.mcc$of(toMatch));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends SimpleTicket<Vec3d>> T addCustomTicket(World world, T ticket, ChunkPos masterChunk, ChunkPos... additionalChunks) {
        var level = world == null ? null : world.mcc$level();
        if (level == null || level.isClientSide) throw new IllegalArgumentException("Water region is only determined server-side");
        ModernTicket modern = new ModernTicket(ticket);
        net.minecraft.world.level.ChunkPos[] extra = new net.minecraft.world.level.ChunkPos[additionalChunks.length];
        for (int i = 0; i < extra.length; i++) extra[i] = new net.minecraft.world.level.ChunkPos(additionalChunks[i].x, additionalChunks[i].z);
        net.neoforged.neoforge.common.FarmlandWaterManager.addCustomTicket(level, modern,
                new net.minecraft.world.level.ChunkPos(masterChunk.x, masterChunk.z), extra);
        // the legacy ticket's validate/invalidate move the modern one in and out of the modern manager
        ticket.setManager(new ITicketManager<Vec3d>() {
            @Override
            public void add(SimpleTicket<Vec3d> t) {
                modern.validate();
            }

            @Override
            public void remove(SimpleTicket<Vec3d> t) {
                modern.invalidate();
            }
        });
        ticket.validate();
        return ticket;
    }

    /** A ticket for the box: farmland inside it counts as next to water. */
    public static AABBTicket addAABBTicket(World world, AxisAlignedBB aabb) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        int minX = ((int) aabb.minX) >> 4;
        int minZ = ((int) aabb.minZ) >> 4;
        int maxX = ((int) aabb.maxX) >> 4;
        int maxZ = ((int) aabb.maxZ) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) chunks.add(new ChunkPos(x, z));
        }
        ChunkPos master = new ChunkPos(minX, minZ);
        chunks.remove(master);
        return addCustomTicket(world, new AABBTicket(aabb), master, chunks.toArray(new ChunkPos[0]));
    }
}
