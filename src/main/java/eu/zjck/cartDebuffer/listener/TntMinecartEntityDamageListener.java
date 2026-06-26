package eu.zjck.cartDebuffer.listener;

import eu.zjck.cartDebuffer.CartDebuffer;
import eu.zjck.cartDebuffer.config.ConfigManager;
import eu.zjck.cartDebuffer.util.ExplosionMath;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class TntMinecartEntityDamageListener implements Listener {

    private final CartDebuffer plugin;

    public TntMinecartEntityDamageListener(CartDebuffer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isEntityDamageEnabled()) {
            return;
        }

        if (event.getCause() != DamageCause.ENTITY_EXPLOSION
                && event.getCause() != DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Entity victim = event.getEntity();

        if (cfg.isOnlyPlayers() && !(victim instanceof Player)) {
            return;
        }

        if (cfg.getDisabledWorlds().contains(victim.getWorld().getName())) {
            return;
        }

        DamageSource damageSource = event.getDamageSource();
        Entity causing = damageSource.getCausingEntity();
        Entity direct = damageSource.getDirectEntity();

        boolean fromMinecart = isExplosiveMinecart(causing) || isExplosiveMinecart(direct);
        if (!fromMinecart) {
            return;
        }

        Entity minecartEntity = isExplosiveMinecart(direct) ? direct : causing;
        Location explosionLocation = minecartEntity != null
                ? minecartEntity.getLocation()
                : damageSource.getDamageLocation();
        if (explosionLocation == null) {
            explosionLocation = victim.getLocation();
        }

        double incomingDamage = event.getDamage();
        String activeMode = cfg.getEntityActiveMode();
        ConfigurationSection modeSection = cfg.getEntityModeSection(activeMode);

        double newDamage = computeDamage(activeMode, modeSection, incomingDamage, victim, explosionLocation);

        if (cfg.isDebug()) {
            plugin.getLogger().info(String.format(
                    "[entity-damage] mode=%s victim=%s incoming=%.3f new=%.3f distance=%.2f explosionLoc=%s victimLoc=%s",
                    activeMode, victim.getName(), incomingDamage, newDamage,
                    victim.getLocation().distance(explosionLocation),
                    formatLoc(explosionLocation), formatLoc(victim.getLocation())));
        }

        applyFinalDamageIgnoringArmor(event, newDamage);

        if (cfg.isNotifyAdminsInChat()) {
            plugin.broadcastToAdmins("§7[CartDebuffer] §f" + victim.getName()
                    + " §7got §f" + String.format("%.2f", newDamage)
                    + " §7(mode: §e" + activeMode + "§7)");
        }
    }

    private void applyFinalDamageIgnoringArmor(EntityDamageEvent event, double desiredFinalDamage) {
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (modifier == EntityDamageEvent.DamageModifier.BASE) {
                continue;
            }
            if (event.isApplicable(modifier)) {
                event.setDamage(modifier, 0.0);
            }
        }
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, desiredFinalDamage);
    }

    private boolean isExplosiveMinecart(Entity entity) {
        return entity instanceof org.bukkit.entity.minecart.ExplosiveMinecart;
    }

    private static String formatLoc(Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }

    private double computeDamage(String mode, ConfigurationSection section, double incoming,
                                 Entity victim, Location explosionLoc) {
        if (section == null) {
            return incoming;
        }

        switch (mode) {
            case "multiplier": {
                double multiplier = section.getDouble("multiplier", 1.0);
                double min = section.getDouble("min-damage", 0.0);
                double max = section.getDouble("max-damage", 20.0);
                double result = incoming * multiplier;
                return clamp(result, min, max);
            }

            case "fixed": {
                return section.getDouble("damage", 6.0);
            }

            case "distance-falloff": {
                double base = section.getDouble("base-damage", 10.0);
                double radius = section.getDouble("falloff-radius", 8.0);
                String curve = section.getString("falloff-curve", "quadratic");
                double reductionPerBlock = section.getDouble("damage-reduction-per-block", 0.15);
                int maxBlocks = section.getInt("max-blocks-checked", 12);
                double minDamage = section.getDouble("min-damage", 0.0);

                double distance = victim.getLocation().distance(explosionLoc);
                double afterDistance = ExplosionMath.applyFalloffCurve(base, distance, radius, curve);

                int obstructions = ExplosionMath.countObstructingBlocks(explosionLoc, victim.getLocation(), maxBlocks);
                double obstructionFactor = Math.max(0.0, 1.0 - (obstructions * reductionPerBlock));

                double result = afterDistance * obstructionFactor;
                return Math.max(minDamage, result);
            }

            case "stepped-zones": {
                double distance = victim.getLocation().distance(explosionLoc);
                List<?> rawZones = section.getList("zones");
                if (rawZones == null) {
                    return incoming;
                }
                for (Object obj : rawZones) {
                    if (obj instanceof java.util.Map<?, ?> map) {
                        double maxDistance = toDouble(map.get("max-distance"), 999.0);
                        double dmg = toDouble(map.get("damage"), 0.0);
                        if (distance <= maxDistance) {
                            return dmg;
                        }
                    }
                }
                return 0.0;
            }

            default:
                return incoming;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double toDouble(Object obj, double fallback) {
        if (obj instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}