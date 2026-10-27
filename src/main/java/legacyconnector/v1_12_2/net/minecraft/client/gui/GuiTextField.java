package legacyconnector.v1_12_2.net.minecraft.client.gui;

import com.google.common.base.Predicate;

/**
 * 1.12.2's single-line text box: text, caret, selection, horizontal scrolling, a validator and the usual editing keys (LWJGL 2 key codes,
 * as the hosting screen hands them to {@link #textboxKeyTyped}).
 */
public class GuiTextField extends Gui {
    private final int id;
    private final FontRenderer fontRenderer;
    public int x;
    public int y;
    public int width;
    public int height;
    private String text = "";
    private int maxStringLength = 32;
    private int cursorCounter;
    private boolean enableBackgroundDrawing = true;
    private boolean canLoseFocus = true;
    private boolean isFocused;
    private boolean isEnabled = true;
    private int lineScrollOffset;
    private int cursorPosition;
    private int selectionEnd;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;
    private boolean visible = true;
    private Predicate<String> validator = s -> true;

    public GuiTextField(int componentId, FontRenderer fontRenderer, int x, int y, int width, int height) {
        this.id = componentId;
        this.fontRenderer = fontRenderer != null ? fontRenderer : FontRenderer.mcc$INSTANCE;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateCursorCounter() {
        ++cursorCounter;
    }

    public void setText(String textIn) {
        if (textIn == null) textIn = "";
        if (!validator.apply(textIn)) return;
        this.text = textIn.length() > maxStringLength ? textIn.substring(0, maxStringLength) : textIn;
        setCursorPositionEnd();
    }

    public String getText() {
        return text;
    }

    public String getSelectedText() {
        int i = Math.min(cursorPosition, selectionEnd);
        int j = Math.max(cursorPosition, selectionEnd);
        return text.substring(i, j);
    }

    public void setValidator(Predicate<String> validator) {
        this.validator = validator == null ? s -> true : validator;
    }

    private static String filterAllowedCharacters(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) if (isAllowedCharacter(c)) sb.append(c);
        return sb.toString();
    }

    private static boolean isAllowedCharacter(char c) {
        return c != 167 && c >= ' ' && c != 127;
    }

    public void writeText(String textToWrite) {
        if (textToWrite == null) return;
        String out = "";
        String filtered = filterAllowedCharacters(textToWrite);
        int i = Math.min(cursorPosition, selectionEnd);
        int j = Math.max(cursorPosition, selectionEnd);
        int k = maxStringLength - text.length() - (i - j);
        if (!text.isEmpty()) out = out + text.substring(0, i);
        int l;
        if (k < filtered.length()) {
            out = out + filtered.substring(0, k);
            l = k;
        } else {
            out = out + filtered;
            l = filtered.length();
        }
        if (!text.isEmpty() && j < text.length()) out = out + text.substring(j);
        if (validator.apply(out)) {
            text = out;
            moveCursorBy(i - selectionEnd + l);
        }
    }

    public void deleteWords(int num) {
        if (text.isEmpty()) return;
        if (selectionEnd != cursorPosition) writeText("");
        else deleteFromCursor(getNthWordFromCursor(num) - cursorPosition);
    }

    public void deleteFromCursor(int num) {
        if (text.isEmpty()) return;
        if (selectionEnd != cursorPosition) {
            writeText("");
            return;
        }
        boolean back = num < 0;
        int i = back ? cursorPosition + num : cursorPosition;
        int j = back ? cursorPosition : cursorPosition + num;
        i = Math.max(0, i);
        j = Math.min(text.length(), j);
        String out = text.substring(0, i) + text.substring(j);
        if (validator.apply(out)) {
            text = out;
            if (back) moveCursorBy(num);
        }
    }

    public int getId() {
        return id;
    }

    public int getNthWordFromCursor(int numWords) {
        return getNthWordFromPos(numWords, cursorPosition);
    }

