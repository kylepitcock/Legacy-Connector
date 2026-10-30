package legacyconnector.v1_12_2.net.minecraft.stats;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * 1.12.2's recipe book of a server player. A view of the player's modern {@code ServerRecipeBook} ({@link #mcc$of}) unlocks and locks
 * recipes there, which also tells the client; a book a mod makes keeps its own state like {@link RecipeBook}.
 */
public class RecipeBookServer extends RecipeBook {
    private final net.minecraft.stats.ServerRecipeBook mcc$server;

    public RecipeBookServer() {
        this(null);
    }

    private RecipeBookServer(net.minecraft.stats.ServerRecipeBook modern) {
        super(modern);
        this.mcc$server = modern;
    }

    /** A view of a server player's modern recipe book (null gives null). */
    public static RecipeBookServer mcc$of(net.minecraft.stats.ServerRecipeBook modern) {
        return modern == null ? null : new RecipeBookServer(modern);
    }

    private static java.util.List<net.minecraft.world.item.crafting.RecipeHolder<?>> holders(List<IRecipe> recipes, EntityPlayerMP player) {
        var out = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>();
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (recipes == null || sp == null) return out;
        var manager = sp.server.getRecipeManager();
        for (IRecipe r : recipes) {
            if (r == null || r.getRegistryName() == null) continue;
            manager.byKey(r.getRegistryName().mcc$modern()).ifPresent(out::add);
        }
        return out;
    }

    /** Unlocks the recipes for the player (marked new and sent to its client). */
    public void add(List<IRecipe> recipesIn, EntityPlayerMP player) {
        if (mcc$server != null && player != null && player.mcc$serverPlayer() != null) {
            mcc$server.addRecipes(holders(recipesIn, player), player.mcc$serverPlayer());
            return;
        }
        if (recipesIn == null) return;
        for (IRecipe r : recipesIn) {
            if (r != null && !isUnlocked(r) && !r.isDynamic()) {
                unlock(r);
                markNew(r);
            }
        }
    }

    /** Locks the recipes again for the player. */
    public void remove(List<IRecipe> recipesIn, EntityPlayerMP player) {
        if (mcc$server != null && player != null && player.mcc$serverPlayer() != null) {
            mcc$server.removeRecipes(holders(recipesIn, player), player.mcc$serverPlayer());
            return;
        }
        if (recipesIn == null) return;
        for (IRecipe r : recipesIn) if (r != null) lock(r);
    }

    /** The book as saved with the player (the modern format: recipe names, open and filtering flags). */
    public NBTTagCompound write() {
        if (mcc$server != null) return new NBTTagCompound(mcc$server.toNbt());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("isGuiOpen", isGuiOpen());
        tag.setBoolean("isFilteringCraftable", isFilteringCraftable());
        return tag;
    }

    public void read(NBTTagCompound tag) {
        if (tag == null) return;
        if (mcc$server != null) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) mcc$server.fromNbt(tag.mcc$modern(), server.getRecipeManager());
            return;
        }
        setGuiOpen(tag.getBoolean("isGuiOpen"));
        setFilteringCraftable(tag.getBoolean("isFilteringCraftable"));
    }

    /** Sends the whole book to the player's client (on login). */
    public void init(EntityPlayerMP player) {
        if (mcc$server != null && player != null && player.mcc$serverPlayer() != null) mcc$server.sendInitialRecipeBook(player.mcc$serverPlayer());
    }
}
