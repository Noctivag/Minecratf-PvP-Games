package com.nextlevel.pvp.manager;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.arena.ArenaRegion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ArenaManager {

    static {
        ConfigurationSerialization.registerClass(Arena.class);
        ConfigurationSerialization.registerClass(ArenaRegion.class);
    }

    private final NextLevelPvP plugin;
    private final Map<String, Arena> arenas;
    private final File arenasFile;

    public ArenaManager(NextLevelPvP plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");

        loadArenas();
    }

    public void loadArenas() {
        if (!arenasFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create arenas.yml", e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);

        if (config.contains("arenas")) {
            List<?> arenaList = config.getList("arenas");
            if (arenaList != null) {
                for (Object obj : arenaList) {
                    if (obj instanceof Arena) {
                        Arena arena = (Arena) obj;
                        arenas.put(arena.getName().toLowerCase(), arena);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }

    public void saveArenas() {
        FileConfiguration config = new YamlConfiguration();
        config.set("arenas", new ArrayList<>(arenas.values()));

        try {
            config.save(arenasFile);
            plugin.getLogger().info("Saved " + arenas.size() + " arenas");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save arenas.yml", e);
        }
    }

    public Arena createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) {
            return null;
        }

        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(String name) {
        Arena removed = arenas.remove(name.toLowerCase());
        if (removed != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return new ArrayList<>(arenas.values());
    }

    public List<Arena> getArenasForGameMode(String gameMode) {
        List<Arena> availableArenas = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isEnabled() &&
                arena.isReady() &&
                (arena.getAllowedGameModes().isEmpty() || arena.getAllowedGameModes().contains(gameMode))) {
                availableArenas.add(arena);
            }
        }
        return availableArenas;
    }

    public Arena getRandomArena(String gameMode) {
        List<Arena> available = getArenasForGameMode(gameMode);
        if (available.isEmpty()) {
            return null;
        }
        return available.get(new Random().nextInt(available.size()));
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }
}
