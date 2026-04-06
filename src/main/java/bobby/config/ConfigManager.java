package bobby.config;

import org.bukkit.configuration.file.FileConfiguration;
import bobby.core.BobbyPlugin;

public class ConfigManager {

    private final BobbyPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(BobbyPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public int getScanIntervalTicks() {
        return config.getInt("scan_interval_ticks", 100);
    }

    public int getEntityLimitPerChunk() {
        return config.getInt("limits.entity_per_chunk", 50);
    }

    public int getHopperChainLength() {
        return config.getInt("limits.hopper_chain_length", 50);
    }

    public int getRedstoneMaxChanges() {
        return config.getInt("limits.redstone.max_changes_per_sec", 15);
    }

    public double getPlayerSpeedLimit() {
        return config.getDouble("limits.player.speed_limit", 1.5);
    }

    public int getChunkLoadLimit() {
        return config.getInt("limits.chunk.load_limit_per_sec", 15);
    }

    public double getAlertThresholdMspt() {
        return config.getDouble("alerts.threshold_mspt", 45.0);
    }

    public String getWebhookUrl() {
        return config.getString("alerts.discord_webhook_url", "");
    }
}
