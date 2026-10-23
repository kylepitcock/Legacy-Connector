package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import java.util.List;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;

/** Events of a legacy screen's life; {@code gui} is deliberately a plain field (Custom Main Menu swaps it reflectively). */
public class GuiScreenEvent extends Event {
    public GuiScreen gui;

    public GuiScreenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public static class InitGuiEvent extends GuiScreenEvent {
        public List buttonList;

        public InitGuiEvent(GuiScreen gui, List buttonList) {
            super(gui);
            this.buttonList = buttonList;
        }

        @Cancelable
        public static class Pre extends InitGuiEvent {
            public Pre(GuiScreen gui, List buttonList) {
                super(gui, buttonList);
            }
        }

        public static class Post extends InitGuiEvent {
            public Post(GuiScreen gui, List buttonList) {
                super(gui, buttonList);
            }
        }
    }

    public static class DrawScreenEvent extends GuiScreenEvent {
        public final int mouseX;
        public final int mouseY;
        public final float renderPartialTicks;

        public DrawScreenEvent(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
            super(gui);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.renderPartialTicks = renderPartialTicks;
        }

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

    public static class ActionPerformedEvent extends GuiScreenEvent {
        public legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton button;
        public List buttonList;

        public ActionPerformedEvent(GuiScreen gui, legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton button, List buttonList) {
            super(gui);
            this.button = button;
            this.buttonList = buttonList;
        }

        @Cancelable
        public static class Pre extends ActionPerformedEvent {
            public Pre(GuiScreen gui, legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton button, List buttonList) {
                super(gui, button, buttonList);
            }
        }

        public static class Post extends ActionPerformedEvent {
            public Post(GuiScreen gui, legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton button, List buttonList) {
                super(gui, button, buttonList);
            }
        }
    }
}
