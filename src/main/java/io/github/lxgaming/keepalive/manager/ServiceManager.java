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

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.service.AbstractService;
import io.github.lxgaming.keepalive.util.Toolbox;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServiceManager {
    
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Toolbox.newScheduledThreadPool(1, 1, 60000L, TimeUnit.MILLISECONDS);
    
    public static void schedule(AbstractService abstractService) {
        try {
            if (!abstractService.prepareService()) {
                throw new IllegalStateException("Service preparation failed");
            }
            
            schedule(abstractService, abstractService.getDelay(), abstractService.getInterval()).ifPresent(abstractService::setScheduledFuture);
        } catch (RuntimeException ex) {
            KeepAlive.getInstance().getLogger().error("Encountered an error processing {}::schedule", "ServiceManager", ex);
        }
    }
    
    public static Optional<ScheduledFuture> schedule(Runnable runnable, long delay, long interval) {
        try {
            if (interval <= 0L) {
                return Optional.of(getScheduledExecutorService().schedule(runnable, Math.max(delay, 0L), TimeUnit.MILLISECONDS));
            }
            
            return Optional.of(getScheduledExecutorService().scheduleWithFixedDelay(runnable, Math.max(delay, 0L), Math.max(interval, 0L), TimeUnit.MILLISECONDS));
        } catch (RuntimeException ex) {
            KeepAlive.getInstance().getLogger().error("Encountered an error processing {}::schedule", "ServiceManager", ex);
            return Optional.empty();
        }
    }
    
    public static void shutdown() {
        try {
            getScheduledExecutorService().shutdown();
            if (!getScheduledExecutorService().awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                throw new InterruptedException();
            }
            
            KeepAlive.getInstance().getLogger().info("Successfully terminated threads, continuing with shutdown process...");
        } catch (InterruptedException | RuntimeException ex) {
            KeepAlive.getInstance().getLogger().error("Failed to terminate threads, continuing with shutdown process...");
        }
    }
    
    private static ScheduledExecutorService getScheduledExecutorService() {
        return SCHEDULED_EXECUTOR_SERVICE;
    }
}