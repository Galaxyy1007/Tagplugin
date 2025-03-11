package me.galaxy1007.tagplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagGameTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("taggame")) {
            return null;
        }

        List<String> subCommands = Arrays.asList("stop", "reload", "removesign", "resetregisters", "help");
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Filter de subcommando’s op basis van de invoer
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
            return completions;
        }

        return null;
    }
}
