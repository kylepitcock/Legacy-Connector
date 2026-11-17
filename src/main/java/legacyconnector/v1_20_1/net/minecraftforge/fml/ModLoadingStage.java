package legacyconnector.v1_20_1.net.minecraftforge.fml;

/** Forge 1.20.1's {@code ModLoadingStage} (NeoForge 1.21.1 reports loading issues without a stage); mods only pass it to {@link ModLoadingWarning}. */
public enum ModLoadingStage {
    ERROR, VALIDATE, CONSTRUCT, CREATE_REGISTRIES, LOAD_REGISTRIES, COMMON_SETUP, SIDED_SETUP, ENQUEUE_IMC, PROCESS_IMC, COMPLETE, GATHER, DONE
}
