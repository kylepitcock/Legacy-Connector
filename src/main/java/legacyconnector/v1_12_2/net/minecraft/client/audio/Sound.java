package legacyconnector.v1_12_2.net.minecraft.client.audio;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** One sound file of a sound event (sounds.json): its name, volume, pitch, weight, kind and whether it streams. */
public class Sound implements ISoundEventAccessor<Sound> {
    private final ResourceLocation name;
    private final float volume;
    private final float pitch;
    private final int weight;
    private final Type type;
    private final boolean streaming;

    public Sound(String name, float volume, float pitch, int weight, Type type, boolean streaming) {
        this.name = new ResourceLocation(name);
        this.volume = volume;
        this.pitch = pitch;
        this.weight = weight;
        this.type = type;
        this.streaming = streaming;
    }

    /** A legacy view of a modern sound entry (its sampled volume and pitch taken once). */
    public static Sound mcc$of(net.minecraft.client.resources.sounds.Sound modern) {
        var random = net.minecraft.util.RandomSource.create();
        return new Sound(modern.getLocation().toString(), modern.getVolume().sample(random), modern.getPitch().sample(random), modern.getWeight(),
                modern.getType() == net.minecraft.client.resources.sounds.Sound.Type.SOUND_EVENT ? Type.SOUND_EVENT : Type.FILE, modern.shouldStream());
    }

    public ResourceLocation getSoundLocation() {
        return name;
    }

    public ResourceLocation getSoundAsOggLocation() {
        return new ResourceLocation(name.getNamespace(), "sounds/" + name.getPath() + ".ogg");
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public Sound cloneEntry() {
        return this;
    }

    public Type getType() {
        return type;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public enum Type {
        FILE("file"),
        SOUND_EVENT("event");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public static Type getByName(String name) {
            for (Type t : values()) if (t.name.equals(name)) return t;
            return null;
        }
    }
}
