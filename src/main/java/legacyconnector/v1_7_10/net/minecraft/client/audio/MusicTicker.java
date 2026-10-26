package legacyconnector.v1_7_10.net.minecraft.client.audio;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10's background music ({@code Minecraft.mcMusicTicker}). The game's own ticker stays idle: the modern music manager
 * plays the music. A mod that installs its own subclass (space dimensions' music) gets it ticked every client tick, and
 * the modern music is held back while it does (see LegacyMusic).
 */
public class MusicTicker {
    protected final Random rand = new Random();
    protected final Minecraft mc;
    protected ISound currentMusic;
    protected int timeUntilNextMusic = 100;

    public MusicTicker(Minecraft mc) {
        this.mc = mc;
    }

    /** 1.7.10's music loop: keeps the music of {@code mc.getAmbientMusicType()} playing with its pauses in between. */
    public void update() {
        MusicType type = mc.getAmbientMusicType();
        if (currentMusic != null) {
            if (!type.getMusicTickerLocation().equals(currentMusic.getPositionedSoundLocation())) {
                mc.getSoundHandler().stopSound(currentMusic);
                timeUntilNextMusic = range(0, type.getMinDelay() / 2);
            }
            if (!mc.getSoundHandler().isSoundPlaying(currentMusic)) {
                currentMusic = null;
                timeUntilNextMusic = Math.min(range(type.getMinDelay(), type.getMaxDelay()), timeUntilNextMusic);
            }
        }
        if (currentMusic == null && timeUntilNextMusic-- <= 0) {
            currentMusic = PositionedSoundRecord.func_147673_a(type.getMusicTickerLocation());
            mc.getSoundHandler().playSound(currentMusic);
            timeUntilNextMusic = Integer.MAX_VALUE;
        }
    }

    private int range(int min, int max) {
        return min >= max ? min : min + rand.nextInt(max - min + 1);
    }

    public enum MusicType {
        MENU(new ResourceLocation("minecraft:music.menu"), 20, 600),
        GAME(new ResourceLocation("minecraft:music.game"), 12000, 24000),
        CREATIVE(new ResourceLocation("minecraft:music.game.creative"), 1200, 3600),
        CREDITS(new ResourceLocation("minecraft:music.game.end.credits"), Integer.MAX_VALUE, Integer.MAX_VALUE),
        NETHER(new ResourceLocation("minecraft:music.game.nether"), 1200, 3600),
        END_BOSS(new ResourceLocation("minecraft:music.game.end.dragon"), 0, 0),
        END(new ResourceLocation("minecraft:music.game.end"), 6000, 24000);

        private final ResourceLocation field_148645_h;
        private final int minDelay;
        private final int maxDelay;

        MusicType(ResourceLocation location, int minDelay, int maxDelay) {
            this.field_148645_h = location;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
        }

        public ResourceLocation getMusicTickerLocation() {
            return field_148645_h;
        }

        public int getMinDelay() {
            return minDelay;
        }

        public int getMaxDelay() {
            return maxDelay;
        }
    }
}
