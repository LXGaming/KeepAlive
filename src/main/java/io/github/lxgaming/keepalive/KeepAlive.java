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

import io.github.lxgaming.keepalive.listener.KeepAliveListener;
import io.github.lxgaming.keepalive.listener.NetworkListener;
import io.github.lxgaming.keepalive.service.KeepAliveService;
import io.github.lxgaming.keepalive.util.Reference;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Reference.ID,
        name = Reference.NAME,
        version = Reference.VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = Reference.ACCEPTED_VERSIONS,
        acceptableRemoteVersions = Reference.ACCEPTABLE_REMOTE_VERSIONS,
        certificateFingerprint = Reference.CERTIFICATE_FINGERPRINT
)
public class KeepAlive {
    
    @Mod.Instance
    private static KeepAlive instance;
    
    private final Logger logger = LogManager.getLogger(Reference.NAME);
    private final KeepAliveService service = new KeepAliveService();
    
    @Mod.EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
        throw new SecurityException("Certificate Fingerprint Violation Detected!");
    }
    
    @Mod.EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) {
    }
    
    @Mod.EventHandler
    public void onInitialization(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeepAliveListener());
        MinecraftForge.EVENT_BUS.register(new NetworkListener());
    }
    
    @Mod.EventHandler
    public void onPostInitialization(FMLPostInitializationEvent event) {
    }
    
    public static KeepAlive getInstance() {
        return instance;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public KeepAliveService getService() {
        return service;
    }
}