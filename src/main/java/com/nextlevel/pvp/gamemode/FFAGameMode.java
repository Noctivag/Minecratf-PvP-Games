package com.nextlevel.pvp.gamemode;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FFAGameMode extends com.nextlevel.pvp.gamemode.GameMode {

    private final NextLevelPvP plugin;
    private final int killLimit;
    private final int timeLimit;

    public FFAGameMode(Arena arena, NextLevelPvP plugin) {
        super("ffa", "Free-For-All", "Every player for themselves!", arena);
        this.plugin = plugin;
        this.killLimit = plugin.getConfig().getInt("gamemodes.ffa.kill-limit", 20);
        this.timeLimit = plugin.getConfig().getInt("gamemodes.ffa.time-limit", 600);
    }

    @Override
    public void start() {
        state = GameState.ACTIVE;
        startTime = System.currentTimeMillis();

        // Teleport players to spawn points
        List<UUID> playerList = new ArrayList<>(players);
        for (int i = 0; i < playerList.size(); i++) {
            Player player = Bukkit.getPlayer(playerList.get(i));
            if (player != null) {
                player.teleport(arena.getRandomSpawnPoint());
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.getInventory().clear();
                giveStarterKit(player);
                MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.game-start"));
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
                    MessageUtil.sendMessage(player, "&a&lYOU WON!");
                    MessageUtil.sendMessage(player, "&aKills: &e" + getKills(playerId) + " &aDeaths: &e" + getDeaths(playerId));
                } else {
                    MessageUtil.sendMessage(player, "&c&lGAME OVER!");
                    MessageUtil.sendMessage(player, "&aKills: &e" + getKills(playerId) + " &aDeaths: &e" + getDeaths(playerId));
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

        // Broadcast kill
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                MessageUtil.sendMessage(player, "&e" + killer.getName() + " &7killed &e" + victim.getName() +
                    " &7(&c" + getKills(killer.getUniqueId()) + "&7/&a" + killLimit + "&7)");
            }
        }

        // Respawn victim
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ACTIVE && players.contains(victim.getUniqueId())) {
                    victim.spigot().respawn();
                    victim.teleport(arena.getRandomSpawnPoint());
                    giveStarterKit(victim);
                }
            }
        }.runTaskLater(plugin, 20L * plugin.getConfig().getInt("game.respawn-time", 5));
    }

    @Override
    public void onPlayerDeath(Player player) {
        addDeath(player.getUniqueId());
    }

    @Override
    public void onPlayerJoin(Player player) {
        addPlayer(player.getUniqueId());
        player.teleport(arena.getLobby());
        player.setGameMode(GameMode.ADVENTURE);

        // Broadcast join
        for (UUID playerId : players) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                MessageUtil.sendMessage(p, plugin.getConfig().getString("messages.player-join")
                    .replace("{player}", player.getName()));
            }
        }

        // Start countdown if enough players
        if (canStart() && state == GameState.WAITING) {
            startCountdown();
        }
    }

    @Override
    public void onPlayerLeave(Player player) {
        removePlayer(player.getUniqueId());
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);

        // Broadcast leave
        for (UUID playerId : players) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                MessageUtil.sendMessage(p, plugin.getConfig().getString("messages.player-leave")
                    .replace("{player}", player.getName()));
            }
        }
    }

    @Override
    public boolean canStart() {
        return players.size() >= arena.getMinPlayers();
    }

    @Override
    public boolean shouldEnd() {
        if (players.size() < 2) {
            return true;
        }

        // Check kill limit
        for (int killCount : kills.values()) {
            if (killCount >= killLimit) {
                return true;
            }
        }

        // Check time limit
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return elapsed >= timeLimit;
    }

    @Override
    public List<UUID> getWinners() {
        int maxKills = 0;
        for (int killCount : kills.values()) {
            maxKills = Math.max(maxKills, killCount);
        }

        List<UUID> winners = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            if (entry.getValue() == maxKills) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }

    private void startCountdown() {
        state = GameState.COUNTDOWN;

        new BukkitRunnable() {
            int countdown = countdownTime;

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

                if (countdown <= 5 || countdown % 10 == 0) {
                    for (UUID playerId : players) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            MessageUtil.sendMessage(player, "&eGame starting in &a" + countdown + " &eseconds...");
                        }
                    }
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void giveStarterKit(Player player) {
        player.getInventory().clear();
        // Basic starter kit - can be customized in config
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_SWORD));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 32));
        player.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS));
    }
}
