package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** 1.12.2 boss bar data (name, progress, colour, overlay, flags); {@link BossInfoServer} shows it to players. */
public abstract class BossInfo {
    private final UUID uniqueId;
    protected ITextComponent name;
    protected float percent;
    protected Color color;
    protected Overlay overlay;
    protected boolean darkenSky;
    protected boolean playEndBossMusic;
    protected boolean createFog;

    public BossInfo(UUID uniqueId, ITextComponent name, Color color, Overlay overlay) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.color = color;
        this.overlay = overlay;
        this.percent = 1.0F;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public ITextComponent getName() {
        return name;
    }

    public void setName(ITextComponent name) {
        this.name = name;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Overlay getOverlay() {
        return overlay;
    }

    public void setOverlay(Overlay overlay) {
        this.overlay = overlay;
    }

    public boolean shouldDarkenSky() {
        return darkenSky;
    }

    public BossInfo setDarkenSky(boolean darkenSky) {
        this.darkenSky = darkenSky;
        return this;
    }

    public boolean shouldPlayEndBossMusic() {
        return playEndBossMusic;
    }

    public BossInfo setPlayEndBossMusic(boolean playEndBossMusic) {
        this.playEndBossMusic = playEndBossMusic;
        return this;
    }

    public BossInfo setCreateFog(boolean createFog) {
        this.createFog = createFog;
        return this;
    }

    public boolean shouldCreateFog() {
        return createFog;
    }

    public enum Color {
        PINK,
        BLUE,
        RED,
        GREEN,
        YELLOW,
        PURPLE,
        WHITE;

        public net.minecraft.world.BossEvent.BossBarColor mcc$modern() {
            return net.minecraft.world.BossEvent.BossBarColor.values()[ordinal()];
        }
    }

    public enum Overlay {
        PROGRESS,
        NOTCHED_6,
        NOTCHED_10,
        NOTCHED_12,
        NOTCHED_20;

        public net.minecraft.world.BossEvent.BossBarOverlay mcc$modern() {
            return net.minecraft.world.BossEvent.BossBarOverlay.values()[ordinal()];
        }
    }
}
