package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** An entry of the tab list: a connected player's name and ping ({@code NetHandlerPlayClient.playerInfoList}). */
public class GuiPlayerInfo {
    public final String name;
    private final String nameinLowerCase;
    public int responseTime;

    public GuiPlayerInfo(String name) {
        this.name = name;
        this.nameinLowerCase = name.toLowerCase(java.util.Locale.ROOT);
    }
}
