package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Forge's screen events: set up (buttons can be added), drawn, background drawn, button pressed, mouse and keyboard input. Fired for the
 * screens of 1.12 mods and, with a stand-in GuiScreen, for the game's own screens (buttons added to those become modern widgets).
 */
public class GuiScreenEvent extends Event {
    private final GuiScreen gui;

    public GuiScreenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public GuiScreen getGui() {
        return gui;
    }

    public static class InitGuiEvent extends GuiScreenEvent {
        private List<GuiButton> buttonList;

        public InitGuiEvent(GuiScreen gui, List<GuiButton> buttonList) {
            super(gui);
            this.buttonList = buttonList;
        }

        public List<GuiButton> getButtonList() {
            return buttonList;
        }

        public void setButtonList(List<GuiButton> buttonList) {
            this.buttonList = buttonList;
        }

        /** Before initGui; cancelling keeps the old buttons. */
        @Cancelable
        public static class Pre extends InitGuiEvent {
            public Pre(GuiScreen gui, List<GuiButton> buttonList) {
                super(gui, buttonList);
            }
        }

        public static class Post extends InitGuiEvent {
            public Post(GuiScreen gui, List<GuiButton> buttonList) {
                super(gui, buttonList);
            }
        }
    }

    public static class DrawScreenEvent extends GuiScreenEvent {
        private final int mouseX;
        private final int mouseY;
        private final float renderPartialTicks;

        public DrawScreenEvent(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
            super(gui);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.renderPartialTicks = renderPartialTicks;
        }

        public int getMouseX() {
            return mouseX;
        }

        public int getMouseY() {
            return mouseY;
        }

        public float getRenderPartialTicks() {
            return renderPartialTicks;
        }

        /** Before the screen draws; cancelling skips its drawing. */
        @Cancelable
        public static class Pre extends DrawScreenEvent {
            public Pre(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
                super(gui, mouseX, mouseY, renderPartialTicks);
            }
        }

        public static class Post extends DrawScreenEvent {
            public Post(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
                super(gui, mouseX, mouseY, renderPartialTicks);
            }
        }
    }

    /** After {@code drawDefaultBackground}: things drawn here are behind the screen's contents. */
    public static class BackgroundDrawnEvent extends GuiScreenEvent {
        private int mouseX;
        private int mouseY;

        public BackgroundDrawnEvent(GuiScreen gui) {
            super(gui);
            var mc = net.minecraft.client.Minecraft.getInstance();
            var window = mc.getWindow();
            this.mouseX = (int) (mc.mouseHandler.xpos() * window.getGuiScaledWidth() / Math.max(1, window.getScreenWidth()));
            this.mouseY = (int) (mc.mouseHandler.ypos() * window.getGuiScaledHeight() / Math.max(1, window.getScreenHeight()));
        }

        public int getMouseX() {
            return mouseX;
        }

        public int getMouseY() {
            return mouseY;
        }
    }

    public static class ActionPerformedEvent extends GuiScreenEvent {
        private GuiButton button;
        private List<GuiButton> buttonList;

        public ActionPerformedEvent(GuiScreen gui, GuiButton button, List<GuiButton> buttonList) {
            super(gui);
            this.button = button;
            this.buttonList = buttonList;
        }

        public GuiButton getButton() {
            return button;
        }

        public void setButton(GuiButton button) {
            this.button = button;
        }

        public List<GuiButton> getButtonList() {
            return buttonList;
        }

        public void setButtonList(List<GuiButton> buttonList) {
            this.buttonList = buttonList;
        }

        /** Before the screen's actionPerformed; cancelling swallows the press. */
        @Cancelable
        public static class Pre extends ActionPerformedEvent {
            public Pre(GuiScreen gui, GuiButton button, List<GuiButton> buttonList) {
                super(gui, button, buttonList);
            }
        }

        public static class Post extends ActionPerformedEvent {
            public Post(GuiScreen gui, GuiButton button, List<GuiButton> buttonList) {
                super(gui, button, buttonList);
            }
        }
    }

    public static class MouseInputEvent extends GuiScreenEvent {
        public MouseInputEvent(GuiScreen gui) {
            super(gui);
        }

        @Cancelable
        public static class Pre extends MouseInputEvent {
            public Pre(GuiScreen gui) {
                super(gui);
            }
        }

        public static class Post extends MouseInputEvent {
            public Post(GuiScreen gui) {
                super(gui);
            }
        }
    }

    public static class KeyboardInputEvent extends GuiScreenEvent {
        public KeyboardInputEvent(GuiScreen gui) {
            super(gui);
        }

        @Cancelable
        public static class Pre extends KeyboardInputEvent {
            public Pre(GuiScreen gui) {
                super(gui);
            }
        }

        public static class Post extends KeyboardInputEvent {
            public Post(GuiScreen gui) {
                super(gui);
            }
        }
    }
}
