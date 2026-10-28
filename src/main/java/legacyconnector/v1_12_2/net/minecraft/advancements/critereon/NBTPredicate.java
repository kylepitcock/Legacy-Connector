package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTUtil;

/** A tag the tested tag must contain (extra keys allowed; lists match element-wise). */
public class NBTPredicate {
    public static final NBTPredicate ANY = new NBTPredicate(null);
    private final NBTTagCompound tag;

    public NBTPredicate(NBTTagCompound tag) {
        this.tag = tag;
    }

    public boolean test(ItemStack item) {
        return this == ANY || test(item.getTagCompound());
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        if (this == ANY) return true;
        var modern = entityIn == null ? null : entityIn.mcc$modern();
        if (modern == null) return false;
        return test(new NBTTagCompound(net.minecraft.advancements.critereon.NbtPredicate.getEntityTagToCompare(modern)));
    }

    public boolean test(NBTBase nbt) {
        if (nbt == null) return this == ANY;
        return tag == null || NBTUtil.areNBTEquals(tag, nbt, true);
    }

    public static NBTPredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        try {
            return new NBTPredicate(legacyconnector.v1_12_2.net.minecraft.nbt.JsonToNBT.getTagFromJson(
                    legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "nbt")));
        } catch (Exception e) {
            throw new JsonSyntaxException("Invalid nbt tag: " + e.getMessage());
        }
    }
}
