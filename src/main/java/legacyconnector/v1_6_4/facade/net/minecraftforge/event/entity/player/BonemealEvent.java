package legacyconnector.v1_6_4.facade.net.minecraftforge.event.entity.player;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code BonemealEvent} for 1.6.4 mods: the block was an id ({@code event.ID}). See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class BonemealEvent {
    private BonemealEvent() {}

    public static int mcc$get$ID(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.BonemealEvent self) {
        return Ids.blockId(self.block);
    }
}
