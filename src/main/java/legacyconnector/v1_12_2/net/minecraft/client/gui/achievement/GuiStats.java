package legacyconnector.v1_12_2.net.minecraft.client.gui.achievement;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManager;

/** 1.12.2's statistics screen; displaying it shows the modern statistics screen with the open screen as its parent. */
public class GuiStats extends GuiScreen {
    protected final GuiScreen parentScreen;
    private final StatisticsManager stats;

    public GuiStats(GuiScreen parent, StatisticsManager manager) {
        this.parentScreen = parent;
        this.stats = manager;
    }
}
