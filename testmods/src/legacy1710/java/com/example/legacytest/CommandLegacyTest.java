package com.example.legacytest;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandLegacyTest extends CommandBase {
    @Override
    public String getCommandName() {
        return "legacytest";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/legacytest";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentText("LegacyTest: " + SelfTest.summary()));
    }
}
