package legacyconnector.v1_12_2.net.minecraft.util;

/** The lengths of the last 240 frames (or ticks), in nanoseconds, in a ring buffer. */
public class FrameTimer {
    private final long[] frames = new long[240];
    private int lastIndex;
    private int counter;
    private int index;

    public void addFrame(long runningTime) {
        frames[index] = runningTime;
        index++;
        if (index == 240) index = 0;
        if (counter < 240) {
            lastIndex = 0;
            counter++;
        } else {
            lastIndex = parseIndex(index + 1);
        }
    }

    public int getLagometerValue(long time, int multiplier) {
        double d = (double) time / 1.6666666E7D;
        return (int) (d * multiplier);
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
