package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * An item tooltip is drawn. Pre (cancelling hides it; handlers move it) and Color (background and border colours) are posted from the
 * game's tooltip events; the game has no event after the tooltip is drawn, so PostBackground/PostText are only posted by mods themselves.
 */
public abstract class RenderTooltipEvent extends Event {
    protected final ItemStack stack;
    protected final List<String> lines;
    protected int x;
    protected int y;
    protected FontRenderer fr;

    public RenderTooltipEvent(ItemStack stack, List<String> lines, int x, int y, FontRenderer fr) {
        this.stack = stack;
        this.lines = Collections.unmodifiableList(lines);
        this.x = x;
        this.y = y;
        this.fr = fr;
    }

    public ItemStack getStack() {
        return stack;
    }

    public List<String> getLines() {
        return lines;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public FontRenderer getFontRenderer() {
        return fr;
    }

    @Cancelable
    public static class Pre extends RenderTooltipEvent {
        private int screenWidth;
        private int screenHeight;
        private int maxWidth;

        public Pre(ItemStack stack, List<String> lines, int x, int y, int screenWidth, int screenHeight, int maxWidth, FontRenderer fr) {
            super(stack, lines, x, y, fr);
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.maxWidth = maxWidth;
        }

        public int getScreenWidth() {
            return screenWidth;
        }

        public void setScreenWidth(int screenWidth) {
            this.screenWidth = screenWidth;
        }

        public int getScreenHeight() {
            return screenHeight;
        }

        public void setScreenHeight(int screenHeight) {
            this.screenHeight = screenHeight;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public void setFontRenderer(FontRenderer fr) {
            this.fr = fr;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    protected static class Post extends RenderTooltipEvent {
        private final int width;
        private final int height;

        public Post(ItemStack stack, List<String> textLines, int x, int y, FontRenderer fr, int width, int height) {
            super(stack, textLines, x, y, fr);
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public static class PostBackground extends Post {
        public PostBackground(ItemStack stack, List<String> textLines, int x, int y, FontRenderer fr, int width, int height) {
            super(stack, textLines, x, y, fr, width, height);
        }
    }

    public static class PostText extends Post {
        public PostText(ItemStack stack, List<String> textLines, int x, int y, FontRenderer fr, int width, int height) {
            super(stack, textLines, x, y, fr, width, height);
        }
    }

    public static class Color extends RenderTooltipEvent {
        private final int originalBackground;
        private final int originalBorderStart;
        private final int originalBorderEnd;
        private int background;
        private int borderStart;
        private int borderEnd;

        public Color(ItemStack stack, List<String> textLines, int x, int y, FontRenderer fr, int background, int borderStart, int borderEnd) {
            super(stack, textLines, x, y, fr);
            this.originalBackground = background;
            this.originalBorderStart = borderStart;
            this.originalBorderEnd = borderEnd;
            this.background = background;
            this.borderStart = borderStart;
            this.borderEnd = borderEnd;
        }

        public int getBackground() {
            return background;
        }

        public void setBackground(int background) {
            this.background = background;
        }

        public int getBorderStart() {
            return borderStart;
        }

        public void setBorderStart(int borderStart) {
            this.borderStart = borderStart;
        }

        public int getBorderEnd() {
            return borderEnd;
        }

        public void setBorderEnd(int borderEnd) {
            this.borderEnd = borderEnd;
        }

        public int getOriginalBackground() {
            return originalBackground;
        }

        public int getOriginalBorderStart() {
            return originalBorderStart;
        }

        public int getOriginalBorderEnd() {
            return originalBorderEnd;
        }
    }
}
