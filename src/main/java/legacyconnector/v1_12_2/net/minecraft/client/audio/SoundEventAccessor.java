package legacyconnector.v1_12_2.net.minecraft.client.audio;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A sound event of sounds.json ({@code SoundHandler.getAccessor}): picks one of its sounds by weight, and its subtitle. */
public class SoundEventAccessor implements ISoundEventAccessor<Sound> {
    private final ResourceLocation location;
    private final legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent subtitle;
    private final net.minecraft.client.sounds.WeighedSoundEvents modern;

    public SoundEventAccessor(ResourceLocation location, String subtitle) {
        this.location = location;
        this.subtitle = subtitle == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(subtitle);
        this.modern = null;
    }

    private SoundEventAccessor(ResourceLocation location, net.minecraft.client.sounds.WeighedSoundEvents modern) {
        this.location = location;
        var text = modern.getSubtitle();
        this.subtitle = text == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(text.getString());
        this.modern = modern;
    }

    /** The view of the game's sound event (not a 1.12 method). */
    public static SoundEventAccessor mcc$of(ResourceLocation location, net.minecraft.client.sounds.WeighedSoundEvents modern) {
        return modern == null ? null : new SoundEventAccessor(location, modern);
    }

    @Override
    public int getWeight() {
        return modern == null ? 0 : modern.getWeight();
    }

    @Override
    public Sound cloneEntry() {
        if (modern == null) return SoundHandler.MISSING_SOUND;
        return Sound.mcc$of(modern.getSound(net.minecraft.util.RandomSource.create()));
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getSubtitle() {
        return subtitle;
    }
}
