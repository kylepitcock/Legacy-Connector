package legacyconnector.v1_12_2.net.minecraft.init;

import legacyconnector.v1_12_2.net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBlockSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** The game's bootstrap; mods use its dispenser behaviour base that may fail (only that, since the modern game bootstraps itself). */
public class Bootstrap {
    public static boolean isRegistered() {
        return true;
    }

    public static void register() {
    }

    /** A dispenser behaviour that sets {@link #successful}; a failed dispense clicks instead of the normal sound. */
    public static class BehaviorDispenseOptional extends BehaviorDefaultDispenseItem {
        protected boolean successful = true;

        @Override
        protected void playDispenseSound(IBlockSource source) {
            var p = source.getBlockPos();
            source.getWorld().mcc$level().levelEvent(successful ? 1000 : 1001, new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()), 0);
        }

        @Override
        protected void spawnDispenseParticles(IBlockSource source, EnumFacing facingIn) {
            super.spawnDispenseParticles(source, facingIn);
        }
    }
}
