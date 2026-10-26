package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.util.EnumChatFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's row of the multiplayer server list: name, message of the day, player count, ping and icon. The server is pinged once
 * (in the background) with the modern status pinger; the answer is copied into the legacy {@link ServerData}. A double click joins.
 */
public class ServerListEntryNormal implements GuiListExtended.IGuiListEntry {
    private static final Logger logger = LogManager.getLogger();
    private static final ThreadPoolExecutor field_148302_b = new ScheduledThreadPoolExecutor(5, r -> {
        Thread t = new Thread(r, "Legacy server pinger");
        t.setDaemon(true);
        return t;
    });
    private static final net.minecraft.client.multiplayer.ServerStatusPinger MCC_PINGER = new net.minecraft.client.multiplayer.ServerStatusPinger();
    private static final net.minecraft.resources.ResourceLocation MCC_UNKNOWN_ICON = net.minecraft.resources.ResourceLocation.withDefaultNamespace(
            "textures/misc/unknown_server.png");

    protected final GuiMultiplayer field_148303_c;
    protected final Minecraft field_148300_d;
    protected final ServerData field_148301_e;
    protected long field_148298_f;
    private net.minecraft.client.multiplayer.ServerData mcc$modern;
    private net.minecraft.client.gui.screens.FaviconTexture mcc$icon;
    private byte[] mcc$iconBytes;

    protected ServerListEntryNormal(GuiMultiplayer screen, ServerData data) {
        this.field_148303_c = screen;
        this.field_148301_e = data;
        this.field_148300_d = Minecraft.getMinecraft();
    }

    @Override
    public void drawEntry(int index, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected) {
        ServerData data = field_148301_e;
        if (!data.field_78841_f) {
            data.field_78841_f = true;
            data.pingToServer = -2L;
            data.serverMOTD = "";
            data.populationInfo = "";
            mcc$ping();
        }
        mcc$copyStatus();
        var font = field_148300_d.fontRenderer;
        boolean incompatible = data.field_82821_f != 5;
        font.drawString(data.serverName, x + 32 + 3, y + 1, 16777215);
        List lines = font.listFormattedStringToWidth(data.serverMOTD == null ? "" : data.serverMOTD, listWidth - 32 - 2);
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            font.drawString(String.valueOf(lines.get(i)), x + 32 + 3, y + 12 + font.FONT_HEIGHT * i, 8421504);
        }
        String right = incompatible && data.pingToServer >= 0 ? EnumChatFormatting.DARK_RED + data.gameVersion : data.populationInfo;
        if (right == null) right = "";
        font.drawString(right, x + listWidth - font.getStringWidth(right) - 15 - 2, y + 1, 8421504);

