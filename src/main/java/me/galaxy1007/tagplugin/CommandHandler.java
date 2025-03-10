package me.galaxy1007.tagplugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final Tagplugin plugin;
    private final GameManager gameManager;
    private final SignManager signManager;
    private final ConfigManager configManager;

    public CommandHandler(Tagplugin plugin, GameManager gameManager, SignManager signManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.signManager = signManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("taggame")) {
            return false;
        }

        if (args.length == 0) {
            sendHelpMenu(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                return handleStartCommand(sender);

            case "stop":
                return handleStopCommand(sender);

            case "reload":
                return handleReloadCommand(sender);

            case "removesign":
                return handleRemoveSignCommand(sender);

            case "resetregisters":
                return handleResetRegistersCommand(sender);

            case "help":
                sendHelpMenu(sender);
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Invalid subcommand!");
                sendHelpMenu(sender);
                return true;
        }
    }

    private boolean handleStartCommand(CommandSender sender) {
        if (gameManager.isGameRunning()) {
            sender.sendMessage(ChatColor.RED + "The tag game is already running!");
            return true;
        }

        if (sender.isOp()) {
            gameManager.startCountdown();
            sender.sendMessage(ChatColor.GREEN + "Starting the game...");
            return true;
        } else if (Bukkit.getOnlinePlayers().size() < 2) {
            sender.sendMessage(ChatColor.RED + "There must be at least 2 players online to start the tag game.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Starting the game...");
        gameManager.startCountdown();
        return true;
    }

    private boolean handleStopCommand(CommandSender sender) {
        if (!gameManager.isGameRunning()) {
            sender.sendMessage(ChatColor.RED + "The tag game is not currently running.");
            return true;
        }

        gameManager.stopGame();
        sender.sendMessage(ChatColor.GREEN + "The tag game has been stopped.");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
        return true;
    }

    private boolean handleRemoveSignCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(3);

        if (targetBlock != null && targetBlock.getState() instanceof Sign) {
            targetBlock.setType(Material.AIR);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GREEN + "Sign removed successfully!"));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "No sign found within 3 blocks radius!"));
        }

        return true;
    }

    private boolean handleResetRegistersCommand(CommandSender sender) {
        signManager.resetSigns();
        sender.sendMessage(ChatColor.RED + "Registers reset!");
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========[ TagGame Help ]========");
        sender.sendMessage(ChatColor.YELLOW + "/taggame start " + ChatColor.GRAY + "- Start the game");
        sender.sendMessage(ChatColor.YELLOW + "/taggame stop " + ChatColor.GRAY + "- Stop the game");
        sender.sendMessage(ChatColor.YELLOW + "/taggame reload " + ChatColor.GRAY + "- Reload the config");
        sender.sendMessage(ChatColor.YELLOW + "/taggame removesign " + ChatColor.GRAY + "- Remove a playsign");
        sender.sendMessage(ChatColor.YELLOW + "/taggame resetregisters " + ChatColor.GRAY + "- Resets all registered players");
        sender.sendMessage(ChatColor.GOLD + "================================");
    }
}