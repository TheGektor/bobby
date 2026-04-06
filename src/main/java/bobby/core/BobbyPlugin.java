package bobby.core;

import org.bukkit.plugin.java.JavaPlugin;
import bobby.config.ConfigManager;
import bobby.logging.BobbyLogger;
import bobby.monitor.ServerProfiler;
import bobby.monitor.TickMonitorTask;
import bobby.analytics.AnalyticsManager;
import bobby.discord.DiscordWebhook;
import bobby.commands.BobbyCommand;
import bobby.detectors.DetectorManager;

public class BobbyPlugin extends JavaPlugin {

    private static BobbyPlugin instance;
    private PluginRegistry registry;

    @Override
    public void onEnable() {
        instance = this;
        this.registry = new PluginRegistry();
        
        saveDefaultConfig();

        // 1. Инициализация базовых систем
        BobbyLogger logger = new BobbyLogger(this);
        registry.register(BobbyLogger.class, logger);
        logger.info("Инициализация плагина Bobby...");

        ConfigManager configManager = new ConfigManager(this);
        registry.register(ConfigManager.class, configManager);

        DiscordWebhook discordWebhook = new DiscordWebhook(configManager.getWebhookUrl());
        registry.register(DiscordWebhook.class, discordWebhook);

        AnalyticsManager analytics = new AnalyticsManager();
        registry.register(AnalyticsManager.class, analytics);

        // 2. Мониторинг и детекторы
        ServerProfiler profiler = new ServerProfiler();
        registry.register(ServerProfiler.class, profiler);

        DetectorManager detectorManager = new DetectorManager(this, registry);
        registry.register(DetectorManager.class, detectorManager);

        TickMonitorTask tickMonitor = new TickMonitorTask(this, registry);
        tickMonitor.start();
        registry.register(TickMonitorTask.class, tickMonitor);

        // 3. Команды
        getCommand("bobby").setExecutor(new BobbyCommand(registry));
        
        logger.info("Плагин Bobby успешно запущен!");
    }

    @Override
    public void onDisable() {
        BobbyLogger logger = registry.get(BobbyLogger.class);
        if (logger != null) {
            logger.info("Выключение плагина Bobby...");
            logger.close();
        }
        
        DetectorManager detectorManager = registry.get(DetectorManager.class);
        if (detectorManager != null) {
            detectorManager.stopAll();
        }
    }

    public static BobbyPlugin getInstance() {
        return instance;
    }

    public PluginRegistry getRegistry() {
        return registry;
    }
}
