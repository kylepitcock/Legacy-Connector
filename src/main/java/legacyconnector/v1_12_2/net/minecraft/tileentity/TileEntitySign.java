package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;

/**
 * 1.12.2's sign: four lines of text (chat components), editable by the player who placed it, running the click commands of its lines
 * when right-clicked. A sign of the game is seen through a view of its front text; lines a mod sets are written back on markDirty.
 */
public class TileEntitySign extends TileEntity implements org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    public final ITextComponent[] signText = new ITextComponent[] {new TextComponentString(""), new TextComponentString(""), new TextComponentString(""),
            new TextComponentString("")};
    public int lineBeingEdited = -1;
    private boolean isEditable = true;
    private EntityPlayer player;
    private final net.minecraft.world.level.block.entity.SignBlockEntity mcc$sign;

    public TileEntitySign() {
        this.mcc$sign = null;
    }

    /** A view of a game sign. */
    public TileEntitySign(net.minecraft.world.level.block.entity.SignBlockEntity sign) {
        this.mcc$sign = sign;
        mcc$refresh();
    }

    private static net.minecraft.core.HolderLookup.Provider mcc$lookup(net.minecraft.world.level.Level level) {
        return level != null ? level.registryAccess()
                : net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }

    @Override
    public void mcc$refresh() {
        if (mcc$sign == null) return;
        var lookup = mcc$lookup(mcc$sign.getLevel());
        var text = mcc$sign.getFrontText();
        for (int i = 0; i < 4; i++) {
            try {
                signText[i] = ITextComponent.Serializer.jsonToComponent(net.minecraft.network.chat.Component.Serializer.toJson(text.getMessage(i, false), lookup));
            } catch (RuntimeException e) {
                signText[i] = new TextComponentString(text.getMessage(i, false).getString());
            }
        }
        isEditable = !mcc$sign.isWaxed();
    }

    /** Writes the lines of a view back into the game sign. */
    private void mcc$flush() {
        if (mcc$sign == null) return;
        var text = mcc$sign.getFrontText();
        for (int i = 0; i < 4; i++) text = text.setMessage(i, signText[i] == null ? net.minecraft.network.chat.Component.empty() : signText[i].mcc$modern());
        mcc$sign.setText(text, true);
        mcc$sign.setChanged();
        var level = mcc$sign.getLevel();
        if (level != null) level.sendBlockUpdated(mcc$sign.getBlockPos(), mcc$sign.getBlockState(), mcc$sign.getBlockState(), 3);
    }

    @Override
    public void markDirty() {
        if (mcc$sign != null) mcc$flush();
        else super.markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        for (int i = 0; i < 4; ++i) compound.setString("Text" + (i + 1), ITextComponent.Serializer.componentToJson(signText[i]));
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        isEditable = false;
        super.readFromNBT(compound);
        for (int i = 0; i < 4; ++i) {
            String s = compound.getString("Text" + (i + 1));
            try {
                ITextComponent c = ITextComponent.Serializer.jsonToComponent(s);
                signText[i] = c == null ? new TextComponentString(s) : c;
            } catch (RuntimeException e) {
                signText[i] = new TextComponentString(s);
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public boolean onlyOpsCanSetNbt() {
        return true;
    }

    public boolean getIsEditable() {
        return isEditable;
    }

    public void setEditable(boolean isEditableIn) {
        this.isEditable = isEditableIn;
        if (!isEditableIn) player = null;
    }

    public void setPlayer(EntityPlayer playerIn) {
        this.player = playerIn;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    /** Runs the click commands of the lines as the player (the sign's position as the command origin). */
    public boolean executeCommand(EntityPlayer playerIn) {
        if (playerIn == null || !(playerIn.mcc$entity instanceof net.minecraft.server.level.ServerPlayer sp)) return true;
        var server = sp.getServer();
        if (server == null) return true;
        var source = sp.createCommandSourceStack().withPosition(net.minecraft.world.phys.Vec3.atCenterOf(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ())))
                .withPermission(2);
        for (ITextComponent line : signText) {
            if (line == null) continue;
            var click = line.mcc$modern().getStyle().getClickEvent();
            if (click != null && click.getAction() == net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND) {
                server.getCommands().performPrefixedCommand(source, click.getValue());
            }
        }
        return true;
    }
}
