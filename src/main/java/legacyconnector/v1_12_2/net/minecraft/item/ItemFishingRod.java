package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.ActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 fishing rod: casts a bobber, or reels the cast one in (damaging the rod by what it caught). The cast and the reel are the modern
 * rod's (its hook takes lure and luck from the held stack), so mod rods extending this class fish like vanilla's.
 */
public class ItemFishingRod extends Item {
    public ItemFishingRod() {
        setMaxDamage(64);
        setMaxStackSize(1);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TOOLS);
        addPropertyOverride(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("cast"), (stack, world, entity) -> {
            if (entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.player.Player p) || p.fishing == null) return 0.0F;
            boolean main = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(p.getMainHandItem()).getItem() == stack.getItem();
            boolean off = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(p.getOffhandItem()).getItem() == stack.getItem();
            if (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem) off = false;
            return main || off ? 1.0F : 0.0F;
        });
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    /** Casts or reels in through the modern rod (the held stack is damaged by the catch, the hook carries its lure and luck). */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack held = mcc$held(playerIn, handIn);
        var p = playerIn == null ? null : playerIn.mcc$player();
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (p == null || level == null) return new ActionResult<>(EnumActionResult.PASS, held);
        var hand = handIn == EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
        net.minecraft.world.item.Items.FISHING_ROD.use(level, p, hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, mcc$held(playerIn, handIn));
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
