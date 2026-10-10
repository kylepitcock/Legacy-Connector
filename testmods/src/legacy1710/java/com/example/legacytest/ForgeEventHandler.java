package com.example.legacytest;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.init.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

public class ForgeEventHandler {
    public static int interactions;

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        interactions++;
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.world.getBlock(event.x, event.y, event.z) == Blocks.bedrock) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.block == LegacyTestMod.machine && event.getPlayer() != null && !event.getPlayer().capabilities.isCreativeMode) {
            LegacyTestMod.log.info("LegacyTest: machine broken by " + event.getPlayer().getCommandSenderName());
        }
    }
}
