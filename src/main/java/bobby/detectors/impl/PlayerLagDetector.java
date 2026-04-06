package bobby.detectors.impl;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.Detector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class PlayerLagDetector implements Detector {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    public PlayerLagDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Детектор игроков";
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Очень быстрый полет или перемещение (например, на элитрах)
                if (player.getVelocity().length() > 2.5) {
                    AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                    analytics.registerReport(new LagReport(
                            "Сверхбыстрое перемещение",
                            "Генерация чанков игроком",
                            15.0, // Условное влияние
                            player.getLocation(),
                            player.getName()
                    ));
                    break;
                }
            }
            future.complete(null);
        });

        return future;
    }

    @Override
    public void stop() { }
}
