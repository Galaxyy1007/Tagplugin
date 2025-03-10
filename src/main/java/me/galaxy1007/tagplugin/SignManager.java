package me.galaxy1007.tagplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignManager {
    private final Tagplugin plugin;
    private final List<Location> signLocations = new ArrayList<>();
    private final Set<Player> registeredPlayers = new HashSet<>();
    private final int maxPlayers = 10;

    public SignManager(Tagplugin plugin) {
        this.plugin = plugin;
        loadSignLocations();
    }

    public void createTagSign(Sign sign) {
        SignSide front = sign.getSide(Side.FRONT);

        front.setLine(0, ChatColor.BLUE + "" + ChatColor.BOLD + "[Minigame]");
        front.setLine(1, ChatColor.YELLOW + "" + ChatColor.BOLD + "Tag");
        front.setLine(2, "0 / " + maxPlayers);
        front.setLine(3, ChatColor.GREEN + "Click to Join");

        sign.update();

        // Save the sign location
        Location loc = sign.getLocation();
        signLocations.add(loc);
        saveSignLocations();
    }

    public void updateJoinSign(Sign sign) {
        SignSide front = sign.getSide(Side.FRONT);
        front.setLine(2, registeredPlayers.size() + "/" + maxPlayers);
        sign.update();
    }

    public void resetSigns() {
        registeredPlayers.clear();

        for (Location loc : signLocations) {
            Block block = loc.getBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                SignSide front = sign.getSide(Side.FRONT);
                front.setLine(2, "0 / " + maxPlayers);
                sign.update();
            }
        }
    }

    public boolean isTagSign(Sign sign) {
        SignSide front = sign.getSide(Side.FRONT);
        String line1 = ChatColor.stripColor(front.getLine(0));
        String line2 = ChatColor.stripColor(front.getLine(1));

        return line1.equalsIgnoreCase("[Minigame]") && line2.equalsIgnoreCase("Tag");
    }

    public boolean addPlayer(Player player) {
        if (registeredPlayers.contains(player)) {
            return false;
        }

        if (registeredPlayers.size() >= maxPlayers) {
            return false;
        }

        registeredPlayers.add(player);
        updateSignsWithPlayerCount();
        return true;
    }

    public void updateSignsWithPlayerCount() {
        for (Location loc : signLocations) {
            Block block = loc.getBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                updateJoinSign(sign);
            }
        }
    }

    private void saveSignLocations() {
        List<String> locationStrings = new ArrayList<>();
        for (Location loc : signLocations) {
            locationStrings.add(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        plugin.getConfig().set("signs", locationStrings);
        plugin.saveConfig();
    }

    private void loadSignLocations() {
        List<String> locationStrings = plugin.getConfig().getStringList("signs");
        for (String str : locationStrings) {
            String[] parts = str.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world != null) {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                signLocations.add(new Location(world, x, y, z));
            }
        }
    }

    public Set<Player> getRegisteredPlayers() {
        return registeredPlayers;
    }

    public List<Location> getSignLocations() {
        return signLocations;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
