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

package io.github.lxgaming.keepalive.handler.handshake;

import com.google.common.base.Preconditions;
import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.handler.AbstractHandler;
import io.github.lxgaming.keepalive.manager.ConnectionManager;
import io.github.lxgaming.keepalive.util.Reference;
import io.github.lxgaming.keepalive.util.State;
import io.github.lxgaming.keepalive.util.Toolbox;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.FMLMessage;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;

@ChannelHandler.Sharable
public class ServerHandshakeHandler extends AbstractHandler {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().attr(NetworkRegistry.FML_CHANNEL).setIfAbsent("FML|HS");
        Attribute<State> state = ConnectionManager.getState(ctx.channel());
        state.set(State.START);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof FMLHandshakeMessage)) {
            super.channelRead(ctx, msg);
            return;
        }
        
        Attribute<State> state = ConnectionManager.getState(ctx.channel());
        if (msg instanceof FMLHandshakeMessage.ClientHello) {
            Preconditions.checkState(state.get() == State.HELLO, String.format("Invalid State: %s", state.get()));
            
            FMLHandshakeMessage.ClientHello packet = (FMLHandshakeMessage.ClientHello) msg;
            KeepAlive.getInstance().getLogger().info("Client protocol version {}", packet.protocolVersion());
            return;
        }
        
        if (msg instanceof FMLHandshakeMessage.ModList) {
            Preconditions.checkState(state.get() == State.HELLO, String.format("Invalid State: %s", state.get()));
            
            FMLHandshakeMessage.ModList packet = (FMLHandshakeMessage.ModList) msg;
            ConnectionManager.getNetworkDispatcher(ctx.channel()).setModList(packet.modList());
            
            KeepAlive.getInstance().getLogger().info("Client attempting to join with {} mods : {}", packet.modListSize(), packet.modListAsString());
            
            String modRejections = FMLNetworkHandler.checkModList(packet.modList(), Side.CLIENT);
            if (StringUtils.isNotBlank(modRejections)) {
                state.set(State.SERVER_ERROR);
                ConnectionManager.getNetworkDispatcher(ctx.channel()).rejectHandshake(modRejections);
                return;
            }
            
            state.set(State.SERVER_WAITINGCACK);
            
            if (!ctx.channel().attr(NetworkDispatcher.IS_LOCAL).get()) {
                String version = packet.modList().get(Reference.ID);
                if (StringUtils.isNotBlank(version)) {
                    ConnectionManager.getValid(ctx.channel()).set(true);
                    KeepAlive.getInstance().getLogger().info("Client has {} v{}", Reference.NAME, version);
                } else {
                    ConnectionManager.getValid(ctx.channel()).set(false);
                    KeepAlive.getInstance().getLogger().warn("Client missing {}", Reference.NAME);
                }
            }
            
            ctx.writeAndFlush(new FMLHandshakeMessage.ModList(Loader.instance().getActiveModList()));
            return;
        }
        
        if (msg instanceof FMLHandshakeMessage.HandshakeAck) {
            FMLHandshakeMessage.HandshakeAck packet = (FMLHandshakeMessage.HandshakeAck) msg;
            State clientState = Toolbox.readState(packet, Side.CLIENT);
            
            if (clientState == State.CLIENT_WAITINGSERVERDATA) {
                Preconditions.checkState(state.get() == State.SERVER_WAITINGCACK, String.format("Invalid State: %s", state.get()));
                
                state.set(State.SERVER_COMPLETE);
                if (!ctx.channel().attr(NetworkDispatcher.IS_LOCAL).get()) {
                    Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot = RegistryManager.ACTIVE.takeSnapshot(false);
                    for (Iterator<Map.Entry<ResourceLocation, ForgeRegistry.Snapshot>> iterator = snapshot.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<ResourceLocation, ForgeRegistry.Snapshot> entry = iterator.next();
                        ctx.writeAndFlush(new FMLHandshakeMessage.RegistryData(iterator.hasNext(), entry.getKey(), entry.getValue())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                }
                
                Toolbox.writeState(ctx.channel(), State.SERVER_WAITINGCACK);
                NetworkRegistry.INSTANCE.fireNetworkHandshake((NetworkDispatcher) ConnectionManager.getNetworkDispatcher(ctx.channel()), Side.SERVER);
                return;
            }
            
            if (clientState == State.CLIENT_WAITINGSERVERCOMPLETE) {
                Preconditions.checkState(state.get() == State.SERVER_COMPLETE, String.format("Invalid State: %s", state.get()));
                
                state.set(State.SERVER_DONE);
                Toolbox.writeState(ctx.channel(), State.SERVER_COMPLETE);
                
                // Don't complete the handshake yet
                // ctx.fireChannelRead(new FMLMessage.CompleteHandshake(Side.SERVER));
                return;
            }
            
            if (clientState == State.CLIENT_PENDINGCOMPLETE) {
                if (state.get() == State.SERVER_COMPLETE) {
                    state.set(State.SERVER_DONE);
                    Toolbox.writeState(ctx.channel(), State.SERVER_COMPLETE);
                    return;
                }
                
                if (state.get() == State.SERVER_DONE) {
                    return;
                }
                
                throw new IllegalStateException(String.format("Current State: %s", state.get()));
            }
            
            if (clientState == State.CLIENT_COMPLETE) {
                Preconditions.checkState(state.get() == State.SERVER_DONE, String.format("Invalid State: %s", state.get()));
                if (ConnectionManager.getValid(ctx.channel()).get() != Boolean.TRUE) {
                    ctx.fireChannelRead(new FMLMessage.CompleteHandshake(Side.SERVER));
                    return;
                }
                
                NetworkManager networkManager = ConnectionManager.getNetworkDispatcher(ctx.channel()).getNetworkManager();
                EntityPlayerMP player = ConnectionManager.getPlayer(networkManager);
                Preconditions.checkState(player != null, "Player is null");
                
                // https://github.com/SpigotMC/BungeeCord/blob/771f1735e5460909175d4a6e9713cdbe60ad8eec/proxy/src/main/java/net/md_5/bungee/ServerConnector.java#L284
                // Causes BungeeCord to initialize the DownstreamBridge handler
                // This allows us to send and receive packets outside of FMLHandshakeMessage
                networkManager.sendPacket(new SPacketJoinGame(
                        player.getEntityId(),
                        GameType.SURVIVAL,
                        false,
                        0,
                        EnumDifficulty.PEACEFUL,
                        FMLServerHandler.instance().getServer().getPlayerList().getMaxPlayers(),
                        WorldType.DEFAULT,
                        false
                ));
                
                ConnectionManager.getDoneServer(networkManager.channel()).set(true);
                return;
            }
        }
        
        throw new UnsupportedOperationException(String.format("Current State: %s", state.get()));
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        Attribute<State> state = ConnectionManager.getState(ctx.channel());
        Preconditions.checkState(state.get() == State.START, String.format("Invalid State: %s", state.get()));
        
        state.set(State.HELLO);
        int overrideDim = ConnectionManager.getNetworkDispatcher(ctx.channel()).serverInitiateHandshake();
        ctx.writeAndFlush(FMLHandshakeMessage.makeCustomChannelRegistration(NetworkRegistry.INSTANCE.channelNamesFor(Side.SERVER))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        ctx.writeAndFlush(new FMLHandshakeMessage.ServerHello(overrideDim)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}