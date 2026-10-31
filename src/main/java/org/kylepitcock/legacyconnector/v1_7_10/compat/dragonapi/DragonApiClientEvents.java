package org.kylepitcock.legacyconnector.v1_7_10.compat.dragonapi;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_7_10.net.minecraftforge.client.ForgeHooksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.compat.dragonapi.DragonApiEvents.LegacyEvent;

/**
 * MOD-SPECIFIC COMPAT (mod id "DragonAPI"), client half of {@link DragonApiEvents}: the render events DragonAPI's ASM
 * posted from {@code EntityRenderer}/{@code RenderGlobal}/{@code ForgeHooksClient}.
 *
 * <p>Not reproduced on the client: {@code FarClippingPlaneEvent} (the projection's far plane is fixed in 1.21; VoidMonster
 * and ChromatiCraft shorten it together with the fog, which is bridged), {@code BossColorEvent} (VoidMonster: the boss
 * darkening of the fog/lightmap no longer exists), {@code LightmapEvent}, {@code NightVisionBrightnessEvent},
 * {@code WeatherSkyStrengthEvent}, {@code WaterColorEvent}, {@code CloudRenderEvent}, {@code GetMouseoverEvent},
 * {@code PlayMusicEvent}, {@code HotbarKeyEvent}, {@code CreativeTabGuiRenderEvent}, {@code RenderFirstPersonItemEvent},
 * {@code RenderItemInSlotEvent}, {@code ItemEffectRenderEvent}, {@code BlockIconEvent}, {@code SoundAttenuationDistanceEvent},
 * {@code SettingsEvent}, {@code ChatEvent}, {@code AddParticleEvent} (ExpandedRedstone), and the static watcher callbacks of
 * {@code EntityRenderEvent}, {@code TileEntityRenderEvent}, {@code RenderBlockAtPosEvent} and {@code ChunkWorldRenderEvent}
 * (ChromatiCraft): they all hook vanilla rendering internals (lightmap texture, chunk display lists, GUI slot drawing) that
 * the modern renderer replaced.</p>
 */
public final class DragonApiClientEvents {
    private static final String EVENTS = "Reika.DragonAPI.Instantiable.Event.Client.";

    // EntityRenderer.renderWorld, right after each RenderGlobal.renderEntities call (pass 0, then pass 1): VoidMonster's fog
    // decay and void growth, ChromatiCraft's ore/mob/structure overlays and dimension sky, RotaryCraft's EMP sparks
    static final LegacyEvent ENTITY_RENDERING_LOOP = new LegacyEvent(EVENTS + "EntityRenderingLoopEvent", 2);
    // EntityRenderer.setupFog: every read of farPlaneDistance (fog start and end) went through FogDistanceEvent.fire
    static final LegacyEvent FOG_DISTANCE = new LegacyEvent(EVENTS + "FogDistanceEvent", 1);
    // ForgeHooksClient.getSkyBlendColour's result (VoidMonster's darkening, DragonAPI's Christmas sky)
    static final LegacyEvent SKY_COLOR = new LegacyEvent(EVENTS + "SkyColorEvent", 1);

    private static java.lang.reflect.Field fogDistance;
    private static java.lang.reflect.Field skyColor;

    private DragonApiClientEvents() {}

    static void register(IEventBus bus) {
        bus.addListener(DragonApiClientEvents::onStage);
        // after Legacy Connector's own fog bridge, so the distance handlers see the fog legacy RenderFogEvent handlers set
        bus.addListener(EventPriority.LOW, DragonApiClientEvents::onFog);
    }

    /**
     * 1.7.10 drew entities twice per frame, pass 0 after the opaque terrain and pass 1 after the translucent terrain; DragonAPI
     * posted EntityRenderingLoopEvent after each. The handlers draw in world space relative to the camera, so they get the
     * same render context as RenderWorldLastEvent handlers.
     */
    private static void onStage(RenderLevelStageEvent e) {
        int pass;
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) pass = 0;
        else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) pass = 1;
        else return;
        if (!ENTITY_RENDERING_LOOP.wanted()) return;
        Event ev = ENTITY_RENDERING_LOOP.create(pass, e.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (ev == null) return;
        Minecraft mc = Minecraft.getInstance();
        var cam = e.getCamera().getPosition();
        RenderManager.renderPosX = cam.x;
        RenderManager.renderPosY = cam.y;
        RenderManager.renderPosZ = cam.z;
        var buffers = mc.renderBuffers().bufferSource();
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.last().pose().set(e.getModelViewMatrix());
        pose.last().normal().set(new org.joml.Matrix3f(e.getModelViewMatrix()));
        int previousPass = ForgeHooksClient.renderPass;
        ForgeHooksClient.renderPass = pass; // MinecraftForgeClient.getRenderPass(), as during renderEntities
        RenderContext.beginWorld(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            ENTITY_RENDERING_LOOP.send(ev);
        } finally {
            RenderContext.end();
            ForgeHooksClient.renderPass = previousPass;
            buffers.endBatch();
        }
    }

    /**
     * FogDistanceEvent(farPlaneDistance): handlers lower {@code fogDistance} (VoidMonster near the void or a monster,
     * ChromatiCraft's End fog). 1.7.10's fog start and end were both derived from that distance, so the modern fog planes are
     * scaled by the same ratio.
     */
    private static void onFog(ViewportEvent.RenderFog e) {
        if (e.getType() != net.minecraft.world.level.material.FogType.NONE || !FOG_DISTANCE.wanted()) return;
        float far = Minecraft.getInstance().gameRenderer.getRenderDistance();
        if (far <= 0.0F) return;
        Event ev = FOG_DISTANCE.create(far);
        if (ev == null) return;
        FOG_DISTANCE.send(ev);
        try {
            if (fogDistance == null) fogDistance = ev.getClass().getField("fogDistance");
            float d = fogDistance.getFloat(ev);
            if (!(d >= 0.0F) || d == far) return;
            float ratio = d / far;
            e.setNearPlaneDistance(e.getNearPlaneDistance() * ratio);
            e.setFarPlaneDistance(e.getFarPlaneDistance() * ratio);
            e.setCanceled(true);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // field missing: nothing to apply
        }
    }

    /**
     * Called from {@code mixin/ClientLevelSkyColorMixin} with the sky colour of {@code ClientLevel.getSkyColor}. DragonAPI
     * passed the biome-blended sky colour (before the time-of-day and weather dimming) through SkyColorEvent; here the final
     * colour goes through it, which differs only for handlers that are not simple mixes (VoidMonster mixes towards grey).
     */
    public static Vec3 skyColor(Vec3 color) {
        if (!SKY_COLOR.wanted()) return color;
        int r = (int) Math.round(Math.min(1.0D, Math.max(0.0D, color.x)) * 255.0D);
        int g = (int) Math.round(Math.min(1.0D, Math.max(0.0D, color.y)) * 255.0D);
        int b = (int) Math.round(Math.min(1.0D, Math.max(0.0D, color.z)) * 255.0D);
        int rgb = (r << 16) | (g << 8) | b;
        Event ev = SKY_COLOR.create(rgb);
        if (ev == null) return color;
        SKY_COLOR.send(ev);
        try {
            if (skyColor == null) skyColor = ev.getClass().getField("color");
            int out = skyColor.getInt(ev) & 0xFFFFFF;
            if (out == rgb) return color;
            return new Vec3(((out >> 16) & 255) / 255.0D, ((out >> 8) & 255) / 255.0D, (out & 255) / 255.0D);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return color;
        }
    }
}
