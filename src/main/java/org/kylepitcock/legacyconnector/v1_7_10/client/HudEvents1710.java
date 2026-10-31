package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.client.gui.ScaledResolution;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderGameOverlayEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The 1.7.10 HUD events: {@code ElementType.ALL} around the whole HUD and the per-element types around their vanilla GUI
 * layers; legacy overlays draw in the Post events through the emulated GL state.
 */
public final class HudEvents1710 {
    private static final Map<ResourceLocation, ElementType> LAYERS = Map.ofEntries(
            Map.entry(VanillaGuiLayers.CROSSHAIR, ElementType.CROSSHAIRS),
            Map.entry(VanillaGuiLayers.HOTBAR, ElementType.HOTBAR),
            Map.entry(VanillaGuiLayers.PLAYER_HEALTH, ElementType.HEALTH),
            Map.entry(VanillaGuiLayers.ARMOR_LEVEL, ElementType.ARMOR),
            Map.entry(VanillaGuiLayers.FOOD_LEVEL, ElementType.FOOD),
            Map.entry(VanillaGuiLayers.AIR_LEVEL, ElementType.AIR),
            Map.entry(VanillaGuiLayers.EXPERIENCE_BAR, ElementType.EXPERIENCE),
            Map.entry(VanillaGuiLayers.VEHICLE_HEALTH, ElementType.HEALTHMOUNT),
            Map.entry(VanillaGuiLayers.JUMP_METER, ElementType.JUMPBAR),
            Map.entry(VanillaGuiLayers.BOSS_OVERLAY, ElementType.BOSSHEALTH),
            Map.entry(VanillaGuiLayers.CHAT, ElementType.CHAT),
            Map.entry(VanillaGuiLayers.TAB_LIST, ElementType.PLAYER_LIST),
            Map.entry(VanillaGuiLayers.DEBUG_OVERLAY, ElementType.DEBUG),
            Map.entry(VanillaGuiLayers.CAMERA_OVERLAYS, ElementType.HELMET));

    private HudEvents1710() {}

    private static final Map<String, Integer> HUD_FAILURES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Pre e) -> {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.EventBridge1710.renderTick(legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent.Phase.START,
                    e.getPartialTick().getGameTimeDeltaPartialTick(false), e.getGuiGraphics());
            // GuiIngameForge started every frame with the rows above the hotbar at 39; handlers that draw their own rows add to them.
            legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.left_height = 39;
            legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.right_height = 39;
            if (wants()) post(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false), ElementType.ALL, false);
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) -> {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.EventBridge1710.renderTick(legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent.Phase.END,
                    e.getPartialTick().getGameTimeDeltaPartialTick(false), e.getGuiGraphics());
            if (!wants()) return;
            float partial = e.getPartialTick().getGameTimeDeltaPartialTick(false);
            post(e.getGuiGraphics(), partial, ElementType.TEXT, false);
            post(e.getGuiGraphics(), partial, ElementType.ALL, true);
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre e) -> {
            ElementType type = LAYERS.get(e.getName());
            if (type != null && wants() && drawnForMode(type) && post(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false), type, false)) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Post e) -> {
            ElementType type = LAYERS.get(e.getName());
            if (type != null && drawnForMode(type)) advanceStack(type);
            if (type != null && wants() && drawnForMode(type)) post(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false), type, true);
        });
    }

    /**
     * GuiIngameForge raised {@code left_height}/{@code right_height} by the room each status row took before it posted that
     * row's Post event, and overlays stacked their own rows above the vanilla ones from them (Fisk's Heroes' armour protection
     * bar, Tinkers' Construct's hearts). The modern layers do not track it, so the rows are counted here.
     */
    private static void advanceStack(ElementType type) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        switch (type) {
            case HEALTH -> {
                float max = player.getMaxHealth();
                float absorb = player.getAbsorptionAmount();
                int rows = (int) Math.ceil((max + absorb) / 2.0F / 10.0F);
                int rowHeight = Math.max(10 - (rows - 2), 3);
                legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.left_height += rows * rowHeight;
                if (rowHeight != 10) legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.left_height += 10 - rowHeight;
            }
            case ARMOR -> legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.left_height += 10;
            case FOOD -> legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.right_height += 10;
            case AIR -> {
                if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
                    legacyconnector.v1_7_10.net.minecraftforge.client.GuiIngameForge.right_height += 10;
                }
            }
            default -> {
            }
        }
    }

    /**
     * 1.7.10's GuiIngameForge only posted the survival elements (health, armour, food, air) when the HUD was drawn for the
     * game mode, and the experience bar in survival and adventure. The modern layers run in every mode and skip their drawing
     * themselves, so without this a handler that replaces the hearts (Tinkers' Construct) draws them in creative.
     */
    private static boolean drawnForMode(ElementType type) {
        var gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return true;
        return switch (type) {
            case HEALTH, ARMOR, FOOD, AIR -> gameMode.canHurtPlayer();
            case EXPERIENCE -> gameMode.hasExperience();
            default -> true;
        };
    }

    private static boolean wants() {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(RenderGameOverlayEvent.class) || MinecraftForge.EVENT_BUS.mcc$hasListeners(RenderGameOverlayEvent.Pre.class)
                || MinecraftForge.EVENT_BUS.mcc$hasListeners(RenderGameOverlayEvent.Post.class) || MinecraftForge.EVENT_BUS.mcc$hasListeners(RenderGameOverlayEvent.Text.class);
    }

    /** Posts one overlay event; returns whether a Pre listener cancelled it. */
    private static boolean post(GuiGraphics graphics, float partial, ElementType type, boolean after) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        int mouseX = (int) (mc.mouseHandler.xpos() * window.getGuiScaledWidth() / Math.max(1, window.getScreenWidth()));
        int mouseY = (int) (mc.mouseHandler.ypos() * window.getGuiScaledHeight() / Math.max(1, window.getScreenHeight()));
        RenderGameOverlayEvent root = new RenderGameOverlayEvent(partial, new ScaledResolution(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft(),
                window.getWidth(), window.getHeight()), mouseX, mouseY);
        RenderContext.beginGui(graphics);
        try {
            if (type == ElementType.TEXT && !after) {
                return MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Text(root, new ArrayList<>(), new ArrayList<>()));
            }
            if (after) {
                MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(root, type));
                return false;
            }
            return MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Pre(root, type));
        } catch (RuntimeException | LinkageError ex) {
            if (HUD_FAILURES.merge(ex.toString(), 1, Integer::sum) == 1) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Hud").warn("A HUD overlay handler failed (reported once per kind of failure): {}", ex.toString());
            }
            return false;
        } finally {
            RenderContext.end();
        }
    }
}
