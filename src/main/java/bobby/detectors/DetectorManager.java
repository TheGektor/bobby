package bobby.detectors;

import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DetectorManager {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;
    private final List<Detector> detectors = new ArrayList<>();

    public DetectorManager(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        
        detectors.add(new EntityLagDetector(plugin, registry));
        detectors.add(new RedstoneLagDetector(plugin, registry));
        detectors.add(new ItemEntityDetector(plugin, registry));
        detectors.add(new HopperSystemDetector(plugin, registry));
        detectors.add(new TileEntityDetector(plugin, registry));
        detectors.add(new PlayerLagDetector(plugin, registry));
        detectors.add(new ChunkLoadDetector(plugin, registry));
        detectors.add(new BlockUpdateDetector(plugin, registry));
        detectors.add(new PathfindingDetector(plugin, registry));
        detectors.add(new MachineDetector(plugin, registry));
    }

    public CompletableFuture<Void> scanAll() {
        CompletableFuture<?>[] futures = detectors.stream()
            .map(Detector::scan)
            .toArray(CompletableFuture[]::new);
            
        return CompletableFuture.allOf(futures);
    }

    public void stopAll() {
        for (Detector d : detectors) {
            d.stop();
        }
    }
    
    public List<Detector> getDetectors() {
        return detectors;
    }
}
