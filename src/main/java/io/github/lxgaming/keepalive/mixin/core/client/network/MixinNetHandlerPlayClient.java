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

package io.github.lxgaming.keepalive.mixin.core.client.network;

import io.github.lxgaming.keepalive.KeepAlive;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NetHandlerPlayClient.class, priority = 1337)
public abstract class MixinNetHandlerPlayClient {
    
    @Redirect(method = "handleCustomPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setServerBrand(Ljava/lang/String;)V"))
    private void onSetServerBrand(EntityPlayerSP player, String brand) {
        // BungeeCord will send a MC|Brand packet with our fake JoinGame packet
        // We can simply ignore this as the brand will get overwritten when the backend server sends its MC|Brand packet
        if (player == null) {
            KeepAlive.getInstance().getLogger().warn("Ignoring brand: {}", brand);
            return;
        }
        
        player.setServerBrand(brand);
    }
}