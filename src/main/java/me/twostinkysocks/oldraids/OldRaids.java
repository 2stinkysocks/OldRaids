package me.twostinkysocks.oldraids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftRaider;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.List;


public final class OldRaids extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        load();
        if(getConfig().getBoolean("metrics")) {
            int pluginId = 22491;
            new Metrics(this, pluginId);
        }
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("oldraids").setExecutor(this);
        getCommand("oldraids").setTabCompleter(this);
    }
    
    public void load() {
        if(!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        File config = new File(this.getDataFolder(), "config.yml");
        if(!config.exists()) {
            saveDefaultConfig();
        }
        this.reloadConfig();
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Player damager = e.getEntity().getKiller();
        LivingEntity damaged = e.getEntity();

        if(damager == null) return;
        if(!(damaged instanceof Raider)) return;

        Raider raider = (Raider) damaged;
        net.minecraft.world.entity.raid.Raider nmsRaider = ((CraftRaider) raider).getHandle();

        if(raider.isPatrolLeader() && raider.getRaid() == null && ((ServerLevel) nmsRaider.level()).getRaidAt(nmsRaider.blockPosition()) == null) {
            int i = 0;
            PotionEffect effect = damager.getPotionEffect(PotionEffectType.BAD_OMEN);
            if(effect != null) {
                i = effect.getAmplifier();
                damager.removePotionEffect(PotionEffectType.BAD_OMEN);
            }
            i = Mth.clamp(i, 0, 4);
            PotionEffect newEffect = new PotionEffect(PotionEffectType.BAD_OMEN, 120000, i, false, false, true);
            if(Boolean.FALSE.equals(damaged.getWorld().getGameRuleValue(GameRule.DISABLE_RAIDS))) {
                damager.addPotionEffect(newEffect);
            }

            if(!getConfig().getBoolean("raidersDropOminousBottles")) e.getDrops().removeIf(item -> item.getType() == Material.OMINOUS_BOTTLE);
        }
    }

    @EventHandler
    public void onPotion(EntityPotionEffectEvent e) {
        if(e.getEntity() instanceof Player && e.getNewEffect() != null && e.getNewEffect().getType() == PotionEffectType.RAID_OMEN && e.getNewEffect().getDuration() > 1) {
            e.setCancelled(true);
            ((Player) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.RAID_OMEN, 1, e.getNewEffect().getAmplifier(), false, true, true));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equals("oldraids")) {
            if(!sender.hasPermission("oldraids.reload") && !(sender instanceof ConsoleCommandSender) && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                return true;
            }
            if(args.length == 0 || !args[0].equals("reload")) {
                sender.sendMessage(ChatColor.RED + "Usage: /oldraids reload");
                return true;
            }
            load();
            sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(alias.equals("oldraids") && args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
