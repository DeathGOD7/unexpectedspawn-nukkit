package com.github.deathgod7.unexpectedspawnnukkit;

import cn.nukkit.plugin.PluginLogger;

public class LogConsole {

//    static String logPrefix = "[UnexpectedSpawn] ";

    static PluginLogger logger = UnexpectedSpawn.getInstance().getLogger();
    static String logPrefix = "";
    enum logTypes {
        log,
        debug
    }
    public static Boolean debugMode = UnexpectedSpawn.getInstance().getConfig().getBoolean("debug");

    public static void severe(String msg, logTypes logType) {
        if (logType == logTypes.debug) {
            if (debugMode) { logger.error(logPrefix + msg); }
        }
        else if (logType == logTypes.log) {
            logger.error(logPrefix + msg);
        }
    }

    public static void warn(String msg, logTypes logType) {
        if (logType == logTypes.debug) {
            if (debugMode) { logger.warning(logPrefix + msg);  }
        }
        else if (logType == logTypes.log) {
            logger.warning(logPrefix + msg);
        }
    }

    public static void info(String msg, logTypes logType) {
        if (logType == logTypes.debug) {
            if (debugMode) { logger.info(logPrefix + msg); }
        }
        else if (logType == logTypes.log) {
            logger.info(logPrefix + msg);
        }
    }

}
