package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemArmor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122;

/**
 * 1.12.2 armour layer: each equipped {@link ItemArmor} drawn with the armour model of its slot (or the item's own, Forge's
 * {@code Item.getArmorModel}) and its material's texture (or the item's {@code getArmorTexture}), dyed and glinting as 1.12 did.
 */
public abstract class LayerArmorBase<T extends ModelBase> implements LayerRenderer<EntityLivingBase> {
    protected static final ResourceLocation ENCHANTED_ITEM_GLINT_RES = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    protected T modelLeggings;
    protected T modelArmor;
    private final RenderLivingBase<?> renderer;
    private float alpha = 1.0F;
    private float colorR = 1.0F;
    private float colorG = 1.0F;
    private float colorB = 1.0F;
    private boolean skipRenderGlint;
    private static final Map<String, ResourceLocation> ARMOR_TEXTURE_RES_MAP = Maps.newHashMap();
    private static final Map<Class<?>, Method> TEXTURE_HOOKS = new ConcurrentHashMap<>();
    private static final Method NONE = noneMethod();

    public LayerArmorBase(RenderLivingBase<?> rendererIn) {
        this.renderer = rendererIn;
        initArmor();
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        renderArmorLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.CHEST);
        renderArmorLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.LEGS);
        renderArmorLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.FEET);
        renderArmorLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.HEAD);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }

    private void renderArmorLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
            float scale, EntityEquipmentSlot slotIn) {
        ItemStack stack = entity.getItemStackFromSlot(slotIn);
        if (!(stack.getItem() instanceof ItemArmor armor) || armor.getEquipmentSlot() != slotIn) return;
        // vanilla armour is drawn by the modern renderer's own armour layer
        if (armor instanceof org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.VanillaArmor) return;
        T model = getModelFromSlot(slotIn);
        model = getArmorModelHook(entity, stack, slotIn, model);
        if (model == null) return;
        model.setModelAttributes(renderer.getMainModel());
        model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        setModelSlotVisible(model, slotIn);
        renderer.bindTexture(getArmorResource(entity, stack, slotIn, null));
        int color = mcc$dyeColor(armor, stack);
        if (color != -1) {
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            GlStateManager.color(colorR * r, colorG * g, colorB * b, alpha);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            renderer.bindTexture(getArmorResource(entity, stack, slotIn, "overlay"));
        }
        GlStateManager.color(colorR, colorG, colorB, alpha);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (!skipRenderGlint && stack.hasEffect()) renderEnchantedGlint(renderer, entity, model, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
    }

    /** Dyeable armour's colour (ItemArmor.hasOverlay/getColor, leather by default), or -1 for undyed armour. */
    private static int mcc$dyeColor(ItemArmor armor, ItemStack stack) {
        try {
            Method has = armor.getClass().getMethod("hasOverlay", ItemStack.class);
            if (!(boolean) has.invoke(armor, stack)) return -1;
            Method get = armor.getClass().getMethod("getColor", ItemStack.class);
            return (int) get.invoke(armor, stack);
        } catch (NoSuchMethodException e) {
            if (!"leather".equals(armor.getArmorMaterial().getName())) return -1;
            var tag = stack.getTagCompound();
            if (tag != null && tag.hasKey("display", 10) && tag.getCompoundTag("display").hasKey("color", 3)) return tag.getCompoundTag("display").getInteger("color");
            return 10511680;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    public T getModelFromSlot(EntityEquipmentSlot slotIn) {
        return isLegSlot(slotIn) ? modelLeggings : modelArmor;
    }

    private boolean isLegSlot(EntityEquipmentSlot slotIn) {
        return slotIn == EntityEquipmentSlot.LEGS;
    }

    /** The glint: the model again with the scrolling glint texture, blended additively (two passes, as 1.12). */
    public static void renderEnchantedGlint(RenderLivingBase<?> renderer, EntityLivingBase entity, ModelBase model, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        float time = (float) Entity.mcc$get$ticksExisted(entity) + partialTicks;
        renderer.bindTexture(ENCHANTED_ITEM_GLINT_RES);
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(514);
        GlStateManager.depthMask(false);
        GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
        for (int i = 0; i < 2; ++i) {
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
            GlStateManager.color(0.38F, 0.19F, 0.608F, 1.0F);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.scale(0.33333334F, 0.33333334F, 0.33333334F);
            GlStateManager.rotate(30.0F - (float) i * 60.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, time * (0.001F + (float) i * 0.003F) * 20.0F, 0.0F);
            GlStateManager.matrixMode(5888);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
        GlStateManager.disableBlend();
    }

    protected abstract void initArmor();

    protected abstract void setModelSlotVisible(T model, EntityEquipmentSlot slotIn);

    /** Forge: the item's own armour model ({@code Item.getArmorModel}), else the layer's. */
    protected T getArmorModelHook(EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot slot, T model) {
        return model;
    }

    /** The armour texture: {@code <domain>:textures/models/armor/<material>_layer_<1|2>[_<type>].png}, or the item's getArmorTexture. */
    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EntityEquipmentSlot slot, String type) {
        ItemArmor item = (ItemArmor) stack.getItem();
        String texture = item.getArmorMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String path = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, isLegSlot(slot) ? 2 : 1, type == null ? "" : String.format("_%s", type));
        String custom = mcc$itemTexture(item, stack, entity, slot, type);
        if (custom != null) path = custom;
        ResourceLocation location = ARMOR_TEXTURE_RES_MAP.get(path);
        if (location == null) {
            location = new ResourceLocation(path);
            ARMOR_TEXTURE_RES_MAP.put(path, location);
        }
        return location;
    }

    /** Forge's {@code Item.getArmorTexture(ItemStack, Entity, EntityEquipmentSlot, String)}, when the item declares it. */
    private static String mcc$itemTexture(ItemArmor item, ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        Method m = TEXTURE_HOOKS.computeIfAbsent(item.getClass(), c -> {
            try {
                return c.getMethod("getArmorTexture", ItemStack.class, Entity.class, EntityEquipmentSlot.class, String.class);
            } catch (NoSuchMethodException e) {
                return NONE;
            }
        });
        if (m == NONE) return null;
        try {
            Object r = m.invoke(item, stack, entity, slot, type);
            return r instanceof String s ? s : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            RenderErrors1122.report("asking " + item.getClass().getName() + " for its armour texture", e);
            return null;
        }
    }

    private static Method noneMethod() {
        try {
            return Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
