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

package io.github.lxgaming.keepalive.service;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.util.Reference;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraftforge.fml.client.FMLClientHandler;

public class KeepAliveService extends AbstractService {
    
    @Override
    public boolean prepareService() {
        setDelay(20000L);
        setInterval(20000L);
        return true;
    }
    
    @Override
    public void executeService() {
        INetHandler netHandler = FMLClientHandler.instance().getClientPlayHandler();
        if (netHandler == null) {
            KeepAlive.getInstance().getLogger().error("NetHandler is null");
            cancel();
            return;
        }
        
        if (Minecraft.getMinecraft().getConnection() == netHandler) {
            KeepAlive.getInstance().getLogger().warn("Minecraft has connected");
            cancel();
            return;
        }
        
        NetworkManager networkManager = ((NetHandlerPlayClient) netHandler).getNetworkManager();
        if (!networkManager.isChannelOpen()) {
            KeepAlive.getInstance().getLogger().error("Channel is not open");
            cancel();
            return;
        }
        
        networkManager.channel().writeAndFlush(new CPacketCustomPayload(Reference.ID, new PacketBuffer(Unpooled.buffer())));
        KeepAlive.getInstance().getLogger().info("Sent CustomPayload");
    }
    
    public boolean cancel() {
        return isRunning() && getScheduledFuture().cancel(false);
    }
}