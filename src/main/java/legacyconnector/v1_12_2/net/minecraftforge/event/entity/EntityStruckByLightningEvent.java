package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

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

/** Lightning hits an entity (cancel to spare it). */
@Cancelable
public class EntityStruckByLightningEvent extends EntityEvent {
    private final legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt lightning;

    public EntityStruckByLightningEvent(Entity entity, legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt lightning) {
        super(entity);
        this.lightning = lightning;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt getLightning() {
        return lightning;
    }
}
