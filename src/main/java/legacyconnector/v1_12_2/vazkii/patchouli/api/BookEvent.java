package legacyconnector.v1_12_2.vazkii.patchouli.api;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Base of the events about one book (Patchouli 1.12 API). */
public abstract class BookEvent extends Event {
    public final ResourceLocation book;

    public BookEvent(ResourceLocation book) {
        this.book = book;
    }
}
