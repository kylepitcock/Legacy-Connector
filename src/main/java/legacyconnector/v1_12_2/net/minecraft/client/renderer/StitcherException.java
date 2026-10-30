package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.Stitcher;

/** A sprite did not fit the atlas being stitched. */
public class StitcherException extends RuntimeException {
    private final transient Stitcher.Holder holder;

    public StitcherException(Stitcher.Holder holderIn, String message) {
        super(message);
        holder = holderIn;
    }
}
