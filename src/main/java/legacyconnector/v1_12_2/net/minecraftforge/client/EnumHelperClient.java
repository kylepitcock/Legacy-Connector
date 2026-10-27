package legacyconnector.v1_12_2.net.minecraftforge.client;

/** Forge's client enum extensions: new music situations for a mod's own {@code MusicTicker}. */
public class EnumHelperClient extends legacyconnector.v1_12_2.net.minecraftforge.common.util.EnumHelper {
    public static legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType addMusicType(String name,
            legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound, int minDelay, int maxDelay) {
        return new legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType(name, sound, minDelay, maxDelay);
    }
}
