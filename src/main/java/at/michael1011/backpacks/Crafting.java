package at.michael1011.backpacks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static at.michael1011.backpacks.BackPack.Type.furnace;
import static at.michael1011.backpacks.Main.*;

public class Crafting {

    public static List<BackPack> backPacks = new ArrayList<>();
    public static HashMap<BackPack, ItemStack> backPacksItems = new HashMap<>();

    public static List<String> backPackNames = new ArrayList<>();

    private static final String path = "BackPacks.";

    private static Boolean slotsDivisible = true;

    public static void initCrafting(CommandSender sender) {
        for (Map.Entry<String, Object> entry : config.getConfigurationSection(path+"enabled")
                .getValues(true).entrySet()) {

            String backPack = entry.getValue().toString();
            String backPackPath = path + backPack+".";

            if (config.contains(backPackPath)) {
                ItemStack item = getItemStack(sender, backPackPath, backPack);

                if (item != null) {
                    if (slotsDivisible) {
                        if (!config.getBoolean(backPackPath + "crafting.disabled")
                                && config.get(backPackPath + "crafting.1") != null) {

                            Bukkit.getServer().addRecipe(createShapedRecipe(sender, item, backPacks.get(backPacks.size() - 1), backPackPath));
                        }

                        sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                                messages.getString("BackPacks.enabled").replaceAll("%backpack%", backPack)));

                    } else {
                        sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                                messages.getString("BackPacks.slotsNotDivisibleBy9").replaceAll("%backpack%", backPack)));

                        slotsDivisible = true;
                    }

                }

            } else {
                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                        messages.getString("BackPacks.couldNotFindConfig").replaceAll("%backpack%", backPack)));
            }

        }

        for (BackPack backPack : backPacks) {
            backPackNames.add(backPack.getName());
        }

    }

    private static ItemStack getItemStack(CommandSender sender, String backPackPath, String backPack) {
        int slots = config.getInt(backPackPath + "slots");

        String materialString = config.getString(backPackPath + "material").toUpperCase();

        if(slots % 9 == 0) {
            try {
                ItemStack item = new ItemStack(Material.valueOf(materialString), 1);

                String name = ChatColor.translateAlternateColorCodes('&',
                        config.getString(backPackPath + "name"));

                List<String> lore = new ArrayList<>();

                for(Map.Entry<String, Object> entry :
                        config.getConfigurationSection(backPackPath + "description").getValues(true).entrySet()) {

                    lore.add(ChatColor.translateAlternateColorCodes('&', entry.getValue().toString()));
                }

                ItemMeta meta = item.getItemMeta();

                meta.setDisplayName(name);

                if(lore.size() > 0) {
                    meta.setLore(lore);
                }

                item.setItemMeta(meta);

                Sound open = null;
                Sound close = null;

                String sound = config.getString(backPackPath + "sounds.open");

                try {
                    if(sound != null) {
                        open = Sound.valueOf(sound.toUpperCase());
                    }

                    sound = config.getString(backPackPath + "sounds.close");

                    if(sound != null) {
                        close = Sound.valueOf(sound.toUpperCase());
                    }

                } catch (IllegalArgumentException e) {
                    assert sound != null;

                    sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                            messages.getString("Help.soundNotValid")
                                    .replaceAll("%sound%", sound.toUpperCase())
                                    .replaceAll("%backpack%", backPack)));

                    return null;
                }

                BackPack.Type type = new BackPack.Type(config.getString(backPackPath + "type"));

                Boolean furnaceGui = false;

                if (type.equals(furnace)) {
                    furnaceGui = config.getBoolean(backPackPath + "gui.enabled");
                }

                String inventoryTitle = "";
                String inventoryTitleConfig = config.getString(backPackPath + "inventoryTitle");

                if (inventoryTitleConfig != null) {
                    inventoryTitle = inventoryTitleConfig;
                }

                BackPack finishedBackPack = new BackPack(backPack, type, slots, furnaceGui, lore, inventoryTitle, open, close)  ;

                backPacks.add(finishedBackPack);
                backPacksItems.put(finishedBackPack, item);

                return item;

            } catch (IllegalArgumentException e) {
                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                        messages.getString("Help.materialNotValid")
                                .replaceAll("%material%", materialString).replaceAll("%backpack%", backPack)));

                return null;
            }

        } else {
            slotsDivisible = false;
        }

        return null;

    }

    private static ShapedRecipe createShapedRecipe(CommandSender sender, ItemStack item, BackPack backPack, String backPackPath) {

        ShapedRecipe recipe = new ShapedRecipe(item);

        String crafting1 = config.getString(backPackPath + "crafting.1");
        String crafting2 = config.getString(backPackPath + "crafting.2");
        String crafting3 = config.getString(backPackPath + "crafting.3");

        recipe.shape(
                crafting1.replaceAll("\\+", ""),
                crafting2.replaceAll("\\+", ""),
                crafting3.replaceAll("\\+", ""));

        backPack.setCraftingRecipe(crafting1 + "/" + crafting2 + "/" + crafting3);

        Map<String, Object> ingredients = config.getConfigurationSection(backPackPath +
                "crafting.materials").getValues(true);

        for (Map.Entry<String, Object> entry : ingredients.entrySet()) {
            char ingredient = entry.getKey().charAt(0);
            String material = entry.getValue().toString().toUpperCase();

            backPack.setMaterials(backPack.getMaterials() + ingredient + ":" + material + "/");

            try {
                recipe.setIngredient(ingredient, Material.valueOf(material));

            } catch (IllegalArgumentException e) {
                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                        messages.getString("Help.materialNotValid")
                                .replaceAll("%material%", material).replaceAll("%backpack%", backPack.getName())));

                return null;
            }

        }

        String materials = backPack.getMaterials();

        if (!materials.equals("")) {
            backPack.setMaterials(materials.substring(0, materials.length() - 1));
        }

        return recipe;
    }

}
