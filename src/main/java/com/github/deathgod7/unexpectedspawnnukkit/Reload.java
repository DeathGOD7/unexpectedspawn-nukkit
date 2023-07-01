package com.github.deathgod7.unexpectedspawnnukkit;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;

public class Reload implements CommandExecutor {

    private final UnexpectedSpawn plugin;

    Reload(UnexpectedSpawn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("unexpectedspawn")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("unexpectedspawn.use")) {
                    plugin.configAccessor.reloadConfig();
                    //plugin.reloadConfig();
                    sender.sendMessage(Util.colorize("&8UnexpectedSpawn reloaded!"));
                    return true;
                }
            }

            String authors = String.join(", ", plugin.getDescription().getAuthors());
            String version = plugin.getDescription().getVersion();
            sender.sendMessage(Util.colorize("UnexpectedSpawn Version : &8" + version));
            sender.sendMessage(Util.colorize("Authors: &8" + authors));

            // By returning false, server will send out available commands.
            return !sender.hasPermission("unexpectedspawn.use");
        }

        return true;
    }

}
