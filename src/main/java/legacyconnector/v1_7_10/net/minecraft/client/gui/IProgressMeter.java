package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** A screen that waits for data from the server (statistics, achievements) and is told when it has arrived. */
public interface IProgressMeter {
    /** The "searching" animation frames. */
    String[] lanSearchStates = new String[] {"oooooo", "Oooooo", "oOoooo", "ooOooo", "oooOoo", "ooooOo", "oooooO"};

    void doneLoading();
}
