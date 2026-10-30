package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** The client world's save handler: a world on a server saves nothing on the client. */
public class SaveHandlerMP implements ISaveHandler {
    public SaveHandlerMP() {
    }

    @Override
    public WorldInfo loadWorldInfo() {
        return null;
    }

    @Override
    public void checkSessionLock() {
    }

    @Override
    public void saveWorldInfoWithPlayer(WorldInfo info, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound player) {
    }

    @Override
    public void saveWorldInfo(WorldInfo info) {
    }

    @Override
    public void flush() {
    }

    @Override
    public java.io.File getWorldDirectory() {
        return null;
    }

    @Override
    public java.io.File getMapFileFromName(String name) {
        return null;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager getStructureTemplateManager() {
        return null;
    }

    @Override
    public IPlayerFileData getPlayerNBTManager() {
        return null;
    }
}
