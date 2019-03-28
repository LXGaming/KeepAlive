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

package io.github.lxgaming.keepalive.mixin.forge.fml.common.network.handshake;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.handler.AbstractHandler;
import io.github.lxgaming.keepalive.handler.handshake.ClientHandshakeHandler;
import io.github.lxgaming.keepalive.handler.handshake.ServerHandshakeHandler;
import io.github.lxgaming.keepalive.handler.packet.ClientPacketHandler;
import io.github.lxgaming.keepalive.handler.packet.ServerPacketHandler;
import io.github.lxgaming.keepalive.interfaces.fml.common.network.handshake.IMixinNetworkDispatcher;
import io.github.lxgaming.keepalive.manager.ConnectionManager;
import io.github.lxgaming.keepalive.util.Reference;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraftforge.fml.common.network.handshake.HandshakeMessageHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = NetworkDispatcher.class, priority = 1337, remap = false)
@Implements(@Interface(iface = IMixinNetworkDispatcher.class, prefix = "keepalive$"))
public abstract class MixinNetworkDispatcher {
    
    @Shadow
    @Final
    public NetworkManager manager;
    
    @Shadow
    protected abstract void shadow$setModList(Map<String, String> modList);
    
    @Shadow
    abstract int shadow$serverInitiateHandshake();
    
    @Shadow
    abstract void shadow$clientListenForServerHandshake();
    
    @Shadow
    public abstract void shadow$rejectHandshake(String result);
    
    @Shadow
    public abstract void shadow$completeHandshake(Side target);
    
    @Shadow
    public abstract void shadow$completeClientHandshake();
    
    @Shadow
    public abstract void shadow$abortClientHandshake(String type);
    
    @Shadow
    public abstract void shadow$setOverrideDimension(int overrideDim);
    
    @Shadow
    public abstract NetworkDispatcher.ConnectionType shadow$getConnectionType();
    
    @Redirect(method = "<init>(Lnet/minecraft/network/NetworkManager;)V",
            at = @At(value = "NEW",
                    args = "class=io/netty/channel/embedded/EmbeddedChannel"
            )
    )
    private EmbeddedChannel createClientEmbeddedChannel(ChannelHandler... handlers) {
        KeepAlive.getInstance().debugMessage("Injecting CLIENT handlers");
        injectHandlers(handlers, new ClientHandshakeHandler(), new ClientPacketHandler());
        return new EmbeddedChannel(handlers);
    }
    
    @Redirect(method = "<init>(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/server/management/PlayerList;)V",
            at = @At(value = "NEW",
                    args = "class=io/netty/channel/embedded/EmbeddedChannel"
            )
    )
    private EmbeddedChannel createServerEmbeddedChannel(ChannelHandler... handlers) {
        KeepAlive.getInstance().debugMessage("Injecting SERVER handlers");
        injectHandlers(handlers, new ServerHandshakeHandler(), new ServerPacketHandler());
        ConnectionManager.addConnection(keepalive$getNetworkManager());
        return new EmbeddedChannel(handlers);
    }
    
    @Inject(method = "handleVanilla", at = @At(value = "HEAD"), cancellable = true)
    private void onHandleVanilla(Packet packet, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (packet instanceof SPacketKeepAlive) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }
    
    private void injectHandlers(ChannelHandler[] handlers, AbstractHandler handshakeHandler, AbstractHandler packetHandler) {
        for (int index = 0; index < handlers.length; index++) {
            if (handlers[index] instanceof HandshakeMessageHandler) {
                handlers[index] = handshakeHandler;
                break;
            }
        }
        
        keepalive$getNetworkManager().channel().pipeline().addBefore("packet_handler", Reference.ID + ":" + packetHandler.getClass().getSimpleName(), packetHandler);
    }
    
    @Intrinsic(displace = true)
    public void keepalive$setModList(Map<String, String> modList) {
        shadow$setModList(modList);
    }
    
    @Intrinsic(displace = true)
    public int keepalive$serverInitiateHandshake() {
        return shadow$serverInitiateHandshake();
    }
    
    @Intrinsic(displace = true)
    public void keepalive$clientListenForServerHandshake() {
        shadow$clientListenForServerHandshake();
    }
    
    @Intrinsic
    public void keepalive$rejectHandshake(String result) {
        shadow$rejectHandshake(result);
    }
    
    @Intrinsic
    public void keepalive$completeHandshake(Side target) {
        shadow$completeHandshake(target);
    }
    
    @Intrinsic
    public void keepalive$completeClientHandshake() {
        shadow$completeClientHandshake();
    }
    
    @Intrinsic
    public void keepalive$abortClientHandshake(String type) {
        shadow$abortClientHandshake(type);
    }
    
    @Intrinsic
    public void keepalive$setOverrideDimension(int overrideDim) {
        shadow$setOverrideDimension(overrideDim);
    }
    
    @Intrinsic
    public NetworkDispatcher.ConnectionType keepalive$getConnectionType() {
        return shadow$getConnectionType();
    }
    
    public NetworkManager keepalive$getNetworkManager() {
        return manager;
    }
}