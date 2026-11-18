package legacyconnector.v1_20_1.net.minecraftforge.event.entity.player;

import legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Forge 1.20.1's pickup event (renamed {@code ItemEntityPickupEvent.Pre} in NeoForge 1.21.1, which posts it; {@code DENY} or cancelling blocks the pickup). */
public class EntityItemPickupEvent extends PlayerEvent implements ICancellableEvent {
    private final ItemEntity item;
    private Event$Result result = Event$Result.DEFAULT;

    public EntityItemPickupEvent(Player player, ItemEntity item) {
        super(player);
        this.item = item;
    }

    public ItemEntity getItem() {
        return item;
    }

    public Event$Result getResult() {
        return result;
    }

    public void setResult(Event$Result result) {
        this.result = result;
    }

    public boolean hasResult() {
        return true;
    }
}
