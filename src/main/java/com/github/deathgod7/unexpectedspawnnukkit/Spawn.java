package com.github.deathgod7.unexpectedspawnnukkit;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.level.Position;
import cn.nukkit.potion.Effect;
import org.jline.utils.Log;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Spawn implements Listener {

    private final UnexpectedSpawn plugin;
    private final HashSet<Integer> blacklistedMaterial = new HashSet<>();
    private final HashSet<Level> blacklistedWorlds = new HashSet<>();

    Spawn(UnexpectedSpawn plugin) {
        this.plugin = plugin;

        List<String> worldList = plugin.getConfig().getStringList("blacklisted-worlds");
        for (String name : worldList) {
            Level world = Server.getInstance().getLevelByName(name);
            if (world == null) {
                LogConsole.warn("Couldn't find world " + name + ". Either it doesn't exist or is not valid.", LogConsole.logTypes.log);
                continue;
            }
            blacklistedWorlds.add(world);
        }
    }

    Level deathWorld;
    Player deadPlayer;
    Location deathLocation;

    Location newRespawnLocation;


    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        deathWorld = event.getEntity().getLevel();
        deadPlayer = event.getEntity();
        deathLocation = deadPlayer.getLocation();

        int x = (int)deathLocation.getLevelBlock().getX();
        int y = (int)deathLocation.getLevelBlock().getY();
        int z = (int)deathLocation.getLevelBlock().getZ();

        LogConsole.info("Player " + deadPlayer.getName() + " died at (X "
                    + x + ", Y " + y + ", Z " + z +
                    ") at world (" + deathWorld.getName() + ").", LogConsole.logTypes.debug);

        if (deadPlayer != null && deadPlayer.hasPermission("unexpectedspawn.notify")) {
            String msg = String.format("Your death location (&4X %s&r, &2Y %s&r, &1Z %s&r) in world (%s).", x, y, z, deathWorld.getName());
            String out = Util.colorize(msg);
            deadPlayer.sendMessage(out);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Level joinWorld = event.getPlayer().getLevel();

        if (event.getPlayer().hasPermission("unexpectedspawn.bypass")) {
            LogConsole.info("Player " + event.getPlayer().getName()
                + " has (unexpectedspawn.bypass) permission. So skipping random join spawn.", LogConsole.logTypes.debug);
            return;
        }

        if ((joinWorld.getDimension() == Level.DIMENSION_NETHER)
                || (joinWorld.getDimension() == Level.DIMENSION_THE_END)) {
            LogConsole.info("User in NETHER or END. So random join spawn is disabled.", LogConsole.logTypes.debug);
            return;
        }

        if (blacklistedWorlds.contains(joinWorld)) {
            LogConsole.info("User in blacklisted world (" + joinWorld + "). So random join spawn is disabled.", LogConsole.logTypes.debug);
            return;
        }

        String useCustomOnFirstJoin = checkWorldConfig(joinWorld, "random-respawn.on-first-join");
        String useCustomAlwaysOnJoin = checkWorldConfig(joinWorld, "random-respawn.always-on-join");
        
        if ((!event.getPlayer().hasPlayedBefore() && plugin.config().getConfig().getBoolean(useCustomOnFirstJoin + "random-respawn.on-first-join"))
                || plugin.config().getConfig().getBoolean(useCustomAlwaysOnJoin + "random-respawn.always-on-join")) {
            Location joinLocation = getRandomSpawnLocation(joinWorld);
            event.getPlayer().teleport(joinLocation);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Level respawnWorld = event.getRespawnPosition().level;;
        String wName = respawnWorld.getName();

        if (event.getPlayer().hasPermission("unexpectedspawn.bypass")) {
            LogConsole.info("Player " + event.getPlayer().getName()
                    + " has (unexpectedspawn.bypass) permission. So skipping random respawn.", LogConsole.logTypes.debug);
            return;
        }

        if (deathWorld != null) {
            String useCustomWorld = checkWorldConfig(deathWorld, "respawn-world");
            String obtainedData = plugin.config().getConfig().getString(useCustomWorld + "respawn-world");

            if (obtainedData != null) {
                if (obtainedData.isEmpty()) {
                    respawnWorld = deathWorld;
                    wName = respawnWorld.getName();
                    LogConsole.info("Using world (" + wName + ") where player died.", LogConsole.logTypes.debug);
                }

                else {
                    Level obtainedWorld = Server.getInstance().getLevelByName(obtainedData);

                    if (obtainedWorld == null) {
                        LogConsole.warn("Couldn't find world " + obtainedData + ". Either it doesn't exist or is not valid.", LogConsole.logTypes.log);
                        respawnWorld = deathWorld;
                        wName = respawnWorld.getName();
                        LogConsole.info("Using world (" + wName + ") where player died.", LogConsole.logTypes.debug);
                    }

                    else {
                        respawnWorld = obtainedWorld;

                        wName = respawnWorld.getName();
                        LogConsole.info("Using world (" + wName + ") specified in config.", LogConsole.logTypes.debug);
                    }
                }
            } else {
                LogConsole.severe("Respawn World in (" + useCustomWorld + ") cannot be null. Please add empty string to disable it.", LogConsole.logTypes.log);
            }
        }
        else {
            LogConsole.info("Using world (" + wName + ") where player will respawn normally. Probably coming back to OVERWORLD from END.", LogConsole.logTypes.debug);
        }

        if (blacklistedWorlds.contains(respawnWorld)) {
            LogConsole.info("User in blacklisted world ("+ respawnWorld +"). So random respawn is disabled.", LogConsole.logTypes.debug);
            return;
        }

        // tba respawn anchor

        String useCustomOnDeath = checkWorldConfig(respawnWorld, "random-respawn.on-death");
        String useCustomBedRespawn = checkWorldConfig(respawnWorld, "random-respawn.bed-respawn-enabled");


        if(plugin.config().getConfig().getBoolean(useCustomOnDeath + "random-respawn.on-death")) {
            if (!hasBedRespawn(event.getRespawnPosition()) || !plugin.config().getConfig().getBoolean(useCustomBedRespawn + "random-respawn.bed-respawn-enabled")) {
                    respawnNew(respawnWorld, event);
//                newRespawnLocation = getRandomSpawnLocation(respawnWorld);
//                LogConsole.warn("Triggered random respawn!!", LogConsole.logTypes.debug);
//
//                event.setRespawnPosition(newRespawnLocation);
//
//                event.getPlayer().addEffect(Effect.getEffect(15).setAmplifier(5000).setDuration(20).setVisible(false));
//                event.getPlayer().getServer().getScheduler().scheduleDelayedTask(UnexpectedSpawn.getInstance(), () -> {
//                    newRespawnLocation.y =  event.getPlayer().getLevel().getHighestBlockAt((int)newRespawnLocation.x, (int)newRespawnLocation.z) + 2;
//                    event.getPlayer().teleport(new Location(newRespawnLocation.x, newRespawnLocation.y, newRespawnLocation.z));
//                    LogConsole.warn("Modified Location : " + "(X "+newRespawnLocation.x+", Y "+newRespawnLocation.y+", Z "+newRespawnLocation.z+")", LogConsole.logTypes.debug);
//                }, 20);
            }
        }
    }

    private void respawnNew(Level respawnWorld, PlayerRespawnEvent event){
        newRespawnLocation = getRandomSpawnLocation(respawnWorld);
        LogConsole.warn("Triggered random respawn!!", LogConsole.logTypes.debug);

        event.setRespawnPosition(newRespawnLocation);

        event.getPlayer().addEffect(Effect.getEffect(15).setAmplifier(5000).setDuration(20).setVisible(false));
        event.getPlayer().getServer().getScheduler().scheduleDelayedTask(UnexpectedSpawn.getInstance(), () -> {
            newRespawnLocation.y =  event.getPlayer().getLevel().getHighestBlockAt((int)newRespawnLocation.x, (int)newRespawnLocation.z) + 2;
            if (!blacklistedMaterial.contains(newRespawnLocation.getLevelBlock().getId())) {
                event.getPlayer().teleport(new Location(newRespawnLocation.x, newRespawnLocation.y, newRespawnLocation.z));
                LogConsole.warn("Modified Location : " + "(X "+newRespawnLocation.x+", Y "+newRespawnLocation.y+", Z "+newRespawnLocation.z+")", LogConsole.logTypes.debug);
            }
            else {
                LogConsole.warn("Retry Again!!", LogConsole.logTypes.debug);
                respawnNew(respawnWorld, event);
            }
        }, 20);
    }

    public boolean hasBedRespawn(Position respawnPos) {
        // Get the respawn location
        Location respawnLocation = Location.fromObject(respawnPos);

        // Check if the respawn location is a bed
        boolean hasBedRespawn = false;
        int blockId = respawnLocation.getLevelBlock().getId();
        if (blockId == BlockID.BED_BLOCK) {
            hasBedRespawn = true;
        }
        return hasBedRespawn;
    }


    private String checkWorldConfig(Level world, String config) {
        //List<String> worldList = plugin.config.getConfig().getStringList("worlds");
        String worldName = world.getName();
        if (plugin.getConfig().get("worlds." + worldName + "." + config) != null) {
            return ("worlds." + worldName + ".");
        }
        else {
            return ("global.");
        }
    }

    private HashSet<Integer> getBlacklistedMaterials(String prefix) {
        List<String> materialList = plugin.getConfig().getStringList(prefix + "spawn-block-blacklist");
        blacklistedMaterial.clear();
        for (String name : materialList) {
            LogConsole.warn(name, LogConsole.logTypes.debug);
            Block material = Item.fromString(name).getBlock();
            if (material == null) {
                LogConsole.warn("Material " + name + " is not valid. See https://papermc.io/javadocs/paper/org/bukkit/Material.html", LogConsole.logTypes.log);
                continue;
            }
            LogConsole.warn(material.getName(), LogConsole.logTypes.debug);
            blacklistedMaterial.add(material.getId());
        }
        LogConsole.warn("Total Values : " + String.valueOf(blacklistedMaterial.toArray().length), LogConsole.logTypes.debug);
        return blacklistedMaterial;
    }

    public static int AddFailRange(int previous, int rangetoadd) {
        int result = 0;
        int valtype = Integer.signum(previous);

        if (valtype == 0 || valtype == 1){
            result = previous + rangetoadd;
        }
        else {
            result = previous - rangetoadd;
        }
        return result;
    }

    private Location getRandomSpawnLocation(Level world) {
        int tryCount = 0;
        String useCustomMinX = checkWorldConfig(world, "x-min");
        String useCustomMaxX = checkWorldConfig(world, "x-max");
        String useCustomMinZ = checkWorldConfig(world, "z-min");
        String useCustomMaxZ = checkWorldConfig(world, "z-max");
        String useCustomRetryOnFail = checkWorldConfig(world, "fail-radius");

        int xmin = plugin.config().getConfig().getInt(useCustomMinX + "x-min");
        int xmax = plugin.config().getConfig().getInt(useCustomMaxX + "x-max");
        int zmin = plugin.config().getConfig().getInt(useCustomMinZ + "z-min");
        int zmax = plugin.config().getConfig().getInt(useCustomMaxZ + "z-max");
        int retryonfail = plugin.config().getConfig().getInt(useCustomRetryOnFail + "fail-radius");


        LogConsole.info("Used config: " + useCustomMinX + " for random respawn area and the values are ("+xmin+","+xmax+","+zmin+","+zmax+").", LogConsole.logTypes.debug);


        String useCustomBlacklistedMaterials = checkWorldConfig(world, "spawn-block-blacklist");
        HashSet<Integer> worldBlacklistedMaterials = getBlacklistedMaterials(useCustomBlacklistedMaterials);

        String useCustomSpawnBlacklistInverted = checkWorldConfig(world, "invert-block-blacklist");
        boolean isSpawnBlacklistInverted = plugin.config().getConfig().getBoolean(useCustomSpawnBlacklistInverted + "invert-block-blacklist");

        LogConsole.info("Used config: " + useCustomSpawnBlacklistInverted + " and the blacklist invert is " + isSpawnBlacklistInverted + " in " + world.getName(), LogConsole.logTypes.debug);
        LogConsole.info("Used config: " + useCustomBlacklistedMaterials + " and the values are : " + worldBlacklistedMaterials, LogConsole.logTypes.debug);

        while (true) {

            if(tryCount == 5000) {
                xmin = AddFailRange(xmin ,retryonfail);
                xmax = AddFailRange(xmax ,retryonfail);
                zmin = AddFailRange(zmin ,retryonfail);
                zmax = AddFailRange(zmax ,retryonfail);
                LogConsole.warn("Couldn't find suitable location after "+tryCount+" try. Updating range as per fail-radius.", LogConsole.logTypes.log);
                LogConsole.info("Used config: " + useCustomRetryOnFail + " for retry fail radius ("+retryonfail+") so the current values are ("+xmin+","+xmax+","+zmin+","+zmax+").", LogConsole.logTypes.debug);
            }
            else if (tryCount >= 10000) {
                LogConsole.warn("Couldn't find suitable location for random respawn after "+tryCount+" so respawning at world spawn point.", LogConsole.logTypes.log);
                Location location = world.getSpawnLocation().getLocation();
                //Location location = new Location(world,0,world.getHighestBlockYAt(0, 0),0);
                return location.add(0.5d, 1d, 0.5d);
            }

            int x = xmin + ThreadLocalRandom.current().nextInt((xmax - xmin) + 1);
            int z = zmin + ThreadLocalRandom.current().nextInt((zmax - zmin) + 1);

            //generate chunk
            world.generateChunk(x, z, true);

            // then get highest y
            int y = world.getHighestBlockAt(x, z);

            tryCount++;

            LogConsole.warn("Original Location "+tryCount+" : (X "+x+", Y "+y+", Z "+z+")", LogConsole.logTypes.debug);

            Location location = new Location(x,y,z,world);
                   // Location(world, x, y, z);

            // Special case for server version < 1.15.2 (?)
            // Related: https://www.spigotmc.org/threads/gethighestblockat-returns-air.434090/
            if (location.getLevelBlock().getId()  == BlockID.AIR) {
                location = location.add(0, -1, 0);
            }

            if(!isSpawnBlacklistInverted) {
                if (worldBlacklistedMaterials.contains(location.getLevelBlock().getId())) {
                 continue;
                }
            }
            else {
                if (!worldBlacklistedMaterials.contains(location.getLevelBlock().getId())) {
                    continue;
                }
            }

            LogConsole.warn("Found location for random respawn after "+tryCount+" (X "+x+", Y "+y+", Z "+z+")", LogConsole.logTypes.log);

            return location.add(0.5d, 1d, 0.5d);
        }
    }
}
