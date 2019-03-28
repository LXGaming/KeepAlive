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

import io.github.lxgaming.keepalive.configuration.Configuration;
import io.github.lxgaming.keepalive.util.Toolbox;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class DebugCommand extends CommandBase {
    
    @Override
    public String getName() {
        return "debug";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.keepalive.debug.usage";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (Configuration.debug) {
            Configuration.debug = false;
            sender.sendMessage(Toolbox.getTextPrefix().appendSibling(new TextComponentString("Debugging disabled").setStyle(new Style().setColor(TextFormatting.RED))));
        } else {
            Configuration.debug = true;
            sender.sendMessage(Toolbox.getTextPrefix().appendSibling(new TextComponentString("Debugging enabled").setStyle(new Style().setColor(TextFormatting.GREEN))));
        }
    }
}