package listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import main.main;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SaveLoad implements Listener {

    public main plugin;

    public SaveLoad(main main) {
        this.plugin = main;
        this.plugin.getServer().getPluginManager().registerEvents(this, main);
    }

    public static void save(ConfigurationSection sec, ItemStack stack) {
        sec.set("type", stack.getType().name());
        sec.set("amount", stack.getAmount());
        sec.set("dur", stack.getDurability());

        if(stack.getItemMeta().hasEnchants()) {

            for (Enchantment enchantment : stack.getEnchantments().keySet()) {
                sec.set("enchantments."+enchantment.getName()+".lvl", stack.getEnchantmentLevel(enchantment));
            }
        }

        if(stack.hasItemMeta()) {
            if(stack.getItemMeta().hasDisplayName()) {
                sec.set("name", stack.getItemMeta().getDisplayName());
            }
            if(stack.getItemMeta().hasLore()) {
                sec.set("lore", stack.getItemMeta().getLore());
            }
        }
    }

    private ItemStack load(ConfigurationSection sec) {
        Short dur = (short) sec.getLong("dur");

        ItemStack item = new ItemStack(Material.valueOf(sec.getString("type")));
        ItemMeta itemM = item.getItemMeta();
        item.setAmount(sec.getInt("amount"));
        item.setDurability(dur);

        if(sec.get("enchantments") != null) {
            for (String enchantmentStr : sec.getConfigurationSection("enchantments").getKeys(false)) {

                Enchantment ench = Enchantment.getByName(enchantmentStr);
                int level = sec.getInt("enchantments." + enchantmentStr + ".lvl");

                item.addEnchantment(ench, level);
                itemM.addEnchant(ench, level, true);
            }
        }

        if(sec.getString("name") != null) {
            itemM.setDisplayName(sec.getString("name"));
        }

        if(sec.getString("lore") != null) {
            itemM.setLore(sec.getStringList("lore"));
        }

        item.setItemMeta(itemM);
        return new ItemStack(item);
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        if(!main.config.getBoolean("MySQL.enable")) {
            Player p = e.getPlayer();
            UUID id = p.getUniqueId();

            if(p.hasPermission("backpacks.littleBackPack")) {
                Inventory inv = Bukkit.getServer().createInventory(p, main.names.getInt("LittleBackPack.Slots"), ChatColor.translateAlternateColorCodes('&', main.names.getString("LittleBackPack.Name")));

                if(main.backpacks.contains("littleB."+id)) {
                    for(String item : main.backpacks.getConfigurationSection("littleB."+id).getKeys(false)) {
                        inv.addItem(load(main.backpacks.getConfigurationSection("littleB."+id+"."+item)));
                    }
                }

                main.littleB.put(id, inv);
            }

            if(p.hasPermission("backpacks.normalBackPack")) {
                Inventory inv = Bukkit.getServer().createInventory(p, main.names.getInt("NormalBackPack.Slots"), ChatColor.translateAlternateColorCodes('&', main.names.getString("NormalBackPack.Name")));

                if(main.backpacks.contains("normalB."+id)) {
                    for(String item : main.backpacks.getConfigurationSection("normalB."+id).getKeys(false)) {
                        inv.addItem(load(main.backpacks.getConfigurationSection("normalB."+id+"."+item)));
                    }
                }

                main.normalB.put(id, inv);
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if(!main.config.getBoolean("MySQL.enable")) {
            Player p = e.getPlayer();
            UUID id = p.getUniqueId();

            if(p.hasPermission("backpacks.littleBackPack")) {
                for(int in = 1; in < main.names.getInt("LittleBackPack.Slots"); in++) {
                    if(main.backpacks.get("littleB."+id+"."+in) != null) {
                        main.backpacks.set("littleB."+id+"."+in, null);
                    }
                }

                if(!main.backpacks.contains("littleB."+id)) {
                    main.backpacks.createSection("littleB."+id);
                }

                int i = 1;
                for(ItemStack stack : main.littleB.get(id)) {
                    if(stack != null) {
                        save(main.backpacks.createSection("littleB."+id+"."+i++), stack);
                    }
                }
            }

            if(p.hasPermission("backpacks.normalBackPack")) {
                for(int in2 = 1; in2 < main.names.getInt("NormalBackPack.Slots"); in2++) {
                    if(main.backpacks.get("normalB."+id+"."+in2) != null) {
                        main.backpacks.set("normalB."+id+"."+in2, null);
                    }
                }

                if(!main.backpacks.contains("normalB."+id)) {
                    main.backpacks.createSection("normalB."+id);
                }

                int i = 1;
                for(ItemStack stack : main.normalB.get(id)) {
                    if(stack != null) {
                        save(main.backpacks.createSection("normalB."+id+"."+i++), stack);
                    }
                }
            }

            try {
                main.backpacks.save(main.backpacksF);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

}