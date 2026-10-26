package legacyconnector.v1_7_10.net.minecraftforge.event;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A furnace asks how long an item burns: set the result to ALLOW and {@link #burnTime} to decide it. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event.HasResult
public class FuelBurnTimeEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
    public final ItemStack fuel;
    public int burnTime;

    public FuelBurnTimeEvent(ItemStack fuel) {
        this.fuel = fuel;
    }
}
