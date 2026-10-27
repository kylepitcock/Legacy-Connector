package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** A screen waiting for the statistics the server sends (the statistics screen), with its loading animation strings. */
public interface IProgressMeter {
    String[] LOADING_STRINGS = new String[] {"oooooo", "Oooooo", "oOoooo", "ooOooo", "oooOoo", "ooooOo", "oooooO"};

    void onStatsUpdated();
}
