/*
 * Copyright 2019 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lxgaming.keepalive.util;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.interfaces.client.IMixinMinecraft;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Toolbox {
    
    public static ITextComponent getTextPrefix() {
        TextComponentString text = new TextComponentString(String.format("[%s] ", Reference.NAME));
        text.getStyle().setColor(TextFormatting.BLUE).setBold(true);
        return new TextComponentString("").appendSibling(text);
    }
    
    public static IMixinMinecraft getMinecraft() {
        return Toolbox.cast(Minecraft.getMinecraft(), IMixinMinecraft.class);
    }
    
    public static Side getOppositeSide(Side side) {
        if (side == Side.CLIENT) {
            return Side.SERVER;
        }
        
        return Side.CLIENT;
    }
    
    public static State readState(FMLHandshakeMessage.HandshakeAck packet, Side side) {
        ByteBuf byteBuf = Unpooled.buffer(1, 1);
        packet.toBytes(byteBuf);
        
        byte id = byteBuf.readByte();
        byteBuf.release();
        return State.getState(id, side);
    }
    
    public static void writeState(Channel channel, State state) {
        PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer());
        packetBuffer.writeByte(-1); // HandshakeAck
        packetBuffer.writeByte(state.getId());
        channel.writeAndFlush(new FMLProxyPacket(packetBuffer, "FML|HS")).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        KeepAlive.getInstance().debugMessage("Sent {} to {}", state.getName(), getOppositeSide(FMLCommonHandler.instance().getSide()).name());
    }
    
    public static ThreadFactory buildThreadFactory(String namingPattern) {
        return new BasicThreadFactory.Builder().namingPattern(namingPattern).daemon(true).priority(Thread.NORM_PRIORITY).build();
    }
    
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, buildThreadFactory("Service Thread #%d"));
        scheduledThreadPoolExecutor.setMaximumPoolSize(maximumPoolSize);
        scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveTime, unit);
        return scheduledThreadPoolExecutor;
    }
    
    public static <T> T cast(Object object, Class<T> type) {
        return type.cast(object);
    }
}