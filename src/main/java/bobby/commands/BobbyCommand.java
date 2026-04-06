package bobby.commands;

import bobby.analytics.AnalyticsManager;
import bobby.analytics.LagReport;
import bobby.core.PluginRegistry;
import bobby.detectors.DetectorManager;
import bobby.monitor.ServerProfiler;
import bobby.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BobbyCommand implements CommandExecutor {

    private final PluginRegistry registry;

    public BobbyCommand(PluginRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bobby.viewer")) {
            MessageUtils.sendMessage(sender, "<red>У вас нет прав для использования данной команды.</red>");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "scan":
                handleScan(sender);
                break;
            case "report":
                handleReport(sender);
                break;
            case "profiler":
                handleProfiler(sender);
                break;
            case "lag":
            case "entities":
            case "redstone":
            case "chunks":
            case "debug":
                MessageUtils.sendMessage(sender, "<gray>Раздел " + args[0] + " находится в разработке.</gray>");
                break;
            default:
                MessageUtils.sendMessage(sender, "<red>Неизвестная подкоманда. Используйте /bobby для справки.</red>");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "<gold>=== Bobby Мониторинг ===</gold>");
        MessageUtils.sendMessage(sender, "<yellow>/bobby scan</yellow> <gray>- запустить проверку лагов</gray>");
        MessageUtils.sendMessage(sender, "<yellow>/bobby report</yellow> <gray>- последние источники лагов</gray>");
        MessageUtils.sendMessage(sender, "<yellow>/bobby profiler</yellow> <gray>- статистика сервера</gray>");
    }

    private void handleScan(CommandSender sender) {
        if (!sender.hasPermission("bobby.moderator")) {
            MessageUtils.sendMessage(sender, "<red>Недостаточно прав.</red>");
            return;
        }
        MessageUtils.sendMessage(sender, "<aqua>Запуск ручного сканирования всех систем...</aqua>");
        
        DetectorManager detectorManager = registry.get(DetectorManager.class);
        detectorManager.scanAll().thenRun(() -> {
            MessageUtils.sendMessage(sender, "<green>Сканирование завершено. Если что-то найдено, вы получите уведомление в чат/логи.</green>");
        });
    }

    private void handleReport(CommandSender sender) {
        AnalyticsManager analytics = registry.get(AnalyticsManager.class);
        List<LagReport> reports = analytics.getRecentReports();
        
        if (reports.isEmpty()) {
            MessageUtils.sendMessage(sender, "<gray>История отчетов пуста. Источников лагов не найдено.</gray>");
            return;
        }

        MessageUtils.sendMessage(sender, "<gold>=== Последние 5 отчетов о лагах ===</gold>");
        int count = Math.min(reports.size(), 5);
        for (int i = reports.size() - 1; i >= reports.size() - count; i--) {
            LagReport report = reports.get(i);
            sender.sendMessage(MessageUtils.createLagMessage(
                report.getType() + " (" + report.getSourceName() + ")",
                report.getSeverity(),
                report.getImpactMspt(),
                report.getLocation()
            ));
        }
    }

    private void handleProfiler(CommandSender sender) {
        ServerProfiler profiler = registry.get(ServerProfiler.class);
        double tps = profiler.getTps();
        double mspt = profiler.getMspt();
        
        String tpsColor = tps > 19.0 ? "green" : tps > 15.0 ? "yellow" : "red";
        String msptColor = mspt < 25.0 ? "green" : mspt < 45.0 ? "yellow" : "red";

        MessageUtils.sendMessage(sender, "<gold>=== Состояние сервера ===</gold>");
        MessageUtils.sendMessage(sender, String.format("<gray>TPS:</gray> <%s>%.2f</%s>", tpsColor, tps, tpsColor));
        MessageUtils.sendMessage(sender, String.format("<gray>Средний MSPT:</gray> <%s>%.2f ms</%s>", msptColor, mspt, msptColor));
    }
}
