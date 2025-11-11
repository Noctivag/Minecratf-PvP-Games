package com.nextlevel.pvp.gamemode;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TeamDeathmatchMode extends com.nextlevel.pvp.gamemode.GameMode {

    private final NextLevelPvP plugin;
    private final int killLimit;
    private final int teamSize;
    private final Map<String, Set<UUID>> teams; // team name -> players
    private final Map<UUID, String> playerTeams; // player -> team name
    private final Map<String, Integer> teamKills;

    public TeamDeathmatchMode(Arena arena, NextLevelPvP plugin) {
        super("tdm", "Team Deathmatch", "Team up and dominate!", arena);
        this.plugin = plugin;
        this.killLimit = plugin.getConfig().getInt("gamemodes.tdm.kill-limit", 50);
        this.teamSize = plugin.getConfig().getInt("gamemodes.tdm.team-size", 4);
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.teamKills = new HashMap<>();

        // Initialize teams
        teams.put("RED", new HashSet<>());
        teams.put("BLUE", new HashSet<>());
        teamKills.put("RED", 0);
        teamKills.put("BLUE", 0);
    }

    @Override
    public void start() {
        state = GameState.ACTIVE;
        startTime = System.currentTimeMillis();

        // Teleport players to team spawns
        for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
            String teamName = entry.getKey();
            org.bukkit.Location spawn = arena.getTeamSpawns().get(teamName);
            if (spawn == null) {
                spawn = arena.getRandomSpawnPoint();
            }

            for (UUID playerId : entry.getValue()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.teleport(spawn);
                    setupPlayer(player, teamName);
                    MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.game-start"));
                    MessageUtil.sendMessage(player, "&aYou are on team " + getTeamColor(teamName) + teamName);
                }
            }
        }
    }

    @Override
    public void end() {
        state = GameState.ENDING;

        String winningTeam = getWinningTeam();

        // Announce results
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                String playerTeam = playerTeams.get(playerId);

                if (winningTeam != null && winningTeam.equals(playerTeam)) {
                    MessageUtil.sendMessage(player, "&a&lYOUR TEAM WON!");
                } else {
                    MessageUtil.sendMessage(player, "&c&lYOUR TEAM LOST!");
                }

                MessageUtil.sendMessage(player, "&eRED Team: &c" + teamKills.get("RED") + " &ekills");
                MessageUtil.sendMessage(player, "&eBLUE Team: &9" + teamKills.get("BLUE") + " &ekills");
                MessageUtil.sendMessage(player, "&aYour K/D: &e" + getKills(playerId) + "&7/&e" + getDeaths(playerId));

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

            String playerTeam = playerTeams.get(playerId);
            if (winningTeam != null && winningTeam.equals(playerTeam)) {
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

        String killerTeam = playerTeams.get(killer.getUniqueId());
        String victimTeam = playerTeams.get(victim.getUniqueId());

        // Add team kill
        teamKills.put(killerTeam, teamKills.get(killerTeam) + 1);

        // Broadcast kill
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                MessageUtil.sendMessage(player, getTeamColor(killerTeam) + killer.getName() +
                    " &7killed " + getTeamColor(victimTeam) + victim.getName() +
                    " &7(" + getTeamColor(killerTeam) + teamKills.get(killerTeam) + "&7/&a" + killLimit + "&7)");
            }
        }

        // Respawn victim
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ACTIVE && players.contains(victim.getUniqueId())) {
                    victim.spigot().respawn();
                    org.bukkit.Location spawn = arena.getTeamSpawns().get(victimTeam);
                    if (spawn == null) {
                        spawn = arena.getRandomSpawnPoint();
                    }
                    victim.teleport(spawn);
                    setupPlayer(victim, victimTeam);
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
        // Assign to team with fewer players
        String team = getSmallestTeam();
        addPlayer(player.getUniqueId());
        teams.get(team).add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), team);

        player.teleport(arena.getLobby());
        player.setGameMode(GameMode.ADVENTURE);

        MessageUtil.sendMessage(player, "&aYou joined " + getTeamColor(team) + team + " &ateam!");

        // Broadcast join
        for (UUID playerId : players) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && !p.equals(player)) {
                MessageUtil.sendMessage(p, getTeamColor(team) + player.getName() + " &ajoined " + getTeamColor(team) + team + " &ateam!");
            }
        }

        // Start countdown if enough players
        if (canStart() && state == GameState.WAITING) {
            startCountdown();
        }
    }

    @Override
    public void onPlayerLeave(Player player) {
        String team = playerTeams.get(player.getUniqueId());
        if (team != null) {
            teams.get(team).remove(player.getUniqueId());
        }
        playerTeams.remove(player.getUniqueId());
        removePlayer(player.getUniqueId());

        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
    }

    @Override
    public boolean canStart() {
        return teams.get("RED").size() >= 1 && teams.get("BLUE").size() >= 1 && players.size() >= arena.getMinPlayers();
    }

    @Override
    public boolean shouldEnd() {
        // End if not enough players
        if (teams.get("RED").isEmpty() || teams.get("BLUE").isEmpty()) {
            return true;
        }

        // Check kill limit
        for (int kills : teamKills.values()) {
            if (kills >= killLimit) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<UUID> getWinners() {
        String winningTeam = getWinningTeam();
        if (winningTeam != null) {
            return new ArrayList<>(teams.get(winningTeam));
        }
        return new ArrayList<>();
    }

    private String getWinningTeam() {
        int redKills = teamKills.get("RED");
        int blueKills = teamKills.get("BLUE");

        if (redKills > blueKills) {
            return "RED";
        } else if (blueKills > redKills) {
            return "BLUE";
        }
        return null; // Draw
    }

    private String getSmallestTeam() {
        if (teams.get("RED").size() <= teams.get("BLUE").size()) {
            return "RED";
        }
        return "BLUE";
    }

    private String getTeamColor(String team) {
        switch (team.toUpperCase()) {
            case "RED":
                return ChatColor.RED.toString();
            case "BLUE":
                return ChatColor.BLUE.toString();
            default:
                return ChatColor.WHITE.toString();
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

    private void setupPlayer(Player player, String team) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        giveTeamKit(player, team);
    }

    private void giveTeamKit(Player player, String team) {
        player.getInventory().clear();
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 64));

        // Team-colored armor
        org.bukkit.Color armorColor = team.equals("RED") ?
            org.bukkit.Color.RED : org.bukkit.Color.BLUE;

        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET);
        org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
        org.bukkit.inventory.ItemStack leggings = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS);
        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS);

        org.bukkit.inventory.meta.LeatherArmorMeta helmetMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(armorColor);
        helmet.setItemMeta(helmetMeta);

        org.bukkit.inventory.meta.LeatherArmorMeta chestplateMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();
        chestplateMeta.setColor(armorColor);
        chestplate.setItemMeta(chestplateMeta);

        org.bukkit.inventory.meta.LeatherArmorMeta leggingsMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) leggings.getItemMeta();
        leggingsMeta.setColor(armorColor);
        leggings.setItemMeta(leggingsMeta);

        org.bukkit.inventory.meta.LeatherArmorMeta bootsMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(armorColor);
        boots.setItemMeta(bootsMeta);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    public String getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public boolean areOnSameTeam(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);
        return team1 != null && team1.equals(team2);
    }
}
