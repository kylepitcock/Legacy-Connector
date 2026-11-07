package org.kylepitcock.legacyconnector.compat.patchouli;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * The guide book item: Patchouli 1.12 kept the book in the stack's {@code patchouli:book} NBT tag, the modern one in a data component of the same
 * name. A 1.12 stack with the tag becomes the modern book stack, and a modern book stack shows the tag to legacy code.
 */
final class BookStacks1122 {
    private static final ResourceLocation GUIDE_BOOK = ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static final ResourceLocation BOOK_COMPONENT = ResourceLocation.fromNamespaceAndPath("patchouli", "book");
    private static final String TAG = "patchouli:book";

    private BookStacks1122() {}

    static void register() {
        Stacks1122.addHook(new Stacks1122.Hook() {
            @Override
            public net.minecraft.world.item.ItemStack toModern(ItemStack legacy, net.minecraft.world.item.ItemStack converted) {
                if (converted.isEmpty() || !isGuideBook(converted)) return converted;
                NBTTagCompound tag = legacy.getTagCompound();
                if (tag == null || !tag.hasKey(TAG)) return converted;
                ResourceLocation book = ResourceLocation.tryParse(tag.getString(TAG));
                if (book == null) return converted;
                var stack = vazkii.patchouli.api.PatchouliAPI.get().getBookStack(book);
                if (stack.isEmpty()) return converted;
                stack.setCount(converted.getCount());
                return stack;
            }

            @Override
            public void fromModern(net.minecraft.world.item.ItemStack modern, ItemStack legacy) {
                if (!isGuideBook(modern)) return;
                DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(BOOK_COMPONENT);
                if (type == null) return;
                Object book = modern.get(type);
                if (book == null) return;
                NBTTagCompound tag = legacy.getTagCompound();
                if (tag == null) tag = new NBTTagCompound();
                tag.setString(TAG, book.toString());
                legacy.setTagCompound(tag);
            }
        });
    }

    private static boolean isGuideBook(net.minecraft.world.item.ItemStack stack) {
        return GUIDE_BOOK.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
