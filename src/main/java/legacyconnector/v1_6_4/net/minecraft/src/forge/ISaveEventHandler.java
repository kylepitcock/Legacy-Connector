package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface ISaveEventHandler {
    void onWorldLoad(legacyconnector.v1_7_10.net.minecraft.world.World world);

    void onWorldSave(legacyconnector.v1_7_10.net.minecraft.world.World world);

    void onChunkLoad(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk);

    void onChunkUnload(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk);

    void onChunkSaveData(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk, legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound data);

    void onChunkLoadData(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk, legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound data);
}
