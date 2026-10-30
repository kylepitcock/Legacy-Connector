package legacyconnector.v1_12_2.net.minecraft.client.gui;

/**
 * The singleplayer world list; displaying it shows the modern world list with the open screen as its parent. The modern list's
 * legacy stand-in carries a {@link GuiListWorldSelection} mirroring it in {@code selectionList}.
 */
public class GuiWorldSelection extends GuiScreen {
    protected GuiScreen prevScreen;
    protected String title = "Select world";
    private String worldVersTooltip;
    private GuiButton deleteButton;
    private GuiButton selectButton;
    private GuiButton renameButton;
    private GuiButton copyButton;
    private GuiListWorldSelection selectionList;

    public GuiWorldSelection(GuiScreen screenIn) {
        this.prevScreen = screenIn;
    }

    /** The legacy mirror of the world list (set by the modern screen's stand-in). */
    public GuiListWorldSelection mcc$selectionList() {
        return selectionList;
    }

    public void mcc$setSelectionList(GuiListWorldSelection list) {
        this.selectionList = list;
    }

    public void setVersionTooltip(String p_184861_1_) {
        this.worldVersTooltip = p_184861_1_;
    }

    /** Enables the world buttons when a world is selected. */
    public void selectWorld(GuiListWorldSelectionEntry entry) {
        boolean flag = entry != null;
        if (selectButton != null) selectButton.enabled = flag;
        if (deleteButton != null) deleteButton.enabled = flag;
        if (renameButton != null) renameButton.enabled = flag;
        if (copyButton != null) copyButton.enabled = flag;
    }
}
