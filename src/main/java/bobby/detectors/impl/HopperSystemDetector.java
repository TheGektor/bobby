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
import org.bukkit.block.Hopper;

import java.util.concurrent.CompletableFuture;

public class HopperSystemDetector implements Detector {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    public HopperSystemDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Детектор воронок";
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean found = false;
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    int hopperCount = 0;
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Hopper) {
                            hopperCount++;
                        }
                    }
                    if (hopperCount > 50) {
                        double estimatedImpact = Math.min((hopperCount - 50) * 0.2, 40.0);
                        if(estimatedImpact > 6.0) {
                            AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                            analytics.registerReport(new LagReport(
                                    "Перегрузка воронками",
                                    "Массив воронок (" + hopperCount + " шт.)",
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
