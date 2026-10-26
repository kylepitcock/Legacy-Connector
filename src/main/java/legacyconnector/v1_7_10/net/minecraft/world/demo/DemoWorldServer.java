package legacyconnector.v1_7_10.net.minecraft.world.demo;

import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSettings;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;

/**
 * 1.7.10's demo world. Custom main menus read its settings for their "Play Demo World" button: the fixed demo seed, survival,
 * structures, bonus chest. The modern game runs any world itself, so no instance is ever made here.
 */
public class DemoWorldServer extends WorldServer {
    public static final long demoWorldSeed = "North Carolina".hashCode();
    public static final WorldSettings demoWorldSettings = new WorldSettings(demoWorldSeed, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT)
            .enableBonusChest();

    protected DemoWorldServer(net.minecraft.world.level.LevelAccessor level) {
        super(level);
    }
}
