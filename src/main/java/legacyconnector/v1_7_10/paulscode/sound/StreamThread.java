package legacyconnector.v1_7_10.paulscode.sound;

/**
 * paulscode's streaming thread. Streams play through the modern sound engine, so this one only idles; it stays alive as
 * paulscode's did, because mods check {@code isAlive()} and start a new one when it died (DragonAPI, ChromatiCraft).
 */
public class StreamThread extends Thread {
    public StreamThread() {
        super("Legacy paulscode stream thread");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(60_000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void kill() {
        interrupt();
    }
}
