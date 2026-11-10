package legacyconnector.vb1_7_3.net.minecraft.world.chunk;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.LoadingDisplay;
/** Beta chunk generation and population contract. */
public interface ChunkSource {
 boolean isChunkLoaded(int x,int z);
 Chunk getChunk(int x,int z);
 Chunk loadChunk(int x,int z);
 void decorate(ChunkSource source,int x,int z);
 boolean save(boolean force,LoadingDisplay display);
 boolean tick();
 boolean canSave();
 String getDebugInfo();
}
