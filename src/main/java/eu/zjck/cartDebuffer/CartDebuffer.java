package eu.zjck.cartDebuffer;

import eu.zjck.cartDebuffer.command.CartDebuffCommand;
import eu.zjck.cartDebuffer.config.ConfigManager;
import eu.zjck.cartDebuffer.listener.TntMinecartEntityDamageListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CartDebuffer extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        getServer().getPluginManager().registerEvents(new TntMinecartEntityDamageListener(this), this);

        PluginCommand command = getCommand("cartdebuff");
        if (command != null) {
            CartDebuffCommand executor = new CartDebuffCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Failed to register the /cartdebuff command");
        }

        getLogger().info("CartDebuffer enabled. Active mode: " + configManager.getEntityActiveMode());
    }

    @Override
    public void onDisable() {
        getLogger().info("CartDebuffer disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public void broadcastToAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("cartdebuffer.admin")) {
                player.sendMessage(message);
            }
        }
    }
}