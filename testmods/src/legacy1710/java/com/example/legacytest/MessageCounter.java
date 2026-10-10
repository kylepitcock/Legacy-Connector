package com.example.legacytest;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageCounter implements IMessage {
    public int value;

    public MessageCounter() {
    }

    public MessageCounter(int value) {
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<MessageCounter, IMessage> {
        @Override
        public IMessage onMessage(MessageCounter message, MessageContext ctx) {
            LegacyTestMod.log.info("LegacyTest: client received counter " + message.value);
            return null;
        }
    }
}
