package legacyconnector.vb1_7_3.net.minecraft.sound;

/** Beta 1.7.3 {@code StepSound}: a sound name ("stone", "wood", "cloth", ...) with volume and pitch. */
public class BlockSoundGroup {
    public final String soundName;
    public final float volume;
    public final float pitch;

    public BlockSoundGroup(String soundName, float volume, float pitch) {
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public String getBreakSound() {
        return "step." + soundName;
    }

    public String getSound() {
        return "step." + soundName;
    }

    /** The modern sound type this group sounds like. */
    public net.minecraft.world.level.block.SoundType mcc$modern() {
        String s = getBreakSound();
        if (s.equals("random.glass")) return net.minecraft.world.level.block.SoundType.GLASS;
        return switch (soundName) {
            case "wood" -> net.minecraft.world.level.block.SoundType.WOOD;
            case "gravel" -> net.minecraft.world.level.block.SoundType.GRAVEL;
            case "grass" -> net.minecraft.world.level.block.SoundType.GRASS;
            case "cloth" -> net.minecraft.world.level.block.SoundType.WOOL;
            case "sand" -> net.minecraft.world.level.block.SoundType.SAND;
            default -> pitch > 1.2F ? net.minecraft.world.level.block.SoundType.METAL : net.minecraft.world.level.block.SoundType.STONE;
        };
    }
}
