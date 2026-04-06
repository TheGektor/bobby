package bobby.monitor;

import org.bukkit.Bukkit;

public class ServerProfiler {

    public double getTps() {
        // Returns the 1m, 5m, 15m TPS averages. We take 1m.
        return Bukkit.getServer().getTPS()[0];
    }

    public double getMspt() {
        return Bukkit.getServer().getAverageTickTime();
    }
}
