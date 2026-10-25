package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer;

public class CompoundDataFixer extends DataFixer {
    public static final CompoundDataFixer INSTANCE = new CompoundDataFixer();

    public ModFixs init(String modid, int version) {
        return new ModFixs();
    }
}
