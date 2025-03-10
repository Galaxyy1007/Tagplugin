package me.galaxy1007.tagplugin;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TagEventListener implements Listener {
    private final Tagplugin plugin;
    private final GameManager gameManager;
    private final SignManager signManager;

    public TagEventListener(Tagplugin plugin, GameManager gameManager, SignManager signManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.signManager = signManager;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase("[Minigame]") && event.getLine(1).equalsIgnoreCase("Tag")) {
            Sign sign = (Sign) event.getBlock().getState();
            signManager.createTagSign(sign);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Join sign created successfully!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();

                if (signManager.isTagSign(sign)) {
                    event.setCancelled(true);
                    Player player = event.getPlayer();

                    if (signManager.getRegisteredPlayers().contains(player)) {
                        if (player.isOp()) {
                            gameManager.startCountdown();
                        } else {
                            player.sendMessage(ChatColor.RED + "You are already registered!");
                        }
                    } else if (signManager.getRegisteredPlayers().size() >= signManager.getMaxPlayers()) {
                        player.sendMessage(ChatColor.RED + "The game is full!");
                    } else {
                        if (signManager.addPlayer(player)) {
                            player.sendMessage(ChatColor.GREEN + "You have joined the game!");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();

            if (signManager.isTagSign(sign)) {
                if (!event.getPlayer().isOp()) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot break this sign!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (gameManager.isGameRunning()) {
            gameManager.addPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (gameManager.isGameRunning()) {
            gameManager.removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player tagged = (Player) event.getEntity();
            Player tagger = (Player) event.getDamager();

            if (gameManager.getItPlayer() == tagger) {
                gameManager.tagPlayer(tagger, tagged);
            }
        }
    }
}
