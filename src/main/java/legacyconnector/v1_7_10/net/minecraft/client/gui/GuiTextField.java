package legacyconnector.v1_7_10.net.minecraft.client.gui;

/**
 * 1.7.10's single-line text box: text, a caret, focus and the usual editing keys. Key codes are LWJGL 2's
 * (see {@code org.lwjgl.input.Keyboard}); mods feed it through {@link #textboxKeyTyped}.
 */
public class GuiTextField extends Gui {
    private final FontRenderer fontRenderer;
    public int xPosition;
    public int yPosition;
    public int width;
    public int height;
    private String text = "";
    private int maxStringLength = 32;
    private int cursorCounter;
    private boolean enableBackgroundDrawing = true;
    private boolean canLoseFocus = true;
    private boolean isFocused;
    private boolean isEnabled = true;
    private int cursorPosition;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;
    private boolean visible = true;

    public GuiTextField(FontRenderer fontRenderer, int x, int y, int width, int height) {
        this.fontRenderer = fontRenderer;
        this.xPosition = x;
        this.yPosition = y;
        this.width = width;
        this.height = height;
    }

    public void updateCursorCounter() {
        ++cursorCounter;
    }

    public void setText(String text) {
        if (text == null) text = "";
        this.text = text.length() > maxStringLength ? text.substring(0, maxStringLength) : text;
        cursorPosition = this.text.length();
    }

    public String getText() {
        return text;
    }

    public String getSelectedText() {
        return "";
    }

    public void writeText(String s) {
        if (s == null) return;
        StringBuilder b = new StringBuilder(text);
        b.insert(cursorPosition, s);
        String next = b.toString();
        if (next.length() > maxStringLength) return;
        text = next;
        cursorPosition += s.length();
    }

    public void deleteFromCursor(int count) {
        if (text.isEmpty()) return;
        int from = Math.max(0, Math.min(cursorPosition, cursorPosition + count));
        int to = Math.min(text.length(), Math.max(cursorPosition, cursorPosition + count));
        text = text.substring(0, from) + text.substring(to);
        cursorPosition = from;
    }

    public void deleteWords(int count) {
        deleteFromCursor(count);
    }

    public void moveCursorBy(int by) {
        setCursorPosition(cursorPosition + by);
    }

    public void setCursorPosition(int pos) {
        cursorPosition = Math.max(0, Math.min(text.length(), pos));
    }

    public void setCursorPositionZero() {
        cursorPosition = 0;
    }

    public void setCursorPositionEnd() {
        cursorPosition = text.length();
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public boolean textboxKeyTyped(char c, int keyCode) {
        if (!isFocused) return false;
        switch (keyCode) {
            case 14 -> { // backspace
                if (isEnabled) deleteFromCursor(-1);
                return true;
            }
            case 211 -> { // delete
                if (isEnabled) deleteFromCursor(1);
                return true;
            }
            case 203 -> { // left
                moveCursorBy(-1);
                return true;
            }
            case 205 -> { // right
                moveCursorBy(1);
                return true;
            }
            case 199 -> { // home
                setCursorPositionZero();
                return true;
            }
            case 207 -> { // end
                setCursorPositionEnd();
                return true;
            }
            default -> {
                if (c >= ' ' && c != 127 && c != 167) {
                    if (isEnabled) writeText(Character.toString(c));
                    return true;
                }
                return false;
            }
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean inside = mouseX >= xPosition && mouseX < xPosition + width && mouseY >= yPosition && mouseY < yPosition + height;
        if (canLoseFocus) setFocused(inside);
        if (isFocused && inside && button == 0) {
            int relative = mouseX - xPosition - (enableBackgroundDrawing ? 4 : 0);
            String visibleText = fontRenderer.trimStringToWidth(text, width - 8);
            setCursorPosition(fontRenderer.trimStringToWidth(visibleText, Math.max(0, relative)).length());
        }
    }

    public void drawTextBox() {
        if (!visible) return;
        if (enableBackgroundDrawing) {
            drawRect(xPosition - 1, yPosition - 1, xPosition + width + 1, yPosition + height + 1, -6250336);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, -16777216);
        }
        int color = isEnabled ? enabledColor : disabledColor;
        int x = enableBackgroundDrawing ? xPosition + 4 : xPosition;
        int y = enableBackgroundDrawing ? yPosition + (height - 8) / 2 : yPosition;
        String shown = fontRenderer.trimStringToWidth(text, width - 8);
        fontRenderer.drawStringWithShadow(shown, x, y, color);
        if (isFocused && cursorCounter / 6 % 2 == 0) {
            int cx = x + fontRenderer.getStringWidth(shown.substring(0, Math.min(cursorPosition, shown.length())));
            drawRect(cx, y - 1, cx + 1, y + 9, -3092272);
        }
    }

    public void setMaxStringLength(int length) {
        maxStringLength = length;
        if (text.length() > length) text = text.substring(0, length);
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setFocused(boolean focused) {
        if (focused && !isFocused) cursorCounter = 0;
        isFocused = focused;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public void setCanLoseFocus(boolean value) {
        canLoseFocus = value;
    }

    public void setEnableBackgroundDrawing(boolean value) {
        enableBackgroundDrawing = value;
    }

    public boolean getEnableBackgroundDrawing() {
        return enableBackgroundDrawing;
    }

    public void setTextColor(int color) {
        enabledColor = color;
    }

    public void setDisabledTextColour(int color) {
        disabledColor = color;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean getVisible() {
        return visible;
    }

    public int getWidth() {
        return enableBackgroundDrawing ? width - 8 : width;
    }

    public void setSelectionPos(int position) {
    }
}
