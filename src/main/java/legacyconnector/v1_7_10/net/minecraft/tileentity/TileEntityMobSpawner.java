package legacyconnector.v1_7_10.net.minecraft.tileentity;

import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** Legacy view of a vanilla spawner block (see TileEntityBridge): legacy code picks the mob through its spawner logic. */
public class TileEntityMobSpawner extends TileEntity {
    private final MobSpawnerBaseLogic logic;

    public TileEntityMobSpawner() {
        this.logic = new MobSpawnerBaseLogic(null);
    }

    public TileEntityMobSpawner(SpawnerBlockEntity host) {
        this.logic = new MobSpawnerBaseLogic(host);
    }

    public MobSpawnerBaseLogic getSpawnerBaseLogic() {
        return logic;
    }

    /** 1.7.10 MCP has no readable name for this one: mods call it by its SRG name. */
    public MobSpawnerBaseLogic func_145881_a() {
        return logic;
    }
}
