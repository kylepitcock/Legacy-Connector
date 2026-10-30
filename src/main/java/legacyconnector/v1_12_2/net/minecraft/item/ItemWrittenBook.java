package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2 written book: shows its title and author; used, opens the reading screen (the modern written book's, which resolves its pages). */
public class ItemWrittenBook extends Item {
    public ItemWrittenBook() {
        setMaxStackSize(1);
    }

    /** Whether a book tag is valid: a valid page list, a title of at most 32 characters and an author. */
    public static boolean validBookTagContents(NBTTagCompound nbt) {
        if (!ItemWritableBook.isNBTValid(nbt)) return false;
        if (!nbt.hasKey("title", 8)) return false;
        String title = nbt.getString("title");
        return title != null && title.length() <= 32 && nbt.hasKey("author", 8);
    }

    /** How many times the book was copied (0 original, 1 copy, 2 copy of a copy). */
    public static int getGeneration(ItemStack book) {
        return book.hasTagCompound() ? book.getTagCompound().getInteger("generation") : 0;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack.hasTagCompound()) {
            String title = stack.getTagCompound().getString("title");
            if (title != null && !title.isEmpty()) return title;
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.List<String> tooltip,
            legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flagIn) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            String author = tag.getString("author");
            if (author != null && !author.isEmpty()) {
                tooltip.add("§7" + legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocalFormatted("book.byAuthor", author));
            }
            tooltip.add("§7" + legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal("book.generation." + tag.getInteger("generation")));
        }
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return mcc$useModern(net.minecraft.world.item.Items.WRITTEN_BOOK, worldIn, playerIn, handIn);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
