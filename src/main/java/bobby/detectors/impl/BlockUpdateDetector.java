package bobby.detectors.impl;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.config.ConfigManager;
import bobby.core.BobbyPlugin;
import bobby.core.PluginRegistry;
import bobby.detectors.Detector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockUpdateDetector implements Detector, Listener {

    private final BobbyPlugin plugin;
    private final PluginRegistry registry;

    // Мир -> ChunkKey -> Счетчик
    private final Map<String, Map<Long, AtomicInteger>> activityMap = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Location>> locationMap = new ConcurrentHashMap<>();

    public BlockUpdateDetector(BobbyPlugin plugin, PluginRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Детектор обновлений блоков";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Location loc = event.getBlock().getLocation();
        String worldName = loc.getWorld().getName();
        long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        
        activityMap.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(chunkKey, k -> new AtomicInteger(0))
                   .incrementAndGet();
                   
        locationMap.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
                   .putIfAbsent(chunkKey, loc);
    }

    @Override
    public CompletableFuture<Void> scan() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ConfigManager config = registry.get(ConfigManager.class);
        
        // Лимиты по обновлениям обычно выше, чем по красному камню
        int intervalTicks = config.getScanIntervalTicks();
        double intervalSeconds = intervalTicks / 20.0;
        int maxChangesAllowed = (int) (config.getRedstoneMaxChanges() * 3 * intervalSeconds); // Физики в 3-4 раза больше обычно
        if (maxChangesAllowed < 200) maxChangesAllowed = 200;

        int finalMaxAllowed = maxChangesAllowed;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean found = false;
            
            for (Map.Entry<String, Map<Long, AtomicInteger>> worldEntry : activityMap.entrySet()) {
                String worldName = worldEntry.getKey();
                Map<Long, AtomicInteger> chunks = worldEntry.getValue();
                
                for (Map.Entry<Long, AtomicInteger> chunkEntry : chunks.entrySet()) {
                    long chunkKey = chunkEntry.getKey();
                    int count = chunkEntry.getValue().get();
                    
                    if (count > finalMaxAllowed) {
                        double estimatedImpact = Math.min((count - finalMaxAllowed) * 0.015, 150.0);
                        if (estimatedImpact > 3.0) {
                            Location loc = locationMap.get(worldName).get(chunkKey);
                            if (loc == null) continue;
                            
                            AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                            analytics.registerReport(new LagReport(
                                    "Блоковая лаг-машина",
                                    "Шторм обновлений блоков (" + count + " апдейтов)",
                                    estimatedImpact,
                                    loc,
                                    null
                            ));
                            found = true;
                            break; 
                        }
                    }
                }
                
                chunks.clear();
                Map<Long, Location> locs = locationMap.get(worldName);
                if (locs != null) locs.clear();
                
                if (found) break;
            }
            
            future.complete(null);
        });

        return future;
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
        activityMap.clear();
        locationMap.clear();
    }
    
    private long getChunkKey(int x, int z) {
        return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
    }
}
