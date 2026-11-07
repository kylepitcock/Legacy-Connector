package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

import java.io.File;
import legacyconnector.v1_12_2.net.minecraft.world.storage.SaveFormatOld;

/** 1.12.2's Anvil saves folder: the type mods cast {@code getActiveAnvilConverter()} to (to read {@code savesDirectory}). */
public class AnvilSaveConverter extends SaveFormatOld {
    public AnvilSaveConverter(File savesDirectoryIn, legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer dataFixerIn) {
        super(savesDirectoryIn, dataFixerIn);
    }
}
