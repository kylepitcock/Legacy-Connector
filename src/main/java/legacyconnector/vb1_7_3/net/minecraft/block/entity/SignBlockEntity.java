package legacyconnector.vb1_7_3.net.minecraft.block.entity;

/** Original writable four-line sign data. Native sign views flush after callbacks. */
public class SignBlockEntity extends BlockEntity {
    public String[] texts={"","","",""};
    @Override public void markDirty(){super.markDirty();org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173.flush(this);}
}
