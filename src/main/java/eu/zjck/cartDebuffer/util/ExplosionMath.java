package eu.zjck.cartDebuffer.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public final class ExplosionMath {

    private ExplosionMath() {
    }

    public static int countObstructingBlocks(Location from, Location to, int maxBlocks) {
        World world = from.getWorld();
        if (world == null || to.getWorld() == null || !world.equals(to.getWorld())) {
            return 0;
        }

        Vector start = from.toVector();
        Vector end = to.toVector();
        double distance = start.distance(end);

        if (distance < 0.0001) {
            return 0;
        }

        double step = 0.5;
        int steps = (int) Math.ceil(distance / step);
        Vector direction = end.clone().subtract(start).normalize();

        int obstructions = 0;
        Block lastBlock = null;

        for (int i = 1; i < steps; i++) {
            double traveled = i * step;
            if (traveled >= distance) {
                break;
            }
            Vector point = start.clone().add(direction.clone().multiply(traveled));
            Location loc = new Location(world, point.getX(), point.getY(), point.getZ());
            Block block = loc.getBlock();

            if (block.equals(lastBlock)) {
                continue;
            }
            lastBlock = block;

            if (!block.isPassable() && block.getType().isOccluding()) {
                obstructions++;
                if (obstructions >= maxBlocks) {
                    return obstructions;
                }
            }
        }

        return obstructions;
    }

    public static double applyFalloffCurve(double baseDamage, double distance, double radius, String curve) {
        if (distance >= radius) {
            return 0.0;
        }
        if (radius <= 0) {
            return baseDamage;
        }

        double ratio = 1.0 - (distance / radius);
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        double factor;
        switch (curve == null ? "linear" : curve.toLowerCase()) {
            case "quadratic":
                factor = ratio * ratio;
                break;
            case "exponential":
                factor = 1.0 - Math.exp(-3.0 * ratio) / (1.0 - Math.exp(-3.0));
                factor = Math.max(0.0, Math.min(1.0, factor));
                break;
            case "linear":
            default:
                factor = ratio;
                break;
        }

        return baseDamage * factor;
    }
}