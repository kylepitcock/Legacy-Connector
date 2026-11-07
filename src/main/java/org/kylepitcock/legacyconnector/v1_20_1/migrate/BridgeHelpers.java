package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Argument and result converters the generated bridge methods call ({@code mc-bridges.tsv}): they turn the 1.21.1 parameters of an overridden method
 * back into what the mod's 1.20.1 method expects, and its result into what 1.21.1 wants.
 */
public final class BridgeHelpers {
    private BridgeHelpers() {}

    // ------------------------------------------------------------------ block interaction

    /** The result of the mod's {@code Block.use} as the result of {@code useItemOn}: a pass lets vanilla try the empty-hand interaction. */
    public static ItemInteractionResult toItemResult(InteractionResult r) {
        if (r == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return switch (r) {
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    /** 1.20.1 {@code BlockState.use(Level, Player, InteractionHand, BlockHitResult)}: the held stack first, then the bare interaction. */
    public static InteractionResult use(net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase state, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult item = state.useItemOn(player.getItemInHand(hand), level, player, hand, hit);
        if (item != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && item != ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return switch (item) {
                case SUCCESS -> InteractionResult.SUCCESS;
                case CONSUME -> InteractionResult.CONSUME;
                case CONSUME_PARTIAL -> InteractionResult.CONSUME_PARTIAL;
                default -> InteractionResult.FAIL;
            };
        }
        if (item == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return state.useWithoutItem(level, player, hit);
    }

    /** {@code super.use(...)} of a block whose parent is Minecraft's: the base block does nothing. */
    public static InteractionResult superUse(Object block, BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ tooltips

    /** {@code super.appendHoverText(...)} of an item whose parent is Minecraft's: nothing to add (1.21.1's own tooltips come from components). */
    public static void superAppendHoverText(Object item, ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines,
                                            net.minecraft.world.item.TooltipFlag flag) {}

    public static Level tooltipLevel(Item.TooltipContext ctx) {
        return ctx == null ? null : ctx.level();
    }

    // ------------------------------------------------------------------ recipes

    /** The registry access argument of {@code assemble}/{@code getResultItem}: 1.21.1 passes a provider, the mod wants the {@code RegistryAccess}. */
    public static RegistryAccess registryAccess(HolderLookup.Provider provider) {
        MigrateContext.remember(provider);
        return provider instanceof RegistryAccess ra ? ra : MigrateContext.registries();
    }

    /** A {@link RecipeInput} as the {@link Container} 1.20.1 recipes were matched against (a crafting grid when the input is one). */
    public static Container container(RecipeInput input) {
        if (input instanceof ContainerInput ci) return ci.container();
        if (input instanceof CraftingInput grid) return new GridContainer(grid);
        return new GridContainer(input);
    }

    /** A {@link Container} as the {@link RecipeInput} 1.21.1 recipes are matched against. */
    public static RecipeInput input(Container container) {
        if (container instanceof GridContainer g) return g.input;
        if (container instanceof CraftingContainer cc) return cc.asCraftInput();
        return new ContainerInput(container);
    }

    /** Input view of a container that is not a crafting grid. */
    public record ContainerInput(Container container) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return container.getItem(index);
        }

        @Override
        public int size() {
            return container.getContainerSize();
        }
    }

    /** Container view of an input (copies the stacks: recipes only read them). */
    public static final class GridContainer extends net.minecraft.world.SimpleContainer implements CraftingContainer {
        final RecipeInput input;
        private final int width;
        private final int height;

        GridContainer(RecipeInput input) {
            super(input.size());
            this.input = input;
            this.width = input instanceof CraftingInput g ? g.width() : input.size();
            this.height = input instanceof CraftingInput g ? g.height() : 1;
            for (int i = 0; i < input.size(); i++) setItem(i, input.getItem(i));
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }
}
