package legacyconnector.v1_12_2.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** A minecart's server tick (posted after it moved, with the block it is in). */
public class MinecartUpdateEvent extends MinecartEvent {
    private final BlockPos pos;

    public MinecartUpdateEvent(EntityMinecart minecart, BlockPos pos) {
        super(minecart);
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }
}
