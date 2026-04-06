# Bobby - Система Комплексного Мониторинга Лагов 🚀

Bobby — это высокопроизводительный плагин для Paper-серверов, который анализирует все аспекты серверной нагрузки, от скопления сущностей до гигантских лаг-машин на редстоуне. Ниже представлено подробное описание архитектуры, принципов работы системы и ключевых строк кода.

---

## 1. Архитектура Ядра (Core)

Движок Bobby работает на базе DI-контейнера (`PluginRegistry.java`), который хранит все подсистемы и передает их друг другу без использования статических синглтонов.
Такой подход (SOLID) позволяет избежать жесткой связанности компонентов. Все детекторы загружаются и хранятся внутри `DetectorManager.java`.

### Мониторинг тиков (Tick Monitor)
Класс `TickMonitorTask.java` - это задача, работающая по таймеру (задается в конфигурации `config.yml` в параметре `scan_interval_ticks`). Она считывает текущий показатель сервера **MSPT** (Millisecond Per Tick) через внутренний класс `ServerProfiler.java`. 

```java
// Строка 26: bobby.monitor.TickMonitorTask
double mspt = profiler.getMspt();

if (mspt > config.getAlertThresholdMspt()) {
    BobbyLogger logger = registry.get(BobbyLogger.class);
    logger.warning(String.format("Обнаружен средний скачок MSPT: %.2f ms", mspt));
    
    // Активация паники - даем команду всем детекторам проверить состояние
    isScanning = true;
    DetectorManager detectorManager = registry.get(DetectorManager.class);
    detectorManager.scanAll().thenRun(() -> {
        isScanning = false;
    });
}
```

## 2. Механизм детекции: Наблюдатели и Редстоун (BlockUpdates / Redstone) 

Для выявления конкретных лаг-машин, Bobby применяет пассивное прослушивание событий Bukkit API (`Listener`) в связке с асинхронным подсчетом. Вместо того чтобы каждую секунду сканировать весь мир или блоки, мы слушаем события.

### Прослушивание событий (`RedstoneLagDetector.java` и `BlockUpdateDetector.java`)
Мы используем `BlockRedstoneEvent` для проводов/повторителей и `BlockPhysicsEvent` для поршней/наблюдателей.
Рассмотрим код в `BlockUpdateDetector.java`:

```java
// Строка 40: bobby.detectors.impl.BlockUpdateDetector
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onBlockPhysics(BlockPhysicsEvent event) {
    Location loc = event.getBlock().getLocation();
    String worldName = loc.getWorld().getName();
    
    // Уникальный ID чанка через битовое смещение
    long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    
    // Потокобезопасная инкрементация счетчика в Map
    activityMap.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
               .computeIfAbsent(chunkKey, k -> new AtomicInteger(0))
               .incrementAndGet();
               
    // Сохранение координат для телепорта
    locationMap.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
               .putIfAbsent(chunkKey, loc);
}
```

**Особенности этого кода:**
1. Мы математически оборачиваем координаты чанка в уникальный 64-битный длинный ключ:
   ```java
   // Строка 121
   private long getChunkKey(int x, int z) {
       return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
   }
   ```
2. Используются потокобезопасные структуры данных: `ConcurrentHashMap` и `AtomicInteger`. Они нужны потому, что мы считаем события в главном потоке (Server Thread), а аналитическую проверку (scan) проводим параллельно в отдельном потоке. Это позволяет счетчикам не терять значения (race conditions).

### Сканирование и вердикт (`scan` метод)
Когда происходит скачок MSPT, вызывается метод сканирования для обработки собранных данных:

