package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122;

/** 1.12.2 biped armour layer: armour on the 1.0-inflated biped (0.5 for leggings), only the parts of the slot shown. */
public class LayerBipedArmor extends LayerArmorBase<ModelBiped> {
    private static final Map<Class<?>, Optional<Method>> MODEL_HOOKS = new ConcurrentHashMap<>();

    public LayerBipedArmor(RenderLivingBase<?> rendererIn) {
        super(rendererIn);
    }

    @Override
    protected void initArmor() {
        this.modelLeggings = new ModelBiped(0.5F);
        this.modelArmor = new ModelBiped(1.0F);
    }

    @Override
    protected void setModelSlotVisible(ModelBiped model, EntityEquipmentSlot slotIn) {
        setModelVisible(model);
        switch (slotIn) {
            case HEAD -> {
                model.bipedHead.showModel = true;
                model.bipedHeadwear.showModel = true;
            }
            case CHEST -> {
                model.bipedBody.showModel = true;
                model.bipedRightArm.showModel = true;
                model.bipedLeftArm.showModel = true;
            }
            case LEGS -> {
                model.bipedBody.showModel = true;
                model.bipedRightLeg.showModel = true;
                model.bipedLeftLeg.showModel = true;
            }
            case FEET -> {
                model.bipedRightLeg.showModel = true;
                model.bipedLeftLeg.showModel = true;
            }
            default -> {
            }
        }
    }

    protected void setModelVisible(ModelBiped model) {
        model.setVisible(false);
    }

    /** Forge's {@code Item.getArmorModel(EntityLivingBase, ItemStack, EntityEquipmentSlot, ModelBiped)}; null keeps the default model. */
    @Override
    protected ModelBiped getArmorModelHook(EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot slot, ModelBiped model) {
        Object item = itemStack.getItem();
        Optional<Method> hook = MODEL_HOOKS.computeIfAbsent(item.getClass(), c -> {
            try {
                return Optional.of(c.getMethod("getArmorModel", EntityLivingBase.class, ItemStack.class, EntityEquipmentSlot.class, ModelBiped.class));
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        });
        if (hook.isEmpty()) return model;
        try {
            Object custom = hook.get().invoke(item, entity, itemStack, slot, model);
            return custom instanceof ModelBiped biped ? biped : model;
        } catch (ReflectiveOperationException | RuntimeException e) {
            RenderErrors1122.report("asking " + item.getClass().getName() + " for its armour model", e);
            return model;
        }
    }
}
