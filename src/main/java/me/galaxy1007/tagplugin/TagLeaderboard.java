package me.galaxy1007.tagplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class TagLeaderboard {
    private Map<String, Integer> tagCounts;

    public TagLeaderboard() {
        tagCounts = new HashMap<>();
    }

    public void incrementTagCount(String playerName) {
        tagCounts.put(playerName, tagCounts.getOrDefault(playerName, 0) + 1);
    }

    public void clear() {
        tagCounts.clear();
    }

    public void sendLeaderboard(CommandSender sender) {
        List<Map.Entry<String, Integer>> leaderboard = getSortedLeaderboard();

        sender.sendMessage(ChatColor.YELLOW + "Tag Leaderboard:");

        int position = 1;
        for (Map.Entry<String, Integer> entry : leaderboard) {
            String playerName = entry.getKey();
            int tagCount = entry.getValue();

            sender.sendMessage(ChatColor.YELLOW + String.format("%d. %s - %d tags", position, playerName, tagCount));
            position++;
        }
    }

    private List<Map.Entry<String, Integer>> getSortedLeaderboard() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(tagCounts.entrySet());

        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        return entries;
    }
    public void sendTopTaggedPlayers(CommandSender sender, int count) {
        List<Map.Entry<String, Integer>> leaderboard = getSortedLeaderboard();

        sender.sendMessage(ChatColor.YELLOW + "Top " + count + " Most Tagged Players:");

        int position = 1;
        for (int i = 0; i < count && i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            String playerName = entry.getKey();
            int tagCount = entry.getValue();

            sender.sendMessage(ChatColor.YELLOW + String.format("%d. %s - %d tags", position, playerName, tagCount));
            position++;
        }
    }

}