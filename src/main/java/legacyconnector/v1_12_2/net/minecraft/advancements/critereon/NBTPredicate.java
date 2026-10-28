package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** An NBT condition: the tested NBT contains this compound (partial match, lists in any order, as 1.12's NBTUtil.areNBTEquals). */
public class NBTPredicate {
    public static final NBTPredicate ANY = new NBTPredicate(null);
    private final NBTTagCompound tag;

    public NBTPredicate(NBTTagCompound tag) {
        this.tag = tag;
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return this == ANY || test(stack.getTagCompound());
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        if (this == ANY) return true;
        var modern = entity == null ? null : entity.mcc$modern();
        if (modern == null) return false;
        return net.minecraft.nbt.NbtUtils.compareNbt(tag.mcc$modern(), modern.saveWithoutId(new net.minecraft.nbt.CompoundTag()), true);
    }

    public boolean test(NBTBase nbt) {
        if (nbt == null) return this == ANY;
        return tag == null || net.minecraft.nbt.NbtUtils.compareNbt(tag.mcc$modern(), nbt.mcc$modern(), true);
    }

    public static NBTPredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        try {
            return new NBTPredicate(legacyconnector.v1_12_2.net.minecraft.nbt.JsonToNBT.getTagFromJson(json.getAsString()));
        } catch (Exception e) {
            throw new com.google.gson.JsonSyntaxException("Invalid nbt tag: " + e.getMessage());
        }
    }
}
