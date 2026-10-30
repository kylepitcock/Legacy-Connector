package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** The key a locked container needs (an item named like the code), saved as the "Lock" tag. */
public class LockCode {
    public static final LockCode EMPTY_CODE = new LockCode("");
    private final String lock;

    public LockCode(String code) {
        this.lock = code;
    }

    public boolean isEmpty() {
        return lock == null || lock.isEmpty();
    }

    public String getLock() {
        return lock;
    }

    public void toNBT(NBTTagCompound nbt) {
        nbt.setString("Lock", lock);
    }

    public static LockCode fromNBT(NBTTagCompound nbt) {
        return nbt.hasKey("Lock", 8) ? new LockCode(nbt.getString("Lock")) : EMPTY_CODE;
    }
}
