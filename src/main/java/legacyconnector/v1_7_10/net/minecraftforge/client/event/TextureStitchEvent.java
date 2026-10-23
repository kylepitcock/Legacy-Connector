package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap;

/** Fired around the stitching of a texture map: mods register their block/item icons in {@link Pre}. */
public class TextureStitchEvent extends Event {
    public final TextureMap map;

    public TextureStitchEvent(TextureMap map) {
        this.map = map;
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
