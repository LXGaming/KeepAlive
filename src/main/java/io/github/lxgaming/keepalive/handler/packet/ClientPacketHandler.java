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

package io.github.lxgaming.keepalive.handler.packet;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.handler.AbstractHandler;
import io.github.lxgaming.keepalive.manager.ConnectionManager;
import io.github.lxgaming.keepalive.util.Reference;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.network.play.server.SPacketKeepAlive;
import org.apache.commons.lang3.StringUtils;

@ChannelHandler.Sharable
public class ClientPacketHandler extends AbstractHandler {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof SPacketCustomPayload) {
            SPacketCustomPayload packet = (SPacketCustomPayload) msg;
            if (!StringUtils.equals(packet.getChannelName(), Reference.ID)) {
                super.channelRead(ctx, msg);
                return;
            }
            
            byte id = packet.getBufferData().readByte();
            throw new UnsupportedOperationException(String.format("Invalid Packet: %d", id));
        }
        
        if (msg instanceof SPacketKeepAlive) {
            SPacketKeepAlive packet = (SPacketKeepAlive) msg;
            KeepAlive.getInstance().debugMessage("Received KeepAlive {}", packet.getId());
        }
        
        if (msg instanceof SPacketJoinGame) {
            if (ConnectionManager.getValid(ctx.channel()).get() == Boolean.TRUE && ConnectionManager.getDoneClient(ctx.channel()).get() != Boolean.TRUE) {
                KeepAlive.getInstance().debugMessage("Dropping SPacketJoinGame...");
                return;
            }
            
            KeepAlive.getInstance().debugMessage("Processing SPacketJoinGame...");
        }
        
        super.channelRead(ctx, msg);
    }
}