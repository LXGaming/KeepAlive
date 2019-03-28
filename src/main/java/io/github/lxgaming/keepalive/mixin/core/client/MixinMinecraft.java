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

package io.github.lxgaming.keepalive.mixin.core.client;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import io.github.lxgaming.keepalive.interfaces.client.IMixinMinecraft;
import io.github.lxgaming.keepalive.util.Reference;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Mixin(value = Minecraft.class, priority = 1337)
@Implements(@Interface(iface = IMixinMinecraft.class, prefix = "keepalive$"))
public abstract class MixinMinecraft {
    
    @Shadow
    @Final
    private static Logger LOGGER;
    
    @Shadow
    @Final
    @Mutable
    private Queue<FutureTask<?>> scheduledTasks = new ConcurrentLinkedDeque<>();
    
    @Shadow
    public abstract boolean isCallingFromMinecraftThread();
    
    @Inject(method = "init", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo callbackInfo) {
        LOGGER.info("{} v{} was successfully applied!", Reference.NAME, Reference.VERSION);
    }
    
    /**
     * The addScheduledTask method will block if the Client Thread is iterating over the scheduled tasks
     */
    @SuppressWarnings("UnstableApiUsage")
    public <V> ListenableFuture<V> keepalive$scheduledTask(Callable<V> callable) {
        Validate.notNull(callable);
        if (isCallingFromMinecraftThread()) {
            try {
                return Futures.immediateFuture(callable.call());
            } catch (Exception ex) {
                return Futures.immediateFailedCheckedFuture(ex);
            }
        } else {
            ListenableFutureTask<V> listenableFutureTask = ListenableFutureTask.create(callable);
            scheduledTasks.add(listenableFutureTask);
            return listenableFutureTask;
        }
    }
    
    public ListenableFuture<Object> keepalive$scheduledTask(Runnable runnable) {
        Validate.notNull(runnable);
        return keepalive$scheduledTask(Executors.callable(runnable));
    }
}