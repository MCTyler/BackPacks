package at.michael1011.backpacks.listeners;

import at.michael1011.backpacks.BackPack;
import at.michael1011.backpacks.Main;
import at.michael1011.backpacks.SQL;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static at.michael1011.backpacks.Main.getTrimmedId;
import static at.michael1011.backpacks.Main.version;
import static at.michael1011.backpacks.listeners.RightClick.*;
import static at.michael1011.backpacks.nbt.NbtEncoder.encodeNbt;

@SuppressWarnings("unchecked")
public class InventoryClose implements Listener {

    static List<Player> savingBackPacks = new ArrayList<>();

    public InventoryClose(Main main) {
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void invCloseEvent(final InventoryCloseEvent e) {
        saveBackPack((Player) e.getPlayer(), e.getView(), true, true);
    }

    public static void saveBackPack(Player p, InventoryView inv, Boolean async, Boolean playSound) {
        final BackPack backPack = openInvs.get(p);
        final BackPack furnace = openFurnaces.get(p);

        final String backPackCommand = openInvsOwners.get(p);

        final String trimmedID = getTrimmedId(p);

        if (backPackCommand != null) {
            openInvs.remove(p);
            openInvsOwners.remove(p);

            savingBackPacks.add(p);

            if (playSound) {
                playCloseSound(p, backPack);
            }

            saveBackPack(backPack, p, backPackCommand, inv, async);

        } else if (backPack != null) {
            openInvs.remove(p);

            savingBackPacks.add(p);

            if(playSound) {
                playCloseSound(p, backPack);
            }

            if (!backPack.getType().equals(BackPack.Type.trash)) {
                saveBackPack(backPack, p, trimmedID, inv, async);
            }

        } else if (furnace != null) {
            openFurnaces.remove(p);
            openFurnacesInvs.remove(p);

            int amount = 0;

            ItemStack coal = inv.getItem(35);

            if (coal != null) {
                if (coal.getType().equals(Material.COAL)) {
                    amount = coal.getAmount();

                } else if (!coal.getType().equals(Material.AIR)) {
                    Location location = p.getLocation();

                    location.getWorld().dropItemNaturally(location, coal);
                }

            }

            if (playSound) {
                playCloseSound(p, furnace);
            }

            SQL.query("UPDATE bp_furnaces SET coal=" + amount + " WHERE uuid='" + trimmedID + "'",
                    new SQL.Callback<Boolean>() {

                        @Override
                        public void onSuccess(Boolean rs) {}

                        @Override
                        public void onFailure(Throwable e) {}

            });

        } else if (openInvsOther.containsKey(p)) {
            if (playSound) {
                playCloseSound(p, openInvsOther.get(p));
            }

            openInvsOther.remove(p);
        }

    }

    private static void saveBackPack(final BackPack backPack, final Player p, final String trimmedID, final InventoryView inv,
                                     final Boolean async) {

        SQL.query("DELETE FROM bp_" + backPack.getName() + "_" + trimmedID, new SQL.Callback<Boolean>() {

            @Override
            public void onSuccess(Boolean bool) {
                for (int i = 0; i < backPack.getSlots(); i++) {
                    ItemStack item = inv.getItem(i);

                    if (item != null) {
                        if (!item.getType().equals(Material.AIR)) {
                            Boolean hasItemMeta = item.hasItemMeta();

                            String name = "";
                            String lore = "";
                            String nbt = "";

                            try {
                                Object nbtItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class)
                                        .invoke(null, item);

                                Object nbtCompound = nbtItemStack.getClass().getMethod("getTag").invoke(nbtItemStack);

                                if (nbtCompound != null) {
                                    nbt = encodeNbt(nbtCompound);
                                }

                            } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
                                e.printStackTrace();
                            }

                            String material = item.getType().toString();

                            if (hasItemMeta) {
                                ItemMeta itemM = item.getItemMeta();

                                if (itemM.hasDisplayName()) {
                                    name = itemM.getDisplayName();
                                }

                                List<String> rawLore = itemM.getLore();

                                StringBuilder builder = new StringBuilder();

                                if (itemM.hasLore()) {
                                    for (String loreLine : rawLore) {
                                        builder.append(loreLine).append("~");
                                    }

                                    lore = builder.toString();
                                }

                            }

                            SQL.query("INSERT INTO bp_" + backPack.getName() + "_" + trimmedID + " (position, material, durability, amount, " +
                                            "name, lore, enchantments, potion, nbt) VALUES ('" + i + "', '" + material + "', '" + item.getDurability() + "', " +
                                            "'" + item.getAmount() + "', '" + name + "', '" + lore + "', '', '', '" + nbt + "')",
                                    new SQL.Callback<Boolean>() {

                                        @Override
                                        public void onSuccess(Boolean rs) {}

                                        @Override
                                        public void onFailure(Throwable e) {}

                            }, async);

                        }

                    }

                }

                savingBackPacks.remove(p);

            }

            @Override
            public void onFailure(Throwable e) {}

        }, async);

    }

}
