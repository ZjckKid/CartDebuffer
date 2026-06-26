package eu.zjck.cartDebuffer.config;

import eu.zjck.cartDebuffer.CartDebuffer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final CartDebuffer plugin;

    private boolean debug;
    private boolean notifyAdminsInChat;
    private boolean onlyPlayers;
    private final Set<String> disabledWorlds = new HashSet<>();

    private boolean entityDamageEnabled;
    private String entityActiveMode;

    private FileConfiguration cfg;

    public ConfigManager(CartDebuffer plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();

        ConfigurationSection settings = cfg.getConfigurationSection("settings");
        if (settings != null) {
            debug = settings.getBoolean("debug", false);
            notifyAdminsInChat = settings.getBoolean("notify-admins-in-chat", false);
            onlyPlayers = settings.getBoolean("only-players", true);
            disabledWorlds.clear();
            disabledWorlds.addAll(settings.getStringList("disabled-worlds"));
        }

        ConfigurationSection entitySection = cfg.getConfigurationSection("entity-damage");
        if (entitySection != null) {
            entityDamageEnabled = entitySection.getBoolean("enabled", true);
            entityActiveMode = entitySection.getString("active-mode", "distance-falloff");
        }
    }

    public FileConfiguration raw() {
        return cfg;
    }

    public ConfigurationSection getEntityModeSection(String modeName) {
        return cfg.getConfigurationSection("entity-damage.modes." + modeName);
    }

    public List<String> getEntityModeNames() {
        ConfigurationSection sec = cfg.getConfigurationSection("entity-damage.modes");
        return sec == null ? new ArrayList<>() : new ArrayList<>(sec.getKeys(false));
    }

    public String getMessage(String key) {
        String msg = cfg.getString("messages." + key, "");
        return msg == null ? "" : msg;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isNotifyAdminsInChat() {
        return notifyAdminsInChat;
    }

    public boolean isOnlyPlayers() {
        return onlyPlayers;
    }

    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public boolean isEntityDamageEnabled() {
        return entityDamageEnabled;
    }

    public void setEntityDamageEnabled(boolean enabled) {
        this.entityDamageEnabled = enabled;
    }

    public String getEntityActiveMode() {
        return entityActiveMode;
    }

    public void setEntityActiveMode(String mode) {
        this.entityActiveMode = mode;
    }
}