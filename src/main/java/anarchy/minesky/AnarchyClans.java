package anarchy.minesky;

import anarchy.minesky.commands.ClanChatCommand;
import anarchy.minesky.commands.ClanCommand;
import anarchy.minesky.commands.tabcompleter.ClanTabCompleter;
import anarchy.minesky.config.Config;
import anarchy.minesky.config.managers.ConfigManager;
import anarchy.minesky.data.Clan;
import anarchy.minesky.database.MongoDBManager;
import anarchy.minesky.database.PlayerDataManager;
import anarchy.minesky.enums.ClanChatType;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.handlers.ClanLocatorHandler;
import anarchy.minesky.handlers.InviteHandler;
import anarchy.minesky.handlers.requests.ClanRequestManager;
import anarchy.minesky.handlers.requests.ReagroupHandler;
import anarchy.minesky.hooks.ClansPlaceholder;
import com.mongodb.client.MongoCollection;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class AnarchyClans extends JavaPlugin {
    private static final long AUTO_SAVE_INTERVAL = 20L * 60 * 5;

    private static AnarchyClans instance;
    public static Logger l;

    public MongoDBManager mm;
    public MongoCollection<Document> coll;

    public ClanHandler handler;
    private InviteHandler inviteHandler;
    private ReagroupHandler reagroupHandler;
    private ClanRequestManager requestManager;
    private ClanLocatorHandler locator;
    private ClansPlaceholder clansPlaceholder;
    private ScheduledTask autoSaveTask;
    private PlayerDataManager playerData;

    @Override
    public void onEnable() {
        instance = this;
        l = this.getLogger();
        createConfigs();
        loadConfigs();
        if (!loadMongoDB()) {
            l.severe("Falha crítica ao conectar ao MongoDB. Desativando o plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.handler = new ClanHandler();
        this.inviteHandler = new InviteHandler(this);
        this.reagroupHandler = new ReagroupHandler(this);
        this.requestManager = new ClanRequestManager(this);
        this.locator = new ClanLocatorHandler(this);
        this.locator.syncAllOnlinePlayers();
        this.playerData = new PlayerDataManager();
        registerCommands();
        registerHooks();
        initializeTicks();
        l.info("AnarchyClans inicializado com sucesso!");
    }

    @Override
    public void onDisable() {
        l.info("Desligando AnarchyClans...");
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        if (clansPlaceholder != null) {
            clansPlaceholder.unregister();
        }

        if (handler != null) {
            handler.close();
        }

        instance = null;
    }

    public boolean loadMongoDB() {
        if (mm != null && handler != null) {
            handler.close();
        }

        l.info("Conectando ao MongoDB...");
        try {
            mm = new MongoDBManager();
            mm.connect(Config.MongoUri, Config.StorageDatabase);
            coll = mm.getClans();
            return coll != null;
        } catch (Exception e) {
            l.severe("Erro ao conectar ao MongoDB: " + e.getMessage());
            return false;
        }
    }

    private void createConfigs() {
        l.info("Carregando arquivos de configuração...");
        ConfigManager.createConfig("config.yml");
    }

    public void loadConfigs() {
        Config.loadConfig();
    }

    private void registerCommands() {
        l.info("Registrando comandos...");
        this.getCommand("clan").setExecutor(new ClanCommand(this));
        this.getCommand(".").setExecutor(new ClanChatCommand(this, ClanChatType.CLAN));
        this.getCommand("ally").setExecutor(new ClanChatCommand(this, ClanChatType.ALLY));
        this.getCommand("clan").setTabCompleter(new ClanTabCompleter());
    }

    private void registerHooks() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            l.info("Registrando hook do PlaceholderAPI...");
            clansPlaceholder = new ClansPlaceholder(this);
            clansPlaceholder.register();
        }
    }

    private void initializeTicks() {
        this.autoSaveTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
                this,
                task -> {
                    ClanHandler.saveClans();
                },
                AUTO_SAVE_INTERVAL,
                AUTO_SAVE_INTERVAL
        );
    }

    public static AnarchyClans getInstance() {
        return instance;
    }

    public ClanLocatorHandler getLocator() {
        return locator;
    }

    public ClanHandler getHandler() {
        return handler;
    }

    public InviteHandler getInviteHandler() {
        return inviteHandler;
    }

    public ReagroupHandler getReagroupHandler() {
        return reagroupHandler;
    }

    public ClanRequestManager getRequestManager() {
        return requestManager;
    }

    public PlayerDataManager getPlayerData() {
        return playerData;
    }
}