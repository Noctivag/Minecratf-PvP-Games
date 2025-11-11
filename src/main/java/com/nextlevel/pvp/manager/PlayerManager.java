package com.nextlevel.pvp.manager;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.storage.PlayerStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerManager {

    static {
        ConfigurationSerialization.registerClass(PlayerStats.class);
    }

    private final NextLevelPvP plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final File statsFile;

    public PlayerManager(NextLevelPvP plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");

        loadStats();
    }

    public void loadStats() {
        if (!statsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create stats.yml", e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(statsFile);

        if (config.contains("stats")) {
            List<?> statsList = config.getList("stats");
            if (statsList != null) {
                for (Object obj : statsList) {
                    if (obj instanceof PlayerStats) {
                        PlayerStats stats = (PlayerStats) obj;
                        playerStats.put(stats.getPlayerId(), stats);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded stats for " + playerStats.size() + " players");
    }

    public void saveAllStats() {
        FileConfiguration config = new YamlConfiguration();
        config.set("stats", new ArrayList<>(playerStats.values()));

        try {
            config.save(statsFile);
            plugin.getLogger().info("Saved stats for " + playerStats.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats.yml", e);
        }
    }

    public PlayerStats getStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, PlayerStats::new);
    }

    public void addKills(UUID playerId, int amount) {
        getStats(playerId).addKills(amount);
    }

    public void addDeaths(UUID playerId, int amount) {
        getStats(playerId).addDeaths(amount);
    }

    public void addWin(UUID playerId) {
        getStats(playerId).addWin();
    }

    public void addLoss(UUID playerId) {
        getStats(playerId).addLoss();
    }

    public List<PlayerStats> getTopPlayers(int limit) {
        List<PlayerStats> topPlayers = new ArrayList<>(playerStats.values());
        topPlayers.sort((a, b) -> {
            // Sort by wins, then by K/D ratio
            int winsCompare = Integer.compare(b.getWins(), a.getWins());
            if (winsCompare != 0) {
                return winsCompare;
            }
            return Double.compare(b.getKDRatio(), a.getKDRatio());
        });

        return topPlayers.subList(0, Math.min(limit, topPlayers.size()));
    }

    public void resetStats(UUID playerId) {
        playerStats.remove(playerId);
    }
}
