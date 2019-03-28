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

package io.github.lxgaming.keepalive.util;

import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;

public enum State {
    
    // Common
    START(0, "Start"),
    HELLO(1, "Hello"),
    
    // Client
    CLIENT_WAITINGSERVERDATA(2, "Waiting Server Data", Side.CLIENT),
    CLIENT_WAITINGSERVERCOMPLETE(3, "Waiting Server Complete", Side.CLIENT),
    CLIENT_PENDINGCOMPLETE(4, "Pending Complete", Side.CLIENT),
    CLIENT_COMPLETE(5, "Complete", Side.CLIENT),
    CLIENT_DONE(6, "Done", Side.CLIENT),
    CLIENT_ERROR(7, "Error", Side.CLIENT),
    
    // Server
    SERVER_WAITINGCACK(2, "Waiting ACK", Side.SERVER),
    SERVER_COMPLETE(3, "Complete", Side.SERVER),
    SERVER_DONE(4, "Done", Side.SERVER),
    SERVER_ERROR(5, "Error", Side.SERVER);
    
    private final int id;
    private final String name;
    private final Side side;
    
    State(int id, String name) {
        this(id, name, null);
    }
    
    State(int id, String name, Side side) {
        this.id = id;
        this.name = name;
        this.side = side;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Side getSide() {
        return side;
    }
    
    @Override
    public String toString() {
        if (getSide() == null) {
            return getName();
        }
        
        return String.format("%s %s", StringUtils.capitalize(getSide().name().toLowerCase()), getName());
    }
    
    public static State getState(int id, Side side) {
        for (State state : State.values()) {
            if (state.getId() == id && (state.getSide() == null || state.getSide() == side)) {
                return state;
            }
        }
        
        return null;
    }
}