package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/** Legacy sound events as modern ones (null stays silent). */
public final class Sounds1122 {
    private Sounds1122() {}

    public static net.minecraft.sounds.SoundEvent modern(SoundEvent sound) {
        return sound == null ? null : sound.mcc$event();
    }

    public static SoundEvent legacy(net.minecraft.sounds.SoundEvent sound) {
        if (sound == null) return null;
        var id = sound.getLocation();
        var found = SoundEvent.REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(id.getNamespace(), id.getPath()));
        if (found != null) return found;
        SoundEvent made = new SoundEvent(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(id.getNamespace(), id.getPath()));
        made.mcc$modern = sound;
        return made;
    }
}
