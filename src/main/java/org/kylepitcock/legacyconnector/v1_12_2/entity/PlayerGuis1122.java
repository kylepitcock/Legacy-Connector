package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.world.IInteractionObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The vanilla GUIs 1.12.2 players open directly (not through FML's IGuiHandler): {@code displayGUIChest} on an inventory, {@code displayGui}
 * on an interaction object (crafting table, anvil, enchanting table by GUI id) and {@code displayVillagerTradeGui} on a merchant, as the
 * matching modern menus. Server side only, as in 1.12 (the client opened them when the server told it to).
 */
public final class PlayerGuis1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Gui");
    private static final java.util.Set<String> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private PlayerGuis1122() {}

    private static ServerPlayer server(EntityPlayer player) {
        return player != null && player.mcc$entity instanceof ServerPlayer sp ? sp : null;
    }

    private static Component title(String name, boolean custom) {
        if (name == null) return Component.empty();
        return custom ? Component.literal(name) : Component.translatable(name);
    }

    /** {@code EntityPlayer.displayGUIChest}: a chest-like menu over the inventory (rows of nine; five slots are a hopper, nine a dispenser grid). */
    public static void displayGUIChest(EntityPlayer player, IInventory inventory) {
        ServerPlayer sp = server(player);
        if (sp == null || inventory == null) return;
        if (inventory instanceof IInteractionObject io) {
            displayGui(player, io);
            return;
        }
        net.minecraft.world.Container modern = ModernInventory1122.modern(inventory);
        int size = modern.getContainerSize();
        Component title = title(inventory.getName(), inventory.hasCustomName());
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            if (size == 5) return new net.minecraft.world.inventory.HopperMenu(id, inv, modern);
            if (size == 9 && "container.dispenser".equals(inventory.getName())) return new net.minecraft.world.inventory.DispenserMenu(id, inv, modern);
            int rows = Math.max(1, Math.min(6, size / 9));
            MenuType<?> type = switch (rows) {
                case 1 -> MenuType.GENERIC_9x1;
                case 2 -> MenuType.GENERIC_9x2;
                case 3 -> MenuType.GENERIC_9x3;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                default -> MenuType.GENERIC_9x6;
            };
            return new net.minecraft.world.inventory.ChestMenu(type, id, inv, size < rows * 9 ? padded(modern, rows * 9) : modern, rows);
        }, title));
    }

    /** A container of {@code size} slots over a smaller one (the extra slots hold nothing and take nothing). */
    private static net.minecraft.world.Container padded(net.minecraft.world.Container inner, int size) {
        return new net.minecraft.world.Container() {
            public int getContainerSize() {
                return size;
            }

            public boolean isEmpty() {
                return inner.isEmpty();
            }

            public net.minecraft.world.item.ItemStack getItem(int slot) {
                return slot < inner.getContainerSize() ? inner.getItem(slot) : net.minecraft.world.item.ItemStack.EMPTY;
            }

            public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
                return slot < inner.getContainerSize() ? inner.removeItem(slot, amount) : net.minecraft.world.item.ItemStack.EMPTY;
            }

            public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
                return slot < inner.getContainerSize() ? inner.removeItemNoUpdate(slot) : net.minecraft.world.item.ItemStack.EMPTY;
            }

            public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
                if (slot < inner.getContainerSize()) inner.setItem(slot, stack);
            }

            public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
                return slot < inner.getContainerSize() && inner.canPlaceItem(slot, stack);
            }

            public void setChanged() {
                inner.setChanged();
            }

            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return inner.stillValid(player);
            }

            public void startOpen(net.minecraft.world.entity.player.Player player) {
                inner.startOpen(player);
            }

            public void stopOpen(net.minecraft.world.entity.player.Player player) {
                inner.stopOpen(player);
            }

            public void clearContent() {
                inner.clearContent();
            }
        };
    }

    /**
     * {@code EntityPlayer.displayGui}: the vanilla GUI its id names. The workbench, anvil and enchanting table become their modern menus
     * (valid wherever the player is, as a portable crafting table's interface was); other ids show a chest-like menu when the object is an
     * inventory and are logged otherwise.
     */
    public static void displayGui(EntityPlayer player, IInteractionObject object) {
        ServerPlayer sp = server(player);
        if (sp == null || object == null) return;
        String id = object.getGuiID() == null ? "" : object.getGuiID();
        Component title = object.getDisplayName() != null ? object.getDisplayName().mcc$modern() : title(object.getName(), object.hasCustomName());
        net.minecraft.world.inventory.MenuConstructor menu = switch (id) {
            case "minecraft:crafting_table" -> (w, inv, p) -> new net.minecraft.world.inventory.CraftingMenu(w, inv, ContainerLevelAccess.NULL);
            case "minecraft:anvil" -> (w, inv, p) -> new net.minecraft.world.inventory.AnvilMenu(w, inv, ContainerLevelAccess.NULL);
            case "minecraft:enchanting_table" -> (w, inv, p) -> new net.minecraft.world.inventory.EnchantmentMenu(w, inv,
                    ContainerLevelAccess.create(sp.level(), sp.blockPosition()));
            default -> null;
        };
        if (menu != null) {
            sp.openMenu(new SimpleMenuProvider(menu, title));
            return;
        }
        if (object instanceof IInventory inventory) {
            var modern = ModernInventory1122.modern(inventory);
            if (modern.getContainerSize() % 9 == 0 || modern.getContainerSize() == 5) {
                displayGUIChestPlain(sp, inventory, modern, title);
                return;
            }
        }
        if (WARNED.add(id)) LOG.warn("displayGui with the GUI id {} ({}) has no modern counterpart; mods open their own GUIs with openGui", id,
                object.getClass().getName());
    }

    private static void displayGUIChestPlain(ServerPlayer sp, IInventory inventory, net.minecraft.world.Container modern, Component title) {
        int size = modern.getContainerSize();
        sp.openMenu(new SimpleMenuProvider((w, inv, p) -> size == 5 ? new net.minecraft.world.inventory.HopperMenu(w, inv, modern)
                : new net.minecraft.world.inventory.ChestMenu(switch (Math.max(1, Math.min(6, size / 9))) {
                    case 1 -> MenuType.GENERIC_9x1;
                    case 2 -> MenuType.GENERIC_9x2;
                    case 3 -> MenuType.GENERIC_9x3;
                    case 4 -> MenuType.GENERIC_9x4;
                    case 5 -> MenuType.GENERIC_9x5;
                    default -> MenuType.GENERIC_9x6;
                }, w, inv, modern, Math.max(1, Math.min(6, size / 9))), title));
    }

    /**
     * {@code EntityPlayer.displayVillagerTradeGui}: the modern trading screen. A modern villager trades as itself; any other merchant
     * through {@link Merchant} over its legacy trade list.
     */
    public static void displayVillagerTradeGui(EntityPlayer player, IMerchant merchant) {
        ServerPlayer sp = server(player);
        if (sp == null || merchant == null) return;
        Component title = merchant.getDisplayName() == null ? Component.empty() : merchant.getDisplayName().mcc$modern();
        if (merchant instanceof legacyconnector.v1_12_2.net.minecraft.entity.Entity e && e.mcc$entity instanceof net.minecraft.world.entity.npc.AbstractVillager v
                && !(e.mcc$entity instanceof Host1122)) {
            v.setTradingPlayer(sp);
            v.openTradingScreen(sp, title, v instanceof net.minecraft.world.entity.npc.Villager villager ? villager.getVillagerData().getLevel() : 1);
            return;
        }
        Merchant modern = new Merchant(merchant, sp);
        merchant.setCustomer(player);
        modern.openTradingScreen(sp, title, 1);
    }

    /** A modern merchant over a legacy one: its offers are the legacy trade list's, a completed trade is the legacy {@code useRecipe}. */
    public static final class Merchant implements net.minecraft.world.item.trading.Merchant {
        private final IMerchant legacy;
        private net.minecraft.world.entity.player.Player trading;
        private net.minecraft.world.item.trading.MerchantOffers offers;
        private final java.util.List<legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipe> recipes = new java.util.ArrayList<>();

        public Merchant(IMerchant legacy, net.minecraft.world.entity.player.Player player) {
            this.legacy = legacy;
            this.trading = player;
        }

        @Override
        public void setTradingPlayer(net.minecraft.world.entity.player.Player player) {
            this.trading = player;
            legacy.setCustomer(player == null ? null : EntityBridge1122.wrap(player) instanceof EntityPlayer p ? p : null);
        }

        @Override
        public net.minecraft.world.entity.player.Player getTradingPlayer() {
            return trading;
        }

        @Override
        public net.minecraft.world.item.trading.MerchantOffers getOffers() {
            if (offers == null) {
                offers = new net.minecraft.world.item.trading.MerchantOffers();
                recipes.clear();
                var list = legacy.getRecipes(trading == null ? null : EntityBridge1122.wrap(trading) instanceof EntityPlayer p ? p : null);
                if (list != null) {
                    for (var r : list) {
                        var offer = r.mcc$toModern();
                        if (offer == null) continue;
                        offers.add(offer);
                        recipes.add(r);
                    }
                }
            }
            return offers;
        }

        @Override
        public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers) {
            this.offers = offers;
        }

        @Override
        public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
            offer.increaseUses();
            int i = offers == null ? -1 : offers.indexOf(offer);
            if (i >= 0 && i < recipes.size()) legacy.useRecipe(recipes.get(i));
        }

        @Override
        public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
            legacy.verifySellingItem(Stacks1122.fromModern(stack));
        }

        @Override
        public int getVillagerXp() {
            return 0;
        }

        @Override
        public void overrideXp(int xp) {
        }

        @Override
        public boolean showProgressBar() {
            return false;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
            return net.minecraft.sounds.SoundEvents.VILLAGER_YES;
        }

        @Override
        public boolean isClientSide() {
            return trading != null && trading.level().isClientSide();
        }
    }
}
