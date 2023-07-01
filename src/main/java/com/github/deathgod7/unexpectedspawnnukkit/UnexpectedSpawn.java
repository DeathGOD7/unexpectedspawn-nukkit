package com.github.deathgod7.unexpectedspawnnukkit;

import cn.nukkit.command.Command;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.utils.Config;
import org.jline.utils.Log;

import java.io.File;
import java.util.LinkedHashMap;

public final class UnexpectedSpawn extends PluginBase {
    private static UnexpectedSpawn _instance;
    public static UnexpectedSpawn getInstance() { return _instance; }

    //Config reading and writing
    ConfigAccessor configAccessor;
    public ConfigAccessor config() { return configAccessor; }

    @Override
    public void onEnable() {
        _instance = this;
        configAccessor = new ConfigAccessor(this, "config.yml");

        //this.getServer().getCommandMap().register("unexpectedspawn", new Reload(this));
        getServer().getPluginManager().registerEvents(new Spawn(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
