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

package io.github.lxgaming.keepalive.manager;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.interfaces.fml.common.network.handshake.IMixinNetworkDispatcher;
import io.github.lxgaming.keepalive.interfaces.server.network.IMixinNetHandlerLoginServer;
import io.github.lxgaming.keepalive.util.Reference;
import io.github.lxgaming.keepalive.util.State;
import io.github.lxgaming.keepalive.util.Toolbox;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class ConnectionManager {
    
    private static final AttributeKey<Boolean> DONE_CLIENT = AttributeKey.valueOf(Reference.ID + ":doneClient");
    private static final AttributeKey<Boolean> DONE_SERVER = AttributeKey.valueOf(Reference.ID + ":doneServer");
    private static final AttributeKey<Long> KEY = AttributeKey.valueOf(Reference.ID + ":key");
    private static final AttributeKey<Boolean> REQUESTED = AttributeKey.valueOf(Reference.ID + ":requested");
    private static final AttributeKey<State> STATE = AttributeKey.valueOf(Reference.ID + ":state");
    private static final AttributeKey<Boolean> VALID = AttributeKey.valueOf(Reference.ID + ":valid");
    private static final Set<NetworkManager> CONNECTIONS = Sets.newConcurrentHashSet();
    
    public static void addConnection(NetworkManager networkManager) {
        getConnections().add(networkManager);
        KeepAlive.getInstance().debugMessage("Added {}'s Connection", getUsername(networkManager));
    }
    
    public static void removeConnection(NetworkManager networkManager) {
        getConnections().remove(networkManager);
        networkManager.channel().attr(VALID).set(null);
        networkManager.channel().attr(STATE).set(null);
        networkManager.channel().attr(REQUESTED).set(null);
        networkManager.channel().attr(KEY).set(null);
        networkManager.channel().attr(DONE_SERVER).set(null);
        networkManager.channel().attr(DONE_CLIENT).set(null);
        KeepAlive.getInstance().debugMessage("Removed {}'s Connection", getUsername(networkManager));
    }
    
    public static String getUsername(NetworkManager networkManager) {
        EntityPlayerMP player = getPlayer(networkManager);
        if (player != null) {
            return player.getName();
        }
        
        return null;
    }
    
    public static EntityPlayerMP getPlayer(NetworkManager networkManager) {
        if (networkManager.getNetHandler() instanceof IMixinNetHandlerLoginServer) {
            return Toolbox.cast(networkManager.getNetHandler(), IMixinNetHandlerLoginServer.class).getPlayer();
        }
        
        if (networkManager.getNetHandler() instanceof NetHandlerPlayServer) {
            return Toolbox.cast(networkManager.getNetHandler(), NetHandlerPlayServer.class).player;
        }
        
        return null;
    }
    
    public static Channel getRootChannel(Channel channel) {
        if (StringUtils.equals(channel.attr(NetworkRegistry.FML_CHANNEL).get(), "FML|HS")) {
            IMixinNetworkDispatcher networkDispatcher = getNetworkDispatcher(channel);
            Preconditions.checkState(networkDispatcher != null, "NetworkDispatcher is null");
            Preconditions.checkState(!networkDispatcher.getNetworkManager().hasNoChannel(), "NetworkManager channel is null");
            return networkDispatcher.getNetworkManager().channel();
        }
        
        return channel;
    }
    
    public static IMixinNetworkDispatcher getNetworkDispatcher(Channel channel) {
        return Toolbox.cast(channel.attr(NetworkDispatcher.FML_DISPATCHER).get(), IMixinNetworkDispatcher.class);
    }
    
    public static Attribute<Boolean> getDoneClient(Channel channel) {
        return getRootChannel(channel).attr(DONE_CLIENT);
    }
    
    public static Attribute<Boolean> getDoneServer(Channel channel) {
        return getRootChannel(channel).attr(DONE_SERVER);
    }
    
    public static Attribute<Long> getKey(Channel channel) {
        return getRootChannel(channel).attr(KEY);
    }
    
    public static Attribute<Boolean> getRequested(Channel channel) {
        return getRootChannel(channel).attr(REQUESTED);
    }
    
    public static Attribute<State> getState(Channel channel) {
        Preconditions.checkState(StringUtils.equals(channel.attr(NetworkRegistry.FML_CHANNEL).get(), "FML|HS"), "Invalid Channel");
        return channel.attr(STATE);
    }
    
    public static Attribute<Boolean> getValid(Channel channel) {
        return getRootChannel(channel).attr(VALID);
    }
    
    public static Set<NetworkManager> getConnections() {
        return CONNECTIONS;
    }
}