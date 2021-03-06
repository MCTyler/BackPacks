package at.michael1011.backpacks.commads;

import at.michael1011.backpacks.BackPack;
import at.michael1011.backpacks.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static at.michael1011.backpacks.Crafting.backPacksItems;
import static at.michael1011.backpacks.Main.messages;
import static at.michael1011.backpacks.Main.prefix;
import static at.michael1011.backpacks.commads.ListBackPacks.getBackPacks;

public class Give implements CommandExecutor {

    private static final String path = "Help.bpgive.";

    public Give(Main main) {
        PluginCommand command = main.getCommand("bpgive");

        command.setExecutor(this);
        command.setTabCompleter(new GiveCompleter());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission("backpacks.give") || sender.hasPermission("backpacks.*")) {
            if(args.length == 1) {
                if(sender instanceof Player) {
                    giveBackPack(sender, (Player) sender, args[0]);

                } else {
                    sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                            messages.getString("Help.onlyPlayers")));

                    sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                            messages.getString(path+"onlyPlayersAlternative")));
                }

            } else if(args.length == 2) {
                String targetName = args[1];

                Player target = Bukkit.getServer().getPlayer(targetName);

                if(target != null) {
                    giveBackPack(sender, target, args[0]);

                } else {
                    sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                            messages.getString("Help.playerNotFound").replaceAll("%target%", targetName)));
                }

            } else {
                Map<String, Object> syntaxError = messages.getConfigurationSection(path+"syntaxError").getValues(true);

                for(Map.Entry<String, Object> error : syntaxError.entrySet()) {
                    sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', (String) error.getValue()));
                }

            }

        } else {
            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                    messages.getString("Help.noPermission")));
        }

        return true;
    }

    private static void giveBackPack(CommandSender sender, Player target, String backpack) {
        ItemStack item = null;

        for (Map.Entry<BackPack, ItemStack> entry : backPacksItems.entrySet()) {
            if (entry.getKey().getName().equals(backpack)) {
                item = entry.getValue();
            }
        }

        if(item != null) {
            target.getInventory().addItem(item);

            String targetName = target.getName();

            if (!sender.getName().equals(targetName)) {
                sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                        messages.getString(path + "gaveBackPack").replaceAll("%backpack%", backpack)
                                .replaceAll("%target%", targetName)));
            }

        } else {
            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                    messages.getString("Help.backPackNotFound").replaceAll("%backpack%", backpack)));

            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                    messages.getString("Help.backPackNotFoundAvailable")
                            .replaceAll("%backpacks%", getBackPacks())));
        }

    }

}
