package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.advancements.Advancement;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** A player earned an advancement (posted from the modern earn event). */
public class AdvancementEvent extends PlayerEvent {
    private final Advancement advancement;

    public AdvancementEvent(EntityPlayer player, Advancement earned) {
        super(player);
        this.advancement = earned;
    }

    public Advancement getAdvancement() {
        return advancement;
    }
}
