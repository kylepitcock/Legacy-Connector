package legacyconnector.v1_12_2.codechicken.nei.api;

/** Implemented by classes named NEI*Config; their loadConfig() hides items. */
public interface IConfigureNEI {
    void loadConfig();

    String getName();

    String getVersion();
}
