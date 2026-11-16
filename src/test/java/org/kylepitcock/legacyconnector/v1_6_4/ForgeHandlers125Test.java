package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_6_4.net.minecraft.src.forge.IDestroyToolHandler;
import legacyconnector.v1_6_4.net.minecraft.src.forge.IHoeHandler;
import legacyconnector.v1_6_4.net.minecraft.src.forge.IMinecartHandler;
import legacyconnector.v1_6_4.net.minecraft.src.forge.ISleepHandler;
import legacyconnector.v1_6_4.net.minecraft.src.forge.ISpecialMobSpawnHandler;
import legacyconnector.v1_6_4.net.minecraft.src.forge.MinecraftForge;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartCollisionEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.UseHoeEvent;
import org.junit.jupiter.api.Test;

/** Forge 3.x's handler interfaces dispatch through the shim's event of the same purpose: fake handlers registered, events posted on the shim's bus. */
class ForgeHandlers125Test {
    private static final legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus BUS = legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS;

    @Test
    void sleepHandlerRefusesThroughTheEventResult() {
        MinecraftForge.registerSleepHandler(new ISleepHandler() {
            @Override
            public EntityPlayer.EnumStatus sleepInBedAt(EntityPlayer player, int x, int y, int z) {
                return x == 7 ? EntityPlayer.EnumStatus.NOT_POSSIBLE_HERE : null;
            }
        });
        PlayerSleepInBedEvent refused = new PlayerSleepInBedEvent(null, 7, 64, 0);
        BUS.post(refused);
        assertSame(EntityPlayer.EnumStatus.NOT_POSSIBLE_HERE, refused.result);
        PlayerSleepInBedEvent left = new PlayerSleepInBedEvent(null, 8, 64, 0);
        BUS.post(left);
        assertEquals(null, left.result);
    }

    @Test
    void hoeHandlerAllowsAndDestroyHandlerIsTold() {
        List<String> seen = new ArrayList<>();
        MinecraftForge.registerHoeHandler((hoe, player, world, x, y, z) -> {
            seen.add("hoe" + x);
            return x == 1;
        });
        MinecraftForge.registerDestroyToolHandler(new IDestroyToolHandler() {
            @Override
            public void onDestroyCurrentItem(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack item) {
                seen.add("destroy");
            }
        });
        UseHoeEvent yes = new UseHoeEvent(null, null, null, 1, 0, 0);
        UseHoeEvent no = new UseHoeEvent(null, null, null, 2, 0, 0);
        BUS.post(yes);
        BUS.post(no);
        assertEquals(Event.Result.ALLOW, yes.getResult());
        assertTrue(no.getResult() != Event.Result.ALLOW);
        BUS.post(new PlayerDestroyItemEvent(null, null));
        assertEquals(List.of("hoe1", "hoe2", "destroy"), seen);
    }

    @Test
    void minecartHandlerGetsUpdateCollisionAndInteract() {
        List<String> seen = new ArrayList<>();
        MinecraftForge.registerMinecartHandler(new IMinecartHandler() {
            @Override
            public void onMinecartUpdate(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, int x, int y, int z) {
                seen.add("update " + x + "," + y + "," + z);
            }

            @Override
            public void onMinecartEntityCollision(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
                seen.add("collide");
            }

            @Override
            public boolean onMinecartInteract(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, EntityPlayer player, boolean doInteraction) {
                seen.add("interact " + doInteraction);
                return false; // Forge 3.x: false = the player may NOT interact
            }
        });
        BUS.post(new MinecartUpdateEvent(null, 3.5F, 4.0F, -1.5F));
        BUS.post(new MinecartCollisionEvent(null, null));
        MinecartInteractEvent interact = new MinecartInteractEvent(null, null);
        BUS.post(interact);
        assertEquals(List.of("update 3,4,-2", "collide", "interact true"), seen);
        assertTrue(interact.isCanceled());
    }

    @Test
    void interactHandlerFalseCancelsLikeForgeHooksOnEntityInteract() {
        MinecraftForge.registerEntityInteractHandler((player, entity, isAttack) -> isAttack);
        var right = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.EntityInteractEvent(null, null);
        var left = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.AttackEntityEvent(null, null);
        BUS.post(right);
        BUS.post(left);
        assertTrue(right.isCanceled(), "false = cancel");
        assertTrue(!left.isCanceled(), "true = continue");
    }

    @Test
    void specialSpawnHandlerCancelsTheSpecialSpawn() {
        MinecraftForge.registerSpecialMobSpawnHandler(new ISpecialMobSpawnHandler() {
            @Override
            public boolean onSpecialEntitySpawn(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity,
                    legacyconnector.v1_7_10.net.minecraft.world.World world, float x, float y, float z) {
                return x > 0;
            }
        });
        LivingSpawnEvent.SpecialSpawn taken = new LivingSpawnEvent.SpecialSpawn(null, null, 1F, 0F, 0F);
        LivingSpawnEvent.SpecialSpawn left = new LivingSpawnEvent.SpecialSpawn(null, null, -1F, 0F, 0F);
        BUS.post(taken);
        BUS.post(left);
        assertTrue(taken.isCanceled());
        assertTrue(!left.isCanceled());
    }

    @Test
    void forgeArmourIsTheShimsArmourInterface() throws Exception {
        assertTrue(legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor.class.isAssignableFrom(legacyconnector.v1_6_4.net.minecraft.src.forge.ISpecialArmor.class));
        assertTrue(legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor.ArmorProperties.class
                .isAssignableFrom(legacyconnector.v1_6_4.net.minecraft.src.forge.ArmorProperties.class));
        // the covariant redeclaration makes javac emit the bridge the shared interface needs (a mod implements only the forge/ signature)
        boolean bridged = false;
        boolean forgeShape = false;
        for (var m : legacyconnector.v1_6_4.net.minecraft.src.forge.ISpecialArmor.class.getDeclaredMethods()) {
            if (!m.getName().equals("getProperties")) continue;
            if (m.isBridge()) bridged = m.getReturnType() == legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor.ArmorProperties.class;
            else forgeShape = m.getReturnType() == legacyconnector.v1_6_4.net.minecraft.src.forge.ArmorProperties.class;
        }
        assertTrue(forgeShape);
        assertTrue(bridged);
    }
}
