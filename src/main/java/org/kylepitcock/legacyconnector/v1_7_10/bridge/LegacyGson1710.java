package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;

/**
 * Gson support for the legacy data classes mods serialize reflectively (Equivalent Exchange 3 sends its recipes as JSON, config
 * libraries and research systems store stacks and fluids). The shim classes hold views over modern game objects; plain reflection
 * walks into registries, threads and parent links. The shim classes name these adapters in {@code @JsonAdapter}, so every Gson
 * instance (also the ones mods build with their own type adapters) writes and reads the fields the 1.6.4/1.7.10 classes had:
 * {@code stackSize}, {@code itemDamage}, {@code itemID}, {@code stackTagCompound}; {@code fluidID}, {@code amount}, {@code tag}.
 * The item (or fluid) is also written by registry name, which survives id changes between sessions and is preferred when reading.
 * The 1.6.4 forward layer shares these classes.
 */
public final class LegacyGson1710 {
    private LegacyGson1710() {}

    public static final class ItemStackAdapter extends TypeAdapter<ItemStack> {
        @Override
        public void write(JsonWriter out, ItemStack s) throws IOException {
            if (s == null) {
                out.nullValue();
                return;
            }
            Item item = s.getItem();
            out.beginObject();
            out.name("stackSize").value(s.mcc$count());
            out.name("animationsToGo").value(s.animationsToGo);
            out.name("itemID").value(item == null ? 0 : Item.getIdFromItem(item));
            if (item != null) {
                String name = Item.itemRegistry.getNameForObject(item);
                if (name != null) out.name("item").value(name);
            }
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
            int size = 1, damage = 0, id = 0, anim = 0;
            String name = null, tag = null;
            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    continue;
                }
                switch (key) {
                    case "stackSize" -> size = in.nextInt();
                    case "animationsToGo" -> anim = in.nextInt();
                    case "itemID" -> id = in.nextInt();
                    case "item" -> name = in.nextString();
                    case "itemDamage" -> damage = in.nextInt();
                    case "stackTagCompound" -> tag = in.nextString();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            Item item = null;
            if (name != null) item = Item.itemRegistry.getObject(name) instanceof Item i ? i : null;
            if (item == null && id > 0) item = Item.getItemById(id);
            ItemStack stack = new ItemStack(item, size, damage);
            stack.animationsToGo = anim;
            if (tag != null && item != null) {
                NBTTagCompound parsed = parseCompound(tag);
                if (parsed != null) stack.setTagCompound(parsed);
            }
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
            out.name("fluidID").value(s.getFluidID());
            if (s.fluid != null) out.name("fluidName").value(s.fluid.getName());
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
            int id = -1, amount = 0;
            String name = null, tag = null;
            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    continue;
                }
                switch (key) {
                    case "fluidID" -> id = in.nextInt();
                    case "fluidName" -> name = in.nextString();
                    case "amount" -> amount = in.nextInt();
                    case "tag" -> tag = in.nextString();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            Fluid fluid = name != null ? FluidRegistry.getFluid(name) : null;
            if (fluid == null && id >= 0) fluid = FluidRegistry.getFluid(id);
            return new FluidStack(fluid, amount, tag == null ? null : parseCompound(tag));
        }
    }

    /** A compound as its modern string form (what {@code toString()} prints): the only readable form of a tree of live views. */
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
