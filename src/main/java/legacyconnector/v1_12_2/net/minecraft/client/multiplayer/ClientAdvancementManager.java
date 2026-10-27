package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

/**
 * The client's copy of the advancements ({@code connection.getAdvancementManager()}). The legacy {@code Advancement} classes are not
 * implemented yet (area F), so it holds no entries: the advancement list and selected tab methods are still synthesized.
 */
public class ClientAdvancementManager {
    /** Advancement to its progress (empty until the legacy advancement classes exist). */
    public final java.util.Map<Object, Object> advancementToProgress = new java.util.HashMap<>();

    public ClientAdvancementManager(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc) {
    }
}
