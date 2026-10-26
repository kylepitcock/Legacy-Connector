package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

/** One frame of a texture animation: its index in the strip and its time in ticks (-1: the animation's default). */
public class AnimationFrame {
    private final int frameIndex;
    private final int frameTime;

    public AnimationFrame(int index) {
        this(index, -1);
    }

    public AnimationFrame(int index, int time) {
        this.frameIndex = index;
        this.frameTime = time;
    }

    public boolean hasNoTime() {
        return frameTime == -1;
    }

    public int getFrameTime() {
        return frameTime;
    }

    public int getFrameIndex() {
        return frameIndex;
    }
}
