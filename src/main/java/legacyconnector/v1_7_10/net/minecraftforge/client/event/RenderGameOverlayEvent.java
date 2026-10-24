package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import java.util.ArrayList;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.gui.ScaledResolution;

/** The HUD being drawn: once for the whole HUD ({@code ALL}) and once around each element. Mods draw their own overlays in {@link Post}. */
public class RenderGameOverlayEvent extends Event {
    public enum ElementType {
        ALL, HELMET, PORTAL, CROSSHAIRS, BOSSHEALTH, ARMOR, HEALTH, FOOD, AIR, HOTBAR, EXPERIENCE, TEXT, HEALTHMOUNT, JUMPBAR, CHAT, PLAYER_LIST, DEBUG
    }

    public final float partialTicks;
    public final ScaledResolution resolution;
    public final int mouseX;
    public final int mouseY;
    public final ElementType type;

    public RenderGameOverlayEvent(float partialTicks, ScaledResolution resolution, int mouseX, int mouseY) {
        this.partialTicks = partialTicks;
        this.resolution = resolution;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.type = ElementType.ALL;
    }

    private RenderGameOverlayEvent(RenderGameOverlayEvent parent, ElementType type) {
        this.partialTicks = parent.partialTicks;
        this.resolution = parent.resolution;
        this.mouseX = parent.mouseX;
        this.mouseY = parent.mouseY;
        this.type = type;
    }

    @Cancelable
    public static class Pre extends RenderGameOverlayEvent {
        public Pre(RenderGameOverlayEvent parent, ElementType type) {
            super(parent, type);
        }
    }

    public static class Post extends RenderGameOverlayEvent {
        public Post(RenderGameOverlayEvent parent, ElementType type) {
            super(parent, type);
        }
    }

    /** The debug/HUD text: listeners add lines to {@code left} and {@code right}. */
    public static class Text extends Pre {
        public final ArrayList<String> left;
        public final ArrayList<String> right;

        public Text(RenderGameOverlayEvent parent, ArrayList<String> left, ArrayList<String> right) {
            super(parent, ElementType.TEXT);
            this.left = left;
            this.right = right;
        }
    }

    @Cancelable
    public static class Chat extends Pre {
        public int posX;
        public int posY;

        public Chat(RenderGameOverlayEvent parent, int posX, int posY) {
            super(parent, ElementType.CHAT);
            this.posX = posX;
            this.posY = posY;
        }
    }
}
