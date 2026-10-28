package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2's structure block: its mode and structure name. A structure block of the game is seen through a view of these. */
public class TileEntityStructure extends TileEntity {
    private String name = "";
    private Mode mode = Mode.DATA;
    private final net.minecraft.world.level.block.entity.StructureBlockEntity mcc$structure;

    public TileEntityStructure() {
        this.mcc$structure = null;
    }

    /** A view of a game structure block. */
    public TileEntityStructure(net.minecraft.world.level.block.entity.StructureBlockEntity structure) {
        this.mcc$structure = structure;
    }

    public String getName() {
        return mcc$structure != null ? mcc$structure.getStructureName() : name;
    }

    public void setName(String nameIn) {
        name = nameIn == null ? "" : nameIn;
        if (mcc$structure != null) mcc$structure.setStructureName(name);
    }

    public Mode getMode() {
        return mcc$structure != null ? Mode.valueOf(mcc$structure.getMode().name()) : mode;
    }

    public void setMode(Mode modeIn) {
        mode = modeIn;
        if (mcc$structure != null && modeIn != null) mcc$structure.setMode(net.minecraft.world.level.block.state.properties.StructureMode.valueOf(modeIn.name()));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString("name", getName());
        compound.setString("mode", getMode().toString());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        name = compound.getString("name");
        try {
            mode = Mode.valueOf(compound.getString("mode"));
        } catch (IllegalArgumentException e) {
            mode = Mode.DATA;
        }
    }

    public enum Mode {
        SAVE("save", 0),
        LOAD("load", 1),
        CORNER("corner", 2),
        DATA("data", 3);

        private final String modeName;
        private final int modeId;

        Mode(String modeNameIn, int modeIdIn) {
            this.modeName = modeNameIn;
            this.modeId = modeIdIn;
        }

        public String getName() {
            return modeName;
        }

        public int getModeId() {
            return modeId;
        }

        public static Mode getById(int id) {
            return id >= 0 && id < values().length ? values()[id] : values()[0];
        }
    }
}
