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

package io.github.lxgaming.keepalive.listener;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.configuration.Configuration;
import io.github.lxgaming.keepalive.handler.HandshakeHandler;
import io.github.lxgaming.keepalive.manager.ServiceManager;
import io.github.lxgaming.keepalive.util.Toolbox;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.handshake.HandshakeMessageHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import org.apache.commons.lang3.StringUtils;

public class NetworkListener {
    
    @SubscribeEvent
    public void onClientCustomPacketRegistration(FMLNetworkEvent.CustomPacketRegistrationEvent<INetHandlerPlayClient> event) {
        if (!Configuration.enabled) {
            return;
        }
        
        if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
            KeepAlive.getInstance().getLogger().error("IntegratedServer is running");
            return;
        }
        
        if (!StringUtils.equals(event.getOperation(), "REGISTER")) {
            return;
        }
        
        Channel channel = event.getManager().channel();
        
        // NetworkDispatcher
        NetworkDispatcher networkDispatcher = channel.attr(NetworkDispatcher.FML_DISPATCHER).get();
        if (networkDispatcher == null) {
            KeepAlive.getInstance().getLogger().error("NetworkDispatcher is null");
            return;
        }
        
        // EmbeddedChannel
        EmbeddedChannel embeddedChannel = Toolbox.getField(networkDispatcher, "handshakeChannel", EmbeddedChannel.class);
        if (embeddedChannel == null) {
            KeepAlive.getInstance().getLogger().error("EmbeddedChannel is null");
            return;
        }
        
        if (embeddedChannel.pipeline().get(HandshakeHandler.NAME) != null) {
            KeepAlive.getInstance().getLogger().warn("{} already exists", HandshakeHandler.NAME);
            return;
        }
        
        // HandshakeMessageHandler
        HandshakeMessageHandler handshakeMessageHandler = embeddedChannel.pipeline().get(HandshakeMessageHandler.class);
        if (handshakeMessageHandler == null) {
            KeepAlive.getInstance().getLogger().error("HandshakeMessageHandler is null");
            return;
        }
        
        // StateType
        Class stateType = Toolbox.getField(handshakeMessageHandler, "stateType", Class.class);
        if (stateType == null) {
            KeepAlive.getInstance().getLogger().error("StateType is null");
            return;
        }
        
        // Replace HandshakeMessageHandler with HandshakeHandler
        embeddedChannel.pipeline().replace(HandshakeMessageHandler.class, HandshakeHandler.NAME, new HandshakeHandler(stateType));
        KeepAlive.getInstance().getLogger().info("Added {}", HandshakeHandler.NAME);
        
        // Remove ReadTimeoutHandler
        if (channel.pipeline().get("timeout") != null) {
            channel.pipeline().remove("timeout");
            KeepAlive.getInstance().getLogger().info("Removed {}", "timeout");
        } else {
            KeepAlive.getInstance().getLogger().warn("timeout doesn't exist");
        }
        
        ServiceManager.schedule(KeepAlive.getInstance().getService());
    }
    
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // Cancel Service
        KeepAlive.getInstance().getService().cancel();
        
        // Add ReadTimeoutHandler
        Channel channel = event.getManager().channel();
        if (channel.pipeline().get("timeout") == null) {
            channel.pipeline().addFirst("timeout", new ReadTimeoutHandler(FMLNetworkHandler.READ_TIMEOUT));
            KeepAlive.getInstance().getLogger().info("Added {}", "timeout");
        } else {
            KeepAlive.getInstance().getLogger().warn("timeout already exists");
        }
    }
    
    @SubscribeEvent
    public void onClientDisconnectionFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Cancel Service
        KeepAlive.getInstance().getService().cancel();
        
        // Add ReadTimeoutHandler
        Channel channel = event.getManager().channel();
        if (channel.pipeline().get("timeout") == null) {
            channel.pipeline().addFirst("timeout", new ReadTimeoutHandler(FMLNetworkHandler.READ_TIMEOUT));
            KeepAlive.getInstance().getLogger().info("Added {}", "timeout");
        } else {
            KeepAlive.getInstance().getLogger().warn("timeout already exists");
        }
    }
}