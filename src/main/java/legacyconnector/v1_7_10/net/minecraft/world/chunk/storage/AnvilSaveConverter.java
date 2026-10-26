package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import java.io.File;
import legacyconnector.v1_7_10.net.minecraft.world.storage.SaveFormatOld;

/** The saves folder of Anvil worlds (every world the game opens is one). */
public class AnvilSaveConverter extends SaveFormatOld {
    public AnvilSaveConverter(File savesDirectory) {
        super(savesDirectory);
    }

    @Override
    public String func_154333_a() {
        return "Anvil";
    }

    protected int getSaveVersion() {
        return 19133;
    }
}
