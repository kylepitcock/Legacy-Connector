package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Development remote control of a running (headless) client: command files {@code <dir>/in/<id>.cmd} are executed one
 * per tick and answered in {@code <dir>/out/<id>.txt}; screenshots go to {@code <dir>/out/<name>.png}. Lets a developer
 * drive one long-lived game (run commands, open GUIs, move the mouse, click, press keys, look at screenshots) without
 * restarting it. Enabled by the {@code control} screenshot scenario; see tools/dev/session.sh.
 *
 * <pre>
 * cmd &lt;slash command&gt;   shot &lt;name&gt;   wait &lt;ticks&gt;   look &lt;yaw&gt; &lt;pitch&gt;   use   useat &lt;x&gt; &lt;y&gt; &lt;z&gt;   mouse &lt;x&gt; &lt;y&gt;   click &lt;x&gt; &lt;y&gt; [button]
 * key &lt;glfw code&gt;   type &lt;text&gt;   close   title   info   quit        (x, y are GUI-scaled coordinates)
 * js &lt;JavaScript&gt;   (the rest of the file, several lines allowed; see DevScript)
 * walk forward|back|left|right [ticks] [sprint]   jump [ticks]   sneak [ticks]   attack [ticks]   slot &lt;0-8&gt;   (play like a player)
 * </pre>
 */
public final class RemoteControl {
    private final File in;
    private final File out;
    private String current;      // id of the command in progress
    private int waitTicks;
    private String pendingShot;
    private int shotDelay;

    public RemoteControl(File dir) {
        this.in = new File(dir, "in");
        this.out = new File(dir, "out");
        in.mkdirs();
        out.mkdirs();
    }

