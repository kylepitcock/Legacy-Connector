package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.Camera;
import net.minecraft.client.model.BookModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Client-side 1.20.1 members that 1.21.1 reshaped, ATM 9 gap round 7 (rows in {@code mc-calls.tsv}); kept apart from {@link Round7Compat} so a server never loads them. */
public final class Round7ClientCompat {
    private Round7ClientCompat() {
    }

    /** {@code new SheetedDecalTextureGenerator(delegate, pose, normal, scale)}: 1.21.1 takes the pose entry (both versions invert it). */
    public static SheetedDecalTextureGenerator newSheetedDecal(VertexConsumer delegate, Matrix4f pose, Matrix3f normal, float scale) {
        PoseStack stack = new PoseStack();
        stack.last().pose().set(pose);
        stack.last().normal().set(normal);
        return new SheetedDecalTextureGenerator(delegate, stack.last(), scale);
    }

    /** {@code SkullBlockRenderer.getRenderType(type, profile)}: the profile is a resolvable profile now. */
    public static RenderType skullRenderType(SkullBlock.Type type, GameProfile profile) {
        return SkullBlockRenderer.getRenderType(type, profile == null ? null : new ResolvableProfile(profile));
    }

    /** {@code SkinManager.getInsecureSkinInformation(profile)}: the textures a profile carries, by type (the session service answers a record now). */
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> insecureSkinInformation(SkinManager manager, GameProfile profile) {
        MinecraftProfileTextures textures = Minecraft.getInstance().getMinecraftSessionService().getTextures(profile);
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> out = new EnumMap<>(MinecraftProfileTexture.Type.class);
        if (textures.skin() != null) out.put(MinecraftProfileTexture.Type.SKIN, textures.skin());
        if (textures.cape() != null) out.put(MinecraftProfileTexture.Type.CAPE, textures.cape());
        if (textures.elytra() != null) out.put(MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
        return out;
    }

    /** {@code BakedModel.useAmbientOcclusion(state)}: the model's own flag. */
    public static boolean useAmbientOcclusion(BakedModel model, BlockState state) {
        return model.useAmbientOcclusion();
    }

    /** {@code BakedModel.useAmbientOcclusion(state, renderType)}: the per-render-type answer when the model gives one, else its own flag. */
    public static boolean useAmbientOcclusion(BakedModel model, BlockState state, RenderType renderType) {
        var answer = model.useAmbientOcclusion(state, ModelData.EMPTY, renderType);
        return answer.isDefault() ? model.useAmbientOcclusion() : answer.isTrue();
    }

    // ---- armor textures: Forge's IForgeItem.getArmorTexture(stack, entity, slot, type) became ArmorMaterial layers

    private static final ClassValue<java.lang.reflect.Method> ARMOR_TEXTURE = new ClassValue<>() {
        @Override
        protected java.lang.reflect.Method computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Method m = c.getDeclaredMethod("getArmorTexture", ItemStack.class, Entity.class, EquipmentSlot.class, String.class);
                    m.setAccessible(true);
                    return m;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // next superclass
                }
            }
            return null;
        }
    };

    /** The texture a mod item's own {@code getArmorTexture(stack, entity, slot, type)} names (the method survives in the translated class), or null. */
    public static String legacyArmorTexture(Item item, ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        java.lang.reflect.Method m = ARMOR_TEXTURE.get(item.getClass());
        if (m == null) return null;
        try {
            return (String) m.invoke(item, stack, entity, slot, type);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** {@code item.getArmorTexture(stack, entity, slot, type)} on Item/ArmorItem: dispatches to the override of the item's class; the base answered null. */
    public static String itemArmorTexture(Item item, ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return legacyArmorTexture(item, stack, entity, slot, type);
    }

    /** {@code super.getArmorTexture(...)} of a mod item: Forge's default answered null (use the material's texture). */
    public static String itemArmorTextureSuper(Item item, ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return null;
    }

    /** {@code ClientHooks.getArmorTexture(entity, stack, defaultTexture, slot, type)}: the item's own texture name when it has one, else the default. */
    public static String clientHooksArmorTexture(Entity entity, ItemStack stack, String defaultTexture, EquipmentSlot slot, String type) {
        String custom = legacyArmorTexture(stack.getItem(), stack, entity, slot, type);
        return custom != null ? custom : defaultTexture;
    }

    /** {@code HumanoidArmorLayer.getArmorResource(entity, stack, slot, type)}: the item's own texture, else the first (or overlay) layer of its material. */
    public static ResourceLocation armorResource(Object layer, Entity entity, ItemStack stack, EquipmentSlot slot, String type) {
        String custom = legacyArmorTexture(stack.getItem(), stack, entity, slot, type);
        if (custom != null) return ResourceLocation.parse(custom);
        if (stack.getItem() instanceof ArmorItem armor) {
            var layers = armor.getMaterial().value().layers();
            if (!layers.isEmpty()) {
                var l = "overlay".equals(type) && layers.size() > 1 ? layers.get(1) : layers.get(0);
                return l.texture(slot == EquipmentSlot.LEGS);
            }
        }
        return ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    }

    // ---- camera and book model

    /** {@code Camera.move(x, y, z)}: single precision now (the method is opened by the layer's access transformer). */
    public static void cameraMove(Camera camera, double x, double y, double z) {
        camera.move((float) x, (float) y, (float) z);
    }

    /** {@code BookModel.render(stack, consumer, light, overlay, r, g, b, a)}: one packed colour now. */
    public static void bookRender(BookModel model, PoseStack stack, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
        ClientCompat.renderToBuffer(model, stack, consumer, light, overlay, r, g, b, a);
    }
}
