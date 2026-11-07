package legacyconnector.v1_12_2.vazkii.patchouli.api;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Posted (client side, on the legacy event bus) after the contents of a book were loaded (Patchouli 1.12 API). */
public class BookContentsReloadEvent extends Event {
    public final ResourceLocation book;

    public BookContentsReloadEvent(ResourceLocation book) {
        this.book = book;
    }
}
