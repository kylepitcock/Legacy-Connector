package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;

/**
 * Gson support for 1.12.2 stacks, fluid stacks and NBT (see {@code LegacyGson1710}): the shim classes name these adapters in
 * {@code @JsonAdapter}, so mods that serialize a legacy stack by reflection get the 1.12 fields ({@code stackSize}, {@code itemDamage},
 * {@code stackTagCompound}, the item by registry name; {@code amount}, {@code tag}, the fluid by name) instead of a walk into the
 * modern stack the shim object views.
 */
public final class LegacyGson1122 {
    private LegacyGson1122() {}

    public static final class ItemStackAdapter extends TypeAdapter<ItemStack> {
        @Override
        public void write(JsonWriter out, ItemStack s) throws IOException {
            if (s == null) {
                out.nullValue();
                return;
            }
            Item item = s.getItem();
            out.beginObject();
            if (item != null) {
                var name = Item.REGISTRY.getNameForObject(item);
                if (name != null) out.name("item").value(name.toString());
            }
            out.name("stackSize").value(s.getCount());
            out.name("itemDamage").value(s.getItemDamage());
            NBTTagCompound tag = s.getTagCompound();
            if (tag != null) out.name("stackTagCompound").value(tag.mcc$modern().toString());
            out.endObject();
        }

        @Override
        public ItemStack read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            int size = 1, damage = 0;
            String name = null, tag = null;
            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    continue;
                }
                switch (key) {
                    case "item" -> name = in.nextString();
                    case "stackSize" -> size = in.nextInt();
                    case "itemDamage" -> damage = in.nextInt();
                    case "stackTagCompound" -> tag = in.nextString();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            Item item = name == null ? null : Item.getByNameOrId(name);
            ItemStack stack = new ItemStack(item, size, damage);
            if (tag != null && item != null) stack.setTagCompound(parseCompound(tag));
            return stack;
        }
    }

    public static final class FluidStackAdapter extends TypeAdapter<FluidStack> {
        @Override
        public void write(JsonWriter out, FluidStack s) throws IOException {
            if (s == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name("fluidName").value(s.getFluid().getName());
            out.name("amount").value(s.amount);
            if (s.tag != null) out.name("tag").value(s.tag.mcc$modern().toString());
            out.endObject();
        }

        @Override
        public FluidStack read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            int amount = 0;
            String name = null, tag = null;
            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    continue;
                }
                switch (key) {
                    case "fluidName" -> name = in.nextString();
                    case "amount" -> amount = in.nextInt();
                    case "tag" -> tag = in.nextString();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            Fluid fluid = name == null ? null : FluidRegistry.getFluid(name);
            if (fluid == null) return null;
            return new FluidStack(fluid, amount, tag == null ? null : parseCompound(tag));
        }
    }

    public static final class CompoundAdapter extends TypeAdapter<NBTTagCompound> {
        @Override
        public void write(JsonWriter out, NBTTagCompound t) throws IOException {
            if (t == null) out.nullValue();
            else out.value(t.mcc$modern().toString());
        }

        @Override
        public NBTTagCompound read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return parseCompound(in.nextString());
        }
    }

    public static final class ListAdapter extends TypeAdapter<NBTTagList> {
        @Override
        public void write(JsonWriter out, NBTTagList t) throws IOException {
            if (t == null) out.nullValue();
            else out.value(t.mcc$modern().toString());
        }

        @Override
        public NBTTagList read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            try {
                CompoundTag wrapper = TagParser.parseTag("{v:" + in.nextString() + "}");
                return wrapper.get("v") instanceof ListTag l ? new NBTTagList(l) : null;
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IOException(e);
            }
        }
    }

    private static NBTTagCompound parseCompound(String snbt) throws IOException {
        try {
            return new NBTTagCompound(TagParser.parseTag(snbt));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IOException(e);
        }
    }
}
