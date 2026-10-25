package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/** The sounds a block makes; the vanilla ones map to the modern sound types. */
public class SoundType {
    public static final SoundType WOOD = vanilla("WOOD", 1.0F, 1.0F, SoundEvents.BLOCK_WOOD_BREAK, SoundEvents.BLOCK_WOOD_STEP, SoundEvents.BLOCK_WOOD_PLACE, SoundEvents.BLOCK_WOOD_HIT, SoundEvents.BLOCK_WOOD_FALL);
    public static final SoundType GROUND = vanilla("GROUND", 1.0F, 1.0F, SoundEvents.BLOCK_GRAVEL_BREAK, SoundEvents.BLOCK_GRAVEL_STEP, SoundEvents.BLOCK_GRAVEL_PLACE, SoundEvents.BLOCK_GRAVEL_HIT, SoundEvents.BLOCK_GRAVEL_FALL);
    public static final SoundType PLANT = vanilla("PLANT", 1.0F, 1.0F, SoundEvents.BLOCK_GRASS_BREAK, SoundEvents.BLOCK_GRASS_STEP, SoundEvents.BLOCK_GRASS_PLACE, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
    public static final SoundType STONE = vanilla("STONE", 1.0F, 1.0F, SoundEvents.BLOCK_STONE_BREAK, SoundEvents.BLOCK_STONE_STEP, SoundEvents.BLOCK_STONE_PLACE, SoundEvents.BLOCK_STONE_HIT, SoundEvents.BLOCK_STONE_FALL);
    public static final SoundType METAL = vanilla("METAL", 1.0F, 1.5F, SoundEvents.BLOCK_METAL_BREAK, SoundEvents.BLOCK_METAL_STEP, SoundEvents.BLOCK_METAL_PLACE, SoundEvents.BLOCK_METAL_HIT, SoundEvents.BLOCK_METAL_FALL);
    public static final SoundType GLASS = vanilla("GLASS", 1.0F, 1.0F, SoundEvents.BLOCK_GLASS_BREAK, SoundEvents.BLOCK_GLASS_STEP, SoundEvents.BLOCK_GLASS_PLACE, SoundEvents.BLOCK_GLASS_HIT, SoundEvents.BLOCK_GLASS_FALL);
    public static final SoundType CLOTH = vanilla("CLOTH", 1.0F, 1.0F, SoundEvents.BLOCK_CLOTH_BREAK, SoundEvents.BLOCK_CLOTH_STEP, SoundEvents.BLOCK_CLOTH_PLACE, SoundEvents.BLOCK_CLOTH_HIT, SoundEvents.BLOCK_CLOTH_FALL);
    public static final SoundType SAND = vanilla("SAND", 1.0F, 1.0F, SoundEvents.BLOCK_SAND_BREAK, SoundEvents.BLOCK_SAND_STEP, SoundEvents.BLOCK_SAND_PLACE, SoundEvents.BLOCK_SAND_HIT, SoundEvents.BLOCK_SAND_FALL);
    public static final SoundType SNOW = vanilla("SNOW", 1.0F, 1.0F, SoundEvents.BLOCK_SNOW_BREAK, SoundEvents.BLOCK_SNOW_STEP, SoundEvents.BLOCK_SNOW_PLACE, SoundEvents.BLOCK_SNOW_HIT, SoundEvents.BLOCK_SNOW_FALL);
    public static final SoundType LADDER = vanilla("LADDER", 1.0F, 1.0F, SoundEvents.BLOCK_LADDER_BREAK, SoundEvents.BLOCK_LADDER_STEP, SoundEvents.BLOCK_LADDER_PLACE, SoundEvents.BLOCK_LADDER_HIT, SoundEvents.BLOCK_LADDER_FALL);
    public static final SoundType ANVIL = vanilla("ANVIL", 0.3F, 1.0F, SoundEvents.BLOCK_ANVIL_BREAK, SoundEvents.BLOCK_ANVIL_STEP, SoundEvents.BLOCK_ANVIL_PLACE, SoundEvents.BLOCK_ANVIL_HIT, SoundEvents.BLOCK_ANVIL_FALL);
    public static final SoundType SLIME = vanilla("SLIME", 1.0F, 1.0F, SoundEvents.BLOCK_SLIME_BREAK, SoundEvents.BLOCK_SLIME_STEP, SoundEvents.BLOCK_SLIME_PLACE, SoundEvents.BLOCK_SLIME_HIT, SoundEvents.BLOCK_SLIME_FALL);

    public final float volume;
    public final float pitch;
    private final SoundEvent breakSound;
    private final SoundEvent stepSound;
    private final SoundEvent placeSound;
    private final SoundEvent hitSound;
    private final SoundEvent fallSound;
    /** The modern sound type of a vanilla one (null for mod-made sound types). */
    public net.minecraft.world.level.block.SoundType mcc$modern;

    public SoundType(float volume, float pitch, SoundEvent breakSound, SoundEvent stepSound, SoundEvent placeSound, SoundEvent hitSound, SoundEvent fallSound) {
        this.volume = volume;
        this.pitch = pitch;
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    private static SoundType vanilla(String name, float volume, float pitch, SoundEvent b, SoundEvent s, SoundEvent p, SoundEvent h, SoundEvent f) {
        SoundType t = new SoundType(volume, pitch, b, s, p, h, f);
        t.mcc$modern = switch (name) {
            case "WOOD" -> net.minecraft.world.level.block.SoundType.WOOD;
            case "GROUND" -> net.minecraft.world.level.block.SoundType.GRAVEL;
            case "PLANT" -> net.minecraft.world.level.block.SoundType.GRASS;
            case "STONE" -> net.minecraft.world.level.block.SoundType.STONE;
            case "METAL" -> net.minecraft.world.level.block.SoundType.METAL;
            case "GLASS" -> net.minecraft.world.level.block.SoundType.GLASS;
            case "CLOTH" -> net.minecraft.world.level.block.SoundType.WOOL;
            case "SAND" -> net.minecraft.world.level.block.SoundType.SAND;
            case "SNOW" -> net.minecraft.world.level.block.SoundType.SNOW;
            case "LADDER" -> net.minecraft.world.level.block.SoundType.LADDER;
            case "ANVIL" -> net.minecraft.world.level.block.SoundType.ANVIL;
            default -> net.minecraft.world.level.block.SoundType.SLIME_BLOCK;
        };
        return t;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public SoundEvent getBreakSound() {
        return breakSound;
    }

    public SoundEvent getStepSound() {
        return stepSound;
    }

    public SoundEvent getPlaceSound() {
        return placeSound;
    }

    public SoundEvent getHitSound() {
        return hitSound;
    }

    public SoundEvent getFallSound() {
        return fallSound;
    }
}
