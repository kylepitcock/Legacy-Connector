package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import java.util.ArrayList;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/** A mob died and is about to drop {@link #drops}; listeners add, remove or replace them. */
@Cancelable
public class LivingDropsEvent extends LivingEvent {
    public final DamageSource source;
    public final ArrayList<EntityItem> drops;
    public final int lootingLevel;
    public final boolean recentlyHit;
    public final int specialDropValue;

    public LivingDropsEvent(EntityLivingBase entity, DamageSource source, ArrayList<EntityItem> drops, int lootingLevel, boolean recentlyHit, int specialDropValue) {
        super(entity);
        this.source = source;
        this.drops = drops;
        this.lootingLevel = lootingLevel;
        this.recentlyHit = recentlyHit;
        this.specialDropValue = specialDropValue;
    }
}
