package at.michael1011.backpacks.listeners;

import at.michael1011.backpacks.Main;
import at.michael1011.backpacks.SQL;
import at.michael1011.backpacks.Updater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

import static at.michael1011.backpacks.Main.getTrimmedId;

public class Join implements Listener {

    public Join(Main main) {
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void joinEvent(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        final String playerName = p.getName();

        if (!Main.availablePlayers.contains(playerName)) {
            Main.availablePlayers.add(playerName);
        }

        final String uuid = getTrimmedId(p);

        SQL.getResult("SELECT * FROM bp_users WHERE uuid='"+uuid+"'", new SQL.Callback<ResultSet>() {

            @Override
            public void onSuccess(ResultSet rs) {
                try {
                    if (rs != null) {
                        if (rs.first()) {
                            if (!rs.getString("name").equals(playerName)) {
                                SQL.query("UPDATE bp_users SET name='" + playerName + "' WHERE uuid='" + uuid + "'",
                                        new SQL.Callback<Boolean>() {

                                    @Override
                                    public void onSuccess(Boolean rs) {}

                                    @Override
                                    public void onFailure(Throwable e) {}

                                });
                            }

                        } else {
                            SQL.query("INSERT INTO bp_users (name, uuid) VALUES ('" + playerName + "', '" + uuid + "')",
                                    new SQL.Callback<Boolean>() {

                                @Override
                                public void onSuccess(Boolean rs) {}

                                @Override
                                public void onFailure(Throwable e) {}

                            });
                        }

                        if (Updater.updateAvailable) {
                            if (p.hasPermission("backpacks.update") || p.hasPermission("backpacks.*")) {
                                p.sendMessage(Updater.newVersion);
                                p.sendMessage(Updater.newVersionDownload);
                            }
                        }

                        rs.close();
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

            }

            @Override
            public void onFailure(Throwable e) {}

        });

    }

}