package com.nextlevel.pvp.gamemode;

import com.nextlevel.pvp.arena.Arena;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class GameMode {

    protected final String id;
    protected final String name;
    protected final String description;
    protected final Arena arena;
    protected final Set<UUID> players;
    protected final Map<UUID, Integer> kills;
    protected final Map<UUID, Integer> deaths;
    protected GameState state;
    protected int countdownTime;
    protected long startTime;

    public GameMode(String id, String name, String description, Arena arena) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.arena = arena;
        this.players = new HashSet<>();
        this.kills = new HashMap<>();
        this.deaths = new HashMap<>();
        this.state = GameState.WAITING;
        this.countdownTime = 10;
    }

    public abstract void start();

    public abstract void end();

    public abstract void onPlayerKill(Player killer, Player victim);

    public abstract void onPlayerDeath(Player player);

    public abstract void onPlayerJoin(Player player);

    public abstract void onPlayerLeave(Player player);

    public abstract boolean canStart();

    public abstract boolean shouldEnd();

    public abstract List<UUID> getWinners();

    public void addPlayer(UUID playerId) {
        players.add(playerId);
        kills.put(playerId, 0);
        deaths.put(playerId, 0);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
        kills.remove(playerId);
        deaths.remove(playerId);
    }

    public void addKill(UUID playerId) {
        kills.put(playerId, kills.getOrDefault(playerId, 0) + 1);
    }

    public void addDeath(UUID playerId) {
        deaths.put(playerId, deaths.getOrDefault(playerId, 0) + 1);
    }

    public int getKills(UUID playerId) {
        return kills.getOrDefault(playerId, 0);
    }

    public int getDeaths(UUID playerId) {
        return deaths.getOrDefault(playerId, 0);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Arena getArena() {
        return arena;
    }

    public Set<UUID> getPlayers() {
        return new HashSet<>(players);
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public boolean isInGame(UUID playerId) {
        return players.contains(playerId);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public enum GameState {
        WAITING,
        COUNTDOWN,
        ACTIVE,
        ENDING,
        ENDED
    }
}
