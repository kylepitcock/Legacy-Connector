package legacyconnector.v1_12_2.net.minecraft.stats;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;

/** 1.12.2 server-side recipe book of a player ({@code EntityPlayerMP.getRecipeBook()}): a view of the player's modern recipe book. */
public class RecipeBookServer extends RecipeBook {
    public RecipeBookServer() {
        super(null);
    }

    protected RecipeBookServer(net.minecraft.stats.ServerRecipeBook modern) {
        super(modern);
    }

    /** The view of a player's modern recipe book (null gives null). */
    public static RecipeBookServer mcc$of(net.minecraft.stats.ServerRecipeBook modern) {
        return modern == null ? null : new RecipeBookServer(modern);
    }

    /** Unlocks recipes for the player (the client is told, as the modern book tells it). */
    public void add(List<IRecipe> recipesIn, EntityPlayerMP player) {
        var modern = mcc$modern();
        var p = player == null ? null : player.mcc$serverPlayer();
        if (modern instanceof net.minecraft.stats.ServerRecipeBook book && p != null) {
            var holders = mcc$holders(recipesIn);
            if (!holders.isEmpty()) book.addRecipes(holders, p);
            return;
        }
        for (IRecipe r : recipesIn) unlock(r);
    }

    public void remove(List<IRecipe> recipesIn, EntityPlayerMP player) {
        var modern = mcc$modern();
        var p = player == null ? null : player.mcc$serverPlayer();
        if (modern instanceof net.minecraft.stats.ServerRecipeBook book && p != null) {
            var holders = mcc$holders(recipesIn);
            if (!holders.isEmpty()) book.removeRecipes(holders, p);
            return;
        }
        for (IRecipe r : recipesIn) lock(r);
    }

    private static List<net.minecraft.world.item.crafting.RecipeHolder<?>> mcc$holders(List<IRecipe> recipes) {
        List<net.minecraft.world.item.crafting.RecipeHolder<?>> out = new java.util.ArrayList<>();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || recipes == null) return out;
        for (IRecipe r : recipes) {
            if (r == null || r.getRegistryName() == null) continue;
            server.getRecipeManager().byKey(r.getRegistryName().mcc$modern()).ifPresent(out::add);
        }
        return out;
    }
}
