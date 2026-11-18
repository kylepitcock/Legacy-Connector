package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/**
 * Recipe book contents for the client. The modern client rebuilds its recipe book from the data pack's recipes and the advancement-like
 * unlock state the server sends itself, so this one only carries what a mod put in it.
 */
public class SPacketRecipeBook implements Packet<INetHandlerPlayClient> {
    private State state;
    private java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> recipes = java.util.Collections.emptyList();
    private java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> displayedRecipes = java.util.Collections.emptyList();
    private boolean guiOpen;
    private boolean filteringCraftable;

    public SPacketRecipeBook() {
    }

    public SPacketRecipeBook(State stateIn, java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> recipesIn,
            java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> displayedRecipesIn, boolean guiOpenIn, boolean filteringCraftableIn) {
        this.state = stateIn;
        this.recipes = recipesIn;
        this.displayedRecipes = displayedRecipesIn;
        this.guiOpen = guiOpenIn;
        this.filteringCraftable = filteringCraftableIn;
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> getRecipes() {
        return recipes;
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> getDisplayedRecipes() {
        return displayedRecipes;
    }

    public State getState() {
        return state;
    }

    public boolean isGuiOpen() {
        return guiOpen;
    }

    public boolean isFilteringCraftable() {
        return filteringCraftable;
    }

    public enum State {
        INIT, ADD, REMOVE
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }
}
