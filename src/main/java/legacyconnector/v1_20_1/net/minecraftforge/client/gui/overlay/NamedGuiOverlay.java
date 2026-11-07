package legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay;

import net.minecraft.resources.ResourceLocation;

/** An overlay with its id. {@code id} is the NeoForge 1.21.1 layer id the overlay is registered under. */
public record NamedGuiOverlay(ResourceLocation id, IGuiOverlay overlay) {
}
