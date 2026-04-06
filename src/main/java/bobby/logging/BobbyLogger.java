package bobby.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import bobby.core.BobbyPlugin;

public class BobbyLogger {

    private final Logger pluginLogger;
    private FileHandler fileHandler;

    public BobbyLogger(BobbyPlugin plugin) {
        this.pluginLogger = plugin.getLogger();
        setupFileLogger(plugin.getDataFolder());
    }

    private void setupFileLogger(File dataFolder) {
        try {
            File logFolder = new File(dataFolder, "logs");
            if (!logFolder.exists()) {
                Files.createDirectories(logFolder.toPath());
            }

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File logFile = new File(logFolder, "bobby-" + date + ".log");

            fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setFormatter(new CustomFormatter());
        } catch (IOException e) {
            pluginLogger.log(Level.SEVERE, "Не удалось настроить файл логов Bobby!", e);
        }
    }

    public void info(String message) {
        pluginLogger.info(message);
        logToFile("INFO", message);
    }

    public void warning(String message) {
        pluginLogger.warning(message);
        logToFile("WARNING", message);
    }

    public void severe(String message) {
        pluginLogger.severe(message);
        logToFile("SEVERE", message);
    }
    
    public void logLag(String type, String details, double msptImpact, String location) {
        String msg = String.format("[ЛАГ] Тип: %s | Ущерб MSPT: %.2f | Позиция: %s | Детали: %s", type, msptImpact, location, details);
        warning(msg);
    }

    private void logToFile(String level, String message) {
        if (fileHandler != null) {
            LogRecord record = new LogRecord(Level.parse(level.toUpperCase().equals("SEVERE") ? "SEVERE" : level.toUpperCase().equals("WARNING") ? "WARNING" : "INFO"), message);
            fileHandler.publish(record);
        }
    }

    public void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }

    private static class CustomFormatter extends Formatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            return String.format("[%s] [%s]: %s%n",
                    dateFormat.format(new Date(record.getMillis())),
                    record.getLevel().getLocalizedName(),
                    record.getMessage()
            );
        }
    }
}
