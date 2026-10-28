package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.world.BossInfo;

/** A boss bar the client shows: a live view of the game's boss bar (name, progress, colour, style, flags). */
public class BossInfoClient extends BossInfo {
    private static final java.util.Map<net.minecraft.world.BossEvent, BossInfoClient> VIEWS = new java.util.WeakHashMap<>();
    private final net.minecraft.world.BossEvent modern;

    private BossInfoClient(net.minecraft.world.BossEvent modern) {
        super(modern.getId(), null, Color.values()[modern.getColor().ordinal()], Overlay.values()[modern.getOverlay().ordinal()]);
        this.modern = modern;
    }

    /** The view of one of the game's boss bars (one per bar). */
    public static BossInfoClient mcc$of(net.minecraft.world.BossEvent modern) {
        if (modern == null) return null;
        synchronized (VIEWS) {
            return VIEWS.computeIfAbsent(modern, BossInfoClient::new);
        }
    }

    @Override
    public ITextComponent getName() {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(
                org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.legacyText(modern.getName()));
    }

    @Override
    public float getPercent() {
        return modern.getProgress();
    }

    @Override
    public Color getColor() {
        return Color.values()[modern.getColor().ordinal()];
    }

    @Override
    public Overlay getOverlay() {
        return Overlay.values()[modern.getOverlay().ordinal()];
    }

    @Override
    public boolean shouldDarkenSky() {
        return modern.shouldDarkenScreen();
    }

    @Override
    public boolean shouldPlayEndBossMusic() {
        return modern.shouldPlayBossMusic();
    }

    @Override
    public boolean shouldCreateFog() {
        return modern.shouldCreateWorldFog();
    }
}
