package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired around building the block atlas. {@link Pre} runs while the client's resources (re)load, before the modern atlas reads its
 * sources, so sprites registered there are stitched; {@link Post} runs once the modern atlas is built and the legacy sprites have
 * their positions.
 */
public class TextureStitchEvent extends Event {
    private final TextureMap map;

    public TextureStitchEvent(TextureMap map) {
        this.map = map;
    }

    public TextureMap getMap() {
        return map;
    }

    public static class Pre extends TextureStitchEvent {
        public Pre(TextureMap map) {
            super(map);
        }
    }

    public static class Post extends TextureStitchEvent {
        public Post(TextureMap map) {
            super(map);
        }
    }
}
