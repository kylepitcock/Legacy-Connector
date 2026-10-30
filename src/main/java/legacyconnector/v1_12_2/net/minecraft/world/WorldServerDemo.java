package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 demo world: the settings of the demo's fixed world (mods' main menus start it with these). */
public class WorldServerDemo extends WorldServer {
    private static final long DEMO_WORLD_SEED = "North Carolina".hashCode();
    public static final WorldSettings DEMO_WORLD_SETTINGS = new WorldSettings(DEMO_WORLD_SEED, GameType.SURVIVAL, true, false, WorldType.DEFAULT)
            .enableBonusChest();

    public WorldServerDemo(net.minecraft.world.level.LevelAccessor access) {
        super(access);
    }
}
