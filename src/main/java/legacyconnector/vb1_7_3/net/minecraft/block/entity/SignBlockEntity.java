package legacyconnector.vb1_7_3.net.minecraft.block.entity;

/** Original writable four-line sign data. Native sign views flush after callbacks. */
public class SignBlockEntity extends BlockEntity {
    public String[] texts={"","","",""};
    /** Beta {@code TileEntitySign}: the four lines are saved as Text1..Text4. */
    @Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt){super.writeNbt(nbt);for(int i=0;i<4;i++)nbt.putString("Text"+(i+1),texts[i]==null?"":texts[i]);}
    /** Beta cut a loaded line to 15 characters; here the same tag also carries a mod's live lines to the client copy (Millenaire's panels write
     * longer ones, which Beta's single-player sign, being the server's own object, drew whole), so lines are kept as written. */
    @Override public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt){super.readNbt(nbt);for(int i=0;i<4;i++){String line=nbt.getString("Text"+(i+1));texts[i]=line==null?"":line;}}
    @Override public void markDirty(){super.markDirty();org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173.flush(this);}
}
