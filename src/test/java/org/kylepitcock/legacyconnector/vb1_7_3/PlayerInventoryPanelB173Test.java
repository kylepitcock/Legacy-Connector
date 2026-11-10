package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandlerListener;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

class PlayerInventoryPanelB173Test {
    private static sun.misc.Unsafe unsafe;
    private static net.minecraft.world.level.Level nativeLevel;
    private static World world;

    @BeforeAll static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();VanillaB173.init();
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
        // No world/server is started. The native click engine needs only feature flags and difficulty.
        var bytes = new ClassWriter(0);
        bytes.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "org/kylepitcock/legacyconnector/vb1_7_3/PanelClickLevel", null,
                "net/minecraft/server/level/ServerLevel", null);
        staticReturn(bytes, "enabledFeatures", "net/minecraft/world/flag/FeatureFlagSet", "net/minecraft/world/flag/FeatureFlags", "DEFAULT_FLAGS");
        staticReturn(bytes, "getDifficulty", "net/minecraft/world/Difficulty", "net/minecraft/world/Difficulty", "NORMAL");
        bytes.visitEnd();
        nativeLevel = (net.minecraft.world.level.Level) unsafe.allocateInstance(MethodHandles.lookup().defineHiddenClass(bytes.toByteArray(), true).lookupClass());
        world = new World() {};
        var views = World.class.getDeclaredField("VIEWS");views.setAccessible(true);
        @SuppressWarnings("unchecked") var map = (Map<net.minecraft.world.level.Level, World>) views.get(null);
        map.put(nativeLevel, world);
    }

    private static void staticReturn(ClassWriter bytes, String name, String result, String owner, String field) {
        var method = bytes.visitMethod(Opcodes.ACC_PUBLIC, name, "()L" + result + ";", null, null);
        method.visitCode();method.visitFieldInsn(Opcodes.GETSTATIC, owner, field, "L" + result + ";");
        method.visitInsn(Opcodes.ARETURN);method.visitMaxs(1, 1);method.visitEnd();
    }

    public static class NativePlayer extends net.minecraft.world.entity.player.Player {
        net.minecraft.world.entity.player.Inventory items;
        NativePlayer() { super(null, net.minecraft.core.BlockPos.ZERO, 0, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "fixture")); }
        @Override public net.minecraft.world.entity.player.Inventory getInventory() { return items; }
        @Override public net.minecraft.world.level.Level level() { return nativeLevel; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        @Override public void updateTutorialInventoryAction(net.minecraft.world.item.ItemStack carried,
                net.minecraft.world.item.ItemStack clicked, net.minecraft.world.inventory.ClickAction action) {}
    }

    static class Equipment implements Inventory {
        final ItemStack[] items = new ItemStack[2];
        public int size() { return items.length; }
        public ItemStack getStack(int i) { return items[i]; }
        public ItemStack removeStack(int i, int amount) {
            var stack = items[i];if (stack == null) return null;
            if (stack.count <= amount) { items[i] = null;return stack; }
            return stack.split(amount);
        }
        public void setStack(int i, ItemStack stack) { items[i] = stack; }
        public String getName() { return "fixture equipment"; }
        public int getMaxCountPerStack() { return 4; }
        public void markDirty() {}
        public boolean canPlayerUse(PlayerEntity player) { return true; }
    }
    static class Extension extends PlayerBase {
        final Inventory equipment = new Equipment();
        Extension(ClientPlayerEntity player) { super(player); }
    }
    static class Handler extends PlayerScreenHandler {
        int listeners, closes, pickups;
        Handler(PlayerEntity player, Equipment equipment) {
            super(player.inventory);
            for (int i = 0; i < 2; i++) addSlot(new Slot(equipment, i, 190, i * 18) {
                @Override public boolean canInsert(ItemStack stack) { return stack.itemId == 264; }
                @Override public void onTakeItem(ItemStack stack) { pickups++;super.onTakeItem(stack); }
            });
            addSlot(new Slot(new Equipment(), 0, 210, 0));
        }
        @Override public void addListener(ScreenHandlerListener listener) { listeners++;super.addListener(listener); }
        @Override public void onClosed(PlayerEntity player) { closes++;super.onClosed(player); }
    }
    record Fixture(ClientPlayerEntity actor, NativePlayer nativePlayer, Equipment equipment, Handler handler) {}
    private static Fixture fixture() throws Exception {
        var player = (NativePlayer) unsafe.allocateInstance(NativePlayer.class);
        player.items = new net.minecraft.world.entity.player.Inventory(player);
        player.containerMenu = new net.minecraft.world.inventory.AbstractContainerMenu(null, 99) {
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player actor) { return true; }
            @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player actor, int slot) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        };
        var actor = new ClientPlayerEntity(world) {};actor.mcc$modern = player;
        var equipment = new Equipment();var handler = new Handler(actor, equipment);
        actor.playerScreenHandler = actor.currentScreenHandler = handler;
        handler.syncId = 77;
        return new Fixture(actor, player, equipment, handler);
    }
    private static ItemStack diamonds(int count) {
        var stack = new net.minecraft.world.item.ItemStack(Items.DIAMOND, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Panel component proof"));
        return StacksB173.toLegacy(stack);
    }

    @Test void exposesOnlyCanonicalSlotsOwnedByRegisteredBaseBindings() throws Exception {
        var fixture = fixture();var extension = new Extension(fixture.actor());
        var handler = new Handler(fixture.actor(), (Equipment) extension.equipment);
        var selected = PlayerInventoryPanelB173.select(handler,
                PlayerInventoryStateB173.bindings(List.of(extension), fixture.actor().inventory));
        assertArrayEquals(new int[] {45, 46}, selected);
    }

    @Test void nativeAuxiliaryClicksConserveItemsAndCursorWithoutOpeningOrClosingMenus() throws Exception {
        var fixture = fixture();var actor = fixture.actor();var handler = fixture.handler();
        var realMenu = fixture.nativePlayer().containerMenu;
        fixture.equipment().setStack(0, diamonds(3));
        handler.onSlotClick(45, 0, false, actor);
        assertNull(fixture.equipment().getStack(0));assertEquals(3, actor.inventory.getCursorStack().count);
        handler.onSlotClick(45, 1, false, actor);
        assertEquals(1, fixture.equipment().getStack(0).count);assertEquals(2, actor.inventory.getCursorStack().count);
        handler.onSlotClick(45, 0, false, actor);
        assertEquals(3, fixture.equipment().getStack(0).count);assertNull(actor.inventory.getCursorStack());
        assertEquals("Panel component proof", StacksB173.toModern(fixture.equipment().getStack(0)).getHoverName().getString());
        handler.onSlotClick(45, 0, true, actor);
        assertNull(fixture.equipment().getStack(0));assertEquals(3, actor.inventory.getStack(9).count);
        assertNull(actor.inventory.getCursorStack());assertEquals(2, handler.pickups);
        assertSame(realMenu, fixture.nativePlayer().containerMenu);assertSame(handler, actor.currentScreenHandler);
        assertEquals(77, handler.syncId);assertEquals(0, handler.listeners);assertEquals(0, handler.closes);
    }

    @Test void rightPickupAndRejectedPlacementKeepOriginalPredicatesAndCounts() throws Exception {
        var fixture = fixture();fixture.equipment().setStack(0, diamonds(3));
        fixture.handler().onSlotClick(45, 1, false, fixture.actor());
        assertEquals(1, fixture.equipment().getStack(0).count);assertEquals(2, fixture.actor().inventory.getCursorStack().count);
        fixture.actor().inventory.setCursorStack(StacksB173.toLegacy(new net.minecraft.world.item.ItemStack(Items.STONE, 2)));
        fixture.handler().onSlotClick(46, 0, false, fixture.actor());
        assertNull(fixture.equipment().getStack(1));assertEquals(2, fixture.actor().inventory.getCursorStack().count);
        assertEquals(1, fixture.equipment().getStack(0).count);
    }

    @Test void mappedClickRemainsVirtualForOriginalContainerOverrides() throws Exception {
        var fixture = fixture();
        var called = new int[1];
        legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler handler = new Handler(fixture.actor(), fixture.equipment()) {
            @Override public ItemStack onSlotClick(int slot, int button, boolean quick, PlayerEntity actor) {
                called[0]++;
                assertEquals(45, slot);assertEquals(1, button);assertTrue(quick);
                actor.inventory.setCursorStack(diamonds(1));
                return null;
            }
        };
        handler.onSlotClick(45, 1, true, fixture.actor());
        assertEquals(1, called[0]);assertEquals(1, fixture.actor().inventory.getCursorStack().count);
        assertNull(fixture.equipment().getStack(0));
    }
}
