package legacyconnector.v1_12_2.vazkii.patchouli.api;

import java.io.InputStream;
import java.util.function.Predicate;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Patchouli 1.12's API on top of the modern Patchouli (the bridge is active only when it is installed; see LibraryBridges in the loader).
 * The implementation sits in {@code org.kylepitcock.legacyconnector.compat.patchouli} and is the only code that touches Patchouli's classes.
 */
public class PatchouliAPI {
    public static IPatchouliAPI instance = create();

    private static IPatchouliAPI create() {
        try {
            return new org.kylepitcock.legacyconnector.compat.patchouli.PatchouliApi1122();
        } catch (LinkageError | RuntimeException e) {
            return new Stub();
        }
    }

    public interface IPatchouliAPI {
        boolean isStub();

        void setConfigFlag(String flag, boolean value);

        boolean getConfigFlag(String flag);

        void openBookGUI(EntityPlayerMP player, ResourceLocation book);

        void openBookGUI(ResourceLocation book);

        ResourceLocation getOpenBookGui();

        void reloadBookContents();

        ItemStack getBookStack(String book);

        void registerTemplateAsBuiltin(ResourceLocation res, Supplier<InputStream> streamProvider);

        ItemStack deserializeItemStack(String str);

        String serializeItemStack(ItemStack stack);

        Ingredient deserializeIngredient(String str);

        String serializeIngredient(Ingredient ingredient);

        IMultiblock getMultiblock(ResourceLocation res);

        IMultiblock registerMultiblock(ResourceLocation res, IMultiblock mb);

        IMultiblock makeMultiblock(String[][] pattern, Object... targets);

        IStateMatcher predicateMatcher(IBlockState display, Predicate<IBlockState> predicate);

        IStateMatcher predicateMatcher(Block display, Predicate<IBlockState> predicate);

        IStateMatcher stateMatcher(IBlockState state);

        IStateMatcher looseBlockMatcher(Block block);

        IStateMatcher strictBlockMatcher(Block block);

        IStateMatcher displayOnlyMatcher(IBlockState state);

        IStateMatcher displayOnlyMatcher(Block block);

        IStateMatcher airMatcher();

        IStateMatcher anyMatcher();
    }

    /** Used when the bridge cannot link to Patchouli (never during normal play: the bridge is only active with it installed). */
    private static final class Stub implements IPatchouliAPI {
        public boolean isStub() { return true; }
        public void setConfigFlag(String flag, boolean value) {}
        public boolean getConfigFlag(String flag) { return false; }
        public void openBookGUI(EntityPlayerMP player, ResourceLocation book) {}
        public void openBookGUI(ResourceLocation book) {}
        public ResourceLocation getOpenBookGui() { return null; }
        public void reloadBookContents() {}
        public ItemStack getBookStack(String book) { return ItemStack.EMPTY; }
        public void registerTemplateAsBuiltin(ResourceLocation res, Supplier<InputStream> streamProvider) {}
        public ItemStack deserializeItemStack(String str) { return ItemStack.EMPTY; }
        public String serializeItemStack(ItemStack stack) { return ""; }
        public Ingredient deserializeIngredient(String str) { return Ingredient.EMPTY; }
        public String serializeIngredient(Ingredient ingredient) { return ""; }
        public IMultiblock getMultiblock(ResourceLocation res) { return null; }
        public IMultiblock registerMultiblock(ResourceLocation res, IMultiblock mb) { return mb; }
        public IMultiblock makeMultiblock(String[][] pattern, Object... targets) { return null; }
        public IStateMatcher predicateMatcher(IBlockState display, Predicate<IBlockState> predicate) { return null; }
        public IStateMatcher predicateMatcher(Block display, Predicate<IBlockState> predicate) { return null; }
        public IStateMatcher stateMatcher(IBlockState state) { return null; }
        public IStateMatcher looseBlockMatcher(Block block) { return null; }
        public IStateMatcher strictBlockMatcher(Block block) { return null; }
        public IStateMatcher displayOnlyMatcher(IBlockState state) { return null; }
        public IStateMatcher displayOnlyMatcher(Block block) { return null; }
        public IStateMatcher airMatcher() { return null; }
        public IStateMatcher anyMatcher() { return null; }
    }
}
