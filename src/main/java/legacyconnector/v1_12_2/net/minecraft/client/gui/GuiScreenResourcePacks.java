package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.resources.ResourcePackListEntry;
import legacyconnector.v1_12_2.net.minecraft.client.resources.ResourcePackListEntryFound;
import legacyconnector.v1_12_2.net.minecraft.client.resources.ResourcePackRepository;

/**
 * The resource packs screen; displaying it shows the game's pack selection screen (choosing packs applies them, as the options screen does).
 * Its lists are the repository's available and selected packs.
 */
public class GuiScreenResourcePacks extends GuiScreen {
    private final GuiScreen parentScreen;
    private List<ResourcePackListEntry> availableResourcePacks;
    private List<ResourcePackListEntry> selectedResourcePacks;
    private boolean changed;

    public GuiScreenResourcePacks(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    private void mcc$fill() {
        if (availableResourcePacks != null) return;
        var repository = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourcePackRepository();
        availableResourcePacks = new ArrayList<>();
        selectedResourcePacks = new ArrayList<>();
        List<ResourcePackRepository.Entry> selected = repository.getRepositoryEntries();
        for (var e : repository.getRepositoryEntriesAll()) {
            if (!selected.contains(e)) availableResourcePacks.add(new ResourcePackListEntryFound(this, e));
        }
        for (int i = selected.size() - 1; i >= 0; i--) selectedResourcePacks.add(new ResourcePackListEntryFound(this, selected.get(i)));
    }

    public boolean hasResourcePackEntry(ResourcePackListEntry entry) {
        return getSelectedResourcePacks().contains(entry);
    }

    public List<ResourcePackListEntry> getListContaining(ResourcePackListEntry entry) {
        return hasResourcePackEntry(entry) ? getSelectedResourcePacks() : getAvailableResourcePacks();
    }

    public List<ResourcePackListEntry> getAvailableResourcePacks() {
        mcc$fill();
        return availableResourcePacks;
    }

    public List<ResourcePackListEntry> getSelectedResourcePacks() {
        mcc$fill();
        return selectedResourcePacks;
    }

    public void markChanged() {
        changed = true;
    }

    /** The game's screen doing the same job (not a 1.12 method). */
    public net.minecraft.client.gui.screens.Screen mcc$modern(net.minecraft.client.gui.screens.Screen parent) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return new net.minecraft.client.gui.screens.packs.PackSelectionScreen(mc.getResourcePackRepository(), repo -> {
            mc.options.updateResourcePacks(repo);
            if (parentScreen != null) legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            else mc.setScreen(parent);
        }, mc.getResourcePackDirectory(), net.minecraft.network.chat.Component.translatable("resourcePack.title"));
    }
}
