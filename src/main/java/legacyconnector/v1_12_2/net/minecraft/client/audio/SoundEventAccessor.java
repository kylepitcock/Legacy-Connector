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
        if (modern != null) return modern.getWeight();
        int total = 0;
        for (ISoundEventAccessor<Sound> a : mcc$localAccessors) total += a.getWeight();
        return total;
    }

    @Override
    public Sound cloneEntry() {
        if (modern == null) {
            int total = getWeight();
            if (total > 0) {
                int pick = java.util.concurrent.ThreadLocalRandom.current().nextInt(total);
                for (ISoundEventAccessor<Sound> a : mcc$localAccessors) {
                    pick -= a.getWeight();
                    if (pick < 0) return a.cloneEntry();
                }
            }
            return SoundHandler.MISSING_SOUND;
        }
        return Sound.mcc$of(modern.getSound(net.minecraft.util.RandomSource.create()));
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getSubtitle() {
        return subtitle;
    }

    private final java.util.List<ISoundEventAccessor<Sound>> mcc$localAccessors = new java.util.ArrayList<>();

    /** Phantom: the event's sounds and nested events (the game's sound event's entries, or those added to a mod's own accessor). */
    @Deprecated
    public java.util.List<ISoundEventAccessor<Sound>> accessorList;

    @SuppressWarnings("unchecked")
    public static java.util.List<ISoundEventAccessor<Sound>> mcc$get$accessorList(SoundEventAccessor a) {
        if (a.modern == null) return a.mcc$localAccessors;
        java.util.List<ISoundEventAccessor<Sound>> out = new java.util.ArrayList<>();
        try {
            if (mcc$list == null) {
                var f = net.minecraft.client.sounds.WeighedSoundEvents.class.getDeclaredField("list");
                f.setAccessible(true);
                mcc$list = f;
            }
            for (Object o : (java.util.List<Object>) mcc$list.get(a.modern)) {
                if (o instanceof net.minecraft.client.resources.sounds.Sound s) out.add(Sound.mcc$of(s));
                else if (o instanceof net.minecraft.client.sounds.WeighedSoundEvents w) out.add(new SoundEventAccessor(a.location, w));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // the entries stay unknown
        }
        return out;
    }

    public static void mcc$set$accessorList(SoundEventAccessor a, java.util.List<ISoundEventAccessor<Sound>> value) {
    }

    private static java.lang.reflect.Field mcc$list;

    /** Adds a sound (or nested event) to a mod's own accessor. */
    public void addSound(ISoundEventAccessor<Sound> p_188715_1_) {
        if (modern == null && p_188715_1_ != null) mcc$localAccessors.add(p_188715_1_);
    }
}
