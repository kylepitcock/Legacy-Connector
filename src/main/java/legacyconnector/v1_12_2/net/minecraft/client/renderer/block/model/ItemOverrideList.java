package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * A model's item overrides. Mods subclass it to pick a model per stack ({@link #handleItemState}); the bridge to modern models calls
 * that for every stack drawn with a legacy model.
 */
public class ItemOverrideList {
    public static final ItemOverrideList NONE = new ItemOverrideList();
    private final List<ItemOverride> overrides = Lists.newArrayList();

    private ItemOverrideList() {
    }

    public ItemOverrideList(List<ItemOverride> overridesIn) {
        for (int i = overridesIn.size() - 1; i >= 0; --i) overrides.add(overridesIn.get(i));
    }

    @Deprecated
    public ResourceLocation applyOverride(ItemStack stack, World worldIn, EntityLivingBase entityIn) {
        if (!overrides.isEmpty()) {
            for (ItemOverride override : overrides) if (override.matchesItemStack(stack, worldIn, entityIn)) return override.getLocation();
        }
        return null;
    }

    /** The model for the stack: the matching override's model, or the original one. */
    public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
        if (stack != null && !stack.isEmpty() && stack.getItem().hasCustomProperties()) {
            ResourceLocation location = applyOverride(stack, world, entity);
            if (location != null) {
                IBakedModel model = org.kylepitcock.legacyconnector.v1_12_2.client.Models1122.legacyModel(
                        new ModelResourceLocation(location, "inventory"));
                if (model != null) return model;
            }
        }
        return originalModel;
    }

    public ImmutableList<ItemOverride> getOverrides() {
        return ImmutableList.copyOf(overrides);
    }
}
