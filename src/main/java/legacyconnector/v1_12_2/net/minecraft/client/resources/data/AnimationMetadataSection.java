package legacyconnector.v1_12_2.net.minecraft.client.resources.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The "animation" section of a texture's .mcmeta: frame size, default frame time, interpolation and the frame order (each an index into
 * the strip with an optional own time). Without a frame list the strip plays top to bottom.
 */
public class AnimationMetadataSection implements IMetadataSection {
    private final List<AnimationFrame> animationFrames;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameTime;
    private final boolean interpolate;

    public AnimationMetadataSection(List<AnimationFrame> frames, int frameWidth, int frameHeight, int frameTime, boolean interpolate) {
        this.animationFrames = frames;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameTime = frameTime;
        this.interpolate = interpolate;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameCount() {
        return animationFrames.size();
    }

    public int getFrameTime() {
        return frameTime;
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    private AnimationFrame frame(int i) {
        return animationFrames.get(i);
    }

    public int getFrameTimeSingle(int i) {
        AnimationFrame f = frame(i);
        return f.hasNoTime() ? frameTime : f.getFrameTime();
    }

    public boolean frameHasTime(int i) {
        return !frame(i).hasNoTime();
    }

    public int getFrameIndex(int i) {
        return frame(i).getFrameIndex();
    }

    public Set<Integer> getFrameIndexSet() {
        Set<Integer> out = new HashSet<>();
        for (AnimationFrame f : animationFrames) out.add(f.getFrameIndex());
        return out;
    }
}
