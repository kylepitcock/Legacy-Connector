package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/** Client-side signature changes (kept apart from {@link MiscCompat}: this class links client classes). */
public final class ClientCompat {
    private ClientCompat() {}

    /** {@code Minecraft.getFrameTime()}: the partial tick of this frame. */
    public static float getFrameTime(Minecraft mc) {
        return mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static float getDeltaFrameTime(Minecraft mc) {
        return mc.getTimer().getRealtimeDeltaTicks();
    }

    public static float getPartialTick(Minecraft mc) {
        return mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    /** 1.20.1 color arguments (floats) to the packed ARGB int of 1.21.1. */
    public static int packColor(float r, float g, float b, float a) {
        return net.minecraft.util.FastColor.ARGB32.colorFromFloat(a, r, g, b);
    }

    public static float colorR(int argb) {
        return net.minecraft.util.FastColor.ARGB32.red(argb) / 255.0F;
    }

    public static float colorG(int argb) {
        return net.minecraft.util.FastColor.ARGB32.green(argb) / 255.0F;
    }

    public static float colorB(int argb) {
        return net.minecraft.util.FastColor.ARGB32.blue(argb) / 255.0F;
    }

    public static float colorA(int argb) {
        return net.minecraft.util.FastColor.ARGB32.alpha(argb) / 255.0F;
    }

    public static void renderPart(net.minecraft.client.model.geom.ModelPart part, PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay,
                                  float r, float g, float b, float a) {
        part.render(ps, vc, light, overlay, packColor(r, g, b, a));
    }

    public static void renderToBuffer(net.minecraft.client.model.Model model, PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay,
                                      float r, float g, float b, float a) {
        model.renderToBuffer(ps, vc, light, overlay, packColor(r, g, b, a));
    }

    /** {@code EditBox.tick()}: the cursor blink uses the clock now. */
    public static void editBoxTick(net.minecraft.client.gui.components.EditBox box) {}

    public static float getU(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, double u) {
        return sprite.getU((float) (u / 16.0));
    }

    public static float getV(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, double v) {
        return sprite.getV((float) (v / 16.0));
    }

    /** {@code MultiBufferSource.immediate(BufferBuilder)}: the fixed builder is a byte buffer now (the old builder object only supplied memory). */
    public static net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate(CompatBufferBuilder ignored) {
        return net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
    }

    /** {@code MultiBufferSource.immediateWithBuffers(Map, BufferBuilder)}: the per-layer builders and the fallback are byte buffers now (the old builders only supplied memory, layers get theirs on demand). */
    public static net.minecraft.client.renderer.MultiBufferSource.BufferSource immediateWithBuffers(
            java.util.Map<net.minecraft.client.renderer.RenderType, CompatBufferBuilder> ignoredMap, CompatBufferBuilder ignored) {
        return net.minecraft.client.renderer.MultiBufferSource.immediateWithBuffers(new java.util.LinkedHashMap<>(), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
    }

    /** {@code DefaultPlayerSkin.getDefaultSkin(UUID)}: the skin texture of the default skin for that id. */
    public static net.minecraft.resources.ResourceLocation getDefaultSkin(java.util.UUID id) {
        return net.minecraft.client.resources.DefaultPlayerSkin.get(id).texture();
    }

    private static volatile ModelViewStack modelView;

    /**
     * {@code RenderSystem.getModelViewStack()} returned a {@code PoseStack}; 1.21.1 has a JOML {@code Matrix4fStack}. The returned PoseStack mirrors push, pop and
     * transforms onto it (the stack itself stays the single source of the shader's model-view matrix).
     */
    public static PoseStack getModelViewStack() {
        ModelViewStack s = modelView;
        if (s == null) modelView = s = new ModelViewStack(com.mojang.blaze3d.systems.RenderSystem.getModelViewStack());
        return s;
    }

    private static final class ModelViewStack extends PoseStack {
        private final org.joml.Matrix4fStack target;
        private int depth;

        ModelViewStack(org.joml.Matrix4fStack target) {
            this.target = target;
        }

        @Override
        public void pushPose() {
            super.pushPose();
            target.pushMatrix();
            depth++;
        }

        @Override
        public void popPose() {
            if (depth <= 0) return;
            depth--;
            super.popPose();
            target.popMatrix();
        }

        @Override
        public void translate(double x, double y, double z) {
            super.translate(x, y, z);
            target.translate((float) x, (float) y, (float) z);
        }

        @Override
        public void translate(float x, float y, float z) {
            super.translate(x, y, z);
            target.translate(x, y, z);
        }

        @Override
        public void scale(float x, float y, float z) {
            super.scale(x, y, z);
            target.scale(x, y, z);
        }

        @Override
        public void mulPose(org.joml.Quaternionf q) {
            super.mulPose(q);
            target.rotate(q);
        }

        @Override
        public void mulPose(Matrix4f m) {
            super.mulPose(m);
            target.mul(m);
        }

        @Override
        public void rotateAround(org.joml.Quaternionf q, float x, float y, float z) {
            super.rotateAround(q, x, y, z);
            target.rotateAround(q, x, y, z);
        }

        @Override
        public void setIdentity() {
            super.setIdentity();
            target.identity();
        }
    }

    /** {@code Screen.renderBackground(GuiGraphics)}: 1.21.1's screens draw their background themselves before {@code render}. */
    public static void renderBackground(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.gui.GuiGraphics graphics) {
    }

    public static net.minecraft.resources.ResourceLocation skinTexture(net.minecraft.client.player.AbstractClientPlayer player) {
        return player.getSkin().texture();
    }

    public static net.minecraft.client.renderer.item.ItemPropertyFunction itemProperty(net.minecraft.world.item.Item item, net.minecraft.resources.ResourceLocation name) {
        return net.minecraft.client.renderer.item.ItemProperties.getProperty(item.getDefaultInstance(), name);
    }

    /** {@code HumanoidModel.ArmPose.create(name, twoHanded, transformer)}: the enum cannot be extended at run time, the closest pose stands in. */
    public static net.minecraft.client.model.HumanoidModel.ArmPose createArmPose(String name, boolean twoHanded, net.neoforged.neoforge.client.IArmPoseTransformer transformer) {
        return twoHanded ? net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD : net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
    }

    public static com.mojang.blaze3d.platform.Window guiEventWindow(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre event) {
        return Minecraft.getInstance().getWindow();
    }

    private static java.lang.reflect.Method menuRegister;

    /** {@code MenuScreens.register(type, constructor)}: private in 1.21.1 (NeoForge's {@code RegisterMenuScreensEvent} is the public way); called through reflection. */
    @SuppressWarnings("rawtypes")
    public static void registerMenuScreen(net.minecraft.world.inventory.MenuType type, net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor constructor) {
        try {
            if (menuRegister == null) {
                menuRegister = net.minecraft.client.gui.screens.MenuScreens.class.getDeclaredMethod("register", net.minecraft.world.inventory.MenuType.class,
                        net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor.class);
                menuRegister.setAccessible(true);
            }
            menuRegister.invoke(null, type, constructor);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // a mod's client setup that runs twice (its handler reached through two mod containers) registers the same screen again: the first one stays
            if (e.getCause() instanceof IllegalStateException dup && String.valueOf(dup.getMessage()).startsWith("Duplicate registration")) return;
            throw new IllegalStateException("MenuScreens.register failed", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("MenuScreens.register failed", e);
        }
    }

    /** {@code EntityRenderersEvent.AddLayers.getSkin(String)} named the player model "default" or "slim"; 1.21.1 takes {@code PlayerSkin.Model}. */
    public static net.minecraft.client.resources.PlayerSkin.Model skinModel(String name) {
        return "slim".equals(name) ? net.minecraft.client.resources.PlayerSkin.Model.SLIM : net.minecraft.client.resources.PlayerSkin.Model.WIDE;
    }

    /** {@code EntityRenderersEvent.AddLayers.getSkin("default" | "slim")}: the player renderer of that model. */
    @SuppressWarnings("unchecked")
    public static net.minecraft.client.renderer.entity.LivingEntityRenderer getSkin(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event, String name) {
        return (net.minecraft.client.renderer.entity.LivingEntityRenderer) event.getSkin(skinModel(name));
    }

    /** {@code AddLayers.getSkins()} as it is in 1.21.1 (the models), for code that was adapted to read them. */
    public static java.util.Set<net.minecraft.client.resources.PlayerSkin.Model> getSkinModels(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        return event.getSkins();
    }

    /** {@code EntityRenderersEvent.AddLayers.getSkins()}: the skin names ("default", "slim") of 1.20.1 are {@code PlayerSkin.Model} values now. */
    public static java.util.Set<String> getSkins(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (net.minecraft.client.resources.PlayerSkin.Model m : event.getSkins()) out.add(m == net.minecraft.client.resources.PlayerSkin.Model.SLIM ? "slim" : "default");
        return out;
    }

    /** {@code Options.renderDebug} (F3 overlay shown): the debug overlay keeps the flag now. */
    public static boolean getRenderDebug(net.minecraft.client.Options options) {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen();
    }

    public static void setRenderDebug(net.minecraft.client.Options options, boolean show) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen() != show) mc.getDebugOverlay().toggleOverlay();
    }

    // ---- AbstractSelectionList: the x0/x1/y0/y1 bounds are the widget's position and size now

    public static int listX0(net.minecraft.client.gui.components.AbstractSelectionList<?> list) { return list.getX(); }

    public static int listX1(net.minecraft.client.gui.components.AbstractSelectionList<?> list) { return list.getRight(); }

    public static int listY0(net.minecraft.client.gui.components.AbstractSelectionList<?> list) { return list.getY(); }

    public static int listY1(net.minecraft.client.gui.components.AbstractSelectionList<?> list) { return list.getBottom(); }

    public static void setListX0(net.minecraft.client.gui.components.AbstractSelectionList<?> list, int x0) {
        int right = list.getRight();
        list.setX(x0);
        list.setWidth(right - x0);
    }

    public static void setListX1(net.minecraft.client.gui.components.AbstractSelectionList<?> list, int x1) { list.setWidth(x1 - list.getX()); }

    public static void setListY0(net.minecraft.client.gui.components.AbstractSelectionList<?> list, int y0) {
        int bottom = list.getBottom();
        list.setY(y0);
        list.setHeight(bottom - y0);
    }

    public static void setListY1(net.minecraft.client.gui.components.AbstractSelectionList<?> list, int y1) { list.setHeight(y1 - list.getY()); }
}
