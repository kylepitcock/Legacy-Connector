package legacyconnector.vb1_7_3.net.minecraft.block.entity;

/** Original writable four-line sign data. Native sign views flush after callbacks. */
public class SignBlockEntity extends BlockEntity {
    public String[] texts={"","","",""};
    /** Beta {@code TileEntitySign}: the four lines are saved as Text1..Text4. */
    @Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt){super.writeNbt(nbt);for(int i=0;i<4;i++)nbt.putString("Text"+(i+1),texts[i]==null?"":texts[i]);}
    @Override public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt){super.readNbt(nbt);for(int i=0;i<4;i++){String line=nbt.getString("Text"+(i+1));texts[i]=line==null?"":(line.length()>15?line.substring(0,15):line);}}
    @Override public void markDirty(){super.markDirty();org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173.flush(this);}
}
