package legacyconnector.v1_7_10.paulscode.sound;

/** paulscode's sound library; mods reach its stream thread by reflection (DragonAPI). */
public class Library {
    private StreamThread streamThread;

    public Library() {
        streamThread = new StreamThread();
        streamThread.start();
    }
}
