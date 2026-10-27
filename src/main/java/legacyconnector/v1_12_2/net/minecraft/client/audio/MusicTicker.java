package legacyconnector.v1_12_2.net.minecraft.client.audio;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/**
 * 1.12's background music ({@code Minecraft.getMusicTicker()}). The game's music manager plays the normal music; {@link #playMusic} plays
 * a piece through the sound handler instead (stopping the game's music), and a mod that installs its own ticker gets it updated every
 * client tick while the game's music is held back (see {@code LegacyMusic1122}).
 */
public class MusicTicker {
    private final Random rand = new Random();
    private final Minecraft mc;
    private ISound currentMusic;
    private int timeUntilNextMusic = 100;

    public MusicTicker(Minecraft mc) {
        this.mc = mc;
    }

    /** 1.12's music loop: keeps the music of {@code mc.getAmbientMusicType()} playing with its pauses in between. */
    public void update() {
        MusicType type = mc.getAmbientMusicType();
        if (currentMusic != null) {
            if (!type.getMusicLocation().getSoundName().equals(currentMusic.getSoundLocation())) {
                mc.getSoundHandler().stopSound(currentMusic);
                timeUntilNextMusic = range(0, type.getMinDelay() / 2);
            }
            if (!mc.getSoundHandler().isSoundPlaying(currentMusic)) {
                currentMusic = null;
                timeUntilNextMusic = Math.min(range(type.getMinDelay(), type.getMaxDelay()), timeUntilNextMusic);
            }
        }
        timeUntilNextMusic = Math.min(timeUntilNextMusic, type.getMaxDelay());
        if (currentMusic == null && timeUntilNextMusic-- <= 0) playMusic(type);
    }

    private int range(int min, int max) {
        return min >= max ? min : min + rand.nextInt(max - min + 1);
    }

    /** Plays the type's music now (the game's own music stops). */
    public void playMusic(MusicType type) {
        try {
            net.minecraft.client.Minecraft.getInstance().getMusicManager().stopPlaying();
        } catch (RuntimeException | LinkageError e) {
            // no music manager yet
        }
        currentMusic = PositionedSoundRecord.getMusicRecord(type.getMusicLocation());
        mc.getSoundHandler().playSound(currentMusic);
        timeUntilNextMusic = Integer.MAX_VALUE;
    }

    public void stopMusic() {
        if (currentMusic != null) {
            mc.getSoundHandler().stopSound(currentMusic);
            currentMusic = null;
            timeUntilNextMusic = 0;
        }
    }

    /** Music situations (a class with {@code values()} instead of an enum, so {@code EnumHelperClient.addMusicType} can add some). */
    public static class MusicType {
        private static final java.util.List<MusicType> ALL = new java.util.ArrayList<>();
        public static final MusicType MENU = new MusicType("MENU", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_MENU, 20, 600);
        public static final MusicType GAME = new MusicType("GAME", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_GAME, 12000, 24000);
        public static final MusicType CREATIVE = new MusicType("CREATIVE", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_CREATIVE, 1200, 3600);
        public static final MusicType CREDITS = new MusicType("CREDITS", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_CREDITS,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        public static final MusicType NETHER = new MusicType("NETHER", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_NETHER, 1200, 3600);
        public static final MusicType END_BOSS = new MusicType("END_BOSS", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_DRAGON, 0, 0);
        public static final MusicType END = new MusicType("END", legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.MUSIC_END, 6000, 24000);

        private final String name;
        private final int ordinal;
        private final SoundEvent musicLocation;
        private final int minDelay;
        private final int maxDelay;

        public MusicType(String name, SoundEvent musicLocation, int minDelay, int maxDelay) {
            this.name = name;
            this.musicLocation = musicLocation;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
            synchronized (ALL) {
                this.ordinal = ALL.size();
                ALL.add(this);
            }
        }

        public SoundEvent getMusicLocation() {
            return musicLocation;
        }

        public int getMinDelay() {
            return minDelay;
        }

        public int getMaxDelay() {
            return maxDelay;
        }

        public String name() {
            return name;
        }

        public int ordinal() {
            return ordinal;
        }

        @Override
        public String toString() {
            return name;
        }

        public static MusicType[] values() {
            synchronized (ALL) {
                return ALL.toArray(new MusicType[0]);
            }
        }

        public static MusicType valueOf(String name) {
            for (MusicType t : values()) if (t.name.equals(name)) return t;
            throw new IllegalArgumentException("No enum constant " + name);
        }

        public int compareTo(Object other) {
            return Integer.compare(ordinal, ((MusicType) other).ordinal);
        }
    }
}
