package main;

import commands.Give;
import commands.Help;
import commands.Reload;
import listeners.BackPack;
import listeners.SaveLoadSQL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.UUID;
import listeners.SaveLoad;

public class main extends JavaPlugin {

    private static main instance;

    public static File configF, namesF, backpacksF;
    public static FileConfiguration config, names, backpacks;

    private static Connection connection;
    private String host, port, database, username, password;

    public static HashMap<UUID, Inventory> littleB = new HashMap<UUID, Inventory>();
    public static HashMap<UUID, Inventory> normalB = new HashMap<UUID, Inventory>();

    @Override
    public void onEnable() {
        instance = this;

        createFiles();

        Boolean SQLenable = config.getBoolean("MySQL.enable");

        if(SQLenable) {
            establishMySQL();
            new SaveLoadSQL(this);
        } else {
            new SaveLoad(this);
        }

        new Reload(this);
        new Give(this);
        new Help(this);
        new Crafting(this);
        new BackPack(this);

        Crafting.CraftingB();

        for(Player p : Bukkit.getOnlinePlayers()) {
            createInv(p);
        }

        Bukkit.getConsoleSender().sendMessage(Pref.p+ChatColor.RED+"Plugin enabled!");

        if(connection != null) {
            Bukkit.getConsoleSender().sendMessage(Pref.p+ChatColor.YELLOW+"MySQL connection enabled!");
        }

        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage(Pref.p+ChatColor.RED+"Failed to connect to Metrics!");
        }
    }

    public static void update(String s) {
        try {
            PreparedStatement ps = connection.prepareStatement(s);
            ps.execute();
        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static ResultSet getResult(String s) throws SQLException{
        try {
            PreparedStatement ps = connection.prepareStatement(s);
            return ps.executeQuery();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onDisable() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            save(p);
        }

        if(config.getBoolean("MySQL.enable")) {
            try {
                backpacks.save(backpacksF);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(connection != null) {
            try {
                connection.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            Bukkit.getConsoleSender().sendMessage(Pref.p+ChatColor.YELLOW+"MySQL connection disabled!");
        }

        instance = null;

        Bukkit.getConsoleSender().sendMessage(Pref.p + ChatColor.RED +"Plugin disabled!");
    }

    private void establishMySQL() {
        host = config.getString("MySQL.host");
        port = config.getString("MySQL.port");
        database = config.getString("MySQL.database");
        username = config.getString("MySQL.username");
        password = config.getString("MySQL.password");

        try {
            openConnection();
            Statement statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection openConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://"
                        + this.host+ ":" + this.port + "/" + this.database, this.username, this.password);

        return connection;
    }

    private void createInv(Player p) {

        UUID id = p.getUniqueId();

        if(!config.getBoolean("MySQL.enable")) {
            Inventory inv = Bukkit.getServer().createInventory(p, names.getInt("LittleBackPack.Slots"), ChatColor.translateAlternateColorCodes('&', names.getString("LittleBackPack.Name")));
            Inventory invN = Bukkit.getServer().createInventory(p, names.getInt("NormalBackPack.Slots"), ChatColor.translateAlternateColorCodes('&', names.getString("NormalBackPack.Name")));

            if(backpacks.contains("littleB."+id)) {
                for(String item : backpacks.getConfigurationSection("littleB."+id).getKeys(false)) {
                    inv.addItem(SaveLoad.load(backpacks.getConfigurationSection("littleB."+id+"."+item)));
                }
            }

            if(backpacks.contains("normalB."+id)) {
                for(String item : backpacks.getConfigurationSection("normalB."+id).getKeys(false)) {
                    invN.addItem(SaveLoad.load(backpacks.getConfigurationSection("normalB."+id+"."+item)));
                }
            }

            littleB.put(id, inv);
            normalB.put(id, invN);

        } else {
            String ID = id.toString();
            String trimmedID = ID.replaceAll("-", "");

            if(p.hasPermission("backpacks.littleBackPack")) {
                SaveLoadSQL.createTableLB(trimmedID, "littleBP");

                Inventory inv = Bukkit.getServer().createInventory(p, names.getInt("LittleBackPack.Slots"), ChatColor.translateAlternateColorCodes('&', names.getString("LittleBackPack.Name")));

                try {
                    ResultSet rs = getResult("select * from littleBP_"+trimmedID);

                    assert rs != null;
                    while(rs.next()) {
                        inv.setItem(rs.getInt(4), SaveLoadSQL.load(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(5), rs.getString(6), rs.getString(7)));
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                littleB.put(id, inv);
            }

            if(p.hasPermission("backpacks.normalBackPack")) {
                SaveLoadSQL.createTableLB(trimmedID, "normalBP");

                Inventory inv = Bukkit.getServer().createInventory(p, names.getInt("NormalBackPack.Slots"), ChatColor.translateAlternateColorCodes('&', names.getString("NormalBackPack.Name")));

                try {
                    ResultSet rs = getResult("select * from normalBP_"+trimmedID);

                    assert rs != null;
                    while(rs.next()) {
                        inv.setItem(rs.getInt(4), SaveLoadSQL.load(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(5), rs.getString(6), rs.getString(7)));
                    }

                } catch(SQLException e) {
                    e.printStackTrace();
                }

                normalB.put(id, inv);
            }

        }
    }

    private void save(Player p) {
        UUID id = p.getUniqueId();

        if(!config.getBoolean("MySQL.enable")) {
            if(p.hasPermission("backpacks.littleBackPack")) {
                for(int in = 0; in < names.getInt("LittleBackPack.Slots"); in++) {
                    if(backpacks.get("littleB."+id+"."+in) != null) {
                        backpacks.set("littleB."+id+"."+in, null);
                    }
                }

                if(!backpacks.contains("littleB."+id)) {
                    backpacks.createSection("littleB."+id);
                }

                for(int i = 0; i < names.getInt("LittleBackPack.Slots") ; i++) {
                    if(littleB.get(id).getItem(i) != null) {
                        SaveLoad.save(backpacks.createSection("littleB."+id+"."+i), littleB.get(id).getItem(i), i);
                    }
                }
            }

            if(p.hasPermission("backpacks.normalBackPack")) {
                for(int in2 = 0; in2 < names.getInt("NormalBackPack.Slots"); in2++) {
                    if(backpacks.get("normalB."+id+"."+in2) != null) {
                        backpacks.set("normalB."+id+"."+in2, null);
                    }
                }

                if(!backpacks.contains("normalB."+id)) {
                    backpacks.createSection("normalB."+id);
                }

                for(int i = 0; i < names.getInt("NormalBackPack.Slots") ; i++) {
                    if(normalB.get(id).getItem(i) != null) {
                        SaveLoad.save(backpacks.createSection("normalB."+id+"."+i), normalB.get(id).getItem(i), i);
                    }
                }
            }

            try {
                backpacks.save(backpacksF);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
            String ID = id.toString();
            String trimmedID = ID.replaceAll("-", "");

            if(p.hasPermission("backpacks.littleBackPack")) {
                SaveLoadSQL.createTableLB(trimmedID, "littleBP");

                update("delete from littleBP_"+trimmedID);

                for(int i = 0; i < names.getInt("LittleBackPack.Slots") ; i++) {
                    if(littleB.get(id).getItem(i) != null) {
                        SaveLoadSQL.save(littleB.get(id).getItem(i), trimmedID, "littleBP_", i);
                    }
                }
            }

            if(p.hasPermission("backpacks.normalBackPack")) {
                SaveLoadSQL.createTableLB(trimmedID, "normalBP");

                update("delete from normalBP_"+trimmedID);

                for(int i = 0; i < names.getInt("NormalBackPack.Slots") ; i++) {
                    if(normalB.get(id).getItem(i) != null) {
                        SaveLoadSQL.save(normalB.get(id).getItem(i), trimmedID, "normalBP_", i);
                    }
                }
            }
        }

    }

    private void createFiles() {

        configF = new File(getDataFolder(), "config.yml");
        namesF = new File(getDataFolder(), "names.yml");
        backpacksF = new File(getDataFolder(), "backpacks.yml");

        if(!configF.exists()) {
            configF.getParentFile().mkdirs();
            copy(getResource("config.yml"), configF);
        }
        if(!namesF.exists()) {
            namesF.getParentFile().mkdirs();
            copy(getResource("names.yml"), namesF);
        }

        if(!backpacksF.exists()) {
            backpacksF.getParentFile().mkdirs();
            copy(getResource("backpacks.yml"), backpacksF);
        }

        config = new YamlConfiguration();
        names = new YamlConfiguration();
        backpacks = new YamlConfiguration();

        try {
            config.load(configF);
            names.load(namesF);
            backpacks.load(backpacksF);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void copy(InputStream in, File file) {

        try {

            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {

                out.write(buf, 0, len);

            }
            out.close();
            in.close();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }
}