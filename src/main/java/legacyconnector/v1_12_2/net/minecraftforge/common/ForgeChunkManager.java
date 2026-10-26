package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.io.File;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.config.Configuration;

/** Chunk loading tickets. The layer grants none (chunk loaders using them keep working without keeping chunks loaded). */
public class ForgeChunkManager {
    private static Configuration config;

    public enum Type {
        NORMAL, ENTITY
    }

    public interface LoadingCallback {
        void ticketsLoaded(List<Ticket> tickets, World world);
    }

    public interface OrderedLoadingCallback extends LoadingCallback {
        List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount);
    }

    public static class Ticket {
        private final NBTTagCompound modData = new NBTTagCompound();

        public NBTTagCompound getModData() {
            return modData;
        }

        public Type getType() {
            return Type.NORMAL;
        }

        public void setChunkListDepth(int depth) {
        }

        public int getChunkListDepth() {
            return 0;
        }

        public int getMaxChunkListDepth() {
            return 0;
        }

        public void bindEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        }
    }

    public static synchronized Configuration getConfig() {
        if (config == null) {
            config = new Configuration(new File(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().toFile(), "forgeChunkLoading.cfg"));
        }
        return config;
    }

    /** Re-reads the chunk loading defaults (forgeChunkLoading.cfg, read live by this shim) and saves the file if a value changed. */
    public static void syncConfigDefaults() {
        Configuration c = getConfig();
        c.get("defaults", "maximumTicketCount", 200);
        c.get("defaults", "maximumChunksPerTicket", 25);
        c.get("defaults", "playerTicketCount", 500);
        c.get("defaults", "dormantChunkCacheSize", 0);
        if (c.hasChanged()) c.save();
    }

    public static void setForcedChunkLoadingCallback(Object mod, LoadingCallback callback) {
    }

    public static Ticket requestTicket(Object mod, World world, Type type) {
        return null;
    }

    public static Ticket requestPlayerTicket(Object mod, String player, World world, Type type) {
        return null;
    }

    public static void releaseTicket(Ticket ticket) {
    }

    public static void forceChunk(Ticket ticket, Object chunk) {
    }

    public static void unforceChunk(Ticket ticket, Object chunk) {
    }

    public static void reorderChunk(Ticket ticket, Object chunk) {
    }

    public static int ticketCountAvailableFor(Object mod, World world) {
        return 0;
    }

    public static int getMaxTicketLengthFor(String modid) {
        return 0;
    }
}
