package com.nextlevel.pvp.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public static String getPrefix() {
        return colorize("&8[&cNext&fLevel&bPvP&8] &r");
    }

    public static void sendPrefixedMessage(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + colorize(message));
    }
}
