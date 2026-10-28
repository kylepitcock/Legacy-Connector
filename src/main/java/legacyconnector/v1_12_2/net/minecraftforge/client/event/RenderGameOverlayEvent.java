package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import java.util.ArrayList;
import legacyconnector.v1_12_2.net.minecraft.client.gui.ScaledResolution;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * The HUD being drawn: {@link ElementType#ALL} around the whole HUD and Pre/Post around each element (posted from the game's GUI layers).
 * Cancelling a Pre skips the element; handlers draw their own overlays in Post.
 */
public class RenderGameOverlayEvent extends Event {
    private final float partialTicks;
    private final ScaledResolution resolution;
    private final ElementType type;

    public RenderGameOverlayEvent(float partialTicks, ScaledResolution resolution) {
        this.partialTicks = partialTicks;
        this.resolution = resolution;
        this.type = null;
    }

    private RenderGameOverlayEvent(RenderGameOverlayEvent parent, ElementType type) {
        this.partialTicks = parent.getPartialTicks();
        this.resolution = parent.getResolution();
        this.type = type;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public ScaledResolution getResolution() {
        return resolution;
    }

    public ElementType getType() {
        return type;
    }

    public enum ElementType {
        ALL,
        HELMET,
        PORTAL,
        CROSSHAIRS,
        BOSSHEALTH,
        BOSSINFO,
        ARMOR,
        HEALTH,
        FOOD,
        AIR,
        HOTBAR,
        EXPERIENCE,
        TEXT,
        HEALTHMOUNT,
        JUMPBAR,
        CHAT,
        PLAYER_LIST,
        DEBUG,
        POTION_ICONS,
        SUBTITLES,
        FPS_GRAPH,
        VIGNETTE
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

        @Override
        public boolean isCancelable() {
            return false;
        }
    }

    /** One boss bar about to be drawn; handlers change the spacing to the next one. */
    public static class BossInfo extends Pre {
        private final legacyconnector.v1_12_2.net.minecraft.client.gui.BossInfoClient bossInfo;
        private final int x;
        private final int y;
        private int increment;

        public BossInfo(RenderGameOverlayEvent parent, ElementType type, legacyconnector.v1_12_2.net.minecraft.client.gui.BossInfoClient bossInfo, int x, int y,
                int increment) {
            super(parent, type);
            this.bossInfo = bossInfo;
            this.x = x;
            this.y = y;
            this.increment = increment;
        }

        public legacyconnector.v1_12_2.net.minecraft.client.gui.BossInfoClient getBossInfo() {
            return bossInfo;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getIncrement() {
            return increment;
        }

        public void setIncrement(int increment) {
            this.increment = increment;
        }
    }

    /** The HUD text: lines on the left and right (the debug screen's, and what mods add). */
    public static class Text extends Pre {
        private final ArrayList<String> left;
        private final ArrayList<String> right;

        public Text(RenderGameOverlayEvent parent, ArrayList<String> left, ArrayList<String> right) {
            super(parent, ElementType.TEXT);
            this.left = left;
            this.right = right;
        }

        public ArrayList<String> getLeft() {
            return left;
        }

        public ArrayList<String> getRight() {
            return right;
        }
    }

    /** Where the chat is drawn (handlers move it). */
    public static class Chat extends Pre {
        private int posX;
        private int posY;

        public Chat(RenderGameOverlayEvent parent, int posX, int posY) {
            super(parent, ElementType.CHAT);
            this.posX = posX;
            this.posY = posY;
        }

        public int getPosX() {
            return posX;
        }

        public void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        public void setPosY(int posY) {
            this.posY = posY;
        }
    }
}
