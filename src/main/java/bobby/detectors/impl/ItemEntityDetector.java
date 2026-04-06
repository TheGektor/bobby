package bobby.detectors.impl;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.Detector;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.concurrent.CompletableFuture;

public class ItemEntityDetector implements Detector {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    public ItemEntityDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Детектор предметов";
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean found = false;
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    int itemCount = 0;
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof Item) {
                            itemCount++;
                        }
                    }
                    if (itemCount > 100) { // Хардкодим лимит для примера
                        double estimatedImpact = Math.min((itemCount - 100) * 0.05, 30.0);
                        if(estimatedImpact > 3.0) {
                            AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                            analytics.registerReport(new LagReport(
                                    "Спам предметами",
                                    "Выброшенные предметы (" + itemCount + " шт.)",
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
