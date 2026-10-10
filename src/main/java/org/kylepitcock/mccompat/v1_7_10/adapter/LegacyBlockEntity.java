package org.kylepitcock.mccompat.v1_7_10.adapter;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.network.NetworkManager;
import mccompat.v1_7_10.net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge;
import org.slf4j.LoggerFactory;

/**
 * Modern block entity hosting a 1.7.10 {@link TileEntity}. Persists the legacy NBT verbatim under {@code "legacy"},
 * so the legacy tile entity sees exactly the data it wrote, and forwards ticking and description packets.
 */
public class LegacyBlockEntity extends BlockEntity {
    private TileEntity legacy;
    private CompoundTag pending;
    private boolean tickFailed;

    public LegacyBlockEntity(BlockPos pos, BlockState state) {
        super(TileEntityBridge.type(), pos, state);
    }

    /** The legacy tile entity, created on first use (legacy createTileEntity may need the world). */
    public TileEntity legacy() {
        if (legacy == null) {
            if (pending != null) {
                legacy = TileEntityBridge.createAndLoad(new NBTTagCompound(pending));
                pending = null;
            }
            if (legacy == null && level != null) {
                Block block = (Block) ((LegacyBlockAdapter) getBlockState().getBlock()).legacy();
                legacy = block.createTileEntity(World.mcc$wrap(level), getBlockState().getValue(LegacyBlockAdapter.META));
            }
            if (legacy != null) attach(legacy);
        }
        return legacy;
    }

    public void replaceLegacy(TileEntity te) {
        if (legacy != null) legacy.invalidate();
        legacy = te;
        if (te != null) attach(te);
        setChanged();
    }

    private void attach(TileEntity te) {
        te.mcc$host = this;
        te.xCoord = worldPosition.getX();
        te.yCoord = worldPosition.getY();
        te.zCoord = worldPosition.getZ();
        if (level != null) te.setWorldObj(World.mcc$wrap(level));
        te.validate();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (legacy != null) legacy.setWorldObj(World.mcc$wrap(level));
    }

    void tick() {
        if (tickFailed) return;
        TileEntity te = legacy();
        if (te == null || te.isInvalid() || !te.canUpdate()) return;
        try {
            te.updateEntity();
        } catch (RuntimeException | LinkageError e) {
            tickFailed = true;
            LoggerFactory.getLogger("MCCompat/TileEntity").error("Legacy tile entity {} at {} crashed while ticking; it will no longer tick",
                    te.getClass().getName(), worldPosition, e);
        }
    }

    @Override
    public void setRemoved() {
        if (legacy != null) legacy.invalidate();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (legacy != null) legacy.onChunkUnload();
        super.onChunkUnloaded();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("legacy")) {
            CompoundTag data = tag.getCompound("legacy");
            if (legacy != null) legacy.readFromNBT(new NBTTagCompound(data));
            else pending = data;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        TileEntity te = legacy();
        if (te != null) {
            NBTTagCompound out = new NBTTagCompound();
            te.writeToNBT(out);
            tag.put("legacy", out.mcc$modern());
        } else if (pending != null) {
            tag.put("legacy", pending);
        }
    }

    // ------------------------------------------------------------------ client sync (getDescriptionPacket/onDataPacket)

    private CompoundTag descriptionTag() {
        TileEntity te = legacy();
        if (te == null) return null;
        mccompat.v1_7_10.net.minecraft.network.Packet p = te.getDescriptionPacket();
        if (p instanceof S35PacketUpdateTileEntity s35 && s35.getNbtCompound() != null) {
            CompoundTag tag = s35.getNbtCompound().mcc$modern().copy();
            tag.putInt("mcc$type", s35.getTileEntityType());
            return tag;
        }
        if (p instanceof mccompat.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            // Mods like Iron Chest return a custom-channel packet here; carry its bytes inside the BE update.
            CompoundTag tag = new CompoundTag();
            tag.putString("mcc$channel", fml.channel());
            tag.putByteArray("mcc$fml", fml.mcc$bytes());
            return tag;
        }
        return null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        CompoundTag tag = descriptionTag();
        return tag == null ? null : ClientboundBlockEntityDataPacket.create(this, (be, reg) -> tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = descriptionTag();
        return tag == null ? new CompoundTag() : tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        deliver(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.isEmpty()) deliver(tag);
    }

    private void deliver(CompoundTag tag) {
        if (tag.contains("mcc$fml")) {
            org.kylepitcock.mccompat.v1_7_10.bridge.FmlChannelBridge.deliver(tag.getString("mcc$channel"), tag.getByteArray("mcc$fml"),
                    mccompat.v1_7_10.cpw.mods.fml.relauncher.Side.CLIENT, mccompat.v1_7_10.net.minecraft.network.NetHandlerPlayClient.INSTANCE);
            return;
        }
        TileEntity te = legacy();
        if (te == null) return;
        int type = tag.getInt("mcc$type");
        CompoundTag copy = tag.copy();
        copy.remove("mcc$type");
        te.onDataPacket(NetworkManager.MCC_DUMMY, new S35PacketUpdateTileEntity(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), type,
                new NBTTagCompound(copy)));
    }
}
