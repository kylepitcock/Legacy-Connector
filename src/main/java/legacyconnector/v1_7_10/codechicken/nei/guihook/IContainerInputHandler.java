package legacyconnector.v1_7_10.codechicken.nei.guihook;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/** Keyboard and mouse hooks for container GUIs. */
public interface IContainerInputHandler {
    boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode);

    boolean keyTyped(GuiContainer gui, char keyChar, int keyCode);

    void onKeyTyped(GuiContainer gui, char keyChar, int keyID);

    boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button);

    void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button);

    void onMouseUp(GuiContainer gui, int mousex, int mousey, int button);

    boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled);

    void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled);

    void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime);
}
