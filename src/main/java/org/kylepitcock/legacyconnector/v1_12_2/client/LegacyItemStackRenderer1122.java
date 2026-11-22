package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Draws the 1.12.2 items whose model is built in ({@code IBakedModel.isBuiltInRenderer}), as 1.12's RenderItem did: through the item's
 * {@code TileEntityItemStackRenderer} (Item.setTileEntityItemStackRenderer), else through the tile entity renderer bound to the class
 * the mod gave {@code ForgeHooksClient.registerTESRItemStack}, drawing a fresh tile entity of that class at the origin.
 */
public final class LegacyItemStackRenderer1122 extends BlockEntityWithoutLevelRenderer {
    private final java.util.Map<Class<?>, TileEntity> tiles = new java.util.HashMap<>();

    public LegacyItemStackRenderer1122() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /**
     * CodeChickenLib's {@code IItemRenderer}: a baked model a mod registered for its item (ModelRegistryHelper.register) that draws the item itself
     * in {@code renderItem(stack, transformType)} (Thermal Expansion's Strongbox, cell frames, ...). 1.12's CCRenderItem called it where the
     * model said it was built in; the modern renderer gets here instead and finds the model again.
     */
    private boolean drawItemRenderer(net.minecraft.world.item.ItemStack stack, ItemStack view, ItemDisplayContext context) throws ReflectiveOperationException {
        var model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        if (!(model instanceof Models1122.ModernView mv)) return false;
        Object legacyModel = mv.mcc$legacy();
        java.lang.reflect.Method m = itemRendererMethod(legacyModel.getClass());
        if (m == null) return false;
        m.invoke(legacyModel, view, legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.mcc$of(context));
        return true;
    }

    private static final java.util.Map<Class<?>, java.util.Optional<java.lang.reflect.Method>> ITEM_RENDERERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The {@code renderItem(ItemStack, TransformType)} of a class implementing an interface called IItemRenderer, or null. */
    private static java.lang.reflect.Method itemRendererMethod(Class<?> cls) {
        return ITEM_RENDERERS.computeIfAbsent(cls, c -> {
            for (Class<?> k = c; k != null; k = k.getSuperclass()) {
                for (Class<?> i : allInterfaces(k)) {
                    if (!i.getSimpleName().equals("IItemRenderer")) continue;
                    for (var m : i.getMethods()) {
                        if (m.getName().equals("renderItem") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == ItemStack.class) {
                            try {
                                var impl = c.getMethod(m.getName(), m.getParameterTypes());
                                impl.setAccessible(true);
                                return java.util.Optional.of(impl);
                            } catch (NoSuchMethodException | RuntimeException e) {
                                return java.util.Optional.empty();
                            }
                        }
                    }
                }
            }
            return java.util.Optional.empty();
        }).orElse(null);
    }

    private static java.util.List<Class<?>> allInterfaces(Class<?> c) {
        java.util.List<Class<?>> out = new java.util.ArrayList<>();
        java.util.ArrayDeque<Class<?>> todo = new java.util.ArrayDeque<>(java.util.Arrays.asList(c.getInterfaces()));
        while (!todo.isEmpty()) {
            Class<?> i = todo.poll();
            out.add(i);
            todo.addAll(java.util.Arrays.asList(i.getInterfaces()));
        }
        return out;
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        ItemStack view = Stacks1122.fromModern(stack);
        if (view == null || view.isEmpty()) return;
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            Tessellator.mcc$reset();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >>> 16);
            var item = view.getItem();
            if (drawItemRenderer(stack, view, context)) return;
            // 1.12 asked the item for the stack's metadata when it looked the model up, just before drawing it; mods use that call to hand
            // the stack to their renderer (Lycanites' parts keep it in a static there)
            item.getMetadata(view);
            TileEntityItemStackRenderer teisr = item.getTileEntityItemStackRenderer();
            if (teisr != null && teisr != TileEntityItemStackRenderer.instance) {
                teisr.renderByItem(view); // 1.12 called the one-argument form (it forwards to the two-argument one): Mekanism overrides only that, so its generators, energy cubes and jetpacks drew nothing
                return;
            }
            Class<?> cls = legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.mcc$tesrItem(item, view.getMetadata());
            if (cls == null) return;
            TileEntity te = tiles.get(cls);
            if (te == null) {
                var ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                te = (TileEntity) ctor.newInstance();
                tiles.put(cls, te);
            }
            TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
            if (dispatcher.renderEngine == null) dispatcher.renderEngine = TextureManager.mcc$instance();
            dispatcher.render(te, 0.0D, 0.0D, 0.0D, 0.0F, -1, 1.0F);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            RenderErrors1122.report("drawing a built-in item model", e);
        } finally {
            Tessellator.mcc$reset();
            RenderContext.end();
        }
    }
}
