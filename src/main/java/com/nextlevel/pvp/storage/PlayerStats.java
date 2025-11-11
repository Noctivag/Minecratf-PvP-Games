package com.nextlevel.pvp.storage;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats implements ConfigurationSerializable {

    private final UUID playerId;
    private int kills;
    private int deaths;
    private int wins;
    private int losses;
    private int gamesPlayed;

    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.losses = 0;
        this.gamesPlayed = 0;
    }

    @SuppressWarnings("unchecked")
    public PlayerStats(Map<String, Object> map) {
        this.playerId = UUID.fromString((String) map.get("playerId"));
        this.kills = (int) map.getOrDefault("kills", 0);
        this.deaths = (int) map.getOrDefault("deaths", 0);
        this.wins = (int) map.getOrDefault("wins", 0);
        this.losses = (int) map.getOrDefault("losses", 0);
        this.gamesPlayed = (int) map.getOrDefault("gamesPlayed", 0);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId.toString());
        map.put("kills", kills);
        map.put("deaths", deaths);
        map.put("wins", wins);
        map.put("losses", losses);
        map.put("gamesPlayed", gamesPlayed);
        return map;
    }

    public void addKills(int amount) {
        this.kills += amount;
    }

    public void addDeaths(int amount) {
        this.deaths += amount;
    }

    public void addWin() {
        this.wins++;
        this.gamesPlayed++;
    }

    public void addLoss() {
        this.losses++;
        this.gamesPlayed++;
    }

    public double getKDRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getWinRate() {
        return gamesPlayed == 0 ? 0 : (double) wins / gamesPlayed * 100;
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }
}