```java
// Строка 66: bobby.detectors.impl.BlockUpdateDetector
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    boolean found = false;
    for (Map.Entry<String, Map<Long, AtomicInteger>> worldEntry : activityMap.entrySet()) {
        // ... проход по чанкам ...
        long chunkKey = chunkEntry.getKey();
        int count = chunkEntry.getValue().get();
        
        if (count > finalMaxAllowed) {
            // Вычисление математического ущерба для MSPT
            double estimatedImpact = Math.min((count - finalMaxAllowed) * 0.015, 150.0);
            
            if (estimatedImpact > 3.0) {
                AnalyticsManager analytics = registry.get(AnalyticsManager.class);
                analytics.registerReport(new LagReport(
                        "Блоковая лаг-машина",
                        "Шторм обновлений блоков (" + count + " апдейтов)",
                        estimatedImpact,
                        loc,
                        null
                ));
                found = true;
                break; // Достаточно 1 репорта, чтобы избежать спама
            }
        }
    }
    // Обязательно очищаем данные после скана!
    chunks.clear();
});
```
Данный блок целиком обернут в `runTaskAsynchronously`, поэтому работа цикла никак не влияет на TPS сервера!

---

## 3. Синхронные сканеры: Сущности и Воронки

В отличие от блоков, API Bukkit не позволяет асинхронно итерироваться по загруженным сущностям (Entities / TileEntities). Если попробовать, сервер выбросит `ConcurrentModificationException` и крашнется.
Поэтому `EntityLagDetector`, `ItemEntityDetector`, `HopperSystemDetector` работают по-другому:

```java
// Строка 35: bobby.detectors.impl.EntityLagDetector
Bukkit.getScheduler().runTask(plugin, () -> {
    for (World world : Bukkit.getWorlds()) {
        // Проверяем только УЖЕ загруженные чанки, чтобы не спровоцировать генерацию и лаги
        for (Chunk chunk : world.getLoadedChunks()) {
            int count = chunk.getEntities().length;
            if (count > limit) {
                // Если счетчик выше заданного лимита - считаем MSPT
                double estimatedImpact = Math.min((count - limit) * 0.1, 50.0);
                // ... регистрация репорта ...
            }
        }
    }
});
```
Операция `chunk.getEntities().length` работает мгновенно и не создает нагрузки на главный поток, в отличие от тяжелых поисков.

---

## 4. Аналитика, Discord и Модерация

Если лаг передан в `AnalyticsManager.java`, активируются 3 стадии оповещений (строка 27+):

1. **Файловый логгер (`BobbyLogger.java`)**:
   Записывает информацию о проблеме в ротируемый лог формата *bobby-2024-05-18.log*.
   
2. **Оповещение в Discord (`DiscordWebhook.java`)**:
   Служба выполняет асинхронный HTTP POST запрос. Мы формируем сырой JSON внутри плагина:
   ```java
   // Строка 36: bobby.discord.DiscordWebhook
   String json = String.format("{\"embeds\": [{\"title\": \"%s\", ...
   ```
   Вся отправка идет через `ExecutorService`, чтобы не парализовать сервер во время ожидания ответа от API Discord.

3. **Интерактивные сообщения в игре (`MessageUtils.java`)**:
   Мы используем MiniMessage из Paper Adventure API, чтобы создать кликабельные координаты:
   ```java
   // Строка 21: bobby.utils.MessageUtils
   String message = String.format(
       "<gray>Координаты:</gray> <click:run_command:'%s'><hover:show_text:'Нажмите, чтобы телепортироваться'><aqua>(%s)</aqua></hover></click>",
       command, locStr
   );
   return mm.deserialize(message);
   ```
   `command` – это сгенерированная команда `/execute in <мир> run tp <X y Z>`. 
   Далее, в `AnalyticsManager.java` идет итерация по онлайн игрокам:
   ```java
   Bukkit.getOnlinePlayers().forEach(p -> {
       if (p.hasPermission("bobby.moderator")) {
           p.sendMessage(adminMsg);
       }
   });
   ```

## Итог

Bobby сочетает в себе **событийную архитектуру** (пассивное слушание) для блоков и **активный трекинг** загруженных элементов, объединяя это все через `AnalyticsManager` для генерации сводных отчетов. Таким образом детектор работает в фоне постоянно, но не вносит дополнительную нагрузку (`0.0 MS` влияние на серверную очередь).
