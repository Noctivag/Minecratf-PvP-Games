package com.nextlevel.pvp.manager;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.gamemode.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {

    private final NextLevelPvP plugin;
    private final Map<UUID, GameMode> activeGames;
    private final Map<UUID, UUID> playerGameMap; // Player UUID -> Game UUID
    private final Map<String, Class<? extends GameMode>> registeredModes;
    private BukkitTask gameTask;

    public GameManager(NextLevelPvP plugin) {
        this.plugin = plugin;
        this.activeGames = new HashMap<>();
        this.playerGameMap = new HashMap<>();
        this.registeredModes = new HashMap<>();

        registerGameModes();
        startGameTasks();
    }

    private void registerGameModes() {
        registeredModes.put("ffa", FFAGameMode.class);
        registeredModes.put("duel", DuelGameMode.class);
        registeredModes.put("tdm", TeamDeathmatchMode.class);
        registeredModes.put("koth", KingOfTheHillMode.class);
    }

    private void startGameTasks() {
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (GameMode game : new ArrayList<>(activeGames.values())) {
                if (game.getState() == GameMode.GameState.ACTIVE && game.shouldEnd()) {
                    endGame(game);
                }
            }
        }, 20L, 20L); // Run every second
    }

    public GameMode createGame(String modeId, Arena arena) {
        if (!registeredModes.containsKey(modeId.toLowerCase())) {
            return null;
        }

        try {
            GameMode game = null;
            String mode = modeId.toLowerCase();

            switch (mode) {
                case "ffa":
                    game = new FFAGameMode(arena, plugin);
                    break;
                case "duel":
                    game = new DuelGameMode(arena, plugin);
                    break;
                case "tdm":
                    game = new TeamDeathmatchMode(arena, plugin);
                    break;
                case "koth":
                    game = new KingOfTheHillMode(arena, plugin);
                    break;
            }

            if (game != null) {
                activeGames.put(UUID.randomUUID(), game);
                return game;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create game: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public void startGame(GameMode game) {
        if (game.canStart()) {
            game.start();
        }
    }

    public void endGame(GameMode game) {
        game.end();

        // Remove player mappings
        for (UUID playerId : game.getPlayers()) {
            playerGameMap.remove(playerId);
        }

        // Remove game from active games
        activeGames.entrySet().removeIf(entry -> entry.getValue().equals(game));
    }

    public void endAllGames() {
        for (GameMode game : new ArrayList<>(activeGames.values())) {
            endGame(game);
        }
    }

    public boolean joinGame(Player player, GameMode game) {
        if (isInGame(player)) {
            return false;
        }

        if (game.getPlayerCount() >= game.getArena().getMaxPlayers()) {
            return false;
        }

        game.onPlayerJoin(player);
        playerGameMap.put(player.getUniqueId(), getGameId(game));
        return true;
    }

    public void leaveGame(Player player) {
        GameMode game = getPlayerGame(player);
        if (game != null) {
            game.onPlayerLeave(player);
            playerGameMap.remove(player.getUniqueId());

            if (game.getPlayerCount() == 0) {
                endGame(game);
            }
        }
    }

    public GameMode getPlayerGame(Player player) {
        UUID gameId = playerGameMap.get(player.getUniqueId());
        return gameId != null ? activeGames.get(gameId) : null;
    }

    public boolean isInGame(Player player) {
        return playerGameMap.containsKey(player.getUniqueId());
    }

    public Collection<GameMode> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }

    public List<GameMode> getJoinableGames(String modeId) {
        List<GameMode> joinable = new ArrayList<>();
        for (GameMode game : activeGames.values()) {
            if (game.getId().equalsIgnoreCase(modeId) &&
                game.getState() == GameMode.GameState.WAITING &&
                game.getPlayerCount() < game.getArena().getMaxPlayers()) {
                joinable.add(game);
            }
        }
        return joinable;
    }

    private UUID getGameId(GameMode game) {
        for (Map.Entry<UUID, GameMode> entry : activeGames.entrySet()) {
            if (entry.getValue().equals(game)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<String> getRegisteredModes() {
        return new HashSet<>(registeredModes.keySet());
    }
}
