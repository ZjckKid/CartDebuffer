package eu.zjck.cartDebuffer.command;

import eu.zjck.cartDebuffer.CartDebuffer;
import eu.zjck.cartDebuffer.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartDebuffCommand implements CommandExecutor, TabCompleter {

    private final CartDebuffer plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CartDebuffCommand(CartDebuffer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (!sender.hasPermission("cartdebuffer.admin")) {
            sender.sendMessage(mm.deserialize(cfg.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(cfg.getMessage("invalid-usage")));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                plugin.getConfigManager().load();
                sender.sendMessage(mm.deserialize(cfg.getMessage("reload-success")));
                return true;
            }

            case "info": {
                sender.sendMessage("§6CartDebuffer");
                sender.sendMessage("§7entity-damage: §f" + (cfg.isEntityDamageEnabled() ? "§aenabled" : "§cdisabled")
                        + " §7| mode: §e" + cfg.getEntityActiveMode());
                sender.sendMessage("§7Available modes: §f" + String.join(", ", cfg.getEntityModeNames()));
                return true;
            }

            case "mode": {
                if (args.length < 2) {
                    sender.sendMessage(mm.deserialize(cfg.getMessage("invalid-usage")));
                    return true;
                }
                String modeName = args[1];

                if (!cfg.getEntityModeNames().contains(modeName)) {
                    sender.sendMessage(mm.deserialize(cfg.getMessage("mode-unknown")
                            .replace("%mode%", modeName)));
                    return true;
                }
                cfg.setEntityActiveMode(modeName);
                cfg.raw().set("entity-damage.active-mode", modeName);
                plugin.saveConfig();
                sender.sendMessage(mm.deserialize(cfg.getMessage("mode-changed")
                        .replace("%mode%", modeName)));
                return true;
            }

            case "toggle": {
                boolean newState = !cfg.isEntityDamageEnabled();
                cfg.setEntityDamageEnabled(newState);
                cfg.raw().set("entity-damage.enabled", newState);
                plugin.saveConfig();
                sender.sendMessage(mm.deserialize(cfg.getMessage("toggled")
                        .replace("%state%", newState ? "enabled" : "disabled")));
                return true;
            }

            default:
                sender.sendMessage(mm.deserialize(cfg.getMessage("invalid-usage")));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        ConfigManager cfg = plugin.getConfigManager();

        if (args.length == 1) {
            result.addAll(List.of("reload", "mode", "info", "toggle"));
            return filter(result, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            result.addAll(cfg.getEntityModeNames());
            return filter(result, args[1]);
        }

        return result;
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}