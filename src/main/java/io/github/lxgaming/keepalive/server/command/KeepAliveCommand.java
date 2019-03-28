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

package io.github.lxgaming.keepalive.server.command;

import io.github.lxgaming.keepalive.util.Reference;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.command.CommandTreeBase;

public class KeepAliveCommand extends CommandTreeBase {
    
    public KeepAliveCommand() {
        addSubcommand(new DebugCommand());
    }
    
    @Override
    public String getName() {
        return Reference.ID;
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.keepalive.usage";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}