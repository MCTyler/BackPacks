package at.michael1011.backpacks.commads;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static at.michael1011.backpacks.Crafting.availableList;

class GiveCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if(args.length <= 1) {
            return filterList(args[0], availableList);

        } else {
            List<String> players = new ArrayList<>();

            for(Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }

            return filterList(args[1], players);

        }

    }

    private List<String> filterList(String arg, List<String> rawData) {
        if(!arg.equals("")) {
            List<String> filtered = new ArrayList<>();

            for(String player : rawData) {
                if(player.toLowerCase().contains(arg.toLowerCase())) {
                    filtered.add(player);
                }
            }

            return filtered;

        } else {
            return rawData;
        }

    }

}
