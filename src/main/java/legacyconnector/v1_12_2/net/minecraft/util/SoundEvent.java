package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** A named sound; vanilla ones are wrappers over the modern events. */
public class SoundEvent extends IForgeRegistryEntry.Impl<SoundEvent> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<ResourceLocation, SoundEvent> REGISTRY =
            org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.SOUND_EVENTS, "entity.item.pickup");

    private final ResourceLocation soundName;
    /** The modern sound event, when it is a vanilla one. */
    public net.minecraft.sounds.SoundEvent mcc$modern;

    public SoundEvent(ResourceLocation soundName) {
        this.soundName = soundName;
    }

    public ResourceLocation getSoundName() {
        return soundName;
    }

    /**
     * The modern sound event: the vanilla one, else a variable-range event of the same name (mod sounds are not in the modern
     * registry; a direct holder of such an event reaches the client, which finds the sound in the mod's sounds.json).
     */
    public net.minecraft.sounds.SoundEvent mcc$modernOrDirect() {
        if (mcc$modern != null) return mcc$modern;
        var id = soundName == null ? null : soundName.mcc$modern();
        if (id == null) return null;
        var registered = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);
        return registered != null ? registered : net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id);
    }

    public net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> mcc$holder() {
        var modern = mcc$modernOrDirect();
        if (modern == null) return null;
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(modern);
    }
}
