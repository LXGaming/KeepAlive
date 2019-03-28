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
import io.github.lxgaming.keepalive.manager.ConnectionManager;
import io.netty.util.Attribute;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

import java.util.Iterator;

public class KeepAliveService extends AbstractService {
    
    @Override
    public boolean prepareService() {
        setInterval(1000L);
        return true;
    }
    
    @Override
    public void executeService() {
        for (Iterator<NetworkManager> iterator = ConnectionManager.getConnections().iterator(); iterator.hasNext(); ) {
            NetworkManager networkManager = iterator.next();
            String username = ConnectionManager.getUsername(networkManager);
            if (!networkManager.isChannelOpen()) {
                KeepAlive.getInstance().debugMessage("Connection closed for {}", username);
                ConnectionManager.removeConnection(networkManager);
                continue;
            }
            
            NetworkDispatcher.ConnectionType connectionType = ConnectionManager.getNetworkDispatcher(networkManager.channel()).getConnectionType();
            if (connectionType != null) {
                KeepAlive.getInstance().debugMessage("{} Connection for {}", connectionType, username);
                ConnectionManager.removeConnection(networkManager);
                continue;
            }
            
            if (ConnectionManager.getDoneServer(networkManager.channel()).get() != Boolean.TRUE) {
                KeepAlive.getInstance().debugMessage("Invalid State for {}", username);
                continue;
            }
            
            Attribute<Boolean> requested = ConnectionManager.getRequested(networkManager.channel());
            if (requested.get() == Boolean.TRUE) {
                KeepAlive.getInstance().debugMessage("KeepAlive already requested for {}", username);
                continue;
            }
            
            if (ConnectionManager.getDoneClient(networkManager.channel()).get() == Boolean.TRUE) {
                KeepAlive.getInstance().debugMessage("Client Done for {}", username);
                ConnectionManager.removeConnection(networkManager);
                continue;
            }
            
            Attribute<Long> key = ConnectionManager.getKey(networkManager.channel());
            
            long currentTime = System.currentTimeMillis();
            if (key.get() == null || currentTime - key.get() >= 15000L) {
                KeepAlive.getInstance().debugMessage("Sending KeepAlive {} to {}", currentTime, username);
                requested.set(true);
                key.set(currentTime);
                networkManager.sendPacket(new SPacketKeepAlive(currentTime));
            }
        }
    }
}