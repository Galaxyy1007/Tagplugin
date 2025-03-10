package me.galaxy1007.tagplugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {
    private final Tagplugin plugin;
    private final SignManager signManager;
    private final ConfigManager configManager;

    private List<Player> players;
    private Player itPlayer;
    private Map<Player, Long> tagCooldowns;
    private BossBar gameTimerBar;
    private Set<Player> activePlayers;
    private BukkitRunnable bossBarTask;
    private boolean isGameRunning = false;

    public GameManager(Tagplugin plugin, SignManager signManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.signManager = signManager;
        this.configManager = configManager;

        this.players = new ArrayList<>();
        this.tagCooldowns = new HashMap<>();
        this.activePlayers = new HashSet<>();
        this.gameTimerBar = Bukkit.createBossBar("Game Time Remaining", BarColor.GREEN, BarStyle.SOLID);
        this.gameTimerBar.setVisible(false);
    }

    public void startCountdown() {
        if (isGameRunning) {
            return;
        }

        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    startGame();
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(ChatColor.RED + "Starting in", ChatColor.YELLOW + "" + countdown + " seconds", 5, 20, 5);
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20); // 20 ticks is one second
    }

    public void startGame() {
        if (isGameRunning) {
            return;
        }

        players.clear();
        players.addAll(signManager.getRegisteredPlayers());

//        if (players.size() < 2) {
//            Bukkit.broadcastMessage(ChatColor.RED + "There are too few players in this game!");
//            return;
//        }

        isGameRunning = true;

        for (Player player : players) {
            player.setPlayerListName(player.getName());

            // Send game start message
            String gameStartMessage = configManager.getConfigString("game_start_message");
            if (gameStartMessage != null && !gameStartMessage.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', gameStartMessage));
            } else {
                player.sendMessage(ChatColor.GREEN + "The tag game has started!");
            }

            // Send game settings
            player.sendMessage(ChatColor.GREEN + "Game settings:");
            player.sendMessage(ChatColor.YELLOW + "Speed: " + configManager.getSpeed() + "x");
            player.sendMessage(ChatColor.YELLOW + "Border Size: " + configManager.getBorderSize() + " chunks");
            player.sendMessage(ChatColor.YELLOW + "Duration: " + configManager.getDuration() + " minutes");

            // Set world border
            setWorldBorder(player);

            // Apply speed effect
            applySpeedEffect(player);

            // Set up boss bar
            setupBossBar(player);
        }

        // Select the first "it" player
        selectNewItPlayer();
    }

    public void stopGame() {
        if (!isGameRunning) {
            return;
        }

        isGameRunning = false;

        // Reset player names in the tab list
        for (Player player : players) {
            player.setPlayerListName(player.getName());
        }

        // Reset registered players
        signManager.resetSigns();

        // Remove speed effects
        for (Player player : activePlayers) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
        activePlayers.clear();

        // Reset world border
        World world = Bukkit.getWorlds().get(0);
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setSize(6000);

        // Remove boss bar
        if (gameTimerBar != null) {
            gameTimerBar.removeAll();
            gameTimerBar.setVisible(false);
        }

        // Cancel boss bar task
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }

        // Clear game state
        players.clear();
        itPlayer = null;
    }

    public boolean tagPlayer(Player tagger, Player tagged) {
        // Check if the tagger is the "it" player
        if (tagger != itPlayer) {
            return false;
        }

        // Check tagger cooldown
        if (tagCooldowns.containsKey(tagger)) {
            long lastTagTime = tagCooldowns.get(tagger);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTagTime < configManager.getTagCooldownDuration()) {
                tagger.sendMessage(ChatColor.RED + "You are on cooldown. Wait before tagging again.");
                return false;
            }
        }

        // Check tagged player cooldown
        if (tagCooldowns.containsKey(tagged)) {
            long lastTagTime = tagCooldowns.get(tagged);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTagTime < configManager.getTagCooldownDuration()) {
                tagger.sendMessage(ChatColor.RED + "This player cannot be tagged yet. Please wait.");
                return false;
            }
        }

        // Update tag cooldowns
        tagCooldowns.put(tagger, System.currentTimeMillis());
        tagCooldowns.put(tagged, System.currentTimeMillis());

        // Reset old "it" player name
        itPlayer.setPlayerListName(itPlayer.getName());

        // Set new "it" player
        itPlayer = tagged;
        tagged.setPlayerListName(ChatColor.BOLD + "" + ChatColor.RED + tagged.getName());

        // Send messages
        sendTagMessages(tagger, tagged);

        return true;
    }

    private void sendTagMessages(Player tagger, Player tagged) {
        // Message to tagged player
        String tagMessage = configManager.getConfigString("tag_message");
        if (tagMessage != null && !tagMessage.isEmpty()) {
            tagged.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            tagMessage.replace("{tagged}", tagged.getName()))));
        } else {
            tagged.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "You got tagged!"));
        }

        // Message to tagger
        String taggerMessage = configManager.getConfigString("tagger_message");
        if (taggerMessage != null && !taggerMessage.isEmpty()) {
            tagger.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            taggerMessage.replace("{tagged}", tagged.getName()))));
        } else {
            tagger.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GREEN + "You tagged " + tagged.getName() + "!"));
        }

        // Console message
        String consoleMessage = configManager.getConfigString("console_message");
        if (consoleMessage != null && !consoleMessage.isEmpty()) {
            consoleMessage = consoleMessage.replace("{tagger}", tagger.getName())
                    .replace("{tagged}", tagged.getName());
            plugin.getServer().getConsoleSender().sendMessage(
                    ChatColor.translateAlternateColorCodes('&', consoleMessage));
        }

        // Global message to other players
        String globalMessage = configManager.getConfigString("global_message");
        if (globalMessage != null && !globalMessage.isEmpty()) {
            globalMessage = globalMessage.replace("{tagger}", tagger.getName())
                    .replace("{tagged}", tagged.getName());
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != tagger && player != tagged) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', globalMessage));
                }
            }
        }
    }

    private void selectNewItPlayer() {
        if (players.isEmpty()) {
            return;
        }

        Random random = new Random();
        int index = random.nextInt(players.size());

        itPlayer = players.get(index);

        for (Player player : players) {
            if (player == itPlayer) {
                player.setPlayerListName(ChatColor.RED + player.getName());

                String tagMessage = configManager.getConfigString("tag_message");
                if (tagMessage != null && !tagMessage.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            tagMessage.replace("{tagged}", player.getName())));
                } else {
                    player.sendMessage(ChatColor.RED + "You got tagged!");
                }
            } else {
                player.setPlayerListName(player.getName());
            }
        }
    }

    private void setWorldBorder(Player player) {
        World world = player.getWorld();
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(player.getLocation());
        worldBorder.setSize(configManager.getBorderSize() * 16); // Convert chunks to blocks
        worldBorder.setWarningDistance(5);
    }

    private void applySpeedEffect(Player player) {
        if (!activePlayers.contains(player)) {
            activePlayers.add(player);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    configManager.getDuration() * 60 * 20,
                    Math.min(configManager.getSpeed() - 1, 4),
                    false, false
            ));
        }
    }

    private void setupBossBar(Player player) {
        gameTimerBar.setProgress(1.0);
        gameTimerBar.setTitle("Game Time Remaining: " + configManager.getDuration() + " minutes");
        gameTimerBar.addPlayer(player);
        startBossBarCountdown();
    }

    private void startBossBarCountdown() {
        if (bossBarTask != null) {
            bossBarTask.cancel();
        }

        bossBarTask = new BukkitRunnable() {
            int remainingTime = configManager.getDuration() * 60;

            @Override
            public void run() {
                if (remainingTime <= 0) {
                    stopGame();
                    cancel();
                    return;
                }

                double progress = (double) remainingTime / (configManager.getDuration() * 60);
                gameTimerBar.setProgress(Math.max(0, progress));
                gameTimerBar.setTitle("Game Time Remaining: " + (remainingTime / 60) + " minutes " + (remainingTime % 60) + " seconds");
                remainingTime--;
            }
        };

        bossBarTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);

        if (player == itPlayer) {
            itPlayer = null;
            if (!players.isEmpty()) {
                selectNewItPlayer();
            }
        }
    }

    public Player getItPlayer() {
        return itPlayer;
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }
}
