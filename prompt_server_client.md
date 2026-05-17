# Промпт для агента opencode — серверная + клиентская (не GUI) часть

Реализуй серверную и клиентскую (не GUI) часть для Battlefield-мода teamsystem (Forge 1.20.1). Основная логика уже написана, нужно доработать системы и добавить интеграции.

## 1. FOB (Forward Operating Base) — ПОЛНАЯ РЕАЛИЗАЦИЯ
Файл: `FOBManager.java` (сейчас заглушка)

- FOB — entity (или block, но лучше entity, чтоб можно было разрушить взрывом/киркой). Создай `FOBEntity` (наследник `Entity` или `BlockEntity` с рендером).
- Ставить может только лидер отряда (SquadManager.getSquad(player).getLeader()).
- При установке: открывается поле ввода названия (Anvil GUI или через команду/пакет — придумай механику).
- Ограничение: макс 3 FOB на команду (конфигурируемо в config).
- Минимальное расстояние: 50 блоков от вражеских FOB, 10 от своих FOB/базы.
- Разрушение: враг может бить киркой или взрывом (TACZ/Superb Warfare). При разрушении — уведомление всей команде.
- FOB служит точкой респавна (RespawnManager.getRespawnPointsForPlayer должен включать FOB + beacons).
- Синхронизация FOB всем игрокам команды через новый пакет `FOBSyncPacket`:
  - Поля: fobId (int), worldKey (String), x/y/z (double), name (String), teamOrdinal (int), health (float).
- Рендер на клиенте: добавляй 3D маркер FOB через WorldMarkerRenderer (клиентская часть). В ClientTeamData добавь `public static List<FOBData> fobList = new ArrayList<>()`. FOBData — record с полями name, x, y, z, worldKey, teamOrdinal, health.
- При уничтожении FOB — убрать из списка респавна.

## 2. Simple Voice Chat интеграция (радио + локал чат)
Мод: `de.maxhenkel.voicechat` (Forge-версия). Используй его API через рефлексию или мягкую зависимость.

- **Локал чат**: все игроки в радиусе ~30 блоков слышат друг друга (это дефолт VoiceChat — менять не надо).
- **Сквадное радио**: игроки в одном отряде слышат друг друга на любом расстоянии. Реализуй через VoiceChat API: создай группу/канал для каждого отряда.
- **Командное радио**: вся команда слышит друг друга — через общий командный канал.
- **Индикатор говорящего**: на клиенте при получении события от VoiceChat (кто-то говорит) — показывать на 2-3 сек в HUD слева: «[Ранг] Позывной» с иконкой микрофона. Для этого:
  - Используй VoiceChat API `VoicechatConnection` / `VoicechatEvents`.
  - Создай `ClientVoiceHandler.java` который слушает события VoiceChat.
  - Данные передавай в `ClientTeamData` или в отдельный `VoiceIndicatorData` (list говорящих с таймером).
  - GUI-рендер будет отдельно, твоя задача — данные.

## 3. Русские ранги
Файл: `Rank.java` (enum) — нужно расширить на 2 языка.

