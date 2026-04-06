package bobby.analytics;

import bobby.core.BobbyPlugin;
import bobby.discord.DiscordWebhook;
import bobby.logging.BobbyLogger;
import bobby.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalyticsManager {

    private final List<LagReport> recentReports = Collections.synchronizedList(new ArrayList<>());
    
    public void registerReport(LagReport report) {
        if (recentReports.size() > 100) {
            recentReports.remove(0);
        }
        recentReports.add(report);

        BobbyPlugin plugin = BobbyPlugin.getInstance();
        BobbyLogger logger = plugin.getRegistry().get(BobbyLogger.class);
        DiscordWebhook discord = plugin.getRegistry().get(DiscordWebhook.class);

        // Лог в файл
        logger.logLag(report.getType(), report.getSourceName(), report.getImpactMspt(), MessageUtils.formatLoc(report.getLocation()));

        // Discord Webhook
        discord.sendAlert(
            "Обнаружен источник лагов",
            report.getType() + " (" + report.getSourceName() + ")",
            MessageUtils.formatLoc(report.getLocation()),
            report.getPlayerName(),
            report.getImpactMspt(),
            report.getSeverity(),
            report.getSeverityColor()
        );

        // Сообщение администраторам онлайн
        Component adminMsg = MessageUtils.createLagMessage(
            report.getType(),
            report.getSeverity(),
            report.getImpactMspt(),
            report.getLocation()
        );

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("bobby.moderator")) {
                p.sendMessage(adminMsg);
            }
        });
    }

    public List<LagReport> getRecentReports() {
        return new ArrayList<>(recentReports);
    }
}
