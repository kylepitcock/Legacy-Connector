package legacyconnector.v1_12_2.net.minecraft.client.gui.advancements;

/** Whether an advancement is done: picks the frame and line colours of its widget. */
public enum AdvancementState {
    OBTAINED(0),
    UNOBTAINED(1);

    private final int id;

    AdvancementState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** The modern widget state. */
    public net.minecraft.client.gui.screens.advancements.AdvancementWidgetType mcc$modern() {
        return this == OBTAINED ? net.minecraft.client.gui.screens.advancements.AdvancementWidgetType.OBTAINED
                : net.minecraft.client.gui.screens.advancements.AdvancementWidgetType.UNOBTAINED;
    }
}
