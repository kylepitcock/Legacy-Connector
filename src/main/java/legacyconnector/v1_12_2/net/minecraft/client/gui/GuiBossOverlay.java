package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** The boss bars on the HUD ({@code ingameGUI.getBossOverlay()}): a view of the game's boss overlay. */
public class GuiBossOverlay extends Gui {
    public static final GuiBossOverlay mcc$INSTANCE = new GuiBossOverlay(null);

    /** Phantom: the shown boss bars by id. */
    @Deprecated
    public Map<UUID, BossInfoClient> mapBossInfos;

    public GuiBossOverlay(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc) {
    }

    private static net.minecraft.client.gui.components.BossHealthOverlay modern() {
        return net.minecraft.client.Minecraft.getInstance().gui.getBossOverlay();
    }

    @SuppressWarnings("unchecked")
    public static Map<UUID, BossInfoClient> mcc$get$mapBossInfos(GuiBossOverlay o) {
        Map<UUID, BossInfoClient> out = new LinkedHashMap<>();
        try {
            var f = net.minecraft.client.gui.components.BossHealthOverlay.class.getDeclaredField("events");
            f.setAccessible(true);
            var events = (Map<UUID, net.minecraft.client.gui.components.LerpingBossEvent>) f.get(modern());
            events.forEach((id, e) -> out.put(id, BossInfoClient.mcc$of(e)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // no client
        }
        return out;
    }

    public boolean shouldPlayEndBossMusic() {
        return modern().shouldPlayMusic();
    }

    public boolean shouldDarkenSky() {
        return modern().shouldDarkenScreen();
    }

    public boolean shouldCreateFog() {
        return modern().shouldCreateWorldFog();
    }

    public void clearBossInfos() {
        modern().reset();
    }
}
