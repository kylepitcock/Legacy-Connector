package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player's attack may be a critical hit: ALLOW forces one, DENY prevents it; the damage modifier scales it. */
@Event.HasResult
public class CriticalHitEvent extends PlayerEvent {
    private float damageModifier;
    private final float oldDamageModifier;
    private final Entity target;
    private final boolean vanillaCritical;

    public CriticalHitEvent(EntityPlayer player, Entity target, float damageModifier, boolean vanillaCritical) {
        super(player);
        this.target = target;
        this.damageModifier = damageModifier;
        this.oldDamageModifier = damageModifier;
        this.vanillaCritical = vanillaCritical;
    }

    public Entity getTarget() {
        return target;
    }

    public void setDamageModifier(float modifier) {
        this.damageModifier = modifier;
    }

    public float getDamageModifier() {
        return damageModifier;
    }

    public float getOldDamageModifier() {
        return oldDamageModifier;
    }

    public boolean isVanillaCritical() {
        return vanillaCritical;
    }
}
