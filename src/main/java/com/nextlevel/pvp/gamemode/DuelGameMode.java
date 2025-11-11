package com.nextlevel.pvp.gamemode;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DuelGameMode extends com.nextlevel.pvp.gamemode.GameMode {

    private final NextLevelPvP plugin;
    private final int bestOf;
    private final Map<UUID, Integer> roundWins;
    private int currentRound;

    public DuelGameMode(Arena arena, NextLevelPvP plugin) {
        super("duel", "1v1 Duel", "Fight in an honorable duel!", arena);
        this.plugin = plugin;
        this.bestOf = plugin.getConfig().getInt("gamemodes.duel.best-of", 3);
        this.roundWins = new HashMap<>();
        this.currentRound = 1;
    }

    @Override
    public void start() {
        state = GameState.ACTIVE;
        startTime = System.currentTimeMillis();
        startRound();
    }

    private void startRound() {
        // Teleport players to spawn points
        List<UUID> playerList = new ArrayList<>(players);
        if (playerList.size() >= 2) {
            Player p1 = Bukkit.getPlayer(playerList.get(0));
            Player p2 = Bukkit.getPlayer(playerList.get(1));

            if (p1 != null && p2 != null) {
                p1.teleport(arena.getSpawnPoints().get(0));
                p2.teleport(arena.getSpawnPoints().get(Math.min(1, arena.getSpawnPoints().size() - 1)));

                setupPlayer(p1);
                setupPlayer(p2);

                MessageUtil.sendMessage(p1, "&eRound " + currentRound + " - Fight!");
                MessageUtil.sendMessage(p2, "&eRound " + currentRound + " - Fight!");
            }
        }
    }

    @Override
    public void end() {
        state = GameState.ENDING;

        List<UUID> winners = getWinners();

        // Announce winners
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (winners.contains(playerId)) {
                    MessageUtil.sendMessage(player, "&a&lYOU WON THE DUEL!");
                    MessageUtil.sendMessage(player, "&aRounds won: &e" + roundWins.getOrDefault(playerId, 0));
                } else {
                    MessageUtil.sendMessage(player, "&c&lYOU LOST!");
                    MessageUtil.sendMessage(player, "&aRounds won: &e" + roundWins.getOrDefault(playerId, 0));
                }

                // Teleport to lobby
                player.teleport(arena.getLobby());
                player.getInventory().clear();
                player.setGameMode(GameMode.ADVENTURE);
            }
        }

        // Save stats
        for (UUID playerId : players) {
            plugin.getPlayerManager().addKills(playerId, getKills(playerId));
            plugin.getPlayerManager().addDeaths(playerId, getDeaths(playerId));
            if (winners.contains(playerId)) {
                plugin.getPlayerManager().addWin(playerId);
            } else {
                plugin.getPlayerManager().addLoss(playerId);
            }
        }

        state = GameState.ENDED;
    }

    @Override
    public void onPlayerKill(Player killer, Player victim) {
        addKill(killer.getUniqueId());
        addDeath(victim.getUniqueId());

        // Award round win
        roundWins.put(killer.getUniqueId(), roundWins.getOrDefault(killer.getUniqueId(), 0) + 1);

        MessageUtil.sendMessage(killer, "&aYou won round " + currentRound + "!");
        MessageUtil.sendMessage(victim, "&cYou lost round " + currentRound + "!");

        // Check if duel should end
        if (shouldEnd()) {
            end();
        } else {
            // Start next round
            currentRound++;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (state == GameState.ACTIVE) {
                        startRound();
                    }
                }
            }.runTaskLater(plugin, 60L); // 3 second delay
        }
    }

    @Override
    public void onPlayerDeath(Player player) {
        addDeath(player.getUniqueId());
    }

    @Override
    public void onPlayerJoin(Player player) {
        if (players.size() >= 2) {
            return; // Duel is full
        }

        addPlayer(player.getUniqueId());
        roundWins.put(player.getUniqueId(), 0);
        player.teleport(arena.getLobby());
        player.setGameMode(GameMode.ADVENTURE);

        MessageUtil.sendMessage(player, "&aYou joined the duel!");

        if (players.size() == 2) {
            startCountdown();
        } else {
            MessageUtil.sendMessage(player, "&eWaiting for opponent...");
        }
    }

    @Override
    public void onPlayerLeave(Player player) {
        removePlayer(player.getUniqueId());
        roundWins.remove(player.getUniqueId());
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);

        // End duel if a player leaves during the game
        if (state == GameState.ACTIVE || state == GameState.COUNTDOWN) {
            for (UUID playerId : players) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null) {
                    MessageUtil.sendMessage(p, "&cOpponent left! Duel ended.");
                }
            }
        }
    }

    @Override
    public boolean canStart() {
        return players.size() == 2;
    }

    @Override
    public boolean shouldEnd() {
        // End if less than 2 players
        if (players.size() < 2) {
            return true;
        }

        // Check if someone won enough rounds
        int roundsToWin = (bestOf / 2) + 1;
        for (int wins : roundWins.values()) {
            if (wins >= roundsToWin) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<UUID> getWinners() {
        int maxWins = 0;
        UUID winner = null;

        for (Map.Entry<UUID, Integer> entry : roundWins.entrySet()) {
            if (entry.getValue() > maxWins) {
                maxWins = entry.getValue();
                winner = entry.getKey();
            }
        }

        return winner != null ? Collections.singletonList(winner) : new ArrayList<>();
    }

    private void startCountdown() {
        state = GameState.COUNTDOWN;

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (state != GameState.COUNTDOWN || !canStart()) {
                    state = GameState.WAITING;
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    start();
                    cancel();
                    return;
                }

                for (UUID playerId : players) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        MessageUtil.sendMessage(player, "&eDuel starting in &a" + countdown + " &eseconds...");
                    }
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        giveDuelKit(player);
    }

    private void giveDuelKit(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 16));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 3));
        player.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET));
        player.getInventory().setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS));
    }
}
