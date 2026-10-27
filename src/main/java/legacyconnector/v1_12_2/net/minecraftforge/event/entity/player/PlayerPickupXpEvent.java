package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player is about to pick up an experience orb (cancel to leave it). */
@Cancelable
public class PlayerPickupXpEvent extends PlayerEvent {
    private final legacyconnector.v1_12_2.net.minecraft.entity.item.EntityXPOrb orb;

    public PlayerPickupXpEvent(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.entity.item.EntityXPOrb orb) {
        super(player);
        this.orb = orb;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityXPOrb getOrb() {
        return orb;
    }
}
