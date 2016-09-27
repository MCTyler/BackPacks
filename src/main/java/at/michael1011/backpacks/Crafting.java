package at.michael1011.backpacks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static at.michael1011.backpacks.Main.*;

public class Crafting {

    public static HashMap<ItemStack, String> items = new HashMap<>();
    public static HashMap<String, ItemStack> itemsInverted = new HashMap<>();

    public static HashMap<String, String> type = new HashMap<>();
    public static HashMap<String, Integer> slots = new HashMap<>();
    public static HashMap<String, String> furnaceGui = new HashMap<>();
    public static HashMap<List<String>, String> loreMap = new HashMap<>();

    public static String available = "";
    public static List<String> availableList;

    private static Boolean slots9 = true;

    public static void initCrafting() {
        String path = "BackPacks.";

        Map<String, Object> enabled = config.getConfigurationSection(path+"enabled").getValues(true);

        for(Map.Entry<String, Object> entry : enabled.entrySet()) {
            String backPack = entry.getValue().toString();
            String backPackPath = path+backPack+".";

            if(config.contains(backPackPath)) {
                ItemStack item = getItemStack(config, backPackPath, backPack);

                if(item != null) {
                    if(slots9) {
                        items.put(item, backPack);
                        itemsInverted.put(backPack, item);

                        if(available.equals("")) {
                            available = backPack;

                        } else {
                            available = available+","+backPack;
                        }

                        if(config.getBoolean(backPackPath+"crafting.enabled")) {
                            Bukkit.getServer().addRecipe(createShapedRecipe(item, backPackPath, backPack));
                        }

                        Bukkit.getConsoleSender().sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                messages.getString("BackPacks.enabled").replaceAll("%backpack%", backPack)));

                    } else {
                        Bukkit.getConsoleSender().sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                messages.getString("BackPacks.slotsNotDivisibleBy9").replaceAll("%backpack%", backPack)));

                        slots9 = true;
                    }

                }

            } else {
                Bukkit.getConsoleSender().sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                        messages.getString("BackPacks.couldNotFindConfig").replaceAll("%backpack%", backPack)));
            }

        }

        availableList = Arrays.asList(Crafting.available.split(","));
    }

    private static ItemStack getItemStack(YamlConfiguration config, String backPackPath, String backPack) {
        int itemSlots = config.getInt(backPackPath+"slots");

        String materialString = config.getString(backPackPath+"material");

        if(itemSlots % 9 == 0) {
            try {
                Material material = Material.getMaterial(materialString);

                ItemStack craft = new ItemStack(material, 1);

                String name = ChatColor.translateAlternateColorCodes('&', config.getString(backPackPath+"name"));

                String lore = "";

                Map<String, Object> loreSec = config.getConfigurationSection(backPackPath+"description").getValues(true);

                for(Map.Entry<String, Object> ent : loreSec.entrySet()) {
                    lore = lore+","+ChatColor.translateAlternateColorCodes('&', ent.getValue().toString());
                }

                ItemMeta craftM = craft.getItemMeta();

                craftM.setDisplayName(name);

                if(!lore.equals("")) {
                    List<String> loreList = Arrays.asList(lore.split("\\s*,\\s*"));

                    craftM.setLore(loreList);

                    loreMap.put(loreList, backPack);
                }

                craft.setItemMeta(craftM);

                slots.put(backPack, itemSlots);

                String rawType = config.getString(backPackPath+"type");

                type.put(backPack, rawType);

                if(rawType.equals("furnace")) {
                    furnaceGui.put(backPack, config.getString(backPackPath+"gui.enabled"));
                }

                return craft;

            } catch (IllegalArgumentException | NullPointerException e) {
                Bukkit.getConsoleSender().sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                        messages.getString("Help.materialNotValid")
                                .replaceAll("%material%", materialString).replaceAll("%backpack%", backPack)));
            }

            return null;

        } else {
            slots9 = false;

            return null;
        }

    }

    private static ShapedRecipe createShapedRecipe(ItemStack item, String backPackPath, String backPack) {
        ShapedRecipe recipe = new ShapedRecipe(item);

        recipe.shape(
                config.getString(backPackPath+"crafting.1").replaceAll("\\+", ""),
                config.getString(backPackPath+"crafting.2").replaceAll("\\+", ""),
                config.getString(backPackPath+"crafting.3").replaceAll("\\+", ""));

        Map<String, Object> ingredients = config.getConfigurationSection(backPackPath+
                "crafting.materials").getValues(true);

        for(Map.Entry<String, Object> ing : ingredients.entrySet()) {
            String material = ing.getValue().toString();

            try {
                recipe.setIngredient(ing.getKey().charAt(0), Material.valueOf(material));

            } catch (IllegalArgumentException e) {
                Bukkit.getConsoleSender().sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', messages.getString("Help.materialNotValid")
                        .replaceAll("%material%", material).replaceAll("%backpack%", backPack)));
            }
        }

        return recipe;
    }

}
