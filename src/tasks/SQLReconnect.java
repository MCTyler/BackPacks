package tasks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import static main.Main.closeSQL;
import static main.Main.establishMySQL;

public class SQLReconnect {

    public SQLReconnect(Plugin pl) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(pl, new Runnable() {
            @Override
            public void run() {
                closeSQL();
                establishMySQL();
            }
        }, 72000L, 72000L);
    }

}
