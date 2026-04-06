package bobby.detectors.impl;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.Detector;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.util.concurrent.CompletableFuture;

public class TileEntityDetector implements Detector {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    public TileEntityDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Детектор Tile сущностей";
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean found = false;
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    int count = chunk.getTileEntities().length;
                    if (count > 150) {
                        double estimatedImpact = Math.min((count - 150) * 0.1, 40.0);
                        if(estimatedImpact > 4.0) {
                            AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                            analytics.registerReport(new LagReport(
                                    "Tile-сущности",
                                    "Скопление тайлов (" + count + " шт.)",
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
