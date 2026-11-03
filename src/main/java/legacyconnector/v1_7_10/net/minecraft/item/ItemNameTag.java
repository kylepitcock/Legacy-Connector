package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** The name tag: right-clicking a living entity gives it the tag's display name (consumes one tag). */
public class ItemNameTag extends Item {
    public ItemNameTag(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemNameTag() {
        super();
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabTools);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        if (!stack.hasDisplayName() || !(entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving living)) return false;
        living.setCustomNameTag(stack.getDisplayName());
        stack.mcc$setCount(stack.mcc$count() - 1);
        return true;
    }
}
