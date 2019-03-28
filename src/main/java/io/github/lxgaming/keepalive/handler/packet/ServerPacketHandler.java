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
import io.netty.util.Attribute;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;

@ChannelHandler.Sharable
public class ServerPacketHandler extends AbstractHandler {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof CPacketCustomPayload) {
            CPacketCustomPayload packet = (CPacketCustomPayload) msg;
            if (!StringUtils.equals(packet.getChannelName(), Reference.ID)) {
                super.channelRead(ctx, msg);
                return;
            }
            
            byte id = packet.getBufferData().readByte();
            if (id == 0) { // Complete
                ConnectionManager.getDoneClient(ctx.channel()).set(true);
                ConnectionManager.getNetworkDispatcher(ctx.channel()).completeHandshake(Side.SERVER);
                return;
            }
            
            throw new UnsupportedOperationException(String.format("Invalid Packet: %d", id));
        }
        
        if (msg instanceof CPacketKeepAlive) {
            CPacketKeepAlive packet = (CPacketKeepAlive) msg;
            KeepAlive.getInstance().debugMessage("Received KeepAlive {}", packet.getKey());
            
            Attribute<Boolean> requested = ConnectionManager.getRequested(ctx.channel());
            Attribute<Long> key = ConnectionManager.getKey(ctx.channel());
            if (requested.get() == Boolean.TRUE && key.get() != null && key.get() == packet.getKey()) {
                requested.set(false);
                return;
            }
        }
        
        super.channelRead(ctx, msg);
    }
}