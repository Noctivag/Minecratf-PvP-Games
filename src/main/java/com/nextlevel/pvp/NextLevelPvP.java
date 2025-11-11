package com.nextlevel.pvp;

import com.nextlevel.pvp.commands.PvPCommand;
import com.nextlevel.pvp.listener.GameListener;
import com.nextlevel.pvp.manager.ArenaManager;
import com.nextlevel.pvp.manager.GameManager;
import com.nextlevel.pvp.manager.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NextLevelPvP extends JavaPlugin {

    private static NextLevelPvP instance;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        arenaManager = new ArenaManager(this);
        playerManager = new PlayerManager(this);
        gameManager = new GameManager(this);

        // Register commands
        getCommand("pvp").setExecutor(new PvPCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getLogger().info("NextLevelPvP has been enabled!");
        getLogger().info("Loaded " + arenaManager.getArenas().size() + " arenas");
    }

    @Override
    public void onDisable() {
        // Save all data
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }
        if (playerManager != null) {
            playerManager.saveAllStats();
        }
        if (gameManager != null) {
            gameManager.endAllGames();
        }

        getLogger().info("NextLevelPvP has been disabled!");
    }

    public static NextLevelPvP getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}
