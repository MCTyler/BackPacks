package at.michael1011.backpacks.listeners;

import at.michael1011.backpacks.BackPack;
import at.michael1011.backpacks.Main;
import at.michael1011.backpacks.SQL;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static at.michael1011.backpacks.Crafting.backPacksMap;
import static at.michael1011.backpacks.Main.*;
import static at.michael1011.backpacks.nbt.CheckItem.checkItem;

public class EntityDeath implements Listener {

    public EntityDeath(Main main) {
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    public void entityDeath(final EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();

        final Player p = entity.getKiller();

        if (p != null) {
            switch (entity.getType()) {
                case CHICKEN:
                case COW:
                case MUSHROOM_COW:
                case PIG:
                case SHEEP:
                case RABBIT:
                    ItemStack[] inv = p.getInventory().getContents();

                    for (ItemStack item : inv) {
                        if (item != null) {
                            String backPackString = checkItem(item);

                            if (backPackString != null) {
                                BackPack backPack = backPacksMap.get(backPackString);

                                if (backPack != null) {
                                    if (backPack.getType().equals(BackPack.Type.furnace)) {
                                        final Location loc = e.getEntity().getLocation();

                                        final List<ItemStack> toDrop = new ArrayList<>();

                                        toDrop.addAll(e.getDrops());

                                        e.getDrops().clear();

                                        if (backPack.getFurnaceGui()) {
                                            final String trimmedID = getTrimmedId(p);

                                            SQL.getResult("SELECT * FROM bp_furnaces WHERE uuid='" + trimmedID + "'",
                                                    new SQL.Callback<ResultSet>() {

                                                        @Override
                                                        public void onSuccess(ResultSet rs) {
                                                            try {
                                                                if (rs.first()) {
                                                                    if (Boolean.valueOf(rs.getString("food"))) {
                                                                        int amount = rs.getInt("coal");

                                                                        if (amount > 0) {
                                                                            SQL.query("UPDATE bp_furnaces SET coal=" + String.valueOf(amount-1) + " WHERE uuid='" + trimmedID + "'",
                                                                                    new SQL.Callback<Boolean>() {

                                                                                        @Override
                                                                                        public void onSuccess(Boolean rs) {
                                                                                            smelt(toDrop, loc);
                                                                                        }

                                                                                        @Override
                                                                                        public void onFailure(Throwable e) {}

                                                                                    });

                                                                        } else {
                                                                            drop(toDrop, loc);

                                                                            p.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&',
                                                                                    messages.getString("BackPacks.furnaceBackPack.noCoal")));
                                                                        }

                                                                    } else {
                                                                        drop(toDrop, loc);
                                                                    }

                                                                } else {
                                                                    drop(toDrop, loc);

                                                                    p.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                                                            messages.getString("BackPacks.furnaceBackPack.noCoal")));
                                                                }

                                                            } catch (SQLException e) {
                                                                e.printStackTrace();
                                                            }

                                                        }

                                                        @Override
                                                        public void onFailure(Throwable e) {}

                                                    });

                                        } else {
                                            smelt(toDrop, loc);
                                        }

                                        break;
                                    }

                                }

                            }

                        }

                    }

                    break;

            }

        }

    }

    private void drop(List<ItemStack> toDrop, Location loc) {
        for (ItemStack drop : toDrop) {
            loc.getWorld().dropItem(loc, drop);
        }
    }

    private void smelt(List<ItemStack> toSmelt, Location loc) {
        World world = loc.getWorld();

        for (ItemStack smelt : toSmelt) {
            switch (smelt.getType()) {
                case RAW_CHICKEN:
                    world.dropItem(loc, new ItemStack(Material.COOKED_CHICKEN, smelt.getAmount()));

                    break;

                case RAW_BEEF:
                    world.dropItem(loc, new ItemStack(Material.COOKED_BEEF, smelt.getAmount()));

                    break;

                case PORK:
                    world.dropItem(loc, new ItemStack(Material.GRILLED_PORK, smelt.getAmount()));

                    break;

                case MUTTON:
                    world.dropItem(loc, new ItemStack(Material.COOKED_MUTTON, smelt.getAmount()));

                    break;

                case RABBIT:
                    world.dropItem(loc, new ItemStack(Material.COOKED_RABBIT, smelt.getAmount()));

                    break;

                default:
                    world.dropItem(loc, smelt);

                    break;
            }

        }

    }

}
