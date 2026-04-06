package bobby.analytics;

import org.bukkit.Location;

public class LagReport {
    private final String type;
    private final String sourceName;
    private final double impactMspt;
    private final Location location;
    private final String playerName;

    public LagReport(String type, String sourceName, double impactMspt, Location location, String playerName) {
        this.type = type;
        this.sourceName = sourceName;
        this.impactMspt = impactMspt;
        this.location = location;
        this.playerName = playerName; // Может быть null
    }

    public String getType() { return type; }
    public String getSourceName() { return sourceName; }
    public double getImpactMspt() { return impactMspt; }
    public Location getLocation() { return location; }
    public String getPlayerName() { return playerName; }
    
    public String getSeverity() {
        if (impactMspt > 40.0) return "Критический";
        if (impactMspt > 20.0) return "Высокий";
        if (impactMspt > 10.0) return "Средний";
        return "Низкий";
    }

    public int getSeverityColor() {
        if (impactMspt > 40.0) return 0xFF0000; // Красный
        if (impactMspt > 20.0) return 0xFFA500; // Оранжевый
        if (impactMspt > 10.0) return 0xFFFF00; // Желтый
        return 0x00FF00; // Зеленый
    }
}
