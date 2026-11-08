package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/**
 * The modern item of a b1.7.3 {@code ItemBlock}. The legacy damage of a stack is the metadata in its {@code block_state} component; an
 * item with subtypes (Minefactory's machines are one block with a metadata per machine) names each by the legacy
 * {@code getItemNameIS(stack)} ({@link #setNames}) and shows each with its own model through the {@link #META} model property.
 */
public class LegacyB173BlockItem extends BlockItem {
    /** Item model property: the stack's metadata / 16 (models select the subtype with overrides on it). */
    public static final ResourceLocation META = ResourceLocation.fromNamespaceAndPath("legacyconnector", "b173_meta");

    private String[] names;

    public LegacyB173BlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The metadata (legacy damage) of a stack of this item. */
    public static int meta(ItemStack stack) {
        BlockItemStateProperties state = stack.get(DataComponents.BLOCK_STATE);
        if (state == null) return 0;
        Integer meta = state.get(LegacyB173Block.META);
        return meta == null ? 0 : meta;
    }

    /** The legacy item has subtypes; translation keys per metadata (null entries use the block's name). */
    void setNames(String[] names) {
        this.names = names == null ? new String[16] : names;
    }

    public boolean hasSubtypes() {
        return names != null;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        String[] n = names;
        if (n != null) {
            String key = n[meta(stack) & 15];
            if (key != null) return key;
        }
        return super.getDescriptionId(stack);
    }
}
