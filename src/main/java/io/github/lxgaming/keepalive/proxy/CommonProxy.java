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

package io.github.lxgaming.keepalive.proxy;

import io.github.lxgaming.keepalive.KeepAlive;
import io.github.lxgaming.keepalive.manager.ServiceManager;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public abstract class CommonProxy {
    
    public void onConstruction(FMLConstructionEvent event) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("Shutdown Thread");
            KeepAlive.getInstance().getLogger().info("Shutting down...");
            
            ServiceManager.shutdown();
        }));
    }
    
    public void onPreInitialization(FMLPreInitializationEvent event) {
    }
    
    public void onInitialization(FMLInitializationEvent event) {
    }
    
    public void onPostInitialization(FMLPostInitializationEvent event) {
    }
}