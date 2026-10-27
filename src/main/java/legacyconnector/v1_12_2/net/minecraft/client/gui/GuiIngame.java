package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;

/** The in-game HUD ({@code Minecraft.ingameGUI}) as a view over the modern one: chat, action bar and titles. */
public class GuiIngame extends Gui {
    public static final GuiIngame mcc$INSTANCE = new GuiIngame(null);

    public GuiIngame(Minecraft mcIn) {
    }

    // ------------------------------------------------------------------ 1.12 fields over the modern HUD (phantoms)

    /** Phantom: the client. */
    @Deprecated
    protected Minecraft mc;
    protected final java.util.Random rand = new java.util.Random();
    /** Phantom: HUD ticks since the world was joined. */
    @Deprecated
    protected int updateCounter;
    /** Phantom: the action bar text. */
    @Deprecated
    protected String overlayMessage;
    @Deprecated
    protected int overlayMessageTime;
    @Deprecated
    protected boolean animateOverlayMessageColor;
    /** Phantom: ticks the held item's name stays shown. */
    @Deprecated
    protected int remainingHighlightTicks;
    @Deprecated
    protected int playerHealth;
    @Deprecated
    protected int lastPlayerHealth;
    @Deprecated
    protected long lastSystemTime;
    @Deprecated
    protected long healthUpdateCounter;
    /** Phantom: the tab list. */
    @Deprecated
    protected GuiPlayerTabOverlay overlayPlayerList;

    private static Class<?> hudClass() {
        return net.minecraft.client.gui.Gui.class;
    }

    private static int intField(String name) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.getInt(hudClass(), hud(), name);
    }

    private static void setField(String name, Object value) {
        org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.set(hudClass(), hud(), name, value);
    }

    public static Minecraft mcc$get$mc(GuiIngame g) {
        return Minecraft.getMinecraft();
    }

    public static int mcc$get$updateCounter(GuiIngame g) {
        return hud().getGuiTicks();
    }

    public static String mcc$get$overlayMessage(GuiIngame g) {
        Object c = org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.get(hudClass(), hud(), "overlayMessageString");
        return c instanceof net.minecraft.network.chat.Component comp ? comp.getString() : "";
    }

    public static void mcc$set$overlayMessage(GuiIngame g, String message) {
        setField("overlayMessageString", message == null ? null : net.minecraft.network.chat.Component.literal(message));
    }

    public static int mcc$get$overlayMessageTime(GuiIngame g) {
        return intField("overlayMessageTime");
    }

    public static void mcc$set$overlayMessageTime(GuiIngame g, int value) {
        setField("overlayMessageTime", value);
    }

    public static boolean mcc$get$animateOverlayMessageColor(GuiIngame g) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.getBoolean(hudClass(), hud(), "animateOverlayMessageColor");
    }

    public static void mcc$set$animateOverlayMessageColor(GuiIngame g, boolean value) {
        setField("animateOverlayMessageColor", value);
    }

    public static int mcc$get$remainingHighlightTicks(GuiIngame g) {
        return intField("toolHighlightTimer");
    }

    public static void mcc$set$remainingHighlightTicks(GuiIngame g, int value) {
        setField("toolHighlightTimer", value);
    }

    public static int mcc$get$playerHealth(GuiIngame g) {
        return intField("lastHealth");
    }

    public static void mcc$set$playerHealth(GuiIngame g, int value) {
        setField("lastHealth", value);
    }

    public static int mcc$get$lastPlayerHealth(GuiIngame g) {
        return intField("displayHealth");
    }

    public static void mcc$set$lastPlayerHealth(GuiIngame g, int value) {
        setField("displayHealth", value);
    }

    public static long mcc$get$lastSystemTime(GuiIngame g) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.getLong(hudClass(), hud(), "lastHealthTime");
    }

    public static void mcc$set$lastSystemTime(GuiIngame g, long value) {
        setField("lastHealthTime", value);
    }

    /** 1.12 counted the heart blink in ticks of the HUD; the modern HUD keeps the tick the blinking ends. */
    public static long mcc$get$healthUpdateCounter(GuiIngame g) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.getLong(hudClass(), hud(), "healthBlinkTime");
    }

    public static void mcc$set$healthUpdateCounter(GuiIngame g, long value) {
        setField("healthBlinkTime", value);
    }

    public static GuiPlayerTabOverlay mcc$get$overlayPlayerList(GuiIngame g) {
        return GuiPlayerTabOverlay.mcc$INSTANCE;
    }

    public GuiPlayerTabOverlay getTabList() {
        return GuiPlayerTabOverlay.mcc$INSTANCE;
    }

    /** A chat message of the given kind: game info goes to the action bar, the rest to the chat. */
    public void addChatMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ChatType chatTypeIn, legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent message) {
        if (message == null) return;
        if (chatTypeIn == legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.GAME_INFO) hud().setOverlayMessage(message.mcc$modern(), false);
        else hud().getChat().addMessage(message.mcc$modern());
    }

    private static net.minecraft.client.gui.Gui hud() {
        return net.minecraft.client.Minecraft.getInstance().gui;
    }

    public GuiNewChat getChatGUI() {
        return GuiNewChat.mcc$INSTANCE;
    }

    public int getUpdateCounter() {
        return hud().getGuiTicks();
    }

    public FontRenderer getFontRenderer() {
        return FontRenderer.mcc$INSTANCE;
    }

    /** The action bar text ({@code animateColor}: the jukebox's rainbow). */
    public void setOverlayMessage(String message, boolean animateColor) {
        hud().setOverlayMessage(net.minecraft.network.chat.Component.literal(message), animateColor);
    }

    public void setOverlayMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component, boolean animateColor) {
        if (component != null) hud().setOverlayMessage(component.mcc$modern(), animateColor);
    }

    public void setRecordPlayingMessage(String recordName) {
        hud().setNowPlaying(net.minecraft.network.chat.Component.literal(recordName));
    }

    public void displayTitle(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut) {
        var h = hud();
        if (title == null && subTitle == null && timeFadeIn < 0 && displayTime < 0 && timeFadeOut < 0) {
            h.clear();
            return;
        }
        if (title != null) h.setTitle(net.minecraft.network.chat.Component.literal(title));
        if (subTitle != null) h.setSubtitle(net.minecraft.network.chat.Component.literal(subTitle));
        if (timeFadeIn >= 0 || displayTime >= 0 || timeFadeOut >= 0) h.setTimes(timeFadeIn, displayTime, timeFadeOut);
    }
}
