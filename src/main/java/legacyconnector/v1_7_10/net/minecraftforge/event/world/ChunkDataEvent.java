package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/** A chunk's extra data is read from or written to its save: mods keep their own tags in it. */
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
