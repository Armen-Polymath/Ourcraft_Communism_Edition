package me.polymath;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public final class ShareManager implements Listener {

    private final JavaPlugin plugin;

    private boolean sharing = false;
    private int taskId = -1;

    // Used to prevent recursion / event spam from our own applications
    private boolean applyingSync = false;

    public ShareManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSharingAndClearAll(CommandSender sender) {
        if (sharing) {
            sender.sendMessage("§eSharing is already enabled.");
            return;
        }

        // Clear everyone first (as requested)
        for (Player p : Bukkit.getOnlinePlayers()) {
            clearAll(p);
        }

        sharing = true;

        // Start a 1-tick sync loop. First online player is source each tick.
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::syncTick, 1L, 1L);

        sender.sendMessage("§aSharing enabled. All players are now linked.");
    }

    public void stopSharingAndClearAll(CommandSender sender) {
        if (!sharing) {
            sender.sendMessage("§eSharing is already disabled.");
            // still clear, because you explicitly said both /share and /unshare clear everything
            for (Player p : Bukkit.getOnlinePlayers()) {
                clearAll(p);
            }
            return;
        }

        sharing = false;
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        // Clear everyone (as requested)
        for (Player p : Bukkit.getOnlinePlayers()) {
            clearAll(p);
        }

        sender.sendMessage("§cSharing disabled. All players have been cleared/reset.");
    }

    public void stopSharingAndClearAll() {
        // For shutdown safety
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        sharing = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            clearAll(p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!sharing) return;

        // When a player joins during sharing, they should be synced.
        // We clear them (to be consistent with "clean slate" behavior),
        // then next tick they’ll get the shared state.
        clearAll(event.getPlayer());
    }

    private void syncTick() {
        if (!sharing) return;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        // Choose a source player (first online) each tick.
        Player source = players.get(0);
        if (!source.isOnline()) return;

        SharedState state = SharedState.capture(source);

        applyingSync = true;
        try {
            for (Player target : players) {
                if (target == null || !target.isOnline()) continue;
                if (target.equals(source)) continue;

                // Optional: only sync survival-like modes; remove if you want ALL modes
                // (you said "everything", so we sync regardless of gamemode)
                state.applyTo(target);
            }
        } finally {
            applyingSync = false;
        }
    }

    private void clearAll(Player p) {
        // Inventory (including armor + offhand)
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[] {null, null, null, null});
        inv.setItemInOffHand(null);

        // Effects
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }

        // Core stats reset
        // Health
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = (attr != null ? attr.getValue() : 20.0);
        p.setHealth(Math.min(20.0, maxHealth));
        p.setAbsorptionAmount(0.0);

        // Food
        p.setFoodLevel(20);
        p.setSaturation(5.0f);
        p.setExhaustion(0.0f);

        // Air
        p.setRemainingAir(p.getMaximumAir());

        // Misc survival-ish state
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setFallDistance(0.0f);

        // Damage tick window
        p.setNoDamageTicks(0);

        // XP
        p.setExp(0.0f);
        p.setLevel(0);
        p.setTotalExperience(0);
    }

    /**
     * Snapshot of the "shared everything" state we can represent using Bukkit/Paper API.
     */
    private static final class SharedState {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack offhand;

        private final double health;
        private final double absorption;

        private final int food;
        private final float saturation;
        private final float exhaustion;

        private final int remainingAir;

        private final int fireTicks;
        private final int freezeTicks;
        private final float fallDistance;

        private final int noDamageTicks;

        private final int level;
        private final float exp;
        private final int totalExp;

        private final Collection<PotionEffect> effects;

        private SharedState(
                ItemStack[] contents,
                ItemStack[] armor,
                ItemStack offhand,
                double health,
                double absorption,
                int food,
                float saturation,
                float exhaustion,
                int remainingAir,
                int fireTicks,
                int freezeTicks,
                float fallDistance,
                int noDamageTicks,
                int level,
                float exp,
                int totalExp,
                Collection<PotionEffect> effects
        ) {
            this.contents = contents;
            this.armor = armor;
            this.offhand = offhand;
            this.health = health;
            this.absorption = absorption;
            this.food = food;
            this.saturation = saturation;
            this.exhaustion = exhaustion;
            this.remainingAir = remainingAir;
            this.fireTicks = fireTicks;
            this.freezeTicks = freezeTicks;
            this.fallDistance = fallDistance;
            this.noDamageTicks = noDamageTicks;
            this.level = level;
            this.exp = exp;
            this.totalExp = totalExp;
            this.effects = effects;
        }

        static SharedState capture(Player p) {
            PlayerInventory inv = p.getInventory();

            // Clone arrays to avoid accidental shared references
            ItemStack[] contents = cloneItemStackArray(inv.getContents());
            ItemStack[] armor = cloneItemStackArray(inv.getArmorContents());
            ItemStack offhand = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();

            double health = p.getHealth();
            double absorption = p.getAbsorptionAmount();

            int food = p.getFoodLevel();
            float saturation = p.getSaturation();
            float exhaustion = p.getExhaustion();

            int remainingAir = p.getRemainingAir();

            int fireTicks = p.getFireTicks();
            int freezeTicks = p.getFreezeTicks();
            float fallDistance = p.getFallDistance();

            int noDamageTicks = p.getNoDamageTicks();

            int level = p.getLevel();
            float exp = p.getExp();
            int totalExp = p.getTotalExperience();

            // Clone effects (PotionEffect is immutable enough for our use, but we copy to be safe)
            Collection<PotionEffect> effects = new ArrayList<>(p.getActivePotionEffects());

            return new SharedState(
                    contents, armor, offhand,
                    health, absorption,
                    food, saturation, exhaustion,
                    remainingAir,
                    fireTicks, freezeTicks, fallDistance,
                    noDamageTicks,
                    level, exp, totalExp,
                    effects
            );
        }

        void applyTo(Player p) {
            // Inventory
            PlayerInventory inv = p.getInventory();
            inv.setContents(cloneItemStackArray(contents));
            inv.setArmorContents(cloneItemStackArray(armor));
            inv.setItemInOffHand(offhand == null ? null : offhand.clone());

            // Effects: remove all then re-add
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) {
                p.addPotionEffect(effect, true);
            }

            // Health (clamp to max)
            AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = (attr != null ? attr.getValue() : 20.0);
            p.setHealth(Math.max(0.0, Math.min(health, maxHealth)));
            p.setAbsorptionAmount(Math.max(0.0, absorption));

            // Food
            p.setFoodLevel(Math.max(0, Math.min(20, food)));
            p.setSaturation(Math.max(0.0f, saturation));
            p.setExhaustion(Math.max(0.0f, exhaustion));

            // Air
            p.setRemainingAir(Math.min(remainingAir, p.getMaximumAir()));

            // Misc
            p.setFireTicks(fireTicks);
            p.setFreezeTicks(freezeTicks);
            p.setFallDistance(fallDistance);

            // Damage immunity window
            p.setNoDamageTicks(noDamageTicks);

            // XP
            p.setLevel(Math.max(0, level));
            p.setExp(Math.max(0.0f, Math.min(1.0f, exp)));
            p.setTotalExperience(Math.max(0, totalExp));
        }

        private static ItemStack[] cloneItemStackArray(ItemStack[] arr) {
            if (arr == null) return null;
            ItemStack[] out = new ItemStack[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = arr[i] == null ? null : arr[i].clone();
            }
            return out;
        }
    }
}
