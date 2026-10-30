package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveFormat;
import legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSummary;

/**
 * 1.12.2's singleplayer world list: one {@link GuiListWorldSelectionEntry} per world of the saves folder, newest first. When the
 * modern world list is shown, its legacy stand-in carries one of these mirroring the modern list (entries, scroll, selection).
 */
public class GuiListWorldSelection extends GuiListExtended {
    private final GuiWorldSelection worldSelection;
    private final List<GuiListWorldSelectionEntry> entries = new ArrayList<>();
    private int selectedIdx = -1;

    public GuiListWorldSelection(GuiWorldSelection p_i46590_1_, Minecraft clientIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
        this(p_i46590_1_, clientIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn, true);
    }

    private GuiListWorldSelection(GuiWorldSelection screen, Minecraft clientIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn, boolean read) {
        super(clientIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        this.worldSelection = screen;
        if (read) refreshList();
    }

    /** An empty list whose entries the caller sets ({@link #mcc$setWorlds}): the saves folder is not read. */
    public static GuiListWorldSelection mcc$mirror(GuiWorldSelection screen, Minecraft clientIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
        return new GuiListWorldSelection(screen, clientIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn, false);
    }

    /** Re-reads the saves folder. */
    public void refreshList() {
        ISaveFormat format = mc.getSaveLoader();
        List<WorldSummary> list;
        try {
            list = new ArrayList<>(format.getSaveList());
        } catch (RuntimeException e) {
            list = new ArrayList<>();
        }
        Collections.sort(list);
        mcc$setWorlds(list, format);
    }

    /** Replaces the entries (the stand-in of the modern list sets the modern list's worlds, in its order). */
    public void mcc$setWorlds(List<WorldSummary> worlds, ISaveFormat format) {
        entries.clear();
        for (WorldSummary summary : worlds) entries.add(new GuiListWorldSelectionEntry(this, summary, format));
        if (selectedIdx >= entries.size()) selectedIdx = -1;
    }

    /** The world folders of the entries, in order (lets the stand-in tell whether the modern list changed). */
    public List<String> mcc$fileNames() {
        List<String> out = new ArrayList<>(entries.size());
        for (GuiListWorldSelectionEntry e : entries) out.add(e.mcc$summary().getFileName());
        return out;
    }

    /** Sets the scroll position as the modern list has it. */
    public void mcc$setScroll(float amount) {
        this.amountScrolled = amount;
    }

    /** Sets the selected entry without the selection callback (the stand-in mirrors the modern list's selection). */
    public void mcc$setSelected(int index) {
        this.selectedIdx = index;
    }

    @Override
    public GuiListWorldSelectionEntry getListEntry(int index) {
        return entries.get(index);
    }

    @Override
    protected int getSize() {
        return entries.size();
    }

    @Override
    protected int getScrollBarX() {
        return super.getScrollBarX() + 20;
    }

    @Override
    public int getListWidth() {
        return super.getListWidth() + 50;
    }

    public void selectWorld(int idx) {
        this.selectedIdx = idx;
        if (worldSelection != null) worldSelection.selectWorld(getSelectedWorld());
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return slotIndex == selectedIdx;
    }

    public GuiListWorldSelectionEntry getSelectedWorld() {
        return selectedIdx >= 0 && selectedIdx < getSize() ? getListEntry(selectedIdx) : null;
    }

    public GuiWorldSelection getGuiWorldSelection() {
        return worldSelection;
    }
}