    /** Called every client tick. */
    public void tick(Minecraft mc) {
        if (current != null) {
            if (pendingShot != null) {
                if (--shotDelay > 0) return;
                try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(new File(out, pendingShot + ".png"));
                    finish("saved " + new File(out, pendingShot + ".png").getName());
                } catch (Exception e) {
                    finish("screenshot failed: " + e);
                }
                pendingShot = null;
                return;
            }
            if (--waitTicks > 0) return;
            finish(later != null ? later : "ok");
            later = null;
            return;
        }
        File[] files = in.listFiles((d, n) -> n.endsWith(".cmd"));
        if (files == null || files.length == 0) return;
        Arrays.sort(files);
        File f = files[0];
        String line;
        try {
            line = Files.readString(f.toPath()).trim();
        } catch (IOException e) {
            return;
        }
        f.delete();
        current = f.getName().replace(".cmd", "");
        waitTicks = 1;
        try {
            run(mc, line);
        } catch (RuntimeException | ReflectiveOperationException e) {
            finish("error: " + e);
        }
    }

    /** Keys a player verb holds down (walk, jump, attack, sneak) until its command finishes. */
    private final java.util.List<net.minecraft.client.KeyMapping> held = new java.util.ArrayList<>();

    private void hold(net.minecraft.client.KeyMapping key, int ticks) {
        key.setDown(true);
        held.add(key);
        waitTicks = Math.max(waitTicks, ticks);
    }

    private void finish(String text) {
        for (var key : held) key.setDown(false);
        held.clear();
        try {
            Files.writeString(new File(out, current + ".txt").toPath(), text + "\n");
        } catch (IOException ignored) {
        }
        current = null;
    }

    private void run(Minecraft mc, String line) throws ReflectiveOperationException {
        String[] p = line.split("\\s+", 2);
        String verb = p[0];
        String arg = p.length > 1 ? p[1] : "";
        String[] a = arg.isEmpty() ? new String[0] : arg.split("\\s+");
        switch (verb) {
            case "cmd" -> {
                mc.player.connection.sendCommand(arg.startsWith("/") ? arg.substring(1) : arg);
                waitTicks = 20;
            }
            case "wait" -> waitTicks = Integer.parseInt(a[0]);
            case "shot" -> {
                mc.gui.getChat().clearMessages(false);
                pendingShot = a.length > 0 ? a[0] : "shot";
                shotDelay = 6;
            }
            // player verbs: hold movement/action keys for a number of ticks, like a player would
            case "walk" -> {
                var o = mc.options;
                var key = switch (a[0]) {
                    case "back" -> o.keyDown;
                    case "left" -> o.keyLeft;
                    case "right" -> o.keyRight;
                    default -> o.keyUp;
                };
                hold(key, a.length > 1 ? Integer.parseInt(a[1]) : 20);
                if (a.length > 2 && a[2].equals("sprint")) hold(o.keySprint, a.length > 1 ? Integer.parseInt(a[1]) : 20);
            }
            case "jump" -> hold(mc.options.keyJump, a.length > 0 ? Integer.parseInt(a[0]) : 5);
            case "sneak" -> hold(mc.options.keyShift, a.length > 0 ? Integer.parseInt(a[0]) : 20);
            // holds right-click: items used over time (Thaumometer scans, bows, food) see the use key held
            case "holduse" -> {
                // a press starts the use (the game only starts on a click), holding keeps it going
                net.minecraft.client.KeyMapping.click(mc.options.keyUse.getKey());
                hold(mc.options.keyUse, a.length > 0 ? Integer.parseInt(a[0]) : 40);
            }
            case "attack" -> {
                // a tap hits the entity under the crosshair; holding mines the block under it
                if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult eh) {
                    mc.gameMode.attack(mc.player, eh.getEntity());
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    finishLater("attacked " + eh.getEntity().getName().getString());
                    waitTicks = 5;
                } else {
                    hold(mc.options.keyAttack, a.length > 0 ? Integer.parseInt(a[0]) : 40);
                }
            }
            case "slot" -> {
                mc.player.getInventory().selected = Math.max(0, Math.min(8, Integer.parseInt(a[0])));
                finishLater("holding " + mc.player.getMainHandItem());
            }
            case "look" -> {
                mc.player.setYRot(Float.parseFloat(a[0]));
                mc.player.setXRot(Float.parseFloat(a[1]));
            }
            case "use" -> {
                HitResult hit = mc.player.pick(6.0, 0.0F, false);
                if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
                    var r = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, b);
                    finishLater("use -> " + r);
                } else {
                    finishLater("use: no block in reach");
                }
                waitTicks = 15;
            }
            case "useitem" -> {
                finishLater("useitem -> " + mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND));
                waitTicks = 20;
            }
            case "useat" -> {
                var pos = new net.minecraft.core.BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
                var hit = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
                finishLater("useat -> " + mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                waitTicks = 15;
            }
            case "mouse" -> {
                moveMouse(mc, Double.parseDouble(a[0]), Double.parseDouble(a[1]));
                waitTicks = 3;
            }
            case "click" -> {
                double x = Double.parseDouble(a[0]), y = Double.parseDouble(a[1]);
                int button = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                moveMouse(mc, x, y);
                if (mc.screen != null) {
                    mc.screen.mouseClicked(x, y, button);
                    mc.screen.mouseReleased(x, y, button);
                }
                waitTicks = 5;
            }
            case "key" -> {
                if (mc.screen != null) {
                    int key = Integer.parseInt(a[0]);
                    mc.screen.keyPressed(key, 0, 0);
                    mc.screen.keyReleased(key, 0, 0);
                }
                waitTicks = 3;
            }
            case "type" -> {
                if (mc.screen != null) for (char c : arg.toCharArray()) mc.screen.charTyped(c, 0);
                waitTicks = 3;
            }
            case "inv" -> {
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                waitTicks = 8;
            }
            case "creative" -> {
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true));
                waitTicks = 8;
            }
            case "title" -> {
                // back to the title screen (a mod that replaces it, Custom Main Menu, gets its GuiOpenEvent)
                mc.level.disconnect();
                mc.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(net.minecraft.network.chat.Component.empty()));
                mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                waitTicks = 40;
            }
            case "close" -> {
                // like Escape: a container screen tells the server, or the server keeps the menu open and inventory updates go to it
                if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.player.closeContainer();
                else mc.setScreen(null);
                waitTicks = 3;
            }
            case "info" -> {
                finishLater(info(mc));
                waitTicks = 1;
            }
            case "js" -> {
                // JavaScript in the live game: mc (client), server (integrated server), log
                try {
                    finishLater(org.kylepitcock.legacyconnector.core.DevScript.eval(arg, java.util.Map.of("mc", mc,
                            "server", (Object) java.util.Objects.requireNonNullElse(mc.getSingleplayerServer(), "none"),
                            "log", org.slf4j.LoggerFactory.getLogger("LegacyConnector/DevScript"))));
                } catch (Exception e) {
                    finishLater("error: " + e);
                }
                waitTicks = 1;
            }
            case "quit" -> {
                finishLater("bye");
                mc.stop();
            }
            default -> finishLater("unknown command '" + verb + "'");
        }
    }

    private String later;

    private void finishLater(String text) {
        later = text;
    }

    private void moveMouse(Minecraft mc, double guiX, double guiY) throws ReflectiveOperationException {
        double scale = mc.getWindow().getGuiScale();
        double px = guiX * scale, py = guiY * scale;
        var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
        m.setAccessible(true);
        m.invoke(mc.mouseHandler, mc.getWindow().getWindow(), px, py);
    }

    private String info(Minecraft mc) {
        StringBuilder sb = new StringBuilder();
        var pl = mc.player;
        sb.append("player ").append(String.format("%.1f %.1f %.1f", pl.getX(), pl.getY(), pl.getZ())).append(" yaw ").append((int) pl.getYRot())
                .append(" pitch ").append((int) pl.getXRot()).append(" dimension ").append(mc.level.dimension().location()).append('\n');
        HitResult hit = pl.pick(8.0, 0.0F, false);
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
            sb.append("looking at ").append(mc.level.getBlockState(b.getBlockPos())).append(" at ").append(b.getBlockPos().toShortString()).append('\n');
        }
        sb.append("gui scale ").append(mc.getWindow().getGuiScale()).append(" screen ").append(mc.getWindow().getGuiScaledWidth()).append('x')
                .append(mc.getWindow().getGuiScaledHeight()).append('\n');
        var s = mc.screen;
        if (s == null) return sb.append("no screen").toString();
        sb.append("screen ").append(s.getClass().getName()).append('\n');
        if (s instanceof LegacyContainerScreen lcs && lcs.legacy() != null) sb.append("legacy gui ").append(lcs.legacy().getClass().getName()).append('\n');
        if (s instanceof AbstractContainerScreen<?> acs) {
            sb.append("container origin ").append(acs.getGuiLeft()).append(',').append(acs.getGuiTop()).append(" size ").append(acs.getXSize()).append('x')
                    .append(acs.getYSize()).append('\n');
            int n = 0;
            for (var slot : acs.getMenu().slots) {
                if (n++ < 60) sb.append("  slot ").append(slot.index).append(" at ").append(acs.getGuiLeft() + slot.x).append(',').append(acs.getGuiTop() + slot.y)
                        .append(' ').append(slot.getItem().isEmpty() ? "" : slot.getItem().getHoverName().getString())
                        .append(slot instanceof org.kylepitcock.legacyconnector.v1_7_10.gui.LegacySlot ls ? " [legacy " + ls.legacy().getClass().getName() + " "
                                + ls.legacy().xDisplayPosition + "," + ls.legacy().yDisplayPosition + "]" : "").append('\n');
            }
        }
        for (var child : s.children()) {
            if (child instanceof AbstractWidget w) sb.append("  widget ").append(w.getClass().getSimpleName()).append(" at ").append(w.getX()).append(',').append(w.getY())
                    .append(' ').append(w.getWidth()).append('x').append(w.getHeight()).append(" '").append(w.getMessage().getString()).append("'\n");
        }
        return sb.toString();
    }
}
