package anarchy.minesky;

import anarchy.minesky.config.Config;
import anarchy.minesky.config.managers.ConfigManager;
import anarchy.minesky.database.MongoDBManager;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class AnarchyClans extends JavaPlugin {

    private static AnarchyClans instance;

    public static Logger l;
    public MongoDBManager mm;
    public MongoCollection<Document> coll;

    @Override
    public void onEnable() {
        instance = this;
        l = this.getLogger();
        createConfigs();
        loadConfigs();
        loadMongoDB();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void loadMongoDB() {
        if (mm != null) {
            //handler.close();
        }
        l.info("Connecting MongoDB...");

        mm = new MongoDBManager();
        mm.connect(Config.MongoUri, Config.StorageDatabase);
        coll = mm.getClans();
    }

    private void createConfigs() {
        l.info("Initializing plugin configurations...");
        ConfigManager.createConfig("config.yml");
    }

    public void loadConfigs() {
        Config.loadConfig();
    }


    public static AnarchyClans getInstance() {
        return instance;
    }
}
