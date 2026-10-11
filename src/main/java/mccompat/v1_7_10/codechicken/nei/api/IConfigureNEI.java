package mccompat.v1_7_10.codechicken.nei.api;

/** Implemented by classes named NEI*Config; their loadConfig() registers handlers and hides items. */
public interface IConfigureNEI {
    void loadConfig();

    String getName();

    String getVersion();
}
