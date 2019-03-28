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

package io.github.lxgaming.keepalive;

import io.github.lxgaming.keepalive.configuration.Configuration;
import io.github.lxgaming.keepalive.manager.ServiceManager;
import io.github.lxgaming.keepalive.proxy.CommonProxy;
import io.github.lxgaming.keepalive.server.command.KeepAliveCommand;
import io.github.lxgaming.keepalive.service.KeepAliveService;
import io.github.lxgaming.keepalive.util.Reference;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Reference.ID,
        name = Reference.NAME,
        version = Reference.VERSION,
        acceptedMinecraftVersions = Reference.ACCEPTED_VERSIONS,
        acceptableRemoteVersions = Reference.ACCEPTABLE_REMOTE_VERSIONS,
        certificateFingerprint = Reference.CERTIFICATE_FINGERPRINT
)
public class KeepAlive {
    
    @Mod.Instance
    private static KeepAlive instance;
    
    @SidedProxy(
            clientSide = Reference.CLIENT_PROXY_CLASS,
            serverSide = Reference.SERVER_PROXY_CLASS
    )
    private static CommonProxy proxy;
    
    private final Logger logger = LogManager.getLogger(Reference.NAME);
    
    @Mod.EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
        throw new SecurityException("Certificate Fingerprint Violation Detected!");
    }
    
    @Mod.EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        getProxy().onConstruction(event);
    }
    
    @Mod.EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) {
        getProxy().onPreInitialization(event);
    }
    
    @Mod.EventHandler
    public void onInitialization(FMLInitializationEvent event) {
        getProxy().onInitialization(event);
    }
    
    @Mod.EventHandler
    public void onPostInitialization(FMLPostInitializationEvent event) {
        getProxy().onPostInitialization(event);
    }
    
    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new KeepAliveCommand());
    }
    
    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        ServiceManager.schedule(new KeepAliveService());
    }
    
    public void debugMessage(String format, Object... arguments) {
        if (Configuration.debug) {
            getLogger().info(format, arguments);
        }
    }
    
    public static KeepAlive getInstance() {
        return instance;
    }
    
    public static CommonProxy getProxy() {
        return proxy;
    }
    
    public Logger getLogger() {
        return logger;
    }
}