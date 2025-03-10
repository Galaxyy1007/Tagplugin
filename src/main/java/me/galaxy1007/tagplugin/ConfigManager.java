package me.galaxy1007.tagplugin;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final Tagplugin plugin;
    private int speed;
    private int borderSize;
    private int duration;
    private long tagCooldownDuration;

    public ConfigManager(Tagplugin plugin) {
        this.plugin = plugin;
        loadConfigSettings();
    }

    public void loadConfigSettings() {
        FileConfiguration config = plugin.getConfig();
        speed = Math.min(config.getInt("game-settings.speed", 1), 5);
        borderSize = config.getInt("game-settings.borderSize", 10);
        duration = config.getInt("game-settings.duration", 10);
        tagCooldownDuration = config.getLong("tag_cooldown", 5) * 1000;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfigSettings();
    }

    public String getConfigString(String path) {
        return plugin.getConfig().getString(path);
    }

    public String getConfigString(String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }

    public int getSpeed() {
        return speed;
    }

    public int getBorderSize() {
        return borderSize;
    }

    public int getDuration() {
        return duration;
    }

    public long getTagCooldownDuration() {
        return tagCooldownDuration;
    }
}