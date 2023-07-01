package com.github.deathgod7.unexpectedspawnnukkit;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.LogLevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigAccessor {

    private final String fileName;
    private final UnexpectedSpawn plugin;

    private final File configFile;

    private final Config fileConfiguration;

    public ConfigAccessor(UnexpectedSpawn plugin, String fileName) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        this.fileName = fileName;
        File dataFolder = plugin.getDataFolder();
        if (dataFolder == null) {
            throw new IllegalStateException();
        }
        this.saveDefaultConfig();
        this.configFile = new File(this.plugin.getDataFolder() + "/config.yml");
        this.fileConfiguration = new Config(
                this.plugin.getDataFolder() + "/config.yml",
                Config.YAML
        );

    }

    public void reloadConfig() {
        this.fileConfiguration.load(
                this.plugin.getDataFolder() + "/config.yml",
                Config.YAML
        );
    }

    public Config getConfig() {
        if (fileConfiguration == null) {
            this.reloadConfig();
        }
        return fileConfiguration;
    }

    public void saveConfig() {
        if (fileConfiguration != null) {
            if (fileConfiguration.save(configFile)) {
                plugin.getLogger().info("Saved config to " + configFile);
            }
            else  {
                plugin.getLogger().error("Could not save config to " + configFile);
            }
        }
    }

    public void saveDefaultConfig() {
        this.plugin.getLogger().info("Loading config files!!");
        this.plugin.saveDefaultConfig();
        this.plugin.getLogger().info("Loaded config files!!");
    }

}
