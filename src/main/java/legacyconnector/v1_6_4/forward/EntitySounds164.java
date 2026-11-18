package legacyconnector.v1_6_4.forward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.net.minecraft.client.audio.MovingSound;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.6.4's sounds bound to an entity ({@code SoundManager.playEntitySound}, {@code updateSoundLocation}, {@code stopEntitySound},
 * {@code isEntitySoundPlaying}): the sound is a tickable sound that follows the entity every tick, ends with the entity and can be stopped
 * through it. Volume and pitch can be changed while it plays ({@code setEntitySoundVolume}/{@code Pitch}); the modern engine reads them each tick.
 */
public final class EntitySounds164 {
    private EntitySounds164() {}

    private static final Map<Entity, List<Following>> BOUND = new WeakHashMap<>();

    /** Starts {@code name} at the entity; it follows the entity until it ends, the entity dies or {@link #stop} is called. */
    public static void play(SoundManager manager, String name, Entity entity, float volume, float pitch) {
        if (entity == null || name == null) return;
        Following sound = new Following(new ResourceLocation(name), entity, volume, pitch);
        synchronized (BOUND) {
            List<Following> list = BOUND.computeIfAbsent(entity, e -> new ArrayList<>());
            list.removeIf(Following::finished);
            list.add(sound);
        }
        manager.playSound(sound);
    }

    /** Stops every sound bound to the entity. */
    public static void stop(SoundManager manager, Entity entity) {
        if (entity == null) return;
        List<Following> list;
        synchronized (BOUND) {
            list = BOUND.remove(entity);
        }
        if (list == null) return;
        for (Following sound : list) {
            sound.finish();
            manager.stopSound(sound);
        }
    }

    /** Moves the sounds bound to {@code entity} to the position of {@code at}. */
    public static void move(Entity entity, Entity at) {
        if (entity == null || at == null) return;
        synchronized (BOUND) {
            List<Following> list = BOUND.get(entity);
            if (list == null) return;
            for (Following sound : list) sound.moveTo(at);
        }
    }

    /** Whether a sound bound to the entity is still playing. */
    public static boolean playing(SoundManager manager, Entity entity) {
        if (entity == null) return false;
        synchronized (BOUND) {
            List<Following> list = BOUND.get(entity);
            if (list == null) return false;
            list.removeIf(f -> f.finished() || !manager.isSoundPlaying(f));
            return !list.isEmpty();
        }
    }

    /** Changes the volume of the entity's sounds (the engine picks it up on the next tick). */
    public static void setVolume(Entity entity, float volume) {
        synchronized (BOUND) {
            List<Following> list = entity == null ? null : BOUND.get(entity);
            if (list != null) for (Following sound : list) sound.setVolume(volume);
        }
    }

    /** Changes the pitch of the entity's sounds (the engine picks it up on the next tick). */
    public static void setPitch(Entity entity, float pitch) {
        synchronized (BOUND) {
            List<Following> list = entity == null ? null : BOUND.get(entity);
            if (list != null) for (Following sound : list) sound.setPitch(pitch);
        }
    }

    private static final class Following extends MovingSound {
        private final Entity entity;

        Following(ResourceLocation location, Entity entity, float volume, float pitch) {
            super(location);
            this.entity = entity;
            this.volume = volume;
            this.field_147663_c = pitch;
            moveTo(entity);
        }

        boolean finished() {
            return donePlaying;
        }

        void finish() {
            donePlaying = true;
        }

        void setVolume(float volume) {
            this.volume = volume;
        }

        void setPitch(float pitch) {
            this.field_147663_c = pitch;
        }

        void moveTo(Entity at) {
            this.xPosF = (float) Entity.mcc$get$posX(at);
            this.yPosF = (float) Entity.mcc$get$posY(at);
            this.zPosF = (float) Entity.mcc$get$posZ(at);
        }

        @Override
        public void update() {
            if (Entity.mcc$get$isDead(entity)) {
                donePlaying = true;
                return;
            }
            moveTo(entity);
        }
    }
}