        var gui = RenderContext.gui();
        if (gui == null) return;
        String ping;
        if (data.pingToServer == -2L) {
            int frame = (int) (Minecraft.getSystemTime() / 100L + index * 2L & 7L);
            if (frame > 4) frame = 8 - frame;
            ping = "server_list/pinging_" + (frame + 1);
        } else if (data.pingToServer < 0L) {
            ping = "server_list/unreachable";
        } else if (incompatible) {
            ping = "server_list/incompatible";
        } else {
            long ms = data.pingToServer;
            int bars = ms < 150L ? 5 : ms < 300L ? 4 : ms < 600L ? 3 : ms < 1000L ? 2 : 1;
            ping = "server_list/ping_" + bars;
        }
        gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace(ping), x + listWidth - 15, y, 10, 8);
        mcc$updateIcon();
        net.minecraft.resources.ResourceLocation icon = mcc$icon != null && mcc$iconBytes != null ? mcc$icon.textureLocation() : MCC_UNKNOWN_ICON;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        gui.blit(icon, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private void mcc$ping() {
        String name = field_148301_e.serverName == null ? "" : field_148301_e.serverName;
        String ip = field_148301_e.serverIP == null ? "" : field_148301_e.serverIP;
        mcc$modern = new net.minecraft.client.multiplayer.ServerData(name, ip, net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        var modern = mcc$modern;
        field_148302_b.submit(() -> {
            try {
                MCC_PINGER.pingServer(modern, () -> {}, () -> {});
            } catch (java.net.UnknownHostException e) {
                field_148301_e.pingToServer = -1L;
                field_148301_e.serverMOTD = EnumChatFormatting.DARK_RED + "Can't resolve hostname";
            } catch (Exception e) {
                field_148301_e.pingToServer = -1L;
                field_148301_e.serverMOTD = EnumChatFormatting.DARK_RED + "Can't connect to server.";
            }
        });
    }

    /** Copies what the modern pinger has learnt so far (the pinger's connections are ticked here, as the server list screen did). */
    private void mcc$copyStatus() {
        if (mcc$modern == null) return;
        MCC_PINGER.tick();
        var modern = mcc$modern;
        var state = modern.state();
        if (state == net.minecraft.client.multiplayer.ServerData.State.SUCCESSFUL || state == net.minecraft.client.multiplayer.ServerData.State.INCOMPATIBLE) {
            field_148301_e.pingToServer = modern.ping;
            field_148301_e.serverMOTD = mcc$legacyText(modern.motd);
            field_148301_e.populationInfo = mcc$legacyText(modern.status);
            field_148301_e.gameVersion = modern.version == null ? "" : modern.version.getString();
            // 1.7.10's protocol is 5: a modern server answering in its own protocol reads as compatible here when it is the game's own
            field_148301_e.field_82821_f = modern.protocol == net.minecraft.SharedConstants.getCurrentVersion().getProtocolVersion() ? 5 : modern.protocol;
        } else if (state == net.minecraft.client.multiplayer.ServerData.State.UNREACHABLE) {
            field_148301_e.pingToServer = -1L;
            if (modern.motd != null && field_148301_e.serverMOTD != null && field_148301_e.serverMOTD.isEmpty()) field_148301_e.serverMOTD = mcc$legacyText(modern.motd);
        }
    }

    private void mcc$updateIcon() {
        if (mcc$modern == null) return;
        byte[] bytes = mcc$modern.getIconBytes();
        if (bytes == null || bytes == mcc$iconBytes) return;
        try {
            if (mcc$icon == null) {
                mcc$icon = net.minecraft.client.gui.screens.FaviconTexture.forServer(net.minecraft.client.Minecraft.getInstance().getTextureManager(),
                        mcc$modern.ip);
            }
            mcc$icon.upload(com.mojang.blaze3d.platform.NativeImage.read(bytes));
            mcc$iconBytes = bytes;
            field_148301_e.field_147412_i = java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.error("Invalid icon for server {} ({})", field_148301_e.serverName, field_148301_e.serverIP, e);
            mcc$iconBytes = bytes;
        }
    }

    /** A modern component as 1.7.10 formatted text (section sign codes). */
    private static String mcc$legacyText(net.minecraft.network.chat.Component component) {
        if (component == null) return "";
        StringBuilder sb = new StringBuilder();
        component.visit((style, text) -> {
            if (sb.length() > 0) sb.append("§r");
            var color = style.getColor();
            if (color != null) {
                var format = net.minecraft.ChatFormatting.getByName(color.serialize());
                if (format != null) sb.append(format);
            }
            if (style.isBold()) sb.append("§l");
            if (style.isItalic()) sb.append("§o");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isObfuscated()) sb.append("§k");
            sb.append(text);
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return sb.toString();
    }

    /** A double click joins the server. */
    @Override
    public boolean mousePressed(int index, int mouseX, int mouseY, int button, int relativeX, int relativeY) {
        if (Minecraft.getSystemTime() - field_148298_f < 250L) {
            legacyconnector.v1_7_10.cpw.mods.fml.client.FMLClientHandler.instance().connectToServer(field_148303_c, field_148301_e);
        }
        field_148298_f = Minecraft.getSystemTime();
        return false;
    }

    @Override
    public void mouseReleased(int index, int mouseX, int mouseY, int button, int relativeX, int relativeY) {
    }

    public ServerData getServerData() {
        return field_148301_e;
    }
}
