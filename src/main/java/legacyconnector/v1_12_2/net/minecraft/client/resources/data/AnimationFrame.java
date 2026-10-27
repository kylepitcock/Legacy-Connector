package legacyconnector.v1_12_2.net.minecraft.client.resources.data;

/** One listed frame of an animation: its index in the strip and its own time (-1: the animation's default). */
public class AnimationFrame {
    private final int frameIndex;
    private final int frameTime;

    public AnimationFrame(int frameIndex) {
        this(frameIndex, -1);
    }

    public AnimationFrame(int frameIndex, int frameTime) {
        this.frameIndex = frameIndex;
        this.frameTime = frameTime;
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
