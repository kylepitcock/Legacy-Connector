package legacyconnector.v1_7_10.net.minecraft.tileentity;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * A mob head on the ground or a wall: type 0 skeleton, 1 wither skeleton, 2 zombie, 3 player (with the player's profile),
 * 4 creeper; the rotation (0-15) is set for heads standing on the ground. Saved as 1.7.10 did ("SkullType", "Rot", "Owner").
 */
public class TileEntitySkull extends TileEntity {
    private int skullType;
    private int skullRotation;
    private GameProfile field_152110_j;

    /** {@code func_152107_a}: sets the head type (and forgets the player profile). */
    public void func_152107_a(int type) {
        this.skullType = type;
        this.field_152110_j = null;
    }

    /** {@code func_152106_a}: makes this a player head with that profile. */
    public void func_152106_a(GameProfile profile) {
        this.skullType = 3;
        this.field_152110_j = profile;
        markDirty();
    }

    /** The player profile of a player head, or null. */
    public GameProfile func_152108_a() {
        return field_152110_j;
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

    /** 1.7.2: head type and player name. */
    public void func_145905_a(int type, String owner) {
        this.skullType = type;
        this.field_152110_j = owner == null || owner.isEmpty() ? null : mcc$named(owner);
    }

    public int func_145906_b() {
        return getSkullRotation();
    }

    public void func_145903_a(int rotation) {
        setSkullRotation(rotation);
    }

    public String func_145907_c() {
        return field_152110_j == null || field_152110_j.getName() == null ? "" : field_152110_j.getName();
    }

    /** A profile known only by its name (the modern profile needs an id: the nil one). */
    private static GameProfile mcc$named(String name) {
        return new GameProfile(net.minecraft.Util.NIL_UUID, name);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("SkullType", (byte) (skullType & 255));
        tag.setByte("Rot", (byte) (skullRotation & 255));
        if (field_152110_j != null) {
            NBTTagCompound owner = new NBTTagCompound();
            if (field_152110_j.getName() != null && !field_152110_j.getName().isEmpty()) owner.setString("Name", field_152110_j.getName());
            if (field_152110_j.getId() != null && !net.minecraft.Util.NIL_UUID.equals(field_152110_j.getId())) owner.setString("Id", field_152110_j.getId().toString());
            tag.setTag("Owner", owner);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        skullType = tag.getByte("SkullType");
        skullRotation = tag.getByte("Rot");
        if (skullType == 3) {
            if (tag.hasKey("Owner", 10)) {
                NBTTagCompound owner = tag.getCompoundTag("Owner");
                String name = owner.getString("Name");
                java.util.UUID id = net.minecraft.Util.NIL_UUID;
                try {
                    if (owner.hasKey("Id", 8)) id = java.util.UUID.fromString(owner.getString("Id"));
                } catch (IllegalArgumentException ignored) {
                    // not a uuid: the profile is known by name only
                }
                field_152110_j = name.isEmpty() && id.equals(net.minecraft.Util.NIL_UUID) ? null : new GameProfile(id, name);
            } else if (tag.hasKey("ExtraType", 8) && !tag.getString("ExtraType").isEmpty()) {
                field_152110_j = mcc$named(tag.getString("ExtraType"));
            }
        }
    }
}
