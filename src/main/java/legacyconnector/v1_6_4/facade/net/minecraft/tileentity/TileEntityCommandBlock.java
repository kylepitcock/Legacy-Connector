package legacyconnector.v1_6_4.facade.net.minecraft.tileentity;

/**
 * Facade of the 1.7.10 shim's {@code TileEntityCommandBlock} for 1.4.7 to 1.6.4 mods: the command lived on the tile entity itself, 1.7.10 keeps it in
 * the {@code CommandBlockLogic} the tile owns. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class TileEntityCommandBlock {
    private TileEntityCommandBlock() {}

    public static String getCommand(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityCommandBlock self) {
        return self.func_145993_a().getCustomName();
    }

    public static void setCommand(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityCommandBlock self, String command) {
        self.func_145993_a().setCommand(command);
        self.markDirty();
    }

    public static void setCommandSenderName(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityCommandBlock self, String name) {
        self.func_145993_a().func_145754_b(name);
    }

    /** {@code executeCommandOnPowered(world)}: runs the command and answers how many commands succeeded (what comparators read). */
    public static int executeCommandOnPowered(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityCommandBlock self,
            legacyconnector.v1_7_10.net.minecraft.world.World world) {
        if (world != null && world.isRemote) return 0;
        var logic = self.func_145993_a();
        logic.func_145755_a(world);
        return logic.getSuccessCount();
    }
}