    public int getNthWordFromPos(int n, int pos) {
        return getNthWordFromPosWS(n, pos, true);
    }

    public int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
        int i = pos;
        boolean back = n < 0;
        int count = Math.abs(n);
        for (int k = 0; k < count; ++k) {
            if (!back) {
                int l = text.length();
                i = text.indexOf(' ', i);
                if (i == -1) i = l;
                else while (skipWs && i < l && text.charAt(i) == ' ') ++i;
            } else {
                while (skipWs && i > 0 && text.charAt(i - 1) == ' ') --i;
                while (i > 0 && text.charAt(i - 1) != ' ') --i;
            }
        }
        return i;
    }

    public void moveCursorBy(int num) {
        setCursorPosition(selectionEnd + num);
    }

    public void setCursorPosition(int pos) {
        cursorPosition = Math.max(0, Math.min(text.length(), pos));
        setSelectionPos(cursorPosition);
    }

    public void setCursorPositionZero() {
        setCursorPosition(0);
    }

    public void setCursorPositionEnd() {
        setCursorPosition(text.length());
    }

    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (!isFocused) return false;
        if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            setCursorPositionEnd();
            setSelectionPos(0);
            return true;
        } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            GuiScreen.setClipboardString(getSelectedText());
            return true;
        } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            if (isEnabled) writeText(GuiScreen.getClipboardString());
            return true;
        } else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
            GuiScreen.setClipboardString(getSelectedText());
            if (isEnabled) writeText("");
            return true;
        }
        switch (keyCode) {
            case 14 -> { // backspace
                if (isEnabled) {
                    if (GuiScreen.isCtrlKeyDown()) deleteWords(-1);
                    else deleteFromCursor(-1);
                }
                return true;
            }
            case 199 -> { // home
                if (GuiScreen.isShiftKeyDown()) setSelectionPos(0);
                else setCursorPositionZero();
                return true;
            }
            case 203 -> { // left
                if (GuiScreen.isShiftKeyDown()) setSelectionPos(GuiScreen.isCtrlKeyDown() ? getNthWordFromPos(-1, selectionEnd) : selectionEnd - 1);
                else if (GuiScreen.isCtrlKeyDown()) setCursorPosition(getNthWordFromCursor(-1));
                else moveCursorBy(-1);
                return true;
            }
            case 205 -> { // right
                if (GuiScreen.isShiftKeyDown()) setSelectionPos(GuiScreen.isCtrlKeyDown() ? getNthWordFromPos(1, selectionEnd) : selectionEnd + 1);
                else if (GuiScreen.isCtrlKeyDown()) setCursorPosition(getNthWordFromCursor(1));
                else moveCursorBy(1);
                return true;
            }
            case 207 -> { // end
                if (GuiScreen.isShiftKeyDown()) setSelectionPos(text.length());
                else setCursorPositionEnd();
                return true;
            }
            case 211 -> { // delete
                if (isEnabled) {
                    if (GuiScreen.isCtrlKeyDown()) deleteWords(1);
                    else deleteFromCursor(1);
                }
                return true;
            }
            default -> {
                if (isAllowedCharacter(typedChar)) {
                    if (isEnabled) writeText(Character.toString(typedChar));
                    return true;
                }
                return false;
            }
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean inside = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        if (canLoseFocus) setFocused(inside);
        if (isFocused && inside && mouseButton == 0) {
            int i = mouseX - x;
            if (enableBackgroundDrawing) i -= 4;
            String s = fontRenderer.trimStringToWidth(text.substring(lineScrollOffset), getWidth());
            setCursorPosition(fontRenderer.trimStringToWidth(s, i).length() + lineScrollOffset);
            return true;
        }
        return false;
    }

    public void drawTextBox() {
        if (!getVisible()) return;
        if (getEnableBackgroundDrawing()) {
            drawRect(x - 1, y - 1, x + width + 1, y + height + 1, -6250336);
            drawRect(x, y, x + width, y + height, -16777216);
        }
        int color = isEnabled ? enabledColor : disabledColor;
        int j = cursorPosition - lineScrollOffset;
        int k = selectionEnd - lineScrollOffset;
        String s = fontRenderer.trimStringToWidth(text.substring(Math.min(lineScrollOffset, text.length())), getWidth());
        boolean caretInside = j >= 0 && j <= s.length();
        boolean blink = isFocused && cursorCounter / 6 % 2 == 0 && caretInside;
        int l = enableBackgroundDrawing ? x + 4 : x;
        int i1 = enableBackgroundDrawing ? y + (height - 8) / 2 : y;
        int j1 = l;
        if (k > s.length()) k = s.length();
        if (!s.isEmpty()) {
            String before = caretInside ? s.substring(0, j) : s;
            j1 = fontRenderer.drawStringWithShadow(before, (float) l, (float) i1, color);
        }
        boolean caretAtEnd = cursorPosition < text.length() || text.length() >= maxStringLength;
        int k1 = j1;
        if (!caretInside) k1 = j > 0 ? l + width : l;
        else if (caretAtEnd) {
            k1 = j1 - 1;
            --j1;
        }
        if (!s.isEmpty() && caretInside && j < s.length()) fontRenderer.drawStringWithShadow(s.substring(j), (float) j1, (float) i1, color);
        if (blink) {
            if (caretAtEnd) drawRect(k1, i1 - 1, k1 + 1, i1 + 1 + fontRenderer.FONT_HEIGHT, -3092272);
            else fontRenderer.drawStringWithShadow("_", (float) k1, (float) i1, color);
        }
        if (k != j) {
            int l1 = l + fontRenderer.getStringWidth(s.substring(0, Math.max(0, k)));
            int left = Math.min(k1, l1 - 1);
            int right = Math.max(k1, l1 - 1);
            // the selection, inverted in 1.12 with a colour logic op: drawn as a translucent highlight
            drawRect(left, i1 - 1, right, i1 + 1 + fontRenderer.FONT_HEIGHT, 0x800000FF);
        }
    }

    public void setMaxStringLength(int length) {
        maxStringLength = length;
        if (text.length() > length) text = text.substring(0, length);
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public boolean getEnableBackgroundDrawing() {
        return enableBackgroundDrawing;
    }

    public void setEnableBackgroundDrawing(boolean enableBackgroundDrawingIn) {
        this.enableBackgroundDrawing = enableBackgroundDrawingIn;
    }

    public void setTextColor(int color) {
        this.enabledColor = color;
    }

    public void setDisabledTextColour(int color) {
        this.disabledColor = color;
    }

    public void setFocused(boolean isFocusedIn) {
        if (isFocusedIn && !isFocused) cursorCounter = 0;
        this.isFocused = isFocusedIn;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public int getSelectionEnd() {
        return selectionEnd;
    }

    public int getWidth() {
        return getEnableBackgroundDrawing() ? width - 8 : width;
    }

    /** Moves the other end of the selection, scrolling the visible part of the text so it stays in view. */
    public void setSelectionPos(int position) {
        int i = text.length();
        if (position > i) position = i;
        if (position < 0) position = 0;
        selectionEnd = position;
        if (lineScrollOffset > i) lineScrollOffset = i;
        int j = getWidth();
        String s = fontRenderer.trimStringToWidth(text.substring(lineScrollOffset), j);
        int k = s.length() + lineScrollOffset;
        if (position == lineScrollOffset) lineScrollOffset -= fontRenderer.trimStringToWidth(text, j, true).length();
        if (position > k) lineScrollOffset += position - k;
        else if (position <= lineScrollOffset) lineScrollOffset -= lineScrollOffset - position;
        lineScrollOffset = Math.max(0, Math.min(lineScrollOffset, i));
    }

    public void setCanLoseFocus(boolean canLoseFocusIn) {
        this.canLoseFocus = canLoseFocusIn;
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean isVisible) {
        this.visible = isVisible;
    }
}
