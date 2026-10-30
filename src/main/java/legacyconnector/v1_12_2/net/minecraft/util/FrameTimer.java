package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * 1.12.2's frame time ring (the last 240 frame times in nanoseconds, drawn as the lagometer). The client's ({@code Minecraft.frameTimer})
 * is fed every frame by the layer.
 */
public class FrameTimer {
    private final long[] frames = new long[240];
    private int lastIndex;
    private int counter;
    private int index;

    public synchronized void addFrame(long runningTime) {
        frames[index] = runningTime;
        ++index;
        if (index == 240) index = 0;
        if (counter < 240) {
            ++counter;
        } else {
            lastIndex = parseIndex(lastIndex + 1);
        }
    }

    public int getLagometerValue(long time, int multiplier) {
        return getLagometerValue(time, multiplier, 60);
    }

    public int getLagometerValue(long time, int multiplier, int frameRate) {
        double d0 = (double) time / (double) (1000000000L / (long) frameRate);
        return (int) (d0 * (double) multiplier);
    }

    public int getLastIndex() {
        return lastIndex;
    }

    public int getIndex() {
        return index;
    }

    public int parseIndex(int rawIndex) {
        return rawIndex % 240;
    }

    public long[] getFrames() {
        return frames;
    }
}
