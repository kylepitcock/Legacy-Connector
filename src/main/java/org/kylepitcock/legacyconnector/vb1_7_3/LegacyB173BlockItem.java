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
    private legacyconnector.vb1_7_3.net.minecraft.item.Item legacy;
    private ItemCallbacksB173 callbacks;
    private boolean ownUse;

    public LegacyB173BlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The original {@code ItemBlock} (a mod's own subclass places its block itself, as Beta's onItemUse override did). */
    void setLegacy(legacyconnector.vb1_7_3.net.minecraft.item.Item item) {
        legacy = item;
        callbacks = new ItemCallbacksB173(item);
        try {
            Class<?> owner = item.getClass().getMethod("useOnBlock", legacyconnector.vb1_7_3.net.minecraft.item.ItemStack.class,
                    legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class, legacyconnector.vb1_7_3.net.minecraft.world.World.class,
                    int.class, int.class, int.class, int.class).getDeclaringClass();
            ownUse = owner != legacyconnector.vb1_7_3.net.minecraft.item.Item.class && owner != legacyconnector.vb1_7_3.net.minecraft.item.BlockItem.class;
        } catch (NoSuchMethodException e) {
            ownUse = false;
        }
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return ownUse ? callbacks.useOn(context) : super.useOn(context);
    }

    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        return callbacks == null ? net.minecraft.world.InteractionResult.PASS : callbacks.useFirst(context);
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        if (callbacks != null && damage(stack) > 15) {
            try {
                var name = callbacks.getName(stack);
                if (name != null) return name;
            } catch (RuntimeException | LinkageError ignored) {
                // a subtype the item does not name (Beta threw for unknown damage values)
            }
        }
        return super.getName(stack);
    }

    /** The metadata a stack of this item places (the block state's 0-15). */
    public static int meta(ItemStack stack) {
        BlockItemStateProperties state = stack.get(DataComponents.BLOCK_STATE);
        if (state == null) return 0;
        Integer meta = state.get(LegacyB173Block.META);
        return meta == null ? 0 : meta;
    }

    /** The full legacy damage of a stack: Beta's item damage is a short, beyond a block's 16 metadata (RedPower's wires are 256 and up). */
    public static int damage(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            var tag = data.copyTag();
            if (tag.contains(DAMAGE_KEY)) return tag.getInt(DAMAGE_KEY);
        }
        return meta(stack);
    }

    /** The custom-data key of a damage above 15. */
    public static final String DAMAGE_KEY = "b173_meta";

    /** Keeps a damage that does not fit the block state's metadata whole, in the stack's custom data (the block state keeps its low 4 bits). */
    public static void setFullDamage(ItemStack stack, int damage) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (damage > 15) data.putInt(DAMAGE_KEY, damage);
        else data.remove(DAMAGE_KEY);
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
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
