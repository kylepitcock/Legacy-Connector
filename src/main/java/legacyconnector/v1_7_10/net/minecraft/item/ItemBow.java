package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityArrow;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ArrowLooseEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ArrowNockEvent;

/**
 * The 1.7.10 {@code net.minecraft.item.ItemBow}, ported: drawing (72000 ticks, bow pose), {@link ArrowNockEvent} on right click and
 * {@link ArrowLooseEvent} plus the vanilla arrow on release. Mod bows that extend it and override only some of these (Thaumcraft's bone
 * bow overrides the click and the release but inherits the draw duration) need the rest, or the draw ends in the same tick (#225).
 */
public class ItemBow extends legacyconnector.v1_7_10.net.minecraft.item.Item {
    public static final String[] bowPullIconNameArray = {"pulling_0", "pulling_1", "pulling_2"};

    public ItemBow(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemBow() {
        super();
        this.maxStackSize = 1;
        this.setMaxDamage(384);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
        int charge = getMaxItemUseDuration(stack) - itemInUseCount;
        ArrowLooseEvent event = new ArrowLooseEvent(player, stack, charge);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;
        charge = event.charge;
        boolean infinite = PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)
                || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) > 0;
        if (!infinite && !player.inventory.hasItem(Items.arrow)) return;
        float f = charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f < 0.1D) return;
        if (f > 1.0F) f = 1.0F;
        EntityArrow arrow = new EntityArrow(world, player, f * 2.0F);
        if (f == 1.0F) arrow.setIsCritical(true);
        int power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
        if (power > 0) arrow.setDamage(arrow.getDamage() + power * 0.5D + 0.5D);
        int punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
        if (punch > 0) arrow.setKnockbackStrength(punch);
        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) > 0) arrow.setFire(100);
        stack.damageItem(1, player);
        world.playSoundAtEntity(player, "random.bow", 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);
        if (infinite) arrow.canBePickedUp = 2;
        else player.inventory.consumeInventoryItem(Items.arrow);
        if (!world.isRemote) world.spawnEntityInWorld(arrow);
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.bow;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        ArrowNockEvent event = new ArrowNockEvent(player, stack);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return event.result;
        if (PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities) || player.inventory.hasItem(Items.arrow)) {
            player.setItemInUse(stack, getMaxItemUseDuration(stack));
        }
        return stack;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
