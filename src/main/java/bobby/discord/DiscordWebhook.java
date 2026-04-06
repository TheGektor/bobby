package bobby.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bobby.core.BobbyPlugin;

public class DiscordWebhook {

    private final String webhookUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendAlert(String title, String type, String coordinates, String player, double mspt, String severity, int color) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("ВСТАВЬТЕ_URL_ВЕБХУКА")) {
            return;
        }

        executor.submit(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Bobby-Plugin");
                connection.setDoOutput(true);

                String playerField = player != null ? String.format("{\"name\": \"Игрок:\", \"value\": \"%s\", \"inline\": true},", player) : "";

                String json = String.format(
                    "{" +
                    "  \"embeds\": [{" +
                    "    \"title\": \"%s\"," +
                    "    \"color\": %d," +
                    "    \"fields\": [" +
                    "      {\"name\": \"Тип проблемы:\", \"value\": \"%s\", \"inline\": true}," +
                    "      %s" +
                    "      {\"name\": \"Ущерб MSPT:\", \"value\": \"%.2f\", \"inline\": true}," +
                    "      {\"name\": \"Уровень опасности:\", \"value\": \"%s\", \"inline\": true}," +
                    "      {\"name\": \"Координаты:\", \"value\": \"%s\", \"inline\": false}" +
                    "    ]" +
                    "  }]" +
                    "}",
                    title, color, type, playerField, mspt, severity, coordinates
                );

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
            } catch (Exception e) {
                BobbyPlugin.getInstance().getLogger().warning("Ошибка при отправке оповещения в Discord: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
