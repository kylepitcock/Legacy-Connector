package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The "animation" section of a texture's .mcmeta: the frame size, the default frame time and the frame order (each frame
 * an index into the strip with an optional own time). Without a frame list the strip plays top to bottom.
 */
public class AnimationMetadataSection implements IMetadataSection {
    private final List animationFrames;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameTime;

    public AnimationMetadataSection() {
        this(new ArrayList(), -1, -1, 1);
    }

    public AnimationMetadataSection(List frames, int frameWidth, int frameHeight, int frameTime) {
        this.animationFrames = frames;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameTime = frameTime;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    /** The number of frames listed (0 when the strip plays in order). */
    public int getFrameCount() {
        return animationFrames.size();
    }

    public int getFrameTime() {
        return frameTime;
    }

    private AnimationFrame getAnimationFrame(int i) {
        return (AnimationFrame) animationFrames.get(i);
    }

    /** How many ticks the i-th listed frame shows (its own time, else the default). */
    public int getFrameTimeSingle(int i) {
        AnimationFrame frame = getAnimationFrame(i);
        return frame.hasNoTime() ? frameTime : frame.getFrameTime();
    }

    public boolean frameHasTime(int i) {
        return !getAnimationFrame(i).hasNoTime();
    }

    /** The strip index of the i-th listed frame. */
    public int getFrameIndex(int i) {
        return getAnimationFrame(i).getFrameIndex();
    }

    public Set getFrameIndexSet() {
        Set<Integer> set = new HashSet<>();
        for (Object o : animationFrames) set.add(((AnimationFrame) o).getFrameIndex());
        return set;
    }
}
