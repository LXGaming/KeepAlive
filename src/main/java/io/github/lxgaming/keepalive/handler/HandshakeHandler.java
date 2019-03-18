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

package io.github.lxgaming.keepalive.handler;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.util.Reference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.handshake.HandshakeMessageHandler;

public class HandshakeHandler extends HandshakeMessageHandler {
    
    public static final String NAME = Reference.ID + ":handshake_handler";
    
    @SuppressWarnings("unchecked")
    public HandshakeHandler(Class stateType) {
        super(stateType);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FMLHandshakeMessage msg) {
        // FMLMessage.CompleteHandshake has to be sent from Netty Thread otherwise it doesn't get processed
        if (msg instanceof FMLHandshakeMessage.HandshakeAck) {
            FMLHandshakeMessage.HandshakeAck packet = (FMLHandshakeMessage.HandshakeAck) msg;
            
            ByteBuf byteBuf = Unpooled.buffer(1, 1);
            packet.toBytes(byteBuf);
            
            int phase = byteBuf.readByte();
            byteBuf.release();
            
            KeepAlive.getInstance().getLogger().info("Processing ACK: {}", phase);
            
            if (phase == 3) {
                try {
                    super.channelRead0(ctx, msg);
                } catch (Exception ex) {
                    exceptionCaught(ctx, ex);
                }
                
                return;
            }
        }
        
        // Schedule FMLHandshakeMessage to be processed on the Client Thread
        // This prevents Forge from 'stalling' the Network Thread
        // Reference: https://github.com/MinecraftForge/MinecraftForge/issues/4901
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                super.channelRead0(ctx, msg);
            } catch (Exception ex) {
                exceptionCaught(ctx, ex);
            }
        });
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            super.exceptionCaught(ctx, cause);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}