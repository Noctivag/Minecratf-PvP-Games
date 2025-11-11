package com.nextlevel.pvp.commands;

import com.nextlevel.pvp.NextLevelPvP;
import com.nextlevel.pvp.arena.Arena;
import com.nextlevel.pvp.arena.ArenaRegion;
import com.nextlevel.pvp.gamemode.GameMode;
import com.nextlevel.pvp.storage.PlayerStats;
import com.nextlevel.pvp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PvPCommand implements CommandExecutor, TabCompleter {

    private final NextLevelPvP plugin;

    public PvPCommand(NextLevelPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;

            case "join":
                if (!(sender instanceof Player)) {
                    MessageUtil.sendPrefixedMessage(sender, "&cOnly players can join games!");
                    return true;
                }
                handleJoin((Player) sender, args);
                break;

            case "leave":
                if (!(sender instanceof Player)) {
                    MessageUtil.sendPrefixedMessage(sender, "&cOnly players can leave games!");
                    return true;
                }
                handleLeave((Player) sender);
                break;

            case "stats":
                handleStats(sender, args);
                break;

            case "arena":
                if (!sender.hasPermission("nextlevelpvp.admin")) {
                    MessageUtil.sendPrefixedMessage(sender, "&cYou don't have permission!");
                    return true;
                }
                handleArena(sender, args);
                break;

            case "list":
                handleList(sender, args);
                break;

            case "create":
                if (!sender.hasPermission("nextlevelpvp.admin")) {
                    MessageUtil.sendPrefixedMessage(sender, "&cYou don't have permission!");
                    return true;
                }
                handleCreate(sender, args);
                break;

            default:
                MessageUtil.sendPrefixedMessage(sender, "&cUnknown subcommand. Use /pvp help");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "&8&m                                    ");
        MessageUtil.sendMessage(sender, "&cNext&fLevel&bPvP &7- Commands");
        MessageUtil.sendMessage(sender, "&8&m                                    ");
        MessageUtil.sendMessage(sender, "&e/pvp join <mode> [arena] &7- Join a game");
        MessageUtil.sendMessage(sender, "&e/pvp leave &7- Leave current game");
        MessageUtil.sendMessage(sender, "&e/pvp stats [player] &7- View stats");
        MessageUtil.sendMessage(sender, "&e/pvp list <modes|arenas|games> &7- List information");
        MessageUtil.sendMessage(sender, "&8&m                                    ");
        if (sender.hasPermission("nextlevelpvp.admin")) {
            MessageUtil.sendMessage(sender, "&cAdmin Commands:");
            MessageUtil.sendMessage(sender, "&e/pvp create <mode> <arena> &7- Create a game");
            MessageUtil.sendMessage(sender, "&e/pvp arena create <name> &7- Create arena");
            MessageUtil.sendMessage(sender, "&e/pvp arena setlobby <arena> &7- Set arena lobby");
            MessageUtil.sendMessage(sender, "&e/pvp arena addspawn <arena> &7- Add spawn point");
            MessageUtil.sendMessage(sender, "&e/pvp arena setregion <arena> &7- Set arena region");
            MessageUtil.sendMessage(sender, "&e/pvp arena delete <arena> &7- Delete arena");
            MessageUtil.sendMessage(sender, "&8&m                                    ");
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (plugin.getGameManager().isInGame(player)) {
            MessageUtil.sendPrefixedMessage(player, plugin.getConfig().getString("messages.already-in-game"));
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendPrefixedMessage(player, "&cUsage: /pvp join <mode> [arena]");
            return;
        }

        String modeId = args[1].toLowerCase();
        if (!plugin.getGameManager().getRegisteredModes().contains(modeId)) {
            MessageUtil.sendPrefixedMessage(player, "&cInvalid game mode! Available: " +
                String.join(", ", plugin.getGameManager().getRegisteredModes()));
            return;
        }

        // Try to find existing joinable game
        List<GameMode> joinableGames = plugin.getGameManager().getJoinableGames(modeId);
        GameMode game = null;

        if (!joinableGames.isEmpty()) {
            game = joinableGames.get(0);
        } else {
            // Create new game
            Arena arena = null;
            if (args.length >= 3) {
                arena = plugin.getArenaManager().getArena(args[2]);
                if (arena == null) {
                    MessageUtil.sendPrefixedMessage(player, "&cArena not found!");
                    return;
                }
            } else {
                arena = plugin.getArenaManager().getRandomArena(modeId);
            }

            if (arena == null) {
                MessageUtil.sendPrefixedMessage(player, "&cNo arenas available for this game mode!");
                return;
            }

            if (!arena.isReady()) {
                MessageUtil.sendPrefixedMessage(player, "&cArena is not ready! Missing lobby, spawns, or region.");
                return;
            }

            game = plugin.getGameManager().createGame(modeId, arena);
        }

        if (game == null) {
            MessageUtil.sendPrefixedMessage(player, "&cFailed to create/join game!");
            return;
        }

        if (plugin.getGameManager().joinGame(player, game)) {
            MessageUtil.sendPrefixedMessage(player, "&aYou joined a " + game.getName() + " game!");
        } else {
            MessageUtil.sendPrefixedMessage(player, "&cFailed to join game!");
        }
    }

    private void handleLeave(Player player) {
        if (!plugin.getGameManager().isInGame(player)) {
            MessageUtil.sendPrefixedMessage(player, plugin.getConfig().getString("messages.not-in-game"));
            return;
        }

        plugin.getGameManager().leaveGame(player);
        MessageUtil.sendPrefixedMessage(player, "&aYou left the game!");
    }

    private void handleStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendPrefixedMessage(sender, "&cPlayer not found!");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendPrefixedMessage(sender, "&cPlease specify a player!");
                return;
            }
            target = (Player) sender;
        }

        PlayerStats stats = plugin.getPlayerManager().getStats(target.getUniqueId());

        MessageUtil.sendMessage(sender, "&8&m                                    ");
        MessageUtil.sendMessage(sender, "&6Stats for &e" + target.getName());
        MessageUtil.sendMessage(sender, "&8&m                                    ");
        MessageUtil.sendMessage(sender, "&eKills: &a" + stats.getKills());
        MessageUtil.sendMessage(sender, "&eDeaths: &c" + stats.getDeaths());
        MessageUtil.sendMessage(sender, "&eK/D Ratio: &b" + String.format("%.2f", stats.getKDRatio()));
        MessageUtil.sendMessage(sender, "&eWins: &a" + stats.getWins());
        MessageUtil.sendMessage(sender, "&eLosses: &c" + stats.getLosses());
        MessageUtil.sendMessage(sender, "&eGames Played: &7" + stats.getGamesPlayed());
        MessageUtil.sendMessage(sender, "&eWin Rate: &b" + String.format("%.1f%%", stats.getWinRate()));
        MessageUtil.sendMessage(sender, "&8&m                                    ");
    }

    private void handleArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp arena <create|setlobby|addspawn|setregion|delete>");
            return;
        }

        if (!(sender instanceof Player) && !args[1].equalsIgnoreCase("delete")) {
            MessageUtil.sendPrefixedMessage(sender, "&cOnly players can use this command!");
            return;
        }

        Player player = (sender instanceof Player) ? (Player) sender : null;

        switch (args[1].toLowerCase()) {
            case "create":
                if (args.length < 3) {
                    MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp arena create <name>");
                    return;
                }
                Arena arena = plugin.getArenaManager().createArena(args[2]);
                if (arena != null) {
                    MessageUtil.sendPrefixedMessage(sender, plugin.getConfig().getString("messages.arena-created")
                        .replace("{arena}", args[2]));
                } else {
                    MessageUtil.sendPrefixedMessage(sender, "&cArena already exists!");
                }
                break;

            case "setlobby":
                if (args.length < 3) {
                    MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp arena setlobby <arena>");
                    return;
                }
                Arena lobbyArena = plugin.getArenaManager().getArena(args[2]);
                if (lobbyArena == null) {
                    MessageUtil.sendPrefixedMessage(sender, "&cArena not found!");
                    return;
                }
                lobbyArena.setLobby(player.getLocation());
                plugin.getArenaManager().saveArenas();
                MessageUtil.sendPrefixedMessage(sender, "&aLobby set for arena " + args[2]);
                break;

            case "addspawn":
                if (args.length < 3) {
                    MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp arena addspawn <arena>");
                    return;
                }
                Arena spawnArena = plugin.getArenaManager().getArena(args[2]);
                if (spawnArena == null) {
                    MessageUtil.sendPrefixedMessage(sender, "&cArena not found!");
                    return;
                }
                spawnArena.addSpawnPoint(player.getLocation());
                plugin.getArenaManager().saveArenas();
                MessageUtil.sendPrefixedMessage(sender, "&aSpawn point added to arena " + args[2]);
                break;

            case "setregion":
                if (args.length < 5) {
                    MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp arena setregion <arena> <x1> <y1> <z1>");
                    MessageUtil.sendPrefixedMessage(sender, "&cThen run: /pvp arena setregion <arena> <x2> <y2> <z2>");
                    return;
                }
                // Simplified - use player's current location as both corners
                Arena regionArena = plugin.getArenaManager().getArena(args[2]);
                if (regionArena == null) {
                    MessageUtil.sendPrefixedMessage(sender, "&cArena not found!");
                    return;
                }
                Location pos1 = player.getLocation();
                Location pos2 = player.getLocation().clone().add(50, 20, 50); // Default 50x20x50 region
                regionArena.setRegion(new ArenaRegion(pos1, pos2));
                plugin.getArenaManager().saveArenas();
                MessageUtil.sendPrefixedMessage(sender, "&aRegion set for arena " + args[2]);
                break;

            case "delete":
                if (args.length < 3) {
                    MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp arena delete <arena>");
                    return;
                }
                if (plugin.getArenaManager().deleteArena(args[2])) {
                    MessageUtil.sendPrefixedMessage(sender, plugin.getConfig().getString("messages.arena-deleted")
                        .replace("{arena}", args[2]));
                } else {
                    MessageUtil.sendPrefixedMessage(sender, "&cArena not found!");
                }
                break;

            default:
                MessageUtil.sendPrefixedMessage(sender, "&cUnknown arena subcommand!");
                break;
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp list <modes|arenas|games>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "modes":
                MessageUtil.sendMessage(sender, "&8&m                                    ");
                MessageUtil.sendMessage(sender, "&6Available Game Modes:");
                for (String mode : plugin.getGameManager().getRegisteredModes()) {
                    MessageUtil.sendMessage(sender, "&e- " + mode);
                }
                MessageUtil.sendMessage(sender, "&8&m                                    ");
                break;

            case "arenas":
                MessageUtil.sendMessage(sender, "&8&m                                    ");
                MessageUtil.sendMessage(sender, "&6Available Arenas:");
                for (Arena arena : plugin.getArenaManager().getArenas()) {
                    String status = arena.isReady() ? "&a✓" : "&c✗";
                    MessageUtil.sendMessage(sender, status + " &e" + arena.getName() +
                        " &7(" + arena.getMinPlayers() + "-" + arena.getMaxPlayers() + " players)");
                }
                MessageUtil.sendMessage(sender, "&8&m                                    ");
                break;

            case "games":
                MessageUtil.sendMessage(sender, "&8&m                                    ");
                MessageUtil.sendMessage(sender, "&6Active Games:");
                for (GameMode game : plugin.getGameManager().getActiveGames()) {
                    MessageUtil.sendMessage(sender, "&e" + game.getName() + " &7on &e" + game.getArena().getName() +
                        " &7- &a" + game.getPlayerCount() + " &7players - " + game.getState());
                }
                MessageUtil.sendMessage(sender, "&8&m                                    ");
                break;

            default:
                MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp list <modes|arenas|games>");
                break;
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendPrefixedMessage(sender, "&cUsage: /pvp create <mode> <arena>");
            return;
        }

        String modeId = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(args[2]);

        if (!plugin.getGameManager().getRegisteredModes().contains(modeId)) {
            MessageUtil.sendPrefixedMessage(sender, "&cInvalid game mode!");
            return;
        }

        if (arena == null) {
            MessageUtil.sendPrefixedMessage(sender, "&cArena not found!");
            return;
        }

        if (!arena.isReady()) {
            MessageUtil.sendPrefixedMessage(sender, "&cArena is not ready!");
            return;
        }

        GameMode game = plugin.getGameManager().createGame(modeId, arena);
        if (game != null) {
            MessageUtil.sendPrefixedMessage(sender, "&aGame created successfully!");
        } else {
            MessageUtil.sendPrefixedMessage(sender, "&cFailed to create game!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "join", "leave", "stats", "list"));
            if (sender.hasPermission("nextlevelpvp.admin")) {
                completions.addAll(Arrays.asList("arena", "create"));
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                case "create":
                    completions.addAll(plugin.getGameManager().getRegisteredModes());
                    break;
                case "list":
                    completions.addAll(Arrays.asList("modes", "arenas", "games"));
                    break;
                case "arena":
                    if (sender.hasPermission("nextlevelpvp.admin")) {
                        completions.addAll(Arrays.asList("create", "setlobby", "addspawn", "setregion", "delete"));
                    }
                    break;
                case "stats":
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
                    break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("create")) {
                completions.addAll(plugin.getArenaManager().getArenas().stream()
                    .map(Arena::getName)
                    .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("arena") &&
                !args[1].equalsIgnoreCase("create")) {
                completions.addAll(plugin.getArenaManager().getArenas().stream()
                    .map(Arena::getName)
                    .collect(Collectors.toList()));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
