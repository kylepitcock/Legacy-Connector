package legacyconnector.v1_12_2.net.minecraft.tileentity;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;

/**
 * 1.12.2's mob head: its kind (0 skeleton, 1 wither skeleton, 2 zombie, 3 player, 4 creeper, 5 dragon), its 16-step rotation on the
 * floor and the player profile of a player head (completed with its skin from the session service). A head of the game is seen
 * through a view of the modern head.
 */
public class TileEntitySkull extends TileEntity implements ITickable, org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    private int skullType;
    private int skullRotation;
    private GameProfile playerProfile;
    private int dragonAnimatedTicks;
    private boolean dragonAnimated;
    private static com.mojang.authlib.minecraft.MinecraftSessionService sessionService;
    /** The server's profile cache as set by the server (the game's own cache answers lookups). */
    private static legacyconnector.v1_12_2.net.minecraft.server.management.PlayerProfileCache profileCache;
    private final net.minecraft.world.level.block.entity.SkullBlockEntity mcc$skull;

    public TileEntitySkull() {
        this.mcc$skull = null;
    }

    /** A view of a game head. */
    public TileEntitySkull(net.minecraft.world.level.block.entity.SkullBlockEntity skull) {
        this.mcc$skull = skull;
        mcc$refresh();
    }

    @Override
    public void mcc$refresh() {
        if (mcc$skull == null) return;
        var state = mcc$skull.getBlockState();
        if (state.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock b) {
            var type = b.getType();
            if (type == net.minecraft.world.level.block.SkullBlock.Types.SKELETON) skullType = 0;
            else if (type == net.minecraft.world.level.block.SkullBlock.Types.WITHER_SKELETON) skullType = 1;
            else if (type == net.minecraft.world.level.block.SkullBlock.Types.ZOMBIE) skullType = 2;
            else if (type == net.minecraft.world.level.block.SkullBlock.Types.PLAYER) skullType = 3;
            else if (type == net.minecraft.world.level.block.SkullBlock.Types.CREEPER) skullType = 4;
            else if (type == net.minecraft.world.level.block.SkullBlock.Types.DRAGON) skullType = 5;
        }
        if (state.hasProperty(net.minecraft.world.level.block.SkullBlock.ROTATION)) skullRotation = state.getValue(net.minecraft.world.level.block.SkullBlock.ROTATION);
        var owner = mcc$skull.getOwnerProfile();
        playerProfile = owner == null ? null : owner.gameProfile();
    }

    public static void setSessionService(com.mojang.authlib.minecraft.MinecraftSessionService sessionServiceIn) {
        sessionService = sessionServiceIn;
    }

    /** Server-wide profile cache (the modern server keeps its own). */
    public static void setProfileCache(legacyconnector.v1_12_2.net.minecraft.server.management.PlayerProfileCache profileCacheIn) {
        profileCache = profileCacheIn;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte("SkullType", (byte) (skullType & 255));
        compound.setByte("Rot", (byte) (skullRotation & 255));
        if (playerProfile != null) {
            compound.setString("ExtraType", playerProfile.getName() == null ? "" : playerProfile.getName());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        skullType = compound.getByte("SkullType");
        skullRotation = compound.getByte("Rot");
        if (skullType == 3 && compound.hasKey("ExtraType", 8)) {
            String s = compound.getString("ExtraType");
            if (!s.isEmpty()) {
                playerProfile = new GameProfile(net.minecraft.Util.NIL_UUID, s);
                updatePlayerProfile();
            }
        }
    }

    /** The dragon head's jaw moves while it is powered. */
    @Override
    public void update() {
        if (skullType == 5 && world != null) {
            if (world.isBlockPowered(pos)) {
                dragonAnimated = true;
                ++dragonAnimatedTicks;
            } else {
                dragonAnimated = false;
            }
        }
    }

    public float getAnimationProgress(float partialTicks) {
        return dragonAnimated ? (float) dragonAnimatedTicks + partialTicks : (float) dragonAnimatedTicks;
    }

    public GameProfile getPlayerProfile() {
        return playerProfile;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public void setType(int type) {
        this.skullType = type;
        this.playerProfile = null;
    }

    public void setPlayerProfile(GameProfile playerProfile) {
        this.skullType = 3;
        this.playerProfile = playerProfile;
        updatePlayerProfile();
        if (mcc$skull != null && playerProfile != null) mcc$skull.setOwner(new net.minecraft.world.item.component.ResolvableProfile(playerProfile));
    }

    private void updatePlayerProfile() {
        playerProfile = updateGameProfile(playerProfile);
        markDirty();
    }

    /** A player profile with its skin: the game's profile cache answers when it knows the name (null-safe, never blocks). */
    public static GameProfile updateGameProfile(GameProfile input) {
        if (input == null || input.getName() == null || input.getName().isEmpty()) return input;
        if (input.getProperties().containsKey("textures")) return input;
        try {
            var future = net.minecraft.world.level.block.entity.SkullBlockEntity.fetchGameProfile(input.getName());
            var result = future.getNow(java.util.Optional.empty());
            return result.isPresent() ? result.get() : input;
        } catch (RuntimeException e) {
            return input;
        }
    }

    public int getSkullType() {
        return skullType;
    }

    public int getSkullRotation() {
        return skullRotation;
    }

    public void setSkullRotation(int rotation) {
        this.skullRotation = rotation;
    }

    @Override
    public void mirror(legacyconnector.v1_12_2.net.minecraft.util.Mirror mirrorIn) {
        if (world != null && world.getBlockState(pos).getValue(legacyconnector.v1_12_2.net.minecraft.block.BlockDirectional.FACING) == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP) {
            skullRotation = mirrorIn.mirrorRotation(skullRotation, 16);
        }
    }

    @Override
    public void rotate(legacyconnector.v1_12_2.net.minecraft.util.Rotation rotationIn) {
        if (world != null && world.getBlockState(pos).getValue(legacyconnector.v1_12_2.net.minecraft.block.BlockDirectional.FACING) == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP) {
            skullRotation = rotationIn.rotate(skullRotation, 16);
        }
    }
}
