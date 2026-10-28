package anarchy.minesky.config;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    public static String MongoUri;
    public static String StorageDatabase;
    public static double GuildCreatePrice;
    public static int GuildTagLimit;
    public static double GuildInvitePrice;
    public static int GuildInviteDuration;

    public static void loadConfig() {
        FileConfiguration config = AnarchyClans.getInstance().getConfig();

        MongoUri = getString(config, "mongodb.uri");
        StorageDatabase = getString(config, "mongodb.storage-database");
        GuildCreatePrice = config.getDouble("guilds.price", 0);
        GuildTagLimit = config.getInt("clan.tag-limit", 4);
        GuildInvitePrice = config.getDouble("clan.invite.price", 0);
        GuildInviteDuration = config.getInt("clan.invite.duration", 0);

        config.addDefault("clan.price", 0.0);
        config.addDefault("clan.tag-limit", 4);
        config.addDefault("clan.invite.price", 0.0);
        config.addDefault("clan.invite.duration", 0);

        config.options().copyDefaults(true);
        AnarchyClans.getInstance().saveConfig();
    }

    private static String getString(FileConfiguration config, String path) {
        return Utils.c(config.getString(path, "&cNão foi possivel localizar a mensagem '&e" + path + "&c' do arquivo &nconfig.yml&c."));
    }
}
