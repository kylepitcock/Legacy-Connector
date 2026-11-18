package legacyconnector.v1_7_10.codechicken.nei.forge;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/** NEI 1.4.7 / 1.5.2 keyboard and mouse hook of container GUIs (kept by {@link GuiContainerManager#addInputHandler}; nothing calls it under JEI). */
public interface IContainerInputHandler {
    default boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    default void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {
    }

    default boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyID) {
        return false;
    }

    default boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        return false;
    }

    default void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
    }

    default void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {
    }

    default boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        return false;
    }

    default void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
    }

    default void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {
    }
}
