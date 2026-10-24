package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import java.util.ArrayList;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/** A player died and is about to drop {@link #drops} (graves and soulbound mods change them). */
@Cancelable
public class PlayerDropsEvent extends PlayerEvent {
    public final DamageSource source;
    public final ArrayList<EntityItem> drops;
    public final int lootingLevel;
    public final boolean recentlyHit;

    public PlayerDropsEvent(EntityPlayer player, DamageSource source, ArrayList<EntityItem> drops, boolean recentlyHit) {
        super(player);
        this.source = source;
        this.drops = drops;
        this.lootingLevel = 0;
        this.recentlyHit = recentlyHit;
    }
}
