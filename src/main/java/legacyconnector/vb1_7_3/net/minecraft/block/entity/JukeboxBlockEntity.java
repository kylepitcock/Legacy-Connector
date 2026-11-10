package legacyconnector.vb1_7_3.net.minecraft.block.entity;

/** Canonical Beta record-container payload for original API construction and NBT. */
public class JukeboxBlockEntity extends BlockEntity {
    static {BlockEntity.create(JukeboxBlockEntity.class,"RecordPlayer");}
    public int recordId;
    @Override public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){super.readNbt(tag);recordId=tag.getInt("Record");}
    @Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){super.writeNbt(tag);if(recordId>0)tag.putInt("Record",recordId);}
}
