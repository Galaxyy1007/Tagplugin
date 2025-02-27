package me.galaxy1007.tagplugin;

import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Tagplugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<Player> players = new HashSet<>();
    private final Map<Player, ItemStack[]> storedInventories = new HashMap<>();
    private final Map<Player, ItemStack[]> storedArmor = new HashMap<>();
    private Player firstPlayer;
    private int gameDuration = 300; // 5 minuten
    private double playerSpeed = 0.2; // Normale snelheid
    private boolean gameRunning = false;
    private Player tagger = null;
    private final int maxPlayers = 10;
    private Location signLocation = null;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("tikkertje").setExecutor(this);
    }

    @Override
    public void onDisable() {
        resetGame();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.REDSTONE_TORCH && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Instellingen")) {
            openGameSettingsMenu(player);
            event.setCancelled(true);
        }

        if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Sign) {
            Sign sign = (Sign) event.getClickedBlock().getState();

            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[Minigame]") &&
                    ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("Tikkertje")) {

                if (!players.contains(player) && players.size() < maxPlayers) {
                    players.add(player);
                    storePlayerInventory(player);
                    giveGameItem(player);
                    player.sendMessage(ChatColor.GREEN + "Je hebt je ingeschreven voor tikkertje!");

                    if (players.size() == 1) {
                        firstPlayer = player;
                    }

                    updateSign();
                } else {
                    player.sendMessage(ChatColor.RED + "Het spel zit vol!");
                }

                event.setCancelled(true);
            }
        }
    }

    private void storePlayerInventory(Player player) {
        storedInventories.put(player, player.getInventory().getContents());
        storedArmor.put(player, player.getInventory().getArmorContents());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    private void giveGameItem(Player player) {
        ItemStack torch = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = torch.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Instellingen");
        torch.setItemMeta(meta);
        player.getInventory().addItem(torch);
    }

    private void updateSign() {
        if (signLocation != null && signLocation.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) signLocation.getBlock().getState();
            sign.setLine(0, ChatColor.DARK_BLUE + "[Minigame]");
            sign.setLine(1, ChatColor.GREEN + "Tikkertje");
            sign.setLine(2, ChatColor.WHITE + "" + ChatColor.BOLD + players.size() + " / " + maxPlayers);
            sign.update(true);
        }
    }

    public void openGameSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "Tikkertje Instellingen");
        inv.setItem(0, createItem("Snelheid verhogen", Material.FEATHER));
        inv.setItem(1, createItem("Duur aanpassen", Material.CLOCK));
        inv.setItem(3, createItem("Reset waarden", Material.BARRIER));
        inv.setItem(7, createItem("Stop het spel", Material.REDSTONE));
        inv.setItem(8, createItem("Start het spel", Material.REDSTONE_TORCH));

        player.openInventory(inv);
    }

    private ItemStack createItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Tikkertje Instellingen") && event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            event.setCancelled(true);

            if (event.getCurrentItem() != null) {
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                if (itemName.contains("Snelheid verhogen")) {
                    playerSpeed = 0.3;
                    player.sendMessage(ChatColor.GREEN + "Speler snelheid verhoogd!");
                } else if (itemName.contains("Duur aanpassen")) {
                    gameDuration = 300;
                    player.sendMessage(ChatColor.GREEN + "Spel duurt nu 5 minuten.");
                } else if (itemName.contains("Reset waarden")) {
                    playerSpeed = 0.2;
                    gameDuration = 300;
                    player.sendMessage(ChatColor.GREEN + "Waarden teruggezet naar standaard.");
                } else if (itemName.contains("Stop het spel")) {
                    resetGame();
                    player.closeInventory();
                    Bukkit.broadcastMessage(ChatColor.RED + "Het spel is gestopt!");
                } else if (itemName.contains("Start het spel")) {
                    startGame();
                    player.closeInventory();
                }
            }
        }
    }

    private void startGame() {
        if (players.size() >= 2) {
            gameRunning = true;
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Tikkertje begint over 10 seconden!");

            new BukkitRunnable() {
                int countdown = 10;

                @Override
                public void run() {
                    if (countdown > 0) {
                        for (Player player : players) {
                            player.sendTitle(ChatColor.RED + "Start over", ChatColor.WHITE + "" + countdown + " seconden", 5, 20, 5);
                        }
                        countdown--;
                    } else {
                        cancel();
                    }
                }
            }.runTaskTimer(this, 0, 20);
        } else {
            firstPlayer.sendMessage(ChatColor.RED + "Minimaal 2 spelers nodig!");
        }
    }

    private void resetGame() {
        gameRunning = false;
        for (Player player : players) {
            if (storedInventories.containsKey(player)) {
                player.getInventory().setContents(storedInventories.get(player));
                player.getInventory().setArmorContents(storedArmor.get(player));
            }
        }
        players.clear();
        tagger = null;
        firstPlayer = null;
        updateSign();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof Sign) {
            Player player = event.getPlayer();
            if (!gameRunning || players.isEmpty()) {
                // Als het spel niet bezig is of er geen spelers zijn, kan het bord altijd gebroken worden
                event.setCancelled(false);
                return;
            }
                if (player.getGameMode() == GameMode.CREATIVE) {
                    player.sendMessage(ChatColor.RED + "Bordje verwijderd! Het spel is gereset.");
                    resetGame();
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Je moet in Creative zijn om dit bordje te breken!");
                }
            }
        }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("tikkertje") && args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            resetGame();
            Bukkit.broadcastMessage(ChatColor.RED + "Het spel is geforceerd gestopt door een operator.");
            return true;
        }

        if (label.equalsIgnoreCase("verwijderbordje") && sender instanceof Player) {
            Player player = (Player) sender;
            Location playerLoc = player.getLocation();
            int radius = 3;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location checkLoc = playerLoc.clone().add(x, y, z);
                        if (checkLoc.getBlock().getState() instanceof Sign) {
                            checkLoc.getBlock().setType(Material.AIR);
                            player.sendMessage(ChatColor.GREEN + "Het dichtstbijzijnde bordje is verwijderd!");
                            return true;
                        }
                    }
                }
            }
            player.sendMessage(ChatColor.RED + "Geen bordje gevonden binnen 3 blokken!");
            return true;
        }
        return false;
    }
}

