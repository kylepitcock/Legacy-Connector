package legacyconnector.v1_20_1.net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Forge 1.20.1's bucket filling event (NeoForge 1.21.1 dropped it for the fluid-handler interaction); a stand-in that is not fired. */
public class FillBucketEvent extends PlayerEvent {
    private final ItemStack current;
    private final HitResult target;
    private final Level level;
    private ItemStack result;

    public FillBucketEvent(Player player, ItemStack current, Level level, HitResult target) {
        super(player);
        this.current = current;
        this.level = level;
        this.target = target;
    }

    public ItemStack getEmptyBucket() {
        return current;
    }

    public HitResult getTarget() {
        return target;
    }

    public Level getLevel() {
        return level;
    }

    public ItemStack getFilledBucket() {
        return result;
    }

    public void setFilledBucket(ItemStack bucket) {
        this.result = bucket;
    }
}
