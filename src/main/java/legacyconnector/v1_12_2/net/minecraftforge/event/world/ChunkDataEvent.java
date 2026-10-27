package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/**
 * Forge's chunk data events: the chunk's saved data as it is read and written. The compound is the modern chunk tag itself, so what
 * mods store under their own keys is saved with the chunk.
 */
public class ChunkDataEvent extends ChunkEvent {
    private final NBTTagCompound data;

    public ChunkDataEvent(Chunk chunk, NBTTagCompound data) {
        super(chunk);
        this.data = data;
    }

    public NBTTagCompound getData() {
        return data;
    }

    public static class Load extends ChunkDataEvent {
        public Load(Chunk chunk, NBTTagCompound data) {
            super(chunk, data);
        }
    }

    public static class Save extends ChunkDataEvent {
        public Save(Chunk chunk, NBTTagCompound data) {
            super(chunk, data);
        }
    }
}
