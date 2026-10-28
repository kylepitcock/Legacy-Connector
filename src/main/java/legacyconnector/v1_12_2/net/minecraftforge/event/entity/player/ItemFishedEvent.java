package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player reels in a catch (cancel to catch nothing; damage the rod more). */
@Cancelable
public class ItemFishedEvent extends PlayerEvent {
    private final legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> stacks = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.create();
    private final legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook hook;
    private int rodDamage;

    public ItemFishedEvent(java.util.List<ItemStack> stacks, int rodDamage, legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook hook) {
        super(hook == null ? null : hook.getAngler());
        this.stacks.addAll(stacks);
        this.rodDamage = rodDamage;
        this.hook = hook;
    }

    public int getRodDamage() {
        return rodDamage;
    }

    public void damageRodBy(int damage) {
        this.rodDamage = damage;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> getDrops() {
        return stacks;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook getHookEntity() {
        return hook;
    }
}
