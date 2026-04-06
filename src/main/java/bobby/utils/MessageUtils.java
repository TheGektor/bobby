package bobby.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class MessageUtils {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static String formatLoc(Location loc) {
        if (loc == null) return "Неизвестно";
        return String.format("%d %d %d (%s)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
    }

    public static Component createLagMessage(String type, String severity, double mspt, Location loc) {
        String locStr = formatLoc(loc);
        int x = loc != null ? loc.getBlockX() : 0;
        int y = loc != null ? loc.getBlockY() : 0;
        int z = loc != null ? loc.getBlockZ() : 0;
        
        String command = loc != null ? String.format("/execute in %s run tp %d %d %d", loc.getWorld().getKey().asString(), x, y, z) : "";

        String message = String.format(
            "<gold>[Bobby]</gold> <red>⚠ Обнаружен источник лагов</red>\n" +
            "<gray>Тип:</gray> <white>%s</white>\n" +
            "<gray>Влияние:</gray> <yellow>%.2f MSPT</yellow> (<gray>%s</gray>)\n" +
            "<gray>Координаты:</gray> <click:run_command:'%s'><hover:show_text:'Нажмите, чтобы телепортироваться'><aqua>(%s)</aqua></hover></click>",
            type, mspt, severity, command, locStr
        );
        return mm.deserialize(message);
    }
    
    public static Component parse(String miniMessageString) {
        return mm.deserialize(miniMessageString);
    }
    
    public static void sendMessage(CommandSender sender, String miniMessageString) {
        sender.sendMessage(parse(miniMessageString));
    }
}
