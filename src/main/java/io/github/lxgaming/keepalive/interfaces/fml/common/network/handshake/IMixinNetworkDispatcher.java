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

package io.github.lxgaming.keepalive.interfaces.fml.common.network.handshake;

import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

public interface IMixinNetworkDispatcher {
    
    void setModList(Map<String, String> modList);
    
    int serverInitiateHandshake();
    
    void clientListenForServerHandshake();
    
    void rejectHandshake(String result);
    
    void completeHandshake(Side target);
    
    void completeClientHandshake();
    
    void abortClientHandshake(String type);
    
    void setOverrideDimension(int overrideDim);
    
    NetworkManager getNetworkManager();
    
    NetworkDispatcher.ConnectionType getConnectionType();
}