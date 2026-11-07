package legacyconnector.v1_20_1.net.minecraftforge.common.util;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/** Forge 1.20.1's sound type with lazily resolved events (the events are registered after the block); the getters resolve them. */
public class ForgeSoundType extends SoundType {
    private final Supplier<SoundEvent> breakSound;
    private final Supplier<SoundEvent> stepSound;
    private final Supplier<SoundEvent> placeSound;
    private final Supplier<SoundEvent> hitSound;
    private final Supplier<SoundEvent> fallSound;

    public ForgeSoundType(float volumeIn, float pitchIn, Supplier<SoundEvent> breakSoundIn, Supplier<SoundEvent> stepSoundIn, Supplier<SoundEvent> placeSoundIn,
            Supplier<SoundEvent> hitSoundIn, Supplier<SoundEvent> fallSoundIn) {
        super(volumeIn, pitchIn, SoundType.STONE.getBreakSound(), SoundType.STONE.getStepSound(), SoundType.STONE.getPlaceSound(), SoundType.STONE.getHitSound(),
                SoundType.STONE.getFallSound());
        this.breakSound = breakSoundIn;
        this.stepSound = stepSoundIn;
        this.placeSound = placeSoundIn;
        this.hitSound = hitSoundIn;
        this.fallSound = fallSoundIn;
    }

    @Override
    public SoundEvent getBreakSound() {
        return breakSound.get();
    }

    @Override
    public SoundEvent getStepSound() {
        return stepSound.get();
    }

    @Override
    public SoundEvent getPlaceSound() {
        return placeSound.get();
    }

    @Override
    public SoundEvent getHitSound() {
        return hitSound.get();
    }

    @Override
    public SoundEvent getFallSound() {
        return fallSound.get();
    }
}
