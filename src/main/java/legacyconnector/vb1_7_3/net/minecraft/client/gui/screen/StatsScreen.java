package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen;

/** Original vanilla statistics-screen identity; exact instances use the native statistics view. */
public class StatsScreen extends Screen {
    public final Screen parent;
    public final legacyconnector.vb1_7_3.net.minecraft.stat.PlayerStats stats;
    public StatsScreen(Screen parent,legacyconnector.vb1_7_3.net.minecraft.stat.PlayerStats stats){this.parent=parent;this.stats=stats;}
}
