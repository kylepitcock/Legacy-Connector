package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** A named sound; vanilla ones are wrappers over the modern events. */
public class SoundEvent extends IForgeRegistryEntry.Impl<SoundEvent> {
    private final ResourceLocation soundName;
    /** The modern sound event, when it is a vanilla one. */
    public net.minecraft.sounds.SoundEvent mcc$modern;

    public SoundEvent(ResourceLocation soundName) {
        this.soundName = soundName;
    }

    public ResourceLocation getSoundName() {
        return soundName;
    }
}
