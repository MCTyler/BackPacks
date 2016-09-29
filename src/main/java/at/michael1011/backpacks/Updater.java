package at.michael1011.backpacks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import static at.michael1011.backpacks.Main.*;

class Updater {

    // fixme: check md5 before deleting old file
    // fixme: check if release type is release

    Updater(final Main main) {
        int interval = config.getInt("Updater.interval")*72000;

        Bukkit.getScheduler().runTaskTimerAsynchronously(main, new Runnable() {
            @Override
            public void run() {
                checkUpdates(main, Bukkit.getConsoleSender());
            }

        }, interval, interval);
    }


    static void checkUpdates(Main main, CommandSender sender) {
        try {
            HttpsURLConnection con = (HttpsURLConnection)
                    new URL("https://api.curseforge.com/servermods/files?projectIds=98508").openConnection();

            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();

            if(responseCode == 200) {
                BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));

                JSONArray array = (JSONArray) new JSONParser().parse(input);

                input.close();

                JSONObject latestVersion = (JSONObject) array.get(array.size()-1);

                String downloadUrl = String.valueOf(latestVersion.get("downloadUrl"));

                String latestVersionNumber = String.valueOf(latestVersion.get("name"));
                String installedVersionNumber = main.getDescription().getVersion();

                if(Integer.valueOf(installedVersionNumber.replaceAll("\\.", "")) <
                        Integer.valueOf(latestVersionNumber.replaceAll("\\.", ""))) {

                    sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                            messages.getString("Updater.newVersionAvailable")
                                    .replaceAll("%newVersion%", latestVersionNumber)
                                    .replaceAll("%oldVersion%", installedVersionNumber)));

                    if(config.getBoolean("Updater.autoUpdate")) {
                        String messagesPath = "Updater.autoUpdate.";

                        URL download = followRedirects(downloadUrl);

                        int fileSize = download.openConnection().getContentLength();

                        BufferedInputStream downloadInput = new BufferedInputStream(download.openStream());

                        FileOutputStream fileOutput = new FileOutputStream(new File("plugins",
                                downloadUrl.substring(downloadUrl.lastIndexOf('/')+1, downloadUrl.length())));

                        int byteSize = 1024;

                        final byte[] data = new byte[byteSize];
                        long downloaded = 0;
                        int count;

                        while((count = downloadInput.read(data, 0, byteSize)) != -1) {
                            downloaded += count;

                            fileOutput.write(data, 0, count);

                            int percent = (int) ((downloaded * 100) / fileSize);

                            if((percent % 10) == 0) {
                                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                        messages.getString(messagesPath+"downloading")
                                                .replaceAll("%percent%", String.valueOf(percent))
                                                .replaceAll("%fileSize%", readableByteCount(fileSize, false))));
                            }
                        }

                        downloadInput.close();
                        fileOutput.close();

                        File old = new File(Main.class.getProtectionDomain().getCodeSource().getLocation()
                                .toURI().getPath());

                        if(old.delete()) {
                            sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                    messages.getString(messagesPath+"deletedOldVersion")
                                            .replaceAll("%oldVersion%", old.getName())));
                        }

                        sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                messages.getString(messagesPath+"successful")));

                        Bukkit.getScheduler().cancelTasks(main);

                        Bukkit.dispatchCommand(sender, "reload");

                    } else {
                        sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                                messages.getString("Updater.newVersionAvailableDownload")
                                        .replaceAll("%newVersion%", latestVersionNumber)
                                        .replaceAll("%downloadLink%", downloadUrl)));
                    }

                }

            } else {
                sendConnectionError(sender);
            }

        } catch (IOException | ParseException | URISyntaxException e) {
            // todo: more error codes

            e.printStackTrace();

            sendConnectionError(sender);
        }

    }

    private static void sendConnectionError(CommandSender sender) {
        sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&',
                messages.getString("Updater.failedToReachServer")));
    }

    private static String readableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;

        if (bytes < unit) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");

        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static URL followRedirects(String location) throws IOException {
        URL resourceUrl, base, next;
        HttpURLConnection conn;
        String redLoc;

        while (true) {
            resourceUrl = new URL(location);
            conn = (HttpURLConnection) resourceUrl.openConnection();

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0...");

            switch (conn.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    redLoc = conn.getHeaderField("Location");
                    base = new URL(location);
                    next = new URL(base, redLoc);  // Deal with relative URLs
                    location = next.toExternalForm();
                    continue;
            }
            break;
        }

        return conn.getURL();
    }

}