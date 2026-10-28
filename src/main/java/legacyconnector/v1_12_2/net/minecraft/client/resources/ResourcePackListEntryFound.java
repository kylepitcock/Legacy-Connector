package legacyconnector.v1_12_2.net.minecraft.client.resources;

/** A pack of the repository in the resource packs screen. */
public class ResourcePackListEntryFound extends ResourcePackListEntry {
    private final ResourcePackRepository.Entry resourcePackEntry;

    public ResourcePackListEntryFound(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreenResourcePacks gui, ResourcePackRepository.Entry entry) {
        super(gui);
        this.resourcePackEntry = entry;
    }

    public ResourcePackRepository.Entry getResourcePackEntry() {
        return resourcePackEntry;
    }

    @Override
    protected String getResourcePackName() {
        return resourcePackEntry.getResourcePackName();
    }

    @Override
    protected String getResourcePackDescription() {
        return resourcePackEntry.getTexturePackDescription();
    }
}
