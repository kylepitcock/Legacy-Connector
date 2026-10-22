package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import java.io.File;
import java.util.Properties;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModMetadata;
import org.apache.logging.log4j.Logger;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710;

public class FMLPreInitializationEvent extends FMLStateEvent {
    private final LegacyModRecord mcc$mod;

    public FMLPreInitializationEvent(LegacyModRecord mod) {
        this.mcc$mod = mod;
    }

    @Override
    public legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.PREINITIALIZED;
    }

    public legacyconnector.v1_7_10.cpw.mods.fml.common.discovery.ASMDataTable getAsmData() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.discovery.ASMDataTable.mcc$get();
    }

    public File getModConfigurationDirectory() {
        return Loader.instance().getConfigDir();
    }

    /** Legacy convention: config/&lt;modid&gt;.cfg (the legacy id, case preserved). */
    public File getSuggestedConfigurationFile() {
        return new File(getModConfigurationDirectory(), mcc$mod.legacyId + ".cfg");
    }

    public ModMetadata getModMetadata() {
        return ((LegacyModContainer1710) mcc$mod.platformData).getMetadata();
    }

    public Logger getModLog() {
        return mcc$mod.logger;
    }

    public File getSourceFile() {
        return mcc$mod.sourceFile().toFile();
    }

    public Properties getVersionProperties() {
        return new Properties();
    }
}
