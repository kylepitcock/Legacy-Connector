package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import java.io.File;
import java.util.Properties;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModMetadata;
import org.apache.logging.log4j.Logger;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.v1_12_2.LegacyModContainer1122;

public class FMLPreInitializationEvent extends FMLStateEvent {
    private final LegacyModRecord mcc$mod;
    private File mcc$configDir;

    public FMLPreInitializationEvent(LegacyModRecord mod) {
        this.mcc$mod = mod;
    }

    /** Forge's own constructor ({@code data[0]} the ASM data, {@code data[1]} the config directory): mods build the event to run their initializer by hand (Dimensional Doors). */
    public FMLPreInitializationEvent(Object... data) {
        this.mcc$mod = null;
        if (data != null && data.length > 1 && data[1] instanceof File dir) this.mcc$configDir = dir;
    }

    /** The directory this mod's configuration files go to instead of the shared one (see Platform1122.preInit). */
    public void mcc$useConfigDir(File dir) {
        this.mcc$configDir = dir;
    }

    public boolean mcc$hasConfigDir() {
        return mcc$configDir != null;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.PREINITIALIZED;
    }

    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ASMDataTable getAsmData() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ASMDataTable.mcc$get();
    }

    public File getModConfigurationDirectory() {
        return mcc$configDir != null ? mcc$configDir : Loader.instance().getConfigDir();
    }

    /** Legacy convention: config/&lt;modid&gt;.cfg (the legacy id, case preserved). */
    public File getSuggestedConfigurationFile() {
        return new File(getModConfigurationDirectory(), (mcc$mod == null ? "suggested" : mcc$mod.legacyId) + ".cfg");
    }

    public ModMetadata getModMetadata() {
        return mcc$mod == null ? new ModMetadata() : ((LegacyModContainer1122) mcc$mod.platformData).getMetadata();
    }

    public Logger getModLog() {
        return mcc$mod == null ? org.apache.logging.log4j.LogManager.getLogger("FML") : mcc$mod.logger;
    }

    public File getSourceFile() {
        return mcc$mod == null ? null : mcc$mod.sourceFile().toFile();
    }

    public Properties getVersionProperties() {
        return new Properties();
    }
}
