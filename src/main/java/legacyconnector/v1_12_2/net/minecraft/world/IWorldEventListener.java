package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * 1.12.2 world listener ({@code World.addEventListener}): told about block updates, sounds, particles and entities of a world.
 * The layer calls registered listeners from the legacy world API's own block updates, sounds, events and entity additions.
 */
public interface IWorldEventListener {
    void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags);

    void notifyLightSet(BlockPos pos);

    void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2);

    void playSoundToAllNearExcept(EntityPlayer player, SoundEvent sound, SoundCategory category, double x, double y, double z, float volume, float pitch);

    void playRecord(SoundEvent sound, BlockPos pos);

    void spawnParticle(int particleID, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters);

    void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
            int... parameters);

    void onEntityAdded(Entity entity);

    void onEntityRemoved(Entity entity);

    void broadcastSound(int soundID, BlockPos pos, int data);

    void playEvent(EntityPlayer player, int type, BlockPos pos, int data);

    void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress);
}
