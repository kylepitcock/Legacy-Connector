package legacyconnector.v1_20_1.net.minecraftforge.client.event;

import legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay.ForgeGui;
import legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Forge 1.20.1's overlay registration event (mod bus); posted to each legacy mod from NeoForge's {@code RegisterGuiLayersEvent}, which it forwards to. */
public class RegisterGuiOverlaysEvent extends Event implements IModBusEvent {
    private final RegisterGuiLayersEvent delegate;
    private final String namespace;

    public RegisterGuiOverlaysEvent(RegisterGuiLayersEvent delegate, String namespace) {
        this.delegate = delegate;
        this.namespace = namespace;
    }

    private ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(namespace != null ? namespace : ModLoadingContext.get().getActiveNamespace(), name);
    }

    private static LayeredDraw.Layer layer(IGuiOverlay overlay) {
        return (graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            overlay.render(ForgeGui.mcc$instance(), graphics, delta.getGameTimeDeltaPartialTick(false), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        };
    }

    public void registerBelowAll(String name, IGuiOverlay overlay) {
        delegate.registerBelowAll(id(name), layer(overlay));
    }

    public void registerBelow(ResourceLocation other, String name, IGuiOverlay overlay) {
        delegate.registerBelow(other, id(name), layer(overlay));
    }

    public void registerAbove(ResourceLocation other, String name, IGuiOverlay overlay) {
        delegate.registerAbove(other, id(name), layer(overlay));
    }

    public void registerAboveAll(String name, IGuiOverlay overlay) {
        delegate.registerAboveAll(id(name), layer(overlay));
    }
}
