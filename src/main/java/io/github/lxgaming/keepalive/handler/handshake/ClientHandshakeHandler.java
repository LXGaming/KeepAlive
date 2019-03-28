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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.handler.AbstractHandler;
import io.github.lxgaming.keepalive.interfaces.fml.common.network.handshake.IMixinNetworkDispatcher;
import io.github.lxgaming.keepalive.manager.ConnectionManager;
import io.github.lxgaming.keepalive.util.Reference;
import io.github.lxgaming.keepalive.util.State;
import io.github.lxgaming.keepalive.util.Toolbox;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.FMLMessage;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.Future;

@ChannelHandler.Sharable
public class ClientHandshakeHandler extends AbstractHandler {
    
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
        if (msg instanceof FMLHandshakeMessage.HandshakeReset) {
            Preconditions.checkState(state.get() == State.CLIENT_DONE, String.format("Invalid State: %s", state.get()));
            
            state.set(State.HELLO);
            Toolbox.getMinecraft().scheduledTask(GameData::revertToFrozen);
            return;
        }
        
        if (state.get() == State.HELLO) {
            FMLHandshakeMessage.ServerHello packet = (FMLHandshakeMessage.ServerHello) msg;
            ctx.writeAndFlush(FMLHandshakeMessage.makeCustomChannelRegistration(NetworkRegistry.INSTANCE.channelNamesFor(Side.CLIENT)));
            
            // Vanilla
            // Obsolete code?
            if (packet == null) {
                state.set(State.CLIENT_DONE);
                ConnectionManager.getNetworkDispatcher(ctx.channel()).abortClientHandshake("VANILLA");
                return;
            }
            
            state.set(State.CLIENT_WAITINGSERVERDATA);
            KeepAlive.getInstance().getLogger().info("Server protocol version {}", Integer.toHexString(packet.protocolVersion()));
            if (packet.protocolVersion() > 1) {
                ConnectionManager.getNetworkDispatcher(ctx.channel()).setOverrideDimension(packet.overrideDim());
            }
            
            ctx.writeAndFlush(new FMLHandshakeMessage.ClientHello()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            ctx.writeAndFlush(new FMLHandshakeMessage.ModList(Loader.instance().getActiveModList())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            return;
        }
        
        if (msg instanceof FMLHandshakeMessage.ModList) {
            Preconditions.checkState(state.get() == State.CLIENT_WAITINGSERVERDATA, String.format("Invalid State: %s", state.get()));
            
            FMLHandshakeMessage.ModList packet = (FMLHandshakeMessage.ModList) msg;
            String modRejections = FMLNetworkHandler.checkModList(packet, Side.SERVER);
            if (StringUtils.isNotBlank(modRejections)) {
                state.set(State.CLIENT_ERROR);
                ConnectionManager.getNetworkDispatcher(ctx.channel()).rejectHandshake(modRejections);
                return;
            }
            
            if (!ctx.channel().attr(NetworkDispatcher.IS_LOCAL).get()) {
                state.set(State.CLIENT_WAITINGSERVERCOMPLETE);
                
                String version = packet.modList().get(Reference.ID);
                if (StringUtils.isNotBlank(version)) {
                    ConnectionManager.getValid(ctx.channel()).set(true);
                    KeepAlive.getInstance().getLogger().info("Server has {} v{}", Reference.NAME, version);
                } else {
                    ConnectionManager.getValid(ctx.channel()).set(false);
                    KeepAlive.getInstance().getLogger().warn("Server missing {}", Reference.NAME);
                }
            } else {
                state.set(State.CLIENT_PENDINGCOMPLETE);
            }
            
            Toolbox.writeState(ctx.channel(), State.CLIENT_WAITINGSERVERDATA);
            return;
        }
        
        if (msg instanceof FMLHandshakeMessage.RegistryData) {
            Preconditions.checkState(state.get() == State.CLIENT_WAITINGSERVERCOMPLETE, String.format("Invalid State: %s", state.get()));
            
            FMLHandshakeMessage.RegistryData packet = (FMLHandshakeMessage.RegistryData) msg;
            Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot = ObjectUtils.defaultIfNull(ctx.channel().attr(NetworkDispatcher.FML_GAMEDATA_SNAPSHOT).get(), Maps.newHashMap());
            ctx.channel().attr(NetworkDispatcher.FML_GAMEDATA_SNAPSHOT).compareAndSet(null, snapshot);
            
            ForgeRegistry.Snapshot entry = new ForgeRegistry.Snapshot();
            entry.ids.putAll(packet.getIdMap());
            entry.dummied.addAll(packet.getDummied());
            entry.overrides.putAll(packet.getOverrides());
            snapshot.put(packet.getName(), entry);
            
            if (packet.hasMore()) {
                KeepAlive.getInstance().getLogger().debug("Received Mod Registry mapping for {}: {} IDs {} overrides {} dummied", packet.getName(), entry.ids.size(), entry.overrides.size(), entry.dummied.size());
                return;
            }
            
            ctx.channel().attr(NetworkDispatcher.FML_GAMEDATA_SNAPSHOT).set(null);
            
            // https://github.com/MinecraftForge/MinecraftForge/issues/4901
            // Fuck BungeeCord and Forge
            // We're going to process the RegistryData on the Client Thread and continue the handshake on the Netty Thread
            Future<?> future = Toolbox.getMinecraft().scheduledTask(() -> {
                Multimap<ResourceLocation, ResourceLocation> missing = GameData.injectSnapshot(snapshot, false, false);
                if (!missing.isEmpty()) {
                    state.set(State.CLIENT_ERROR);
                    ConnectionManager.getNetworkDispatcher(ctx.channel()).rejectHandshake("Fatally missing registry entries");
                    KeepAlive.getInstance().getLogger().fatal("Failed to connect to server: there are {} missing registry items", missing.size());
                    missing.asMap().forEach((key, value) -> KeepAlive.getInstance().getLogger().debug("Missing {} Entries: {}", key, value));
                    return;
                }
            });
            
            if (ConnectionManager.getValid(ctx.channel()).get() != Boolean.TRUE) {
                try {
                    future.get();
                    state.set(State.CLIENT_PENDINGCOMPLETE);
                    Toolbox.writeState(ctx.channel(), State.CLIENT_WAITINGSERVERCOMPLETE);
                } catch (Exception ex) {
                    exceptionCaught(ctx, ex);
                }
                
                return;
            }
            
            state.set(State.CLIENT_PENDINGCOMPLETE);
            Toolbox.writeState(ctx.channel(), State.CLIENT_WAITINGSERVERCOMPLETE);
            return;
        }
        
        if (msg instanceof FMLHandshakeMessage.HandshakeAck) {
            FMLHandshakeMessage.HandshakeAck packet = (FMLHandshakeMessage.HandshakeAck) msg;
            State serverState = Toolbox.readState(packet, Side.SERVER);
            
            if (serverState == State.SERVER_WAITINGCACK) {
                Preconditions.checkState(state.get() == State.CLIENT_PENDINGCOMPLETE, String.format("Invalid State: %s", state.get()));
                
                state.set(State.CLIENT_COMPLETE);
                Toolbox.writeState(ctx.channel(), State.CLIENT_PENDINGCOMPLETE);
                return;
            }
            
            if (serverState == State.SERVER_COMPLETE) {
                Preconditions.checkState(state.get() == State.CLIENT_COMPLETE, String.format("Invalid State: %s", state.get()));
                
                state.set(State.CLIENT_DONE);
                if (ConnectionManager.getValid(ctx.channel()).get() != Boolean.TRUE) {
                    ConnectionManager.getNetworkDispatcher(ctx.channel()).completeClientHandshake();
                    ctx.fireChannelRead(new FMLMessage.CompleteHandshake(Side.CLIENT));
                    Toolbox.writeState(ctx.channel(), State.CLIENT_COMPLETE);
                    return;
                }
                
                Toolbox.getMinecraft().scheduledTask(() -> {
                    IMixinNetworkDispatcher networkDispatcher = ConnectionManager.getNetworkDispatcher(ctx.channel());
                    networkDispatcher.completeClientHandshake();
                    networkDispatcher.completeHandshake(Side.CLIENT);
                    
                    PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer());
                    packetBuffer.writeByte(0); // Complete
                    networkDispatcher.getNetworkManager().sendPacket(new CPacketCustomPayload(Reference.ID, packetBuffer));
                    ConnectionManager.getDoneClient(networkDispatcher.getNetworkManager().channel()).set(true);
                });
                
                Toolbox.writeState(ctx.channel(), State.CLIENT_COMPLETE);
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
        ConnectionManager.getNetworkDispatcher(ctx.channel()).clientListenForServerHandshake();
    }
}