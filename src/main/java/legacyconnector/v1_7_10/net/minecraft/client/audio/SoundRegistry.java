package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.util.RegistrySimple;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10's sound events by name ({@code SoundHandler.sndRegistry}). The game's own events are not held here (the modern sound manager has
 * them, and {@link SoundHandler#getSound} asks it); this holds the events legacy code registers itself.
 */
public class SoundRegistry extends RegistrySimple {
    public void registerSound(SoundEventAccessorComposite composite) {
        putObject(composite.getSoundEventLocation(), composite);
    }

    public Object getObject(ResourceLocation location) {
        return super.getObject(location);
    }

    public boolean containsKey(ResourceLocation location) {
        return super.containsKey(location);
    }

    public void clearMap() {
        registryObjects.clear();
    }
}
