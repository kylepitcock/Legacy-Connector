package legacyconnector.v1_12_2.net.minecraft.item;

/** The value of an item model property for a stack (selects the model override, e.g. a bow's pull). */
public interface IItemPropertyGetter {
    float apply(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity);
}
