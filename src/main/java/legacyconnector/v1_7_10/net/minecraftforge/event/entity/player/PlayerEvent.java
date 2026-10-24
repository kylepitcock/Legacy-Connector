package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingEvent;

public class PlayerEvent extends LivingEvent {
    public final EntityPlayer entityPlayer;

    public PlayerEvent(EntityPlayer player) {
        super(player);
        this.entityPlayer = player;
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
}
