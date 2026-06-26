# CartDebuffer

A plugin is tested at **Purpur 1.21.11**. But, it may work from 
Paper 1.21, to Paper 1.21.11 (Purpur too) 

That plugin modifies the damage players receive
from **TNT Minecart** explosions. It allows you to test 
different damage balancing approaches **live** and
choose the best one without restarting the server.

---

## 1. How It Works

The plugin listens for `EntityDamageEvent` events with the cause
`ENTITY_EXPLOSION` or `BLOCK_EXPLOSION`. To ensure that the damage comes
specifically from a **TNT Minecart**, rather than regular TNT or a
Creeper explosion, it checks the damage source through
`event.getDamageSource()`.

If you want to disable the plugin completely and restore vanilla
behavior, simply set `entity-damage.enabled: false` in the configuration
or run `/cartdebuff toggle`.

---

## 2. Damage Modes

Switch modes with:

```text
/cartdebuff mode <mode>
```

### `multiplier` - Simple Multiplier

```text
final_damage = incoming_damage × multiplier
```

The simplest mode. It takes the vanilla damage value and scales it by
`multiplier`. For example, `multiplier: 0.5` halves the damage.
`min-damage` and `max-damage` act as safety limits.

**Best for:** situations where you like the vanilla damage behavior
(distance scaling, explosion strength, etc.) but simply want to make it
stronger or weaker proportionally.

---

### `fixed` - Fixed Damage

Every TNT Minecart explosion always deals exactly `damage` HP,
regardless of distance, the number of minecarts, obstacles, or any other
factors.

**Best for:** highly predictable gameplay, such as PvP arenas where
damage should always remain the same.

---

### `distance-falloff` - Fixed Damage with Distance Falloff

The most advanced and accurate mode. Damage is calculated as follows:

1. `base-damage` is used as the maximum damage at the explosion center.
2. Damage decreases with the player's distance from the explosion using
   the selected `falloff-curve`:
   - `linear` - constant decrease;
   - `quadratic` - falls off more rapidly at medium distances (default);
   - `exponential` - drops very quickly immediately outside the close
     range.
3. Damage is further reduced if solid (opaque) blocks are located
   between the player and the explosion. Each blocking block reduces the
   damage by `damage-reduction-per-block` (percentage). This is
   calculated using ray tracing
   (`ExplosionMath.countObstructingBlocks`).
4. Damage will never drop below `min-damage`.

**Best for:** the most realistic and flexible model. Players behind
walls or farther away receive significantly less damage than those
standing nearby.

---

### `stepped-zones` - Stepped Distance Zones

Instead of a smooth curve, damage is divided into fixed distance ranges.
If the player is within `max-distance`, they receive the corresponding
`damage`. Zones are checked in order, and the first matching zone is
used.

```yaml
zones:
  - max-distance: 2.0   # up to 2 blocks
    damage: 12.0
  - max-distance: 4.0   # from 2 to 4 blocks
    damage: 7.0
  - max-distance: 6.0   # from 4 to 6 blocks
    damage: 3.0
  - max-distance: 999.0 # everything beyond
    damage: 0.0
```

**Best for:** simple, predictable balancing without complex calculations.

---

## 3. General Settings (`settings`)

| Setting | Description |
|---|---|
| `debug` | Logs every damage recalculation to the console, including the original value, new value, mode, and distance from the explosion |
| `notify-admins-in-chat` | Sends the same information to players with the `cartdebuffer.admin` permission |
| `only-players` | `true` - only players are affected; `false` - mobs are affected as well |
| `disabled-worlds` | List of worlds where the plugin does not modify damage |

---

## 4. Commands

| Command | Description |
|---|---|
| `/cartdebuff reload` | Reloads `config.yml` from disk without restarting the server |
| `/cartdebuff mode <mode>` | Switches the active damage mode |
| `/cartdebuff toggle` | Enables or disables all damage processing (restores vanilla behavior) |
| `/cartdebuff info` | Displays the currently active mode and a list of all available modes |

All commands require the `cartdebuffer.admin` permission (by default,
only server operators have it). Tab completion is supported for mode
names.

**Important:** the `mode` command immediately saves the selected mode
both in memory and in `config.yml` (`plugin.saveConfig()`), so the
selected mode persists across server restarts.

---

## 5. Finding the Best Mode (Practical Guide)

1. Join the server with `op` permissions.
2. Try each mode one by one:

   ```text
   /cartdebuff mode multiplier
   /cartdebuff mode fixed
   /cartdebuff mode distance-falloff
   /cartdebuff mode stepped-zones
   ```

3. After manually changing values in the configuration (`damage`,
   `multiplier`, etc.), run `/cartdebuff reload` to apply the changes
   without switching modes.
4. Enable `settings.debug: true` and run `/cartdebuff reload` to see
   every damage recalculation in the console (old value, new value,
   distance).
5. Once you've found the mode you like, simply leave it as
   `active-mode` in `config.yml` (or use the `mode` command, which saves
   it automatically).

---



## 6. Building

```bash
mvn clean package
```

The compiled JAR will be available at `target/CartDebuffer-1.0.jar`.
Place it into your server's `plugins/` directory.
