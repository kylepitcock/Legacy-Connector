package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.gui.ScaledResolution;
import legacyconnector.v1_12_2.net.minecraftforge.client.GuiIngameForge;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.DrawBlockHighlightEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.EntityViewRenderEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.InputUpdateEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderBlockOverlayEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderGameOverlayEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderHandEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderLivingEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderPlayerEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderSpecificHandEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderTooltipEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderWorldLastEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.ScreenshotEvent;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyFog;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Forge's client events for 1.12.2 mods that are not about screens: the HUD (RenderGameOverlayEvent around the game's GUI layers, HUD text,
 * chat position, boss bars, GuiIngameForge's switches), world rendering (RenderWorldLastEvent, block highlight, living entities and players,
 * hands, block overlays), the view (fog density/colour/setup, camera angles, field of view), tooltips, movement input and screenshots.
 * Legacy handlers draw through the emulated GL state of the render context, relative to the camera where 1.12 drew in world space.
 */
public final class ClientEvents1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/ClientEvents");
    private static final Map<String, Integer> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();
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
            Map.entry(VanillaGuiLayers.EFFECTS, ElementType.POTION_ICONS),
            Map.entry(VanillaGuiLayers.SUBTITLE_OVERLAY, ElementType.SUBTITLES),
            Map.entry(VanillaGuiLayers.CAMERA_OVERLAYS, ElementType.HELMET));

    private ClientEvents1122() {}

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientEvents1122::onHudPre);
        bus.addListener(ClientEvents1122::onHudPost);
        bus.addListener(ClientEvents1122::onLayerPre);
        bus.addListener(ClientEvents1122::onLayerPost);
        bus.addListener(ClientEvents1122::onDebugText);
        bus.addListener(ClientEvents1122::onChatPosition);
        bus.addListener(ClientEvents1122::onBossBar);
        bus.addListener(ClientEvents1122::onStage);
        bus.addListener(ClientEvents1122::onHighlight);
        bus.addListener(ClientEvents1122::onLivingPre);
        bus.addListener(ClientEvents1122::onLivingPost);
        bus.addListener(ClientEvents1122::onPlayerPre);
        bus.addListener(ClientEvents1122::onPlayerPost);
        bus.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e) -> frame++);
        bus.addListener(ClientEvents1122::onHand);
        bus.addListener(ClientEvents1122::onBlockOverlay);
        bus.addListener(ClientEvents1122::onFog);
        bus.addListener(ClientEvents1122::onFogColor);
        bus.addListener(ClientEvents1122::onCamera);
        bus.addListener(ClientEvents1122::onFov);
        bus.addListener(ClientEvents1122::onTooltip);
        bus.addListener(ClientEvents1122::onTooltipColor);
        bus.addListener(ClientEvents1122::onInput);
        bus.addListener(ClientEvents1122::onScreenshot);
        bus.addListener(ClientEvents1122::onSoundSource);
        bus.addListener(ClientEvents1122::onStreamingSource);
        bus.addListener(ClientEvents1122::onSoundEngineLoad);
        bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> tickMusic());
        bus.addListener((net.neoforged.neoforge.client.event.SelectMusicEvent e) -> {
            if (replacedMusic() != null) e.setMusic(null);
        });
    }

    static boolean wants(Class<? extends Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    /** Posts and reports a failing handler once per kind of failure; returns whether the event was cancelled. */
    private static boolean post(Event event, String what) {
        try {
            return MinecraftForge.EVENT_BUS.post(event);
        } catch (RuntimeException | LinkageError ex) {
            if (FAILURES.merge(what + ex, 1, Integer::sum) == 1) LOG.warn("A legacy {} handler failed (reported once): {}", what, ex.toString());
            return false;
        }
    }

    private static legacyconnector.v1_12_2.net.minecraft.client.Minecraft legacyMc() {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
    }

    private static void setCamera(net.minecraft.world.phys.Vec3 cam) {
        var manager = legacyMc().getRenderManager();
        if (manager != null) manager.setRenderPosition(cam.x, cam.y, cam.z);
        legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerX = cam.x;
        legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerY = cam.y;
        legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerZ = cam.z;
    }

    // ---------------------------------------------------------------- HUD

    private static boolean wantsHud() {
        return wants(RenderGameOverlayEvent.Pre.class) || wants(RenderGameOverlayEvent.Post.class);
    }

    private static RenderGameOverlayEvent root(float partial) {
        return new RenderGameOverlayEvent(partial, new ScaledResolution(legacyMc()));
    }

    private static void onHudPre(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre e) {
        GuiIngameForge.left_height = 39;
        GuiIngameForge.right_height = 39;
        if (!wantsHud()) return;
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            if (post(new RenderGameOverlayEvent.Pre(root(e.getPartialTick().getGameTimeDeltaPartialTick(false)), ElementType.ALL), "HUD")) e.setCanceled(true);
        } finally {
            RenderContext.end();
        }
    }

    private static void onHudPost(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e) {
        float partial = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        GuiGraphics graphics = e.getGuiGraphics();
        // 1.12 posted the HUD text every frame and drew what handlers added (on top of the debug screen's lines when it was open)
        if (wants(RenderGameOverlayEvent.Text.class) && !Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            ArrayList<String> left = new ArrayList<>();
            ArrayList<String> right = new ArrayList<>();
            RenderContext.beginGui(graphics);
            boolean cancelled;
            try {
                cancelled = post(new RenderGameOverlayEvent.Text(root(partial), left, right), "HUD text");
            } finally {
                RenderContext.end();
            }
            if (!cancelled) drawText(graphics, left, right);
        }
        if (!wantsHud()) return;
        RenderContext.beginGui(graphics);
        try {
            post(new RenderGameOverlayEvent.Post(root(partial), ElementType.ALL), "HUD");
        } finally {
            RenderContext.end();
        }
    }

    private static void drawText(GuiGraphics graphics, List<String> left, List<String> right) {
        var font = Minecraft.getInstance().font;
        int width = graphics.guiWidth();
        int top = 2;
        for (String line : left) {
            if (line != null && !line.isEmpty()) graphics.drawString(font, line, 2, top, 0xE0E0E0);
            top += font.lineHeight;
        }
        top = 2;
        for (String line : right) {
            if (line != null && !line.isEmpty()) graphics.drawString(font, line, width - 2 - font.width(line), top, 0xE0E0E0);
            top += font.lineHeight;
        }
    }

    /** GuiIngameForge's switches: an element turned off is not drawn. */
    private static boolean switchedOff(ElementType type) {
        return switch (type) {
            case CROSSHAIRS -> !GuiIngameForge.renderCrosshairs;
            case HOTBAR -> !GuiIngameForge.renderHotbar;
            case HEALTH -> !GuiIngameForge.renderHealth;
            case ARMOR -> !GuiIngameForge.renderArmor;
            case FOOD -> !GuiIngameForge.renderFood;
            case AIR -> !GuiIngameForge.renderAir;
            case EXPERIENCE -> !GuiIngameForge.renderExperiance;
            case HEALTHMOUNT -> !GuiIngameForge.renderHealthMount;
            case JUMPBAR -> !GuiIngameForge.renderJumpBar;
            case BOSSHEALTH -> !GuiIngameForge.renderBossHealth;
            case HELMET -> !GuiIngameForge.renderHelmet;
            default -> false;
        };
    }

    /** GuiIngameForge only posted the survival elements when they were drawn for the game mode. */
    private static boolean drawnForMode(ElementType type) {
        var gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return true;
        return switch (type) {
            case HEALTH, ARMOR, FOOD, AIR -> gameMode.canHurtPlayer();
            case EXPERIENCE -> gameMode.hasExperience();
            default -> true;
        };
    }

    private static void onLayerPre(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre e) {
        ElementType type = LAYERS.get(e.getName());
        if (type == null) return;
        if (e.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR) && !GuiIngameForge.renderObjective) {
            e.setCanceled(true);
            return;
        }
        if (switchedOff(type)) {
            e.setCanceled(true);
            return;
        }
        if (!wantsHud() || !drawnForMode(type)) return;
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            if (post(new RenderGameOverlayEvent.Pre(root(e.getPartialTick().getGameTimeDeltaPartialTick(false)), type), "HUD")) e.setCanceled(true);
        } finally {
            RenderContext.end();
        }
    }

    private static void onLayerPost(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post e) {
        ElementType type = LAYERS.get(e.getName());
        if (type == null || !wantsHud() || !drawnForMode(type)) return;
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            post(new RenderGameOverlayEvent.Post(root(e.getPartialTick().getGameTimeDeltaPartialTick(false)), type), "HUD");
        } finally {
            RenderContext.end();
        }
    }

    /** With the debug screen open the HUD text is its lines: handlers add to or change them. */
    private static void onDebugText(net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.DebugText e) {
        if (!wants(RenderGameOverlayEvent.Text.class)) return;
        ArrayList<String> left = new ArrayList<>(e.getLeft());
        ArrayList<String> right = new ArrayList<>(e.getRight());
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            if (post(new RenderGameOverlayEvent.Text(root(e.getPartialTick().getGameTimeDeltaPartialTick(false)), left, right), "HUD text")) {
                left.clear();
                right.clear();
            }
        } finally {
            RenderContext.end();
        }
        e.getLeft().clear();
        e.getLeft().addAll(left);
        e.getRight().clear();
        e.getRight().addAll(right);
    }

    private static void onChatPosition(net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.Chat e) {
        if (!wants(RenderGameOverlayEvent.Chat.class)) return;
        var legacy = new RenderGameOverlayEvent.Chat(root(e.getPartialTick().getGameTimeDeltaPartialTick(false)), e.getPosX(), e.getPosY());
        post(legacy, "chat position");
        e.setPosX(legacy.getPosX());
        e.setPosY(legacy.getPosY());
    }

    private static void onBossBar(net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress e) {
        if (!wants(RenderGameOverlayEvent.BossInfo.class)) return;
        var legacy = new RenderGameOverlayEvent.BossInfo(root(e.getPartialTick().getGameTimeDeltaPartialTick(false)), ElementType.BOSSINFO, e.getX(), e.getY(),
                e.getIncrement());
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            if (post(legacy, "boss bar")) e.setCanceled(true);
        } finally {
            RenderContext.end();
        }
        e.setIncrement(legacy.getIncrement());
    }

    // ---------------------------------------------------------------- world rendering

    private static void onStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY) {
            setCamera(e.getCamera().getPosition());
            return;
        }
        if (e.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL || !wants(RenderWorldLastEvent.class)) return;
        Minecraft mc = Minecraft.getInstance();
        setCamera(e.getCamera().getPosition());
        var buffers = mc.renderBuffers().bufferSource();
        // camera-relative and turned to the camera (only the event's model-view holds the camera rotation at this stage)
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.last().pose().set(e.getModelViewMatrix());
        pose.last().normal().set(new org.joml.Matrix3f(e.getModelViewMatrix()));
        RenderContext.beginWorld(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            post(new RenderWorldLastEvent(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$renderGlobal(legacyMc()),
                    e.getPartialTick().getGameTimeDeltaPartialTick(false)), "RenderWorldLastEvent");
        } finally {
            RenderContext.end();
            buffers.endBatch();
        }
    }

    private static void onHighlight(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block e) {
        Minecraft mc = Minecraft.getInstance();
        if (!wants(DrawBlockHighlightEvent.class) || mc.player == null) return;
        if (!(EntityBridge1122.wrap(mc.player) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player)) return;
        var legacy = new DrawBlockHighlightEvent(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$renderGlobal(legacyMc()), player,
                legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.mcc$of(e.getTarget()), 0, e.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        setCamera(e.getCamera().getPosition());
        RenderContext.beginWorld(e.getPoseStack(), e.getMultiBufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            if (post(legacy, "block highlight")) e.setCanceled(true);
        } finally {
            RenderContext.end();
        }
    }

    /** The entity's position relative to the camera, as 1.12's renderers got it. */
    private static double[] relative(net.minecraft.world.entity.Entity entity, float partial) {
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return new double[] {net.minecraft.util.Mth.lerp(partial, entity.xo, entity.getX()) - cam.x,
                net.minecraft.util.Mth.lerp(partial, entity.yo, entity.getY()) - cam.y, net.minecraft.util.Mth.lerp(partial, entity.zo, entity.getZ()) - cam.z};
    }

    /** A pose at the camera (the modern pose is at the entity; legacy handlers translate by x/y/z themselves). */
    private static com.mojang.blaze3d.vertex.PoseStack atCamera(com.mojang.blaze3d.vertex.PoseStack pose, double[] at) {
        var out = new com.mojang.blaze3d.vertex.PoseStack();
        out.last().pose().set(pose.last().pose());
        out.last().normal().set(pose.last().normal());
        out.translate(-at[0], -at[1], -at[2]);
        return out;
    }

    private static void onLivingPre(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> e) {
        if (!wants(RenderLivingEvent.Pre.class) && !wants(RenderLivingEvent.Specials.Pre.class)) return;
        if (!(EntityBridge1122.wrap(e.getEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living)) return;
        float partial = e.getPartialTick();
        double[] at = relative(e.getEntity(), partial);
        RenderContext.beginWorld(atCamera(e.getPoseStack(), at), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        try {
            if (post(new RenderLivingEvent.Pre<>(living, null, partial, at[0], at[1], at[2]), "living render")) {
                e.setCanceled(true);
                return;
            }
            if (wants(RenderLivingEvent.Specials.Pre.class)) post(new RenderLivingEvent.Specials.Pre<>(living, null, at[0], at[1], at[2]), "living specials");
        } finally {
            RenderContext.end();
        }
    }

    private static void onLivingPost(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> e) {
        if (!wants(RenderLivingEvent.Post.class) && !wants(RenderLivingEvent.Specials.Post.class)) return;
        if (!(EntityBridge1122.wrap(e.getEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living)) return;
        float partial = e.getPartialTick();
        double[] at = relative(e.getEntity(), partial);
        RenderContext.beginWorld(atCamera(e.getPoseStack(), at), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        try {
            if (wants(RenderLivingEvent.Specials.Post.class)) post(new RenderLivingEvent.Specials.Post<>(living, null, at[0], at[1], at[2]), "living specials");
            post(new RenderLivingEvent.Post<>(living, null, partial, at[0], at[1], at[2]), "living render");
        } finally {
            RenderContext.end();
        }
    }

    /** The legacy renderer of a player's skin model ("default" or "slim"). */
    private static legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderPlayer playerRenderer(net.minecraft.world.entity.player.Player player) {
        var manager = legacyMc().getRenderManager();
        if (manager == null) return null;
        String type = player instanceof net.minecraft.client.player.AbstractClientPlayer p
                && p.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM ? "slim" : "default";
        try {
            return manager.getSkinMap().get(type);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static void onPlayerPre(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre e) {
        if (!wants(RenderPlayerEvent.Pre.class) && !wants(RenderPlayerEvent.Specials.Pre.class)) return;
        if (!(EntityBridge1122.wrap(e.getEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player)) return;
        float partial = e.getPartialTick();
        double[] at = relative(e.getEntity(), partial);
        var renderer = playerRenderer(e.getEntity());
        RenderContext.beginWorld(atCamera(e.getPoseStack(), at), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        try {
            if (post(new RenderPlayerEvent.Pre(player, renderer, partial, at[0], at[1], at[2]), "player render")) {
                e.setCanceled(true);
                return;
            }
            if (wants(RenderPlayerEvent.Specials.Pre.class)) post(new RenderPlayerEvent.Specials.Pre(player, renderer, partial), "player specials");
        } finally {
            RenderContext.end();
        }
    }

    private static void onPlayerPost(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post e) {
        if (!wants(RenderPlayerEvent.Post.class) && !wants(RenderPlayerEvent.Specials.Post.class)) return;
        if (!(EntityBridge1122.wrap(e.getEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player)) return;
        float partial = e.getPartialTick();
        double[] at = relative(e.getEntity(), partial);
        var renderer = playerRenderer(e.getEntity());
        RenderContext.beginWorld(atCamera(e.getPoseStack(), at), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        try {
            if (wants(RenderPlayerEvent.Specials.Post.class)) post(new RenderPlayerEvent.Specials.Post(player, renderer, partial), "player specials");
            post(new RenderPlayerEvent.Post(player, renderer, partial, at[0], at[1], at[2]), "player render");
        } finally {
            RenderContext.end();
        }
    }

    private static long frame;
    private static long handFrame = -1;
    private static boolean handCanceled;

    /** RenderHandEvent once per frame (its answer holds for both hands), then RenderSpecificHandEvent for each hand. */
    private static void onHand(net.neoforged.neoforge.client.event.RenderHandEvent e) {
        if (handFrame != frame) {
            handFrame = frame;
            handCanceled = false;
            if (wants(RenderHandEvent.class)) {
                RenderContext.beginWorld(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
                try {
                    handCanceled = post(new RenderHandEvent(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$renderGlobal(legacyMc()),
                            e.getPartialTick(), 2), "hand");
                } finally {
                    RenderContext.end();
                }
            }
        }
        if (handCanceled) {
            e.setCanceled(true);
            return;
        }
        if (!wants(RenderSpecificHandEvent.class)) return;
        RenderContext.beginWorld(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        try {
            if (post(new RenderSpecificHandEvent(legacyconnector.v1_12_2.net.minecraft.util.EnumHand.mcc$of(e.getHand()), e.getPartialTick(),
                    e.getInterpolatedPitch(), e.getSwingProgress(), e.getEquipProgress(), Stacks1122.view(e.getItemStack())), "specific hand")) {
                e.setCanceled(true);
            }
        } finally {
            RenderContext.end();
        }
    }

    private static void onBlockOverlay(net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent e) {
        if (!wants(RenderBlockOverlayEvent.class)) return;
        if (!(EntityBridge1122.wrap(e.getPlayer()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player)) return;
        RenderBlockOverlayEvent.OverlayType type = switch (e.getOverlayType()) {
            case FIRE -> RenderBlockOverlayEvent.OverlayType.FIRE;
            case WATER -> RenderBlockOverlayEvent.OverlayType.WATER;
            default -> RenderBlockOverlayEvent.OverlayType.BLOCK;
        };
        var pos = e.getBlockPos();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderContext.beginWorld(e.getPoseStack(), buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            if (post(new RenderBlockOverlayEvent(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), type,
                    StatesBridge1122.toLegacy(e.getBlockState()), new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(pos.getX(), pos.getY(), pos.getZ())),
                    "block overlay")) {
                e.setCanceled(true);
            }
        } finally {
            RenderContext.end();
            buffers.endBatch();
        }
    }

    // ---------------------------------------------------------------- view

    private static legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState stateAt(net.minecraft.client.Camera camera) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        try {
            return StatesBridge1122.toLegacy(level.getBlockState(camera.getBlockPosition()));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer entityRenderer() {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$entityRenderer(legacyMc());
    }

    /**
     * 1.12's setupFog: FogDensity first (cancelled, the density makes exponential fog, drawn as linear fog reaching as far), then
     * RenderFogEvent, whose handlers set the fog with GL fog calls (captured by {@link LegacyFog}).
     */
    private static void onFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog e) {
        boolean density = wants(EntityViewRenderEvent.FogDensity.class);
        boolean render = wants(EntityViewRenderEvent.RenderFogEvent.class);
        if (!density && !render) return;
        var entity = EntityBridge1122.wrap(e.getCamera().getEntity());
        if (entity == null) return;
        var state = stateAt(e.getCamera());
        LegacyFog.begin();
        try {
            if (density) {
                var event = new EntityViewRenderEvent.FogDensity(entityRenderer(), entity, state, e.getPartialTick(), 0.1F);
                if (post(event, "fog density")) {
                    e.setNearPlaneDistance(0.0F);
                    e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), LegacyFog.expEnd(event.getDensity())));
                    e.setCanceled(true);
                    return;
                }
            }
            if (!render) return;
            int mode = e.getMode() == net.minecraft.client.renderer.FogRenderer.FogMode.FOG_SKY ? -1 : 0;
            post(new EntityViewRenderEvent.RenderFogEvent(entityRenderer(), entity, state, e.getPartialTick(), mode, e.getFarPlaneDistance()), "fog");
            boolean changed = false;
            if (!Float.isNaN(LegacyFog.start)) {
                e.setNearPlaneDistance(LegacyFog.start);
                changed = true;
            }
            if (!Float.isNaN(LegacyFog.end)) {
                e.setFarPlaneDistance(LegacyFog.end);
                changed = true;
            } else if (!Float.isNaN(LegacyFog.density) && (LegacyFog.mode == LegacyFog.GL_EXP || LegacyFog.mode == LegacyFog.GL_EXP2)) {
                e.setNearPlaneDistance(0.0F);
                e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), LegacyFog.expEnd(LegacyFog.density)));
                changed = true;
            }
            if (changed) e.setCanceled(true);
        } finally {
            LegacyFog.end();
        }
    }

    private static void onFogColor(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor e) {
        if (!wants(EntityViewRenderEvent.FogColors.class)) return;
        var entity = EntityBridge1122.wrap(e.getCamera().getEntity());
        if (entity == null) return;
        var legacy = new EntityViewRenderEvent.FogColors(entityRenderer(), entity, stateAt(e.getCamera()), e.getPartialTick(), e.getRed(), e.getGreen(), e.getBlue());
        post(legacy, "fog colour");
        e.setRed(legacy.getRed());
        e.setGreen(legacy.getGreen());
        e.setBlue(legacy.getBlue());
    }

    private static void onCamera(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        if (!wants(EntityViewRenderEvent.CameraSetup.class)) return;
        var entity = EntityBridge1122.wrap(e.getCamera().getEntity());
        if (entity == null) return;
        var legacy = new EntityViewRenderEvent.CameraSetup(entityRenderer(), entity, stateAt(e.getCamera()), e.getPartialTick(), e.getYaw(), e.getPitch(), e.getRoll());
        post(legacy, "camera setup");
        e.setYaw(legacy.getYaw());
        e.setPitch(legacy.getPitch());
        e.setRoll(legacy.getRoll());
    }

    private static void onFov(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov e) {
        if (!wants(EntityViewRenderEvent.FOVModifier.class)) return;
        var entity = EntityBridge1122.wrap(e.getCamera().getEntity());
        if (entity == null) return;
        var legacy = new EntityViewRenderEvent.FOVModifier(entityRenderer(), entity, stateAt(e.getCamera()), e.getPartialTick(), (float) e.getFOV());
        post(legacy, "field of view");
        e.setFOV(legacy.getFOV());
    }

    // ---------------------------------------------------------------- tooltips, input, screenshots

    /** The tooltip's lines as 1.12 formatted strings. */
    private static List<String> tooltipLines(net.minecraft.world.item.ItemStack stack) {
        List<String> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        try {
            var flag = mc.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL;
            for (var line : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, flag)) out.add(ClientGui1122.legacyText(line));
        } catch (RuntimeException | LinkageError e) {
            // a tooltip that fails to build has no lines here
        }
        return out;
    }

    private static void onTooltip(net.neoforged.neoforge.client.event.RenderTooltipEvent.Pre e) {
        if (!wants(RenderTooltipEvent.Pre.class) || e.getItemStack().isEmpty()) return;
        var legacy = new RenderTooltipEvent.Pre(Stacks1122.view(e.getItemStack()), tooltipLines(e.getItemStack()), e.getX(), e.getY(), e.getScreenWidth(),
                e.getScreenHeight(), -1, legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer.mcc$INSTANCE);
        RenderContext.beginGui(e.getGraphics());
        try {
            if (post(legacy, "tooltip")) {
                e.setCanceled(true);
                return;
            }
        } finally {
            RenderContext.end();
        }
        e.setX(legacy.getX());
        e.setY(legacy.getY());
    }

    private static void onTooltipColor(net.neoforged.neoforge.client.event.RenderTooltipEvent.Color e) {
        if (!wants(RenderTooltipEvent.Color.class) || e.getItemStack().isEmpty()) return;
        var legacy = new RenderTooltipEvent.Color(Stacks1122.view(e.getItemStack()), tooltipLines(e.getItemStack()), e.getX(), e.getY(),
                legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer.mcc$INSTANCE, e.getBackgroundStart(), e.getBorderStart(), e.getBorderEnd());
        post(legacy, "tooltip colour");
        if (legacy.getBackground() != legacy.getOriginalBackground()) e.setBackground(legacy.getBackground());
        e.setBorderStart(legacy.getBorderStart());
        e.setBorderEnd(legacy.getBorderEnd());
    }

    private static void onInput(net.neoforged.neoforge.client.event.MovementInputUpdateEvent e) {
        if (!wants(InputUpdateEvent.class)) return;
        if (!(EntityBridge1122.wrap(e.getEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player)) return;
        post(new InputUpdateEvent(player, legacyconnector.v1_12_2.net.minecraft.util.MovementInput.mcc$of(e.getInput())), "input update");
    }

    private static void onScreenshot(net.neoforged.neoforge.client.event.ScreenshotEvent e) {
        if (!wants(ScreenshotEvent.class)) return;
        var image = e.getImage();
        java.awt.image.BufferedImage copy = new java.awt.image.BufferedImage(image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                copy.setRGB(x, y, (abgr & 0xFF00FF00) | (abgr & 0xFF) << 16 | (abgr >> 16) & 0xFF);
            }
        }
        var legacy = new ScreenshotEvent(copy, e.getScreenshotFile());
        boolean cancelled = post(legacy, "screenshot");
        e.setScreenshotFile(legacy.getScreenshotFile());
        if (legacy.getResultMessage() != null) e.setResultMessage(legacy.getResultMessage().mcc$modern());
        if (cancelled) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- sound engine

    private static void onSoundSource(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent e) {
        if (!wants(legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.PlaySoundSourceEvent.class)) return;
        post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.PlaySoundSourceEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager.mcc$INSTANCE,
                LegacySounds1122.legacy(e.getSound()), Integer.toHexString(System.identityHashCode(e.getChannel()))), "sound source");
    }

    private static void onStreamingSource(net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent e) {
        if (!wants(legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.PlayStreamingSourceEvent.class)) return;
        post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.PlayStreamingSourceEvent(
                legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager.mcc$INSTANCE, LegacySounds1122.legacy(e.getSound()),
                Integer.toHexString(System.identityHashCode(e.getChannel()))), "streaming sound source");
    }

    private static void onSoundEngineLoad(net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent e) {
        if (!wants(legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.SoundLoadEvent.class)) return;
        post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.SoundLoadEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager.mcc$INSTANCE),
                "sound load");
    }

    // ---------------------------------------------------------------- music

    private static boolean musicFailed;

    private static legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker replacedMusic() {
        return musicFailed || !Client1122.present() ? null : legacyMc().mcc$replacedMusicTicker();
    }

    /** A mod's own music ticker (installed by reflection) is updated every tick and owns the music meanwhile. */
    private static void tickMusic() {
        var ticker = replacedMusic();
        if (ticker == null) return;
        try {
            ticker.update();
        } catch (RuntimeException | LinkageError e) {
            musicFailed = true;
            LOG.warn("The legacy music ticker {} failed; vanilla music plays again: {}", ticker.getClass().getName(), e.toString());
        }
    }
}
