package com.nextlevel.pvp.listener;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.gamemode.GameMode;
import com.nextlevel.pvp.gamemode.TeamDeathmatchMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class GameListener implements Listener {

    private final NextLevelPvP plugin;

    public GameListener(NextLevelPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        GameMode game = plugin.getGameManager().getPlayerGame(victim);

        if (game == null || game.getState() != GameMode.GameState.ACTIVE) {
            return;
        }

        Player killer = victim.getKiller();

        if (killer != null) {
            // Handle kill
            game.onPlayerKill(killer, victim);
        } else {
            // Death without killer
            game.onPlayerDeath(victim);
        }

        // Customize death message
        event.setDeathMessage(null);

        // Handle drops and experience based on config
        if (!plugin.getConfig().getBoolean("player.keep-inventory", false)) {
            event.getDrops().clear();
        }

        if (!plugin.getConfig().getBoolean("player.keep-exp", false)) {
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().isInGame(player)) {
            plugin.getGameManager().leaveGame(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        GameMode game = plugin.getGameManager().getPlayerGame(victim);
        GameMode damagerGame = plugin.getGameManager().getPlayerGame(damager);

        // Check if both players are in the same game
        if (game == null || damagerGame == null || !game.equals(damagerGame)) {
            return;
        }

        // Check if game is active
        if (game.getState() != GameMode.GameState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Check for team damage in team-based modes
        if (game instanceof TeamDeathmatchMode) {
            TeamDeathmatchMode tdm = (TeamDeathmatchMode) game;
            if (tdm.areOnSameTeam(victim.getUniqueId(), damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
