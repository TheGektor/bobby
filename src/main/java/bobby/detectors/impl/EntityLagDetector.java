package bobby.detectors.impl;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.config.ConfigManager;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.Detector;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public class EntityLagDetector implements Detector {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    public EntityLagDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Детектор сущностей";
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ConfigManager config = registry.get(ConfigManager.class);
        int limit = config.getEntityLimitPerChunk();

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean found = false;
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    int count = chunk.getEntities().length;
                    if (count > limit) {
                        double estimatedImpact = Math.min((count - limit) * 0.1, 50.0);
                        if(estimatedImpact > 5.0) {
                            AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                            analytics.registerReport(new LagReport(
                                    "Перегрузка сущностей",
                                    "Моб ферма или спавнер (" + count + " сущн.)",
                                    estimatedImpact,
                                    chunk.getBlock(0, 0, 0).getLocation(),
                                    null
                            ));
                            found = true;
                            break; 
                        }
                    }
                }
                if (found) break;
            }
            future.complete(null);
        });

        return future;
    }

    @Override
    public void stop() { }
}
