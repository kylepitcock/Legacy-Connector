package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/**
 * Forge's fake player (machines that place, break and use as a player): a view of a NeoForge fake player. Mods constructing one get
 * a NeoForge fake player of the given profile in that world.
 */
public class FakePlayer extends EntityPlayerMP {
    public FakePlayer(legacyconnector.v1_12_2.net.minecraft.world.World world, GameProfile profile) {
        super(mcc$create(world, profile));
    }

    public FakePlayer(net.neoforged.neoforge.common.util.FakePlayer modern) {
        super(modern);
    }

    private static net.neoforged.neoforge.common.util.FakePlayer mcc$create(legacyconnector.v1_12_2.net.minecraft.world.World world, GameProfile profile) {
        var level = world == null ? null : world.mcc$level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            throw new IllegalStateException("A fake player needs a server world");
        }
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(sl, profile);
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return false;
    }

    @Override
    public void sendStatusMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent message, boolean actionBar) {
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
    }

    @Override
    public boolean isEntityInvulnerable(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        return true;
    }

    @Override
    public boolean canAttackPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }

    @Override
    public void onDeath(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
    }

    @Override
    public void onUpdate() {
    }
}
