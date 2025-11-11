package com.nextlevel.pvp.arena;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;

public class Arena implements ConfigurationSerializable {

    private final String name;
    private final String displayName;
    private Location lobby;
    private final List<Location> spawnPoints;
    private final Map<String, Location> teamSpawns; // For team-based modes
    private Location spectatorSpawn;
    private ArenaRegion region;
    private Set<String> allowedGameModes;
    private int minPlayers;
    private int maxPlayers;
    private boolean enabled;

    public Arena(String name) {
        this.name = name;
        this.displayName = name;
        this.spawnPoints = new ArrayList<>();
        this.teamSpawns = new HashMap<>();
        this.allowedGameModes = new HashSet<>();
        this.minPlayers = 2;
        this.maxPlayers = 16;
        this.enabled = true;
    }

    @SuppressWarnings("unchecked")
    public Arena(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.displayName = (String) map.getOrDefault("displayName", name);
        this.lobby = (Location) map.get("lobby");
        this.spawnPoints = (List<Location>) map.getOrDefault("spawnPoints", new ArrayList<>());
        this.teamSpawns = (Map<String, Location>) map.getOrDefault("teamSpawns", new HashMap<>());
        this.spectatorSpawn = (Location) map.get("spectatorSpawn");
        this.region = (ArenaRegion) map.get("region");
        this.allowedGameModes = new HashSet<>((List<String>) map.getOrDefault("allowedGameModes", new ArrayList<>()));
        this.minPlayers = (int) map.getOrDefault("minPlayers", 2);
        this.maxPlayers = (int) map.getOrDefault("maxPlayers", 16);
        this.enabled = (boolean) map.getOrDefault("enabled", true);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("displayName", displayName);
        map.put("lobby", lobby);
        map.put("spawnPoints", spawnPoints);
        map.put("teamSpawns", teamSpawns);
        map.put("spectatorSpawn", spectatorSpawn);
        map.put("region", region);
        map.put("allowedGameModes", new ArrayList<>(allowedGameModes));
        map.put("minPlayers", minPlayers);
        map.put("maxPlayers", maxPlayers);
        map.put("enabled", enabled);
        return map;
    }

    public boolean isReady() {
        return lobby != null && !spawnPoints.isEmpty() && region != null;
    }

    public boolean isInArena(Location location) {
        return region != null && region.contains(location);
    }

    public Location getRandomSpawnPoint() {
        if (spawnPoints.isEmpty()) {
            return lobby;
        }
        return spawnPoints.get(new Random().nextInt(spawnPoints.size()));
    }

    public void addSpawnPoint(Location location) {
        spawnPoints.add(location);
    }

    public void addTeamSpawn(String team, Location location) {
        teamSpawns.put(team, location);
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getLobby() {
        return lobby;
    }

    public void setLobby(Location lobby) {
        this.lobby = lobby;
    }

    public List<Location> getSpawnPoints() {
        return new ArrayList<>(spawnPoints);
    }

    public Map<String, Location> getTeamSpawns() {
        return new HashMap<>(teamSpawns);
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn != null ? spectatorSpawn : lobby;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public ArenaRegion getRegion() {
        return region;
    }

    public void setRegion(ArenaRegion region) {
        this.region = region;
    }

    public Set<String> getAllowedGameModes() {
        return new HashSet<>(allowedGameModes);
    }

    public void addGameMode(String gameMode) {
        allowedGameModes.add(gameMode);
    }

    public void removeGameMode(String gameMode) {
        allowedGameModes.remove(gameMode);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
