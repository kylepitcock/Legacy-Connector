package legacyconnector.v1_12_2.net.minecraft.util.datafix;

public class DataFixesManager {
    public static DataFixer createFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }

    public static DataFixer getDataFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }
}
