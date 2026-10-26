package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.ClientChatReceivedEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.EntityViewRenderEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderBlockOverlayEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderHandEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderItemInFrameEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.bus.api.IEventBus;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge client events of 1.7.10 fired from their NeoForge counterparts: fog ({@code FogDensity}, {@code RenderFogEvent}),
 * the block overlay on the screen, the first-person hand, the item in an item frame, received chat, and the armor of a
 * player ({@code RenderPlayerEvent.SetArmorModel}, from the legacy armor layer). Handlers that draw get a render context
 * over the event's pose stack.
 */
public final class ClientEvents1710 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ClientEvents");
    private static int failures;

    private ClientEvents1710() {}

    private static boolean wants(Class<? extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    private static void failed(String what, Throwable e) {
        if (failures++ < 5) LOG.warn("A legacy {} handler failed: {}", what, e.toString());
    }

    public static void register(IEventBus bus) {
        bus.addListener(ClientEvents1710::onFog);
        bus.addListener(ClientEvents1710::onBlockOverlay);
        bus.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e) -> frame++);
        bus.addListener(ClientEvents1710::onHand);
        bus.addListener(ClientEvents1710::onItemInFrame);
        bus.addListener(ClientEvents1710::onChat);
    }

    // ---------------------------------------------------------------- fog

    /**
     * 1.7.10's setupFog: FogDensity first (cancelled, the handler's density makes exponential fog, drawn here as linear fog
     * reaching as far), then, for the plain fog outside fluids and blindness, RenderFogEvent, whose handlers set the fog
     * with glFogf (captured by {@link LegacyFog}).
     */
    private static void onFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog e) {
        boolean density = wants(EntityViewRenderEvent.FogDensity.class);
        boolean render = wants(EntityViewRenderEvent.RenderFogEvent.class);
        if (!density && !render) return;
        if (!(EntityBridge.wrap(e.getCamera().getEntity()) instanceof EntityLivingBase living)) return;
        LegacyFog.begin();
        try {
            if (density) {
                var event = new EntityViewRenderEvent.FogDensity(living, e.getPartialTick(), 0.1F);
                if (MinecraftForge.EVENT_BUS.post(event)) {
                    e.setNearPlaneDistance(0.0F);
                    e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), LegacyFog.expEnd(event.density)));
                    e.setCanceled(true);
                    return;
                }
            }
            if (!render || e.getType() != net.minecraft.world.level.material.FogType.NONE) return;
            if (e.getCamera().getEntity() instanceof net.minecraft.world.entity.LivingEntity l
                    && (l.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) || l.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))) return;
            int mode = e.getMode() == net.minecraft.client.renderer.FogRenderer.FogMode.FOG_SKY ? -1 : 0;
            float far = Minecraft.getInstance().gameRenderer.getRenderDistance();
            MinecraftForge.EVENT_BUS.post(new EntityViewRenderEvent.RenderFogEvent(living, e.getPartialTick(), mode, far));
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
        } catch (RuntimeException | LinkageError ex) {
            failed("fog", ex);
        } finally {
            LegacyFog.end();
        }
    }

    // ---------------------------------------------------------------- block overlay

    private static void onBlockOverlay(net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent e) {
        if (!wants(RenderBlockOverlayEvent.class)) return;
        if (!(EntityBridge.wrap(e.getPlayer()) instanceof EntityPlayer player)) return;
        RenderBlockOverlayEvent.OverlayType type = switch (e.getOverlayType()) {
            case FIRE -> RenderBlockOverlayEvent.OverlayType.FIRE;
            case WATER -> RenderBlockOverlayEvent.OverlayType.WATER;
            default -> RenderBlockOverlayEvent.OverlayType.BLOCK;
        };
        var block = BlockBridge.legacyBlock(e.getBlockState());
        if (block == null) block = legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        var pos = e.getBlockPos();
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderContext.beginWorld(e.getPoseStack(), buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            if (MinecraftForge.EVENT_BUS.post(new RenderBlockOverlayEvent(player, partial, type, block, pos.getX(), pos.getY(), pos.getZ()))) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            failed("block overlay", ex);
        } finally {
            RenderContext.end();
            buffers.endBatch();
        }
    }

    // ---------------------------------------------------------------- hand

    private static long frame;
    private static long handFrame = -1;
    private static boolean handCanceled;

    /** 1.7.10 drew one hand: the legacy event fires with the first hand of each frame, and its answer holds for both. */
    private static void onHand(net.neoforged.neoforge.client.event.RenderHandEvent e) {
        if (handFrame != frame) {
            handFrame = frame;
            handCanceled = false;
            if (!wants(RenderHandEvent.class)) return;
            RenderContext.beginWorld(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
            try {
                handCanceled = MinecraftForge.EVENT_BUS.post(new RenderHandEvent(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().renderGlobal,
                        e.getPartialTick(), 2));
            } catch (RuntimeException | LinkageError ex) {
                failed("hand", ex);
            } finally {
                RenderContext.end();
            }
        }
        if (handCanceled) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- item frames

    private static void onItemInFrame(net.neoforged.neoforge.client.event.RenderItemInFrameEvent e) {
        if (!wants(RenderItemInFrameEvent.class)) return;
        if (!(EntityBridge.wrap(e.getItemFrameEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItemFrame frameEntity)) return;
        RenderContext.beginWorld(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        try {
            if (MinecraftForge.EVENT_BUS.post(new RenderItemInFrameEvent(frameEntity,
                    legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItemFrame.INSTANCE))) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            failed("item frame", ex);
        } finally {
            RenderContext.end();
        }
    }

    // ---------------------------------------------------------------- chat

    private static void onChat(net.neoforged.neoforge.client.event.ClientChatReceivedEvent e) {
        if (!wants(ClientChatReceivedEvent.class)) return;
        try {
            var original = legacyconnector.v1_7_10.net.minecraft.util.IChatComponent.mcc$fromModern(e.getMessage());
            var event = new ClientChatReceivedEvent(original);
            if (MinecraftForge.EVENT_BUS.post(event) || event.message == null) {
                e.setCanceled(true);
            } else if (event.message != original) {
                e.setMessage(event.message.mcc$toModern());
            }
        } catch (RuntimeException | LinkageError ex) {
            failed("chat", ex);
        }
    }

    // ---------------------------------------------------------------- armor

    /**
     * SetArmorModel for a piece of legacy armor about to be drawn on a player ({@code type}: 0 helmet .. 3 boots, the
     * event's slot counts the other way). False when a handler answered that nothing is drawn for it.
     */
    public static boolean armorPiece(net.minecraft.world.entity.LivingEntity entity, int type, net.minecraft.world.item.ItemStack stack, float partial) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player) || !wants(RenderPlayerEvent.SetArmorModel.class)) return true;
        if (!(EntityBridge.wrap(entity) instanceof EntityPlayer player)) return true;
        try {
            var event = new RenderPlayerEvent.SetArmorModel(player,
                    legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager.instance.mcc$playerRenderer(), 3 - type, partial,
                    legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack));
            MinecraftForge.EVENT_BUS.post(event);
            return event.result == -1 || event.result > 0;
        } catch (RuntimeException | LinkageError ex) {
            failed("armor model", ex);
            return true;
        }
    }
}
