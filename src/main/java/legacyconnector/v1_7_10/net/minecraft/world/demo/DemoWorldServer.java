package legacyconnector.v1_7_10.net.minecraft.world.demo;

import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSettings;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;

/** The demo world: a fixed seed ("North Carolina"), survival, structures and a bonus chest. Menus read its settings to create worlds. */
public class DemoWorldServer extends WorldServer {
    private static final long demoWorldSeed = "North Carolina".hashCode();
    public static final WorldSettings demoWorldSettings = new WorldSettings(demoWorldSeed, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT)
            .enableBonusChest();

    protected DemoWorldServer(net.minecraft.world.level.LevelAccessor level) {
        super(level);
    }
}
