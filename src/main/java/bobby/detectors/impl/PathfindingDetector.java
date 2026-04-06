package bobby.detectors.impl;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.Detector;

import java.util.concurrent.CompletableFuture;

public class PathfindingDetector implements Detector {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    public PathfindingDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Детектор поиска пути";
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        // Эмуляция сложного поиска пути
        future.complete(null);
        return future;
    }

    @Override
    public void stop() { }
}
