package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.particle.Particle;
import legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Ticks the 1.12.2 mods' particles with the client and draws them right after the vanilla particles. */
public final class LegacyParticles1122 {
    private LegacyParticles1122() {}

    private static final java.util.Map<Class<?>, Boolean> CUSTOM = new java.util.concurrent.ConcurrentHashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LegacyParticles1122::onTick);
        NeoForge.EVENT_BUS.addListener(LegacyParticles1122::onRender);
    }

    private static ParticleManager effects() {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().effectRenderer;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!effects().mcc$particles().isEmpty()) effects().clearEffects(null);
            return;
        }
        if (!mc.isPaused()) effects().updateEffects();
    }

    /** Whether the particle's class draws itself (overrides renderParticle below the shim). */
    private static boolean custom(Class<?> cls) {
        return CUSTOM.computeIfAbsent(cls, c -> {
            for (Class<?> k = c; k != null && !k.getName().startsWith("legacyconnector."); k = k.getSuperclass()) {
                for (var m : k.getDeclaredMethods()) if (m.getName().equals("renderParticle") && m.getParameterCount() == 8) return true;
            }
            return false;
        });
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var list = effects().mcc$particles();
        if (list.isEmpty()) return;
        var camera = event.getCamera();
        var cam = camera.getPosition();
        Particle.interpPosX = cam.x;
        Particle.interpPosY = cam.y;
        Particle.interpPosZ = cam.z;
        var look = camera.getLookVector();
        Particle.cameraViewDir = new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(look.x(), look.y(), look.z());
        // 1.12's ActiveRenderInfo camera basis, in the order renderParticle takes it
        double yaw = Math.toRadians(camera.getYRot());
        double pitch = Math.toRadians(camera.getXRot());
        float rx = (float) Math.cos(yaw);
        float rz = (float) Math.sin(yaw);
        float ryz = (float) (-rz * Math.sin(pitch));
        float rxy = (float) (rx * Math.sin(pitch));
        float rxz = (float) Math.cos(pitch);
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderContext.beginWorld(event.getPoseStack(), buffers, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        try {
            RenderContext.setBlend(true);
            RenderContext.bindTexture(org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern("minecraft", "textures/particle/particles.png"));
            for (Particle p : list) {
                try {
                    if (custom(p.getClass())) {
                        ParticleManager.CustomRenderer r = ParticleManager.mcc$customRenderer;
                        if (r != null) r.render(p, partial, rx, rxz, rz, ryz, rxy);
                        continue;
                    }
                    if (p.getFXLayer() != 0) continue; // a sprite of the block atlas: drawn by the rendering shim's renderParticle
                    double[][] q = p.mcc$defaultQuad(partial, rx, rxz, rz, ryz, rxy);
                    int light = p.getBrightnessForRender(partial);
                    int argb = p.mcc$argb();
                    double[][] v = new double[4][];
                    for (int i = 0; i < 4; i++) v[i] = new double[] {q[i][0], q[i][1], q[i][2], q[i][3], q[i][4], argb, light};
                    RenderContext.quad(v, 0.0F, 1.0F, 0.0F);
                } catch (RuntimeException | LinkageError e) {
                    p.setExpired();
                }
            }
        } finally {
            RenderContext.resetState();
            RenderContext.end();
        }
        buffers.endBatch();
    }

    /** A vanilla particle by its 1.12 {@code EnumParticleTypes} id, as the modern particle with the same look. */
    public static void spawnVanilla(int id, double x, double y, double z, double vx, double vy, double vz, int... params) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ParticleOptions options = options(id, vx, vy, vz, params);
        if (options == null) return;
        if (id == 30) {
            vx = vy = vz = 0.0D; // the colour was passed as the speed
        }
        level.addParticle(options, x, y, z, vx, vy, vz);
    }

    private static ParticleOptions options(int id, double vx, double vy, double vz, int[] params) {
        return switch (id) {
            case 0 -> ParticleTypes.POOF;
            case 1 -> ParticleTypes.EXPLOSION;
            case 2 -> ParticleTypes.EXPLOSION_EMITTER;
            case 3 -> ParticleTypes.FIREWORK;
            case 4 -> ParticleTypes.BUBBLE;
            case 5 -> ParticleTypes.SPLASH;
            case 6 -> ParticleTypes.FISHING;
            case 7, 8 -> ParticleTypes.UNDERWATER;
            case 9 -> ParticleTypes.CRIT;
            case 10 -> ParticleTypes.ENCHANTED_HIT;
            case 11 -> ParticleTypes.SMOKE;
            case 12 -> ParticleTypes.LARGE_SMOKE;
            case 13 -> ParticleTypes.EFFECT;
            case 14 -> ParticleTypes.INSTANT_EFFECT;
            case 15, 16 -> net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, (float) vx, (float) vy, (float) vz);
            case 17 -> ParticleTypes.WITCH;
            case 18 -> ParticleTypes.DRIPPING_WATER;
            case 19 -> ParticleTypes.DRIPPING_LAVA;
            case 20 -> ParticleTypes.ANGRY_VILLAGER;
            case 21 -> ParticleTypes.HAPPY_VILLAGER;
            case 22 -> ParticleTypes.MYCELIUM;
            case 23 -> ParticleTypes.NOTE;
            case 24 -> ParticleTypes.PORTAL;
            case 25 -> ParticleTypes.ENCHANT;
            case 26 -> ParticleTypes.FLAME;
            case 27 -> ParticleTypes.LAVA;
            case 29 -> ParticleTypes.CLOUD;
            case 30 -> new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(vx == 0.0D ? 1.0F : (float) vx, (float) vy, (float) vz), 1.0F);
            case 31, 32 -> ParticleTypes.ITEM_SNOWBALL;
            case 33 -> ParticleTypes.ITEM_SLIME;
            case 34 -> ParticleTypes.HEART;
            case 36 -> item(params);
            case 37, 38, 46 -> block(id == 46 ? ParticleTypes.FALLING_DUST : ParticleTypes.BLOCK, params);
            case 39 -> ParticleTypes.RAIN;
            case 41 -> ParticleTypes.ELDER_GUARDIAN;
            case 42 -> ParticleTypes.DRAGON_BREATH;
            case 43 -> ParticleTypes.END_ROD;
            case 44 -> ParticleTypes.DAMAGE_INDICATOR;
            case 45 -> ParticleTypes.SWEEP_ATTACK;
            case 47 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 48 -> ParticleTypes.SPIT;
            default -> null;
        };
    }

    private static ParticleOptions item(int[] params) {
        if (params == null || params.length == 0) return null;
        var item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemById(params[0]);
        if (item == null) return null;
        var stack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(
                new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, params.length > 1 ? params[1] : 0));
        return stack.isEmpty() ? null : new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    private static ParticleOptions block(net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.BlockParticleOption> type, int[] params) {
        if (params == null || params.length == 0) return null;
        var state = legacyconnector.v1_12_2.net.minecraft.block.Block.getStateById(params[0]);
        var modern = state == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
        return modern == null ? null : new net.minecraft.core.particles.BlockParticleOption(type, modern);
    }
}
