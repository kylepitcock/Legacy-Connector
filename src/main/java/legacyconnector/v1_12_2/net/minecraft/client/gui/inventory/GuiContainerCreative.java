package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.InventoryEffectRenderer;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryBasic;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2's creative inventory. Displaying it opens the modern creative screen, which is announced to mods as one of these (its legacy
 * stand-in reports the modern screen's selected tab as the legacy tab index); the class carries the 1.12 members mods read: the selected
 * tab, the 9x5 item grid ({@link ContainerCreative}) and its scrolling.
 */
public class GuiContainerCreative extends InventoryEffectRenderer {
    public static final ResourceLocation CREATIVE_INVENTORY_TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    public static InventoryBasic basicInventory = new InventoryBasic("tmp", true, 45);
    private static int selectedTabIndex = CreativeTabs.BUILDING_BLOCKS.getTabIndex();
    private float currentScroll;
    private boolean isScrolling;
    private boolean wasClicking;
    private boolean clearSearch;

    public GuiContainerCreative(EntityPlayer player) {
        super(new ContainerCreative(player));
        this.allowUserInput = true;
        this.ySize = 136;
        this.xSize = 195;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }

    /** Selects a tab and fills the grid with its items, scrolled to the top. */
    protected void setCurrentCreativeTab(CreativeTabs tab) {
        if (tab == null) return;
        selectedTabIndex = tab.getTabIndex();
        ContainerCreative container = (ContainerCreative) inventorySlots;
        container.itemList.clear();
        for (Item item : Item.REGISTRY) {
            try {
                item.getSubItems(tab, container.itemList);
            } catch (RuntimeException | LinkageError e) {
                // an item that cannot list itself is left out, as the bridge does for tabs
            }
        }
        currentScroll = 0.0F;
        container.scrollTo(0.0F);
    }

    protected boolean needsScrollBars() {
        return inventorySlots instanceof ContainerCreative c && c.canScroll();
    }

    /** Loads or saves a hotbar snapshot (the creative hotbar keys), as the modern creative screen does. */
    public static void handleHotbarSnapshots(Minecraft client, int index, boolean load, boolean save) {
        net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.handleHotbarLoadOrSave(net.minecraft.client.Minecraft.getInstance(), index, load, save);
    }

    /** The creative item grid: 5 rows of 9 slots showing {@link #itemList} from the scroll position, and the hotbar. */
    public static class ContainerCreative extends Container {
        public NonNullList<ItemStack> itemList = NonNullList.create();

        public ContainerCreative(EntityPlayer player) {
            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 9; ++j) addSlotToContainer(new Slot(basicInventory, i * 9 + j, 9 + j * 18, 18 + i * 18));
            }
            Container inventory = GuiInventory.mcc$inventoryContainer(player);
            if (inventory != null) {
                for (int k = 0; k < 9; ++k) {
                    Slot hotbar = inventory.inventorySlots.get(36 + k);
                    addSlotToContainer(new Slot(hotbar.inventory, hotbar.getSlotIndex(), 9 + k * 18, 112));
                }
            }
            scrollTo(0.0F);
        }

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }

        public void scrollTo(float pos) {
            int i = (itemList.size() + 9 - 1) / 9 - 5;
            int j = (int) ((double) (pos * (float) i) + 0.5D);
            if (j < 0) j = 0;
            for (int k = 0; k < 5; ++k) {
                for (int l = 0; l < 9; ++l) {
                    int i1 = l + (k + j) * 9;
                    basicInventory.setInventorySlotContents(l + k * 9, i1 >= 0 && i1 < itemList.size() ? itemList.get(i1) : ItemStack.EMPTY);
                }
            }
        }

        public boolean canScroll() {
            return itemList.size() > 45;
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
            if (index >= inventorySlots.size() - 9 && index < inventorySlots.size()) {
                Slot slot = inventorySlots.get(index);
                if (slot != null && slot.getHasStack()) slot.putStack(ItemStack.EMPTY);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
            return slotIn.yPos > 90;
        }

        @Override
        public boolean canDragIntoSlot(Slot slotIn) {
            return slotIn.inventory instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer
                    || slotIn.yPos > 90 && slotIn.xPos <= 162;
        }
    }
}
