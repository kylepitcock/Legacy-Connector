package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraftforge.client.event.ClientChatEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.ClientChatReceivedEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.FOVUpdateEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.MouseEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.PlaySoundEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * Input, chat, field of view and sound events for 1.12.2 mods. While a legacy key or mouse event is posted its LWJGL 2 state is in
 * {@link InputEvent1122} (handlers read {@code Keyboard.getEventKey()}, {@code Mouse.getEventButton()}).
 */
final class InputEvents1122 {
    private InputEvents1122() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onKey);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onMouseButtonPre);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onMouseButtonPost);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onScroll);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onChatReceived);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onChatSent);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onFov);
        NeoForge.EVENT_BUS.addListener(InputEvents1122::onSound);
    }

    /** FML posted KeyInputEvent for every key change. */
    private static void onKey(net.neoforged.neoforge.client.event.InputEvent.Key e) {
        if (!ScreenEvents1122.wants(InputEvent.KeyInputEvent.class)) return;
        int key = org.kylepitcock.legacyconnector.v1_7_10.client.LegacyKeys.toLegacy(e.getKey());
        boolean outer = InputEvent1122.keyDispatching;
        int ok = InputEvent1122.eventKey;
        boolean os = InputEvent1122.eventKeyState;
        char oc = InputEvent1122.eventCharacter;
        InputEvent1122.keyDispatching = true;
        InputEvent1122.eventKey = key;
        InputEvent1122.eventKeyState = e.getAction() != org.lwjgl.glfw.GLFW.GLFW_RELEASE;
        InputEvent1122.eventCharacter = '\0';
        try {
            ScreenEvents1122.post(new InputEvent.KeyInputEvent());
        } finally {
            InputEvent1122.keyDispatching = outer;
            InputEvent1122.eventKey = ok;
            InputEvent1122.eventKeyState = os;
            InputEvent1122.eventCharacter = oc;
        }
    }

    private static int[] pixel() {
        var mc = Minecraft.getInstance();
        var window = mc.getWindow();
        int x = (int) (mc.mouseHandler.xpos() * window.getWidth() / Math.max(1, window.getScreenWidth()));
        int y = window.getHeight() - (int) (mc.mouseHandler.ypos() * window.getHeight() / Math.max(1, window.getScreenHeight())) - 1;
        return new int[] {x, y};
    }

    private static boolean mouse(int button, boolean pressed, int wheel, Runnable post) {
        boolean outer = InputEvent1122.mouseDispatching;
        int ob = InputEvent1122.eventButton, ow = InputEvent1122.eventDWheel, ox = InputEvent1122.eventX, oy = InputEvent1122.eventY;
        boolean os = InputEvent1122.eventButtonState;
        int[] p = pixel();
        InputEvent1122.mouseDispatching = true;
        InputEvent1122.eventButton = button;
        InputEvent1122.eventButtonState = pressed;
        InputEvent1122.eventDWheel = wheel;
        InputEvent1122.eventX = p[0];
        InputEvent1122.eventY = p[1];
        try {
            post.run();
            return true;
        } finally {
            InputEvent1122.mouseDispatching = outer;
            InputEvent1122.eventButton = ob;
            InputEvent1122.eventButtonState = os;
            InputEvent1122.eventDWheel = ow;
            InputEvent1122.eventX = ox;
            InputEvent1122.eventY = oy;
        }
    }

    /** Forge's MouseEvent while playing (no screen); cancelling it stops the game from handling the click. */
    private static void onMouseButtonPre(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre e) {
        if (Minecraft.getInstance().screen != null || !ScreenEvents1122.wants(MouseEvent.class)) return;
        boolean pressed = e.getAction() != org.lwjgl.glfw.GLFW.GLFW_RELEASE;
        int[] p = pixel();
        MouseEvent legacy = new MouseEvent(p[0], p[1], 0, 0, 0, e.getButton(), pressed, System.nanoTime());
        boolean[] cancelled = {false};
        mouse(e.getButton(), pressed, 0, () -> cancelled[0] = ScreenEvents1122.post(legacy));
        if (cancelled[0]) e.setCanceled(true);
    }

    private static void onMouseButtonPost(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post e) {
        if (!ScreenEvents1122.wants(InputEvent.MouseInputEvent.class)) return;
        mouse(e.getButton(), e.getAction() != org.lwjgl.glfw.GLFW.GLFW_RELEASE, 0, () -> ScreenEvents1122.post(new InputEvent.MouseInputEvent()));
    }

    private static void onScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) {
        int wheel = (int) Math.round(e.getScrollDeltaY() * 120.0D);
        if (wheel == 0) return;
        if (ScreenEvents1122.wants(MouseEvent.class)) {
            int[] p = pixel();
            MouseEvent legacy = new MouseEvent(p[0], p[1], 0, 0, wheel, -1, false, System.nanoTime());
            boolean[] cancelled = {false};
            mouse(-1, false, wheel, () -> cancelled[0] = ScreenEvents1122.post(legacy));
            if (cancelled[0]) {
                e.setCanceled(true);
                return;
            }
        }
        if (ScreenEvents1122.wants(InputEvent.MouseInputEvent.class)) mouse(-1, false, wheel, () -> ScreenEvents1122.post(new InputEvent.MouseInputEvent()));
    }

    private static void onChatReceived(net.neoforged.neoforge.client.event.ClientChatReceivedEvent e) {
        if (!ScreenEvents1122.wants(ClientChatReceivedEvent.class)) return;
        // the message as formatted text (handlers read its text; an unchanged message keeps its modern form, click events included)
        legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent original =
                new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(ClientGui1122.legacyText(e.getMessage()));
        ClientChatReceivedEvent legacy = new ClientChatReceivedEvent(original);
        if (ScreenEvents1122.post(legacy) || legacy.getMessage() == null) {
            e.setCanceled(true);
        } else if (legacy.getMessage() != original) {
            e.setMessage(legacy.getMessage().mcc$modern());
        }
    }

    private static void onChatSent(net.neoforged.neoforge.client.event.ClientChatEvent e) {
        if (!ScreenEvents1122.wants(ClientChatEvent.class)) return;
        ClientChatEvent legacy = new ClientChatEvent(e.getMessage());
        if (ScreenEvents1122.post(legacy)) e.setCanceled(true);
        else e.setMessage(legacy.getMessage());
    }

    private static void onFov(net.neoforged.neoforge.client.event.ComputeFovModifierEvent e) {
        if (!ScreenEvents1122.wants(FOVUpdateEvent.class)) return;
        if (!(EntityBridge1122.wrap(e.getPlayer()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player)) return;
        FOVUpdateEvent legacy = new FOVUpdateEvent(player, e.getNewFovModifier());
        ScreenEvents1122.post(legacy);
        e.setNewFovModifier(legacy.getNewfov());
    }

    private static void onSound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent e) {
        if (e.getSound() == null || !ScreenEvents1122.wants(PlaySoundEvent.class)) return;
        var original = LegacySounds1122.legacy(e.getSound());
        PlaySoundEvent legacy = new PlaySoundEvent(original);
        ScreenEvents1122.post(legacy);
        if (legacy.getResultSound() == null) e.setSound(null);
        else if (legacy.getResultSound() != original) e.setSound(LegacySounds1122.instance(legacy.getResultSound()));
    }
}
