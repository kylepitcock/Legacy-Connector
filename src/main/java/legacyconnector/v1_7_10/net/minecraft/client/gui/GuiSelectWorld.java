package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;

/** 1.7.10's world list; displaying it opens the modern world selection screen (see ClientGui). */
public class GuiSelectWorld extends GuiScreen implements GuiYesNoCallback {
    protected GuiScreen field_146632_a;
    protected String field_146628_f = "Select world";
    private boolean field_146634_i;

    public GuiSelectWorld(GuiScreen parent) {
        this.field_146632_a = parent;
    }

    public GuiScreen mcc$parent() {
        return field_146632_a;
    }

    /** The "delete this world?" question for the named world. */
    public static GuiYesNo func_152129_a(GuiYesNoCallback callback, String worldName, int id) {
        String question = I18n.format("selectWorld.deleteQuestion");
        String warning = "'" + worldName + "' " + I18n.format("selectWorld.deleteWarning");
        return new GuiYesNo(callback, question, warning, I18n.format("selectWorld.deleteButton"), I18n.format("gui.cancel"), id);
    }
}
