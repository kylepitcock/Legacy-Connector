package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingEvent;

public class PlayerEvent extends LivingEvent {
    public final EntityPlayer entityPlayer;

    public PlayerEvent(EntityPlayer player) {
        super(player);
        this.entityPlayer = player;
    }

    /** The player entity was recreated (death, or returning from the End): copy what must survive from {@link #original}. */
    public static class Clone extends PlayerEvent {
        public final EntityPlayer original;
        public final boolean wasDeath;

        public Clone(EntityPlayer newPlayer, EntityPlayer original, boolean wasDeath) {
            super(newPlayer);
            this.original = original;
            this.wasDeath = wasDeath;
        }
    }

    /** How fast a player mines a block: handlers change {@link #newSpeed}. */
    @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
    public static class BreakSpeed extends PlayerEvent {
        public final legacyconnector.v1_7_10.net.minecraft.block.Block block;
        public final int metadata;
        public final float originalSpeed;
        public float newSpeed;
        public final int x;
        public final int y;
        public final int z;

        public BreakSpeed(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata, float original, int x, int y, int z) {
            super(player);
            this.block = block;
            this.metadata = metadata;
            this.originalSpeed = original;
            this.newSpeed = original;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Whether the player can harvest a block (get its drops): handlers change {@link #success}. */
    public static class HarvestCheck extends PlayerEvent {
        public final legacyconnector.v1_7_10.net.minecraft.block.Block block;
        public boolean success;

        public HarvestCheck(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.block.Block block, boolean success) {
            super(player);
            this.block = block;
            this.success = success;
        }
    }

    /** The player starts seeing (tracking) an entity: the server sends it to their client now. */
    public static class StartTracking extends PlayerEvent {
        public final legacyconnector.v1_7_10.net.minecraft.entity.Entity target;

        public StartTracking(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
            super(player);
            this.target = target;
        }
    }

    /** The player stops tracking an entity. */
    public static class StopTracking extends PlayerEvent {
        public final legacyconnector.v1_7_10.net.minecraft.entity.Entity target;

        public StopTracking(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
            super(player);
            this.target = target;
        }
    }

    /** The name shown for a player is being formed: handlers change {@link #displayname}. */
    public static class NameFormat extends PlayerEvent {
        public final String username;
        public String displayname;

        public NameFormat(EntityPlayer player, String username) {
            super(player);
            this.username = username;
            this.displayname = username;
        }
    }

    /**
     * The player's data was read from the playerdata directory: mods keep their own file next to it (Thaumcraft's research,
     * {@code <uuid>.thaum}) and read it here.
     */
    public static class LoadFromFile extends PlayerEvent {
        public final java.io.File playerDirectory;
        public final String playerUUID;

        public LoadFromFile(EntityPlayer player, java.io.File originDirectory, String playerUUID) {
            super(player);
            this.playerDirectory = originDirectory;
            this.playerUUID = playerUUID;
        }

        public java.io.File getPlayerFile(String suffix) {
            if ("dat".equals(suffix)) throw new IllegalArgumentException("The suffix 'dat' is reserved");
            return new java.io.File(playerDirectory, playerUUID + "." + suffix);
        }
    }

    /** The player's data is being written to the playerdata directory: mods save their own file next to it. */
    public static class SaveToFile extends PlayerEvent {
        public final java.io.File playerDirectory;
        public final String playerUUID;

        public SaveToFile(EntityPlayer player, java.io.File originDirectory, String playerUUID) {
            super(player);
            this.playerDirectory = originDirectory;
            this.playerUUID = playerUUID;
        }

        public java.io.File getPlayerFile(String suffix) {
            if ("dat".equals(suffix)) throw new IllegalArgumentException("The suffix 'dat' is reserved");
            return new java.io.File(playerDirectory, playerUUID + "." + suffix);
        }
    }
}
