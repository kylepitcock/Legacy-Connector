package legacyconnector.v1_12_2.net.minecraft.world.storage;

/**
 * A world's save folder. The members naming chunk loaders and player data are left to the implementing
 * class (calls on the world's handler find them through the synthesized helpers).
 */
public interface ISaveHandler {
    WorldInfo loadWorldInfo();

    void checkSessionLock() throws legacyconnector.v1_12_2.net.minecraft.world.MinecraftException;

    void saveWorldInfoWithPlayer(WorldInfo info, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound player);

    void saveWorldInfo(WorldInfo info);

    void flush();

    java.io.File getWorldDirectory();

    java.io.File getMapFileFromName(String name);

    legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager getStructureTemplateManager();
}
