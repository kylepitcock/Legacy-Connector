package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.ActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 bow: drawn while the use key is held, shoots an arrow from the player's ammunition when released. The shot itself is the
 * modern bow's (velocity from the draw time, power/punch/flame/infinity), fired from a vanilla bow stack carrying this stack's
 * enchantments and damage, which are copied back.
 */
public class ItemBow extends Item {
    public ItemBow() {
        this.maxStackSize = 1;
        setMaxDamage(384);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.COMBAT);
        addPropertyOverride(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("pull"), (stack, world, entity) -> {
            if (entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) || !l.isUsingItem()) return 0.0F;
            var using = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(l.getUseItem());
            return using.getItem() != this ? 0.0F : (float) (stack.getMaxItemUseDuration() - l.getUseItemRemainingTicks()) / 20.0F;
        });
        addPropertyOverride(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("pulling"), (stack, world, entity) ->
                entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l && l.isUsingItem()
                        && org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(l.getUseItem()).getItem() == this ? 1.0F : 0.0F);
    }

    /** The arrows the player would shoot (offhand, main hand, then the inventory), or EMPTY. */
    protected ItemStack findAmmo(EntityPlayer player) {
        if (player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)) return ItemStack.EMPTY;
        if (isArrow(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(p.getOffhandItem()))) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(p.getOffhandItem());
        }
        if (isArrow(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(p.getMainHandItem()))) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(p.getMainHandItem());
        }
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var s = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(inv.getItem(i));
            if (isArrow(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    protected boolean isArrow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemArrow) return true;
        var b = stack.mcc$backing();
        return b != null && b.getItem() instanceof net.minecraft.world.item.ArrowItem;
    }

    /** Shoots like the vanilla bow (the modern code), from a vanilla bow stack standing in for this one. */
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (world == null || entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l)) return;
        var modern = stack.mcc$backing() != null ? stack.mcc$backing() : org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        var vanilla = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW);
        var enchants = modern.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchants != null) vanilla.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchants);
        vanilla.setDamageValue(modern.getDamageValue());
        net.minecraft.world.item.Items.BOW.releaseUsing(vanilla, world.mcc$level(), l, timeLeft);
        if (vanilla.isEmpty()) modern.shrink(1);
        else if (modern.isDamageableItem()) modern.setDamageValue(Math.min(vanilla.getDamageValue(), modern.getMaxDamage()));
        else stack.setItemDamage(vanilla.getDamageValue());
    }

    /** How far a bow drawn for this many ticks throws (1 when fully drawn after 20 ticks). */
    public static float getArrowVelocity(int charge) {
        float f = (float) charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = mcc$held(player, hand);
        boolean hasAmmo = !findAmmo(player).isEmpty();
        var p = player == null ? null : player.mcc$entity instanceof net.minecraft.world.entity.player.Player pl ? pl : null;
        if (p == null || !p.getAbilities().instabuild && !hasAmmo) return new ActionResult<>(EnumActionResult.FAIL, held);
        p.startUsingItem(hand == EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
