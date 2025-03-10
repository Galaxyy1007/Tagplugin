package me.galaxy1007.tagplugin;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class Tagplugin extends JavaPlugin {
    private GameManager gameManager;
    private SignManager signManager;
    private CommandHandler commandHandler;
    private ConfigManager configManager;
    private BossBar gameTimerBar;
    private Set<Player> activePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        configManager = new ConfigManager(this);
        signManager = new SignManager(this);
        gameManager = new GameManager(this, signManager, configManager);
        commandHandler = new CommandHandler(this, gameManager, signManager, configManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new TagEventListener(this, gameManager, signManager), this);

        // Register commands
        getCommand("taggame").setExecutor(commandHandler);
        getCommand("taggame").setTabCompleter(new TagGameTabCompleter());

        getLogger().info("TagPlugin has been enabled!");

        gameTimerBar = Bukkit.createBossBar("Game Time Remaining", BarColor.GREEN, BarStyle.SOLID);
        gameTimerBar.setVisible(false);
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopGame();
        }
        getLogger().info("TagPlugin has been disabled!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public SignManager getSignManager() {
        return signManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}

