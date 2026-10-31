package anarchy.minesky.database;

import anarchy.minesky.AnarchyClans;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MongoDBManager {
    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection<Document> clans;

    static {
        silenciarLogsMongo();
    }

    public void connect(String uri, String dbName) {
        silenciarLogsMongo();

        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .serverApi(serverApi)
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                )

                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(5, TimeUnit.SECONDS)
                )

                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(30)
                        .minSize(2)
                        .maxWaitTime(3, TimeUnit.SECONDS)
                        .maxConnectionIdleTime(60, TimeUnit.SECONDS)
                        .maxConnectionLifeTime(30, TimeUnit.MINUTES)
                )
                .build();

        try {
            client = MongoClients.create(settings);
            db = client.getDatabase(dbName);
            clans = db.getCollection("clans");
            AnarchyClans.l.info("| Conectado ao MongoDB com sucesso!");
        } catch (Exception e) {
            AnarchyClans.l.severe("| Falha ao conectar ao MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public MongoCollection<Document> getClans() {
        return clans;
    }

    public void close() {
        if (client != null) {
            MongoClient clientToClose = client;
            client = null;
            db = null;
            clans = null;

            CompletableFuture.runAsync(() -> {
                try {
                    clientToClose.close();
                    AnarchyClans.l.info("| Desconectado do MongoDB com sucesso.");
                } catch (Exception e) {
                    AnarchyClans.l.warning("| Erro ao fechar conexão com MongoDB: " + e.getMessage());
                }
            });
        }
    }

    public boolean isConnected() {
        if (client == null || db == null) {
            return false;
        }

        try {
            db.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void silenciarLogsMongo() {
        String[] loggers = {
                "org.mongodb.driver",
                "org.mongodb.driver.cluster",
                "org.mongodb.driver.client",
                "org.mongodb.driver.connection"
        };

        try {
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object warnLevel = levelClass.getField("WARN").get(null);

            java.lang.reflect.Method setLevel = configuratorClass.getMethod("setLevel", String.class, levelClass);
            for (String loggerName : loggers) {
                setLevel.invoke(null, loggerName, warnLevel);
            }
        } catch (Throwable ignored) {
        }

        try {
            for (String loggerName : loggers) {
                java.util.logging.Logger.getLogger(loggerName).setLevel(java.util.logging.Level.WARNING);
            }
        } catch (Throwable ignored) {
        }
    }
}