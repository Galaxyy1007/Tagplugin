package me.galaxy1007.tagplugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Tagplugin extends JavaPlugin implements Listener {
    private List<Player> players;
    private Player itPlayer;
    private FileConfiguration config;
    private long tagCooldownDuration;
    private Map<Player, Long> tagCooldowns;


    @Override
    public void onEnable() {
        players = new ArrayList<>();
        itPlayer = null;
        saveDefaultConfig();
        config = getConfig();
        tagCooldowns = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("removesign").setExecutor(this);

    }

    @Override
    public void onDisable() {
        players.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("starttag")) {
            if (itPlayer != null) {
                sender.sendMessage(ChatColor.RED + "The tag game is already running!");
                return true;
            }

            if (sender.isOp()) {
                startCountdown();
            } else if (Bukkit.getOnlinePlayers().size() < 2) {
                sender.sendMessage(ChatColor.RED + "There must be at least 2 players online to start the tag game.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Starting the game...");
            return true;
        } else if (command.getName().equalsIgnoreCase("stoptag")) {
            if (itPlayer == null) {
                sender.sendMessage(ChatColor.RED + "The tag game is not currently running.");
                return true;
            }

            stopGame();
            sender.sendMessage(ChatColor.GREEN + "The tag game has been stopped.");
            return true;
        } else if (command.getName().equalsIgnoreCase("removesign")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                    return true;
                }

                Player player = (Player) sender;
                Block targetBlock = player.getTargetBlockExact(3);
                if (targetBlock != null && targetBlock.getState() instanceof Sign) {
                    targetBlock.setType(Material.AIR);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Sign removed successfully!"));
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No sign found within 3 blocks radius!"));
                }
                return true;
            }
            return false;
        }
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase("[Minigame]") && event.getLine(1).equalsIgnoreCase("Tikkertje")) {
            Sign sign = (Sign) event.getBlock().getState();
            SignSide front = sign.getSide(Side.FRONT);

            front.setLine(0, ChatColor.BLUE + "" + ChatColor.BOLD + "[Minigame]");
            front.setLine(1, ChatColor.YELLOW + "" + ChatColor.BOLD + "Tikkertje");

            sign.update();
            event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Join sign created successfully!"));
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                SignSide front = sign.getSide(Side.FRONT);

                String line1 = ChatColor.stripColor(front.getLine(0));
                String line2 = ChatColor.stripColor(front.getLine(1));

                if (line1.equalsIgnoreCase("[Minigame]") && line2.equalsIgnoreCase("Tikkertje")) {
                    event.setCancelled(true);
                    Player player = event.getPlayer();
                    player.performCommand("starttag");
                    sign.update(true, true);
                }
            }
        }
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            SignSide front = sign.getSide(Side.FRONT);

            String line1 = ChatColor.stripColor(front.getLine(0));
            String line2 = ChatColor.stripColor(front.getLine(1));

            if (line1.equalsIgnoreCase("[Minigame]") && line2.equalsIgnoreCase("Tikkertje")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot break this sign!");
                sign.update(true, true);
            }
        }
    }
    private void startCountdown() {
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
        }.runTaskTimer(this, 0, 20); // 20 ticks = 1 second
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (itPlayer != null) {
            players.add(player);
            player.setPlayerListName(player.getName());

            String gameStartMessage = config.getString("game_start_message");
            if (gameStartMessage != null && !gameStartMessage.isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', gameStartMessage)));
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "The tag game has started!"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        players.remove(player);

        if (player == itPlayer) {
            itPlayer = null;
            if (players.size() > 0) {
                selectNewItPlayer();
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player tagged = (Player) event.getEntity();
            Player tagger = (Player) event.getDamager();

            if (itPlayer == tagger) {
                if (tagCooldowns.containsKey(tagger)) {
                    long lastTagTime = tagCooldowns.get(tagger);
                    long currentTime = System.currentTimeMillis();
                    config = getConfig(); // Load the config
                    tagCooldownDuration = config.getLong("tag_cooldown", 5) * 1000;

                    long cooldownTime = tagCooldownDuration;

                    if (currentTime - lastTagTime < cooldownTime) {
                        tagger.sendMessage(ChatColor.RED + "You are on " + cooldownTime + " seconds cooldown!");
                        return;
                    }
                }

                if (tagCooldowns.containsKey(tagged)) {
                    long lastTagTime = tagCooldowns.get(tagged);
                    long currentTime = System.currentTimeMillis();
                    config = getConfig();
                    tagCooldownDuration = config.getLong("tag_cooldown", 30) * 1000;

                    long cooldownTime = tagCooldownDuration;

                    if (currentTime - lastTagTime < cooldownTime) {
                        tagger.sendMessage(ChatColor.RED + "This player cannot be tagged yet. Please wait.");
                        return;
                    }
                }

                tagPlayer(tagged);
                tagCooldowns.put(tagger, System.currentTimeMillis());
                tagCooldowns.put(tagged, System.currentTimeMillis());

                String tagMessage = config.getString("tag_message");
                if (tagMessage != null && !tagMessage.isEmpty()) {
                    tagged.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', tagMessage.replace("{tagged}", tagged.getName()))));
                } else {
                    tagged.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "You got tagged!"));
                }

                String taggerMessage = config.getString("tagger_message");
                if (taggerMessage != null && !taggerMessage.isEmpty()) {
                    tagger.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', taggerMessage.replace("{tagged}", tagged.getName()))));
                } else {
                    tagger.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "You tagged " + tagged.getName() + "!"));
                }

                String consoleMessage = config.getString("console_message");
                if (consoleMessage != null && !consoleMessage.isEmpty()) {
                    consoleMessage = consoleMessage.replace("{tagger}", tagger.getName()).replace("{tagged}", tagged.getName());
                    getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', consoleMessage));
                }
                String globalMessage = config.getString("global_message");
                if (globalMessage != null && !globalMessage.isEmpty()) {
                    globalMessage = globalMessage.replace("{tagger}", tagger.getName()).replace("{tagged}", tagged.getName());
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player != tagger && player != tagged) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', globalMessage));
                        }
                    }
                }
            }
        }
    }

    private void startGame() {
        players.clear();
        players.addAll(Bukkit.getOnlinePlayers());

        for (Player player : players) {
            player.setPlayerListName(player.getName());

            String gameStartMessage = config.getString("game_start_message");
            if (gameStartMessage != null && !gameStartMessage.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', gameStartMessage));
            } else {
                player.sendMessage(ChatColor.GREEN + "The tag game has started!");
            }
        }

        selectNewItPlayer();
    }
    private void stopGame() {
        for (Player player : players) {
            player.setPlayerListName(player.getName());
        }

        players.clear();
        itPlayer = null;
    }

    private void selectNewItPlayer() {
        Random random = new Random();
        int index = random.nextInt(players.size());

        itPlayer = players.get(index);

        for (Player player : players) {
            if (player == itPlayer) {
                player.setPlayerListName(ChatColor.RED + player.getName());

                String tagMessage = config.getString("tag_message");
                if (tagMessage != null && !tagMessage.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', tagMessage.replace("{tagged}", player.getName())));
                } else {
                    player.sendMessage(ChatColor.RED + "You got tagged!");
                }
            } else {
                player.setPlayerListName(player.getName());
            }
        }
    }

    private void tagPlayer(Player tagger) {
        if (tagger == itPlayer) {
            return;
        }

        itPlayer.setPlayerListName(itPlayer.getName());
        tagger.setPlayerListName(ChatColor.RED + tagger.getName());

        itPlayer = tagger;
    }
}

