package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

/** 1.12.2 save handler of an Anvil world folder; chunk storage per dimension folder (DIM-1, DIM1, DIMn). */
public class AnvilSaveHandler extends legacyconnector.v1_12_2.net.minecraft.world.storage.SaveHandler {
    public AnvilSaveHandler(java.io.File savesDirectory, String directoryName, boolean storePlayerdata, legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer dataFixer) {
        super(savesDirectory, directoryName, storePlayerdata, dataFixer);
    }

    @Override
    public IChunkLoader getChunkLoader(legacyconnector.v1_12_2.net.minecraft.world.WorldProvider provider) {
        java.io.File dir = getWorldDirectory();
        String folder = provider == null ? null : provider.getSaveFolder();
        if (folder != null) dir = new java.io.File(dir, folder);
        return new AnvilChunkLoader(dir, null);
    }
}