- Добавь поля: `russianName`, `russianPrefix`.
- Значения:
  - PRIVATE: ["Рядовой", "[Ряд]"]
  - PFC: ["Ефрейтор", "[Ефр]"]
  - CORPORAL: ["Младший сержант", "[МлСр]"]
  - SERGEANT: ["Сержант", "[Серж]"]
  - STAFF_SERGEANT: ["Старший сержант", "[СтСр]"]
  - LIEUTENANT: ["Лейтенант", ["Лейт]"]
  - CAPTAIN: ["Капитан", "[Кап]"]
  - MAJOR: ["Майор", "[Май]"]
  - COLONEL: ["Полковник", "[Плк]"]
  - GENERAL: ["Генерал", "[Ген]"]
- Добавь методы `getDisplayName(boolean russian)`, `getPrefix(boolean russian)`.
- Флаг языка: берется из TeamSystemConfig.language (уже есть поле, по умолчанию "ru"). Передавай на клиент. В `RankSyncPacket` добавь флаг `isRussian` или на клиенте проверяй настройку языка.
- ClientTeamData.localPlayerRank уже есть — теперь при получении ранга используй `Rank.fromOrdinal(rank).getPrefix(isRussian)`.

## 4. Звуковая система
Создай `ModSounds.java` с регистрацией звуков через `DeferredRegister<SoundEvent>`:
- `gui_button_click` — клик кнопки
- `gui_button_hover` — наведение на кнопку
- `gui_error` — нет доступа/ошибка
- `gui_success` — удачное действие
- `gui_transition` — переход между экранами/меню
- `gui_first_deploy` — первый деплой в матче
- `gui_respawn` — респавн
- `game_capture_point` — захват точки (слышат все в радиусе точки, ~50 блоков, через SoundEvent с позицией)
- `game_victory` — победа в матче
- `game_defeat` — поражение
- `game_map_change` — смена карты
- `notification_info` — уведомление (инфо)
- `notification_alert` — уведомление (важное)

Файлы звуков — ogg в `assets/teamsystem/sounds/gui/` и `assets/teamsystem/sounds/game/`. Создай `assets/teamsystem/sounds.json` с описанием всех звуков.

**Громкость**: прокси метод `playGUISound(SoundEvent)`, `playPositionedSound(Level, SoundEvent, x, y, z, radius)`.

## 5. Локализация (языковая система)
Уже есть `TeamSystemConfig.language` (поле String, "ru" или "en").

- Создай `assets/teamsystem/lang/ru_ru.json` с русскими переводами.
- Создай/дополни `assets/teamsystem/lang/en_us.json` с английскими переводами.
- Ключи локализации для всех сообщений мода: `teamsystem.ui.*`, `teamsystem.rank.*`, `teamsystem.team.*`, `teamsystem.notification.*` и т.д.
- На клиенте: получай `TeamSystemConfig.language` через пакет конфига (новый `ConfigSyncPacket`) при подключении. Используй для выбора языка в GUI (но GUI — в следующей итерации).

## 6. Новые пакеты (дополнить PacketHandler.java)

Добавь и зарегистрируй следующие пакеты (все PLAY_TO_CLIENT):
1. `ConfigSyncPacket` — синхронизирует TeamSystemConfig.language + другие настройки на клиент.
2. `FOBSyncPacket` — список FOB (см. раздел 1).
3. `SoundPacket` — для проигрывания звуков у игроков (позиционные + GUI).
4. `KitDataSyncPacket` — полные данные китов для KitSelectionScreen (name, displayName, description, icon, minRank, cooldown, available, loadout слотов).
5. `VehicleDataSyncPacket` — аналогично для VehicleSelectionScreen.
6. `NotificationPacket` — отправка уведомления конкретному игроку (text, type, duration, sound).
7. `TeamChangeRequestPacket` — C2S пакет когда игрок нажал кнопку смены стороны в TeamSelectionScreen (teamOrdinal).
8. `KitSelectPacket` — C2S выбор кита.
9. `VehicleDeployPacket` — C2S развертывание техники.
10. `FOBPlacePacket` — C2S установка FOB (name + позиция).

## 7. Доработка существующих пакетов

- `KitSyncPacket` — сейчас handler пустой. Заполни: пиши данные в `ClientTeamData` (добавь поля для списка китов).
- `VehicleSyncPacket` — то же самое.
- `CapturePointSyncPacket` — то же самое.

## 8. Кастомные никнеймы (Nametag — серверная часть)

На сервере: при обновлении `PlayerCombatData.setDisplayName()` формировать строку: `[Ранг] Позывной Игрок`. Ранг берется из Rank.getPrefix(language), позывной из PlayerCombatData.callsign (новое поле).

Добавь в `PlayerCombatData`:
- `String callsign` — позывной (уже есть? проверь. Если нет — добавь).
- `String rankPrefix` — кэшированный префикс ранга.
- `boolean isAdmin` — флаг админа (для иконки).
- `int donatTier` — 0-3 (0 = нет доната).
- `String playerTitle` — кастомный титул (например "ТикТок", "Админ" и т.д.).

`TeamManager.updatePlayerDisplayName()` — обновляй отображаемое имя в формате `[Ранг] Callsign §7(RealName)`. Для Spectator — просто `[Spectator] Name`.

## 9. Партиклы вокруг точек захвата
В `CaptureProcessor.java` или отдельном `CaptureParticleManager`:
- Раз в 2-3 сек спавнить вокруг точек захвата кольцо частиц.
- Тип частицы: `ParticleTypes.END_ROD` или кастомный.
- Цвет: синий для NATO, красный для Russia, белый для нейтральной.
- Радиус кольца = CaptureZone.radius.
- Частицы движутся по кругу (slow rotation).
- Оптимизация: обновлять только если есть игроки рядом (< 64 блоков).
- На сервере спавнить через `ServerLevel.sendParticles()` для игроков рядом.

## 10. Доработка хотбара (серверная часть уже готова, клиентский миксин в GUI)
GUI уже делает кастомный хотбар, но нужно обеспечить:
- Событие смены слота: `PlayerSelectedSlotCallback` или через `InputEvent.MouseScrolled` — для анимации GUI.
- `ClientHotbarData.java` с полем `selectedSlot` и `lastChangeTime` для анимации fade.

## 11. Добавление новых полей в ClientTeamData
Добавь:
- `public static List<KitData> kits = new ArrayList<>()` — список доступных китов.
- `public static List<VehicleData> vehicles = new ArrayList<>()` — список техники.
- `public static List<FOBData> fobs = new ArrayList<>()` — список FOB.
- `public static String language = "ru"` — язык клиента.
- `public static List<SpeakingPlayer> speakingPlayers = new ArrayList<>()` — кто сейчас говорит в VoiceChat.
- `public static KitData selectedKit = null` — выбранный кит.
- Вспомогательные классы-рекорды: `KitData`, `VehicleData`, `FOBData`, `SpeakingPlayer` в отдельных файлах.

## 12. Новые команды (если нужно)
- `/fob place <name>` — установка FOB.
- `/fob remove` — удаление FOB.
- `/fob list` — список FOB.
- `/kit select <name>` — выбор кита (уже есть /kit claim? сделай отдельный выбор).
- `/deploy` — деплой с выбранным китом на выбранном FOB/базе.

## Технические требования:
- Все новые классы — в соответствующих пакетах: `core/`, `network/`, `events/`, `client/`.
- `PacketHandler.CHANNEL` — используй существующий SimpleChannel, зарегистрируй все новые пакеты с builder-стилем (как остальные).
- Все изменения должны быть обратно совместимы с существующими пакетами (protocol version остается "1").
- Используй существующие паттерны: `DeferredRegister` для звуков, `SimpleChannel` для пакетов, `MinecraftForge.EVENT_BUS` для событий.
- Не добавляй никакие GUI/Overlay/экранные классы (это в отдельной задаче).
- После изменений проверь что `PlayerEventHandler`, `CombatEventHandler`, `KitManager`, `VehicleManager`, `SquadManager` корректно работают с новыми полями.
