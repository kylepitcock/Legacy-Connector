package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.io.File;

/** The save handler of a client world (and of mods' display-only worlds): nothing is loaded or saved. */
public class SaveHandlerMP implements ISaveHandler {
    public SaveHandlerMP() {
    }

    public WorldInfo loadWorldInfo() {
        return null;
    }

    @Override
    public void checkSessionLock() {
    }

    public legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.IChunkLoader getChunkLoader(legacyconnector.v1_7_10.net.minecraft.world.WorldProvider provider) {
        return null;
    }

    public void saveWorldInfoWithPlayer(WorldInfo info, legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound player) {
    }

    public void saveWorldInfo(WorldInfo info) {
    }

    @Override
    public IPlayerFileData getSaveHandler() {
        return null;
    }

    @Override
    public void flush() {
    }

    @Override
    public File getMapFileFromName(String name) {
        return null;
    }

    @Override
    public String getWorldDirectoryName() {
        return "none";
    }

    @Override
    public File getWorldDirectory() {
        return null;
    }
}
