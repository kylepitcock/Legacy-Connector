package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair;

/** A chunk starts or stops being sent to a player. */
public class ChunkWatchEvent extends Event {
    public final ChunkCoordIntPair chunk;
    public final EntityPlayerMP player;

    public ChunkWatchEvent(ChunkCoordIntPair chunk, EntityPlayerMP player) {
        this.chunk = chunk;
        this.player = player;
    }

    public static class Watch extends ChunkWatchEvent {
        public Watch(ChunkCoordIntPair chunk, EntityPlayerMP player) {
            super(chunk, player);
        }
    }

    public static class UnWatch extends ChunkWatchEvent {
        public UnWatch(ChunkCoordIntPair chunk, EntityPlayerMP player) {
            super(chunk, player);
        }
    }
}
