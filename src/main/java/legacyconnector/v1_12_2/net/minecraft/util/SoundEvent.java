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
     * The modern event to play: the vanilla one, or for a mod sound an unregistered event of the mod's (modern) namespace, sent
     * to clients by name (the client plays it from the mod's sounds.json).
     */
    public net.minecraft.sounds.SoundEvent mcc$event() {
        var m = mcc$modern;
        if (m != null) return m;
        ResourceLocation name = getRegistryName() != null ? getRegistryName() : soundName;
        if (name == null) return null;
        var id = net.minecraft.resources.ResourceLocation.tryBuild(org.kylepitcock.legacyconnector.core.Ids.modernModId(name.getNamespace()),
                name.getPath().toLowerCase(java.util.Locale.ROOT));
        if (id == null) return null;
        var registered = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);
        m = registered != null ? registered : net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id);
        mcc$modern = m;
        return m;
    }
}
