package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.IItemPropertyGetter;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A model override: the model shown when every listed item property is at least its value. */
public class ItemOverride {
    private final ResourceLocation location;
    private final Map<ResourceLocation, Float> mapResourceValues;

    public ItemOverride(ResourceLocation locationIn, Map<ResourceLocation, Float> propertyValues) {
        location = locationIn;
        mapResourceValues = propertyValues;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    boolean matchesItemStack(ItemStack stack, World worldIn, EntityLivingBase livingEntity) {
        Item item = stack.getItem();
        for (Map.Entry<ResourceLocation, Float> entry : mapResourceValues.entrySet()) {
            IItemPropertyGetter getter = item.getPropertyGetter(entry.getKey());
            if (getter == null || getter.apply(stack, worldIn, livingEntity) < entry.getValue()) return false;
        }
        return true;
    }

    public com.google.common.collect.ImmutableMap<ResourceLocation, Float> getMapResourceValues() {
        return com.google.common.collect.ImmutableMap.copyOf(mapResourceValues);
    }
}
