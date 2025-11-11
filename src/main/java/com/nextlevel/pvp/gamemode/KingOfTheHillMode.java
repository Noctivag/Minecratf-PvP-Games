package com.nextlevel.pvp.gamemode;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class KingOfTheHillMode extends com.nextlevel.pvp.gamemode.GameMode {

    private final NextLevelPvP plugin;
    private final int captureTime;
    private final int pointValue;
    private final int winScore;
    private final Map<UUID, Integer> scores;
    private Location hillCenter;
    private double hillRadius;
    private UUID currentKing;
    private int kingCaptureProgress;
    private BukkitTask hillTask;

    public KingOfTheHillMode(Arena arena, NextLevelPvP plugin) {
        super("koth", "King of the Hill", "Control the hill to win!", arena);
        this.plugin = plugin;
        this.captureTime = plugin.getConfig().getInt("gamemodes.koth.capture-time", 60);
        this.pointValue = plugin.getConfig().getInt("gamemodes.koth.point-value", 1);
        this.winScore = 100;
        this.scores = new HashMap<>();
        this.hillRadius = 5.0;
        this.kingCaptureProgress = 0;

        // Set hill center to arena center or first spawn
        if (!arena.getSpawnPoints().isEmpty()) {
            this.hillCenter = arena.getSpawnPoints().get(0);
        }
    }

    @Override
    public void start() {
        state = GameState.ACTIVE;
        startTime = System.currentTimeMillis();

        // Teleport players to spawn points
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.teleport(arena.getRandomSpawnPoint());
                setupPlayer(player);
                scores.put(playerId, 0);
                MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.game-start"));
                MessageUtil.sendMessage(player, "&eCapture and hold the hill to earn points!");
            }
        }

        // Start hill capture task
        startHillTask();
    }

    @Override
    public void end() {
        state = GameState.ENDING;

        if (hillTask != null) {
            hillTask.cancel();
        }

        List<UUID> winners = getWinners();

        // Announce winners
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (winners.contains(playerId)) {
                    MessageUtil.sendMessage(player, "&a&lYOU WON!");
                } else {
                    MessageUtil.sendMessage(player, "&c&lGAME OVER!");
                }

                MessageUtil.sendMessage(player, "&aYour Score: &e" + scores.getOrDefault(playerId, 0));
                MessageUtil.sendMessage(player, "&aKills: &e" + getKills(playerId) + " &aDeaths: &e" + getDeaths(playerId));

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

        // Award points for kill
        scores.put(killer.getUniqueId(), scores.getOrDefault(killer.getUniqueId(), 0) + 5);

        // Broadcast kill
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                MessageUtil.sendMessage(player, "&e" + killer.getName() + " &7killed &e" + victim.getName());
            }
        }

        // Reset king if victim was king
        if (victim.getUniqueId().equals(currentKing)) {
            currentKing = null;
            kingCaptureProgress = 0;
            broadcastMessage("&cThe King has fallen!");
        }

        // Respawn victim
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ACTIVE && players.contains(victim.getUniqueId())) {
                    victim.spigot().respawn();
                    victim.teleport(arena.getRandomSpawnPoint());
                    setupPlayer(victim);
                }
            }
        }.runTaskLater(plugin, 20L * plugin.getConfig().getInt("game.respawn-time", 5));
    }

    @Override
    public void onPlayerDeath(Player player) {
        addDeath(player.getUniqueId());

        if (player.getUniqueId().equals(currentKing)) {
            currentKing = null;
            kingCaptureProgress = 0;
            broadcastMessage("&cThe King has fallen!");
        }
    }

    @Override
    public void onPlayerJoin(Player player) {
        addPlayer(player.getUniqueId());
        scores.put(player.getUniqueId(), 0);
        player.teleport(arena.getLobby());
        player.setGameMode(GameMode.ADVENTURE);

        broadcastMessage(plugin.getConfig().getString("messages.player-join").replace("{player}", player.getName()));

        if (canStart() && state == GameState.WAITING) {
            startCountdown();
        }
    }

    @Override
    public void onPlayerLeave(Player player) {
        if (player.getUniqueId().equals(currentKing)) {
            currentKing = null;
            kingCaptureProgress = 0;
        }

        removePlayer(player.getUniqueId());
        scores.remove(player.getUniqueId());
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);

        broadcastMessage(plugin.getConfig().getString("messages.player-leave").replace("{player}", player.getName()));
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

        // Check if someone reached win score
        for (int score : scores.values()) {
            if (score >= winScore) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<UUID> getWinners() {
        int maxScore = 0;
        for (int score : scores.values()) {
            maxScore = Math.max(maxScore, score);
        }

        List<UUID> winners = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            if (entry.getValue() == maxScore) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }

    private void startHillTask() {
        hillTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                if (state != GameState.ACTIVE) {
                    cancel();
                    return;
                }

                tickCounter++;

                // Find players on the hill
                List<Player> playersOnHill = new ArrayList<>();
                for (UUID playerId : players) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && isOnHill(player.getLocation())) {
                        playersOnHill.add(player);
                    }
                }

                // Handle hill capture logic
                if (playersOnHill.isEmpty()) {
                    // No one on hill
                    if (currentKing != null) {
                        kingCaptureProgress = Math.max(0, kingCaptureProgress - 1);
                        if (kingCaptureProgress <= 0) {
                            currentKing = null;
                        }
                    }
                } else if (playersOnHill.size() == 1) {
                    // One player on hill
                    Player player = playersOnHill.get(0);
                    if (player.getUniqueId().equals(currentKing)) {
                        // Award points
                        if (tickCounter % 20 == 0) { // Every second
                            scores.put(player.getUniqueId(), scores.getOrDefault(player.getUniqueId(), 0) + pointValue);
                        }
                    } else {
                        // Capturing hill
                        kingCaptureProgress++;
                        if (kingCaptureProgress >= captureTime) {
                            currentKing = player.getUniqueId();
                            kingCaptureProgress = captureTime;
                            broadcastMessage("&a" + player.getName() + " &eis now the King of the Hill!");
                        }
                    }
                } else {
                    // Multiple players contested
                    if (tickCounter % 20 == 0) {
                        broadcastMessage("&cThe hill is contested!");
                    }
                    kingCaptureProgress = Math.max(0, kingCaptureProgress - 1);
                }

                // Display action bar with scores
                if (tickCounter % 20 == 0) {
                    displayScores();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isOnHill(Location location) {
        if (hillCenter == null) {
            return false;
        }
        return location.getWorld().equals(hillCenter.getWorld()) &&
               location.distance(hillCenter) <= hillRadius;
    }

    private void displayScores() {
        List<Map.Entry<UUID, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                StringBuilder message = new StringBuilder("&6Scores: ");
                for (int i = 0; i < Math.min(3, sortedScores.size()); i++) {
                    UUID id = sortedScores.get(i).getKey();
                    int score = sortedScores.get(i).getValue();
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        message.append("&e").append(p.getName()).append(": &a").append(score).append(" ");
                    }
                }
                MessageUtil.sendActionBar(player, message.toString());
            }
        }
    }

    private void broadcastMessage(String message) {
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                MessageUtil.sendMessage(player, message);
            }
        }
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
                    broadcastMessage("&eGame starting in &a" + countdown + " &eseconds...");
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
        giveStarterKit(player);
    }

    private void giveStarterKit(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 32));
        player.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_BOOTS));
    }

    public void setHillLocation(Location location, double radius) {
        this.hillCenter = location;
        this.hillRadius = radius;
    }
}
