package bobby.monitor;

import org.bukkit.scheduler.BukkitRunnable;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.config.ConfigManager;
import bobby.logging.BobbyLogger;
import bobby.detectors.DetectorManager;

public class TickMonitorTask extends BukkitRunnable {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;
    private boolean isScanning = false;

    public TickMonitorTask(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        ConfigManager config = registry.get(ConfigManager.class);
        runTaskTimer(plugin, 600L, config.getScanIntervalTicks()); // Задержка старта 30 сек
    }

    @Override
    public void run() {
        if (isScanning) return; // Пропускаем, если сканирование уже идет

        ServerProfiler profiler = registry.get(ServerProfiler.class);
        ConfigManager config = registry.get(ConfigManager.class);

        double mspt = profiler.getMspt();

        if (mspt > config.getAlertThresholdMspt()) {
            BobbyLogger logger = registry.get(BobbyLogger.class);
            logger.warning(String.format("Обнаружен средний скачок MSPT: %.2f ms", mspt));

            isScanning = true;
            DetectorManager detectorManager = registry.get(DetectorManager.class);
            detectorManager.scanAll().thenRun(() -> {
                isScanning = false;
            });
        }
    }
}
