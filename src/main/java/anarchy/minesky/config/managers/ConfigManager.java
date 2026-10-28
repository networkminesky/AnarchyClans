package anarchy.minesky.config.managers;

import anarchy.minesky.AnarchyClans;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class ConfigManager {
    public static YamlConfiguration yml;
    public static void createConfig(String file) {
        if (!new File(AnarchyClans.getInstance().getDataFolder(), file).exists()) {
            AnarchyClans.l.info("| Criando a config " + file + "...");
            AnarchyClans.getInstance().saveResource(file, false);
        }
    }

    public static void createConfig(String file, String folder) {
        File dataFolder = new File(AnarchyClans.getInstance().getDataFolder(), folder);
        if (!dataFolder.exists()) {
            AnarchyClans.l.info("| Crinado a pasta " + folder + "...");
            dataFolder.mkdirs();
        }
        File configFolder = new File(dataFolder, file);
        yml = YamlConfiguration.loadConfiguration(configFolder);
        if (!configFolder.exists()) {
            InputStreamReader defConfigFolder = new InputStreamReader(Objects.requireNonNull(AnarchyClans.getInstance().getResource(file)));
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defConfigFolder);
            yml.setDefaults(defaultConfig);
            try {
                AnarchyClans.l.info("| Criando a config " + file + "...");
                defaultConfig.save(configFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static FileConfiguration getConfig(String file) {
        File arquivo = new File(AnarchyClans.getInstance().getDataFolder() + File.separator + file);
        return YamlConfiguration.loadConfiguration(arquivo);
    }

    public static FileConfiguration getConfig(String file, String folder) {
        File dataFolder = new File(AnarchyClans.getInstance().getDataFolder(), folder);
        File configFolder = new File(dataFolder, file);
        return YamlConfiguration.loadConfiguration(configFolder);
    }
}
