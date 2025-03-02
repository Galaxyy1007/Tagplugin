package me.galaxy1007.tagplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.BlockState;
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
    private TagLeaderboard leaderboard;
    private List<Player> players;
    private Player itPlayer;
    private FileConfiguration config;
    private long tagCooldownDuration;
    private Map<Player, Long> tagCooldowns; // Declare the tagCooldowns variable here


    @Override
    public void onEnable() {
        players = new ArrayList<>();
        itPlayer = null;
        saveDefaultConfig();
        config = getConfig();
        tagCooldowns = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);

        leaderboard = new TagLeaderboard();
    }

    @Override
    public void onDisable() {
        players.clear();
        leaderboard.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("starttag")) {
            if (itPlayer != null) {
                sender.sendMessage(ChatColor.RED + "The tag game is already running!");
                return true;
            }

            if (sender.isOp()) {
                startGame();
            } else if (Bukkit.getOnlinePlayers().size() < 2) {
                sender.sendMessage(ChatColor.RED + "There must be at least 2 players online to start the tag game.");
                return true;
            }
            startGame();
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
        } else if (command.getName().equalsIgnoreCase("tagleaderboard")) {
            if (itPlayer == null) {
                sender.sendMessage(ChatColor.RED + "There is no game currently running.");
                return true;
            }

            leaderboard.sendLeaderboard(sender);
            return true;
        } else if (command.getName().equalsIgnoreCase("leaderboard")) {
            if (itPlayer == null) {
                sender.sendMessage(ChatColor.RED + "There is no game currently running.");
                return true;
            }

            leaderboard.sendTopTaggedPlayers(sender, 5);
            return true;
        }

        return false;
    }
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase("[Minigame]") && event.getLine(1).equalsIgnoreCase("Tikkertje")) {
            if (event.getBlock().getState() instanceof Sign) {
                Sign sign = (Sign) event.getBlock().getState();

                // Gebruik de FRONT zijde van het bord (voorzijde)
                sign.getSide(Side.FRONT).setLine(0, ChatColor.BLUE + "" + ChatColor.BOLD + "[Minigame]");
                sign.getSide(Side.FRONT).setLine(1, ChatColor.YELLOW + "" + ChatColor.BOLD + "Tikkertje");

                sign.update(); // Slaat de wijzigingen op

                event.getPlayer().sendMessage(ChatColor.GREEN + "Join sign created successfully!");
            }
        }
    }
    @EventHandler
    public void onSignEdit(SignChangeEvent event) {
        if (event.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) event.getBlock().getState();

            // Check of dit bord al een Minigame-bord is
            String line1 = ChatColor.stripColor(sign.getSide(Side.BACK).getLine(0));
            String line2 = ChatColor.stripColor(sign.getSide(Side.BACK).getLine(1));

            if (line1.equalsIgnoreCase("[Minigame]")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot edit this sign!");
            } else if (line2.equalsIgnoreCase("Tikkertje")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot edit this sign!");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Sign) {
            Sign sign = (Sign) event.getClickedBlock().getState();

            String line1 = sign.getSide(Side.BACK).getLine(0);
            String line2 = sign.getSide(Side.BACK).getLine(1);

            if (ChatColor.stripColor(line1).equalsIgnoreCase("[Minigame]") &&
                    ChatColor.stripColor(line2).equalsIgnoreCase("Tikkertje")) {

                Player player = event.getPlayer();
                if (!players.contains(player)) {
                    players.add(player);
                    player.sendMessage(ChatColor.GREEN + "You have joined the tag game!");
                } else {
                    player.sendMessage(ChatColor.RED + "You are already in the game!");
                }
            }
            Player player = event.getPlayer();
            if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE)) {
                event.setCancelled(true);
            } else {
                event.setCancelled(false);
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
        }.runTaskTimer(this, 0, 20); // 20 ticks is een seconde
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (itPlayer != null) {
            players.add(player);
            player.setPlayerListName(player.getName());

            String gameStartMessage = config.getString("game_start_message");
            if (gameStartMessage != null && !gameStartMessage.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', gameStartMessage));
            } else {
                player.sendMessage(ChatColor.GREEN + "The tag game has started!");
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
                    tagCooldownDuration = config.getLong("tag_cooldown", 30) * 1000;

                    long cooldownTime = tagCooldownDuration;

                    if (currentTime - lastTagTime < cooldownTime) {
                        tagger.sendMessage(ChatColor.RED + "You must wait before tagging another player.");
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
                    tagged.sendMessage(ChatColor.translateAlternateColorCodes('&', tagMessage.replace("{tagged}", tagged.getName())));
                } else {
                    tagged.sendMessage(ChatColor.RED + "You got tagged!");
                }

                String taggerMessage = config.getString("tagger_message");
                if (taggerMessage != null && !taggerMessage.isEmpty()) {
                    tagger.sendMessage(ChatColor.translateAlternateColorCodes('&', taggerMessage.replace("{tagged}", tagged.getName())));
                } else {
                    tagger.sendMessage(ChatColor.GREEN + "You tagged " + tagged.getName() + "!");
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

                if (tagger != tagged) {
                    leaderboard.incrementTagCount(tagger.getName());
                }

                leaderboard.sendLeaderboard(Bukkit.getServer().getConsoleSender());
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
        leaderboard.clear();
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

