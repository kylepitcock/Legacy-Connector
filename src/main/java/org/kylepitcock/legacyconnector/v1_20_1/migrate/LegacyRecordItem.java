package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

/**
 * Stands in for {@code net.minecraft.world.item.RecordItem} (1.20.1): music discs are a data component ({@code jukebox_playable}) of a plain item in 1.21.1, with
 * the song in a data pack registry. The item registers and stacks like a disc; it carries its sound and length for the mod's own code and does not play in a vanilla
 * jukebox (that needs a {@code jukebox_song} entry in a data pack, which a legacy mod cannot have).
 */
public class LegacyRecordItem extends Item {
    private final int analogOutput;
    private final Supplier<SoundEvent> sound;
    private final int lengthInTicks;

    public LegacyRecordItem(int analogOutput, Supplier<SoundEvent> sound, Item.Properties properties, int lengthInTicks) {
        super(properties.stacksTo(1));
        this.analogOutput = analogOutput;
        this.sound = sound;
        this.lengthInTicks = lengthInTicks;
    }

    public LegacyRecordItem(int analogOutput, SoundEvent sound, Item.Properties properties, int lengthInTicks) {
        this(analogOutput, () -> sound, properties, lengthInTicks);
    }

    public int getAnalogOutput() {
        return analogOutput;
    }

    public SoundEvent getSound() {
        return sound.get();
    }

    public int getLengthInTicks() {
        return lengthInTicks;
    }

    /** The disc's description line ("C418 - 11"). */
    public net.minecraft.network.chat.MutableComponent getDisplayName() {
        return net.minecraft.network.chat.Component.translatable(getDescriptionId() + ".desc");
    }

    public static LegacyRecordItem getBySound(SoundEvent sound) {
        return null;
    }
}
