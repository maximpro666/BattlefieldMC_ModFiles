# Prompt: Rewrite Custom Nickname (Callsign) System — Logic + Design

## Задача

Переписать всю логику и дизайн кастомного ника (callsign) в Project Warfare Pigeo (PWP) — Minecraft Forge 1.20.1 мод.

## Текущая архитектура (что нужно переписать)

Система размазана по куче файлов и имеет путаницу между `callsign`, `displayName`, `prefix`, `suffix`. Нужно:
1. **Упростить** — оставить только callsign как кастомный ник
2. **Удалить дублирование** — `displayName`, `prefix`, `suffix` из PlayerCombatData (если они не используются для другого)
3. **Улучшить отображение** — формат ника над головой, в HUD, в GUI
4. **Добавить валидацию** — regex для callsign (буквы, цифры, `_`, `-`)
5. **Добавить Color Codes** — поддержка `§` цветов в callsign
6. **Хранить callsign отдельно от display-форматирования** (ранг, админка, донат — собирать на лету, не хранить в БД как отдельное поле)

---

## Все файлы текущей реализации (пути относительно src/main/java/)

### 1. Хранение данных
**`com/pigeostudios/pwp/core/PlayerCombatData.java`**
- `private String callsign = "";` (строка 28)
- `private String displayName;` (строка 19) — дублирует callsign!
- `private String prefix;` (строка 17)
- `private String suffix;` (строка 18)
- NBT сериализация: callsign (строка 251), displayName (строка 246), prefix/suffix (244-245)
- `mergeData()` в TeamManager (строка 99-100) копирует и callsign и displayName

### 2. Команда /callsign
**`com/pigeostudios/pwp/commands/CallsignCommand.java`**
- Валидация: только макс 32 символа, не пустой
- Устанавливает `pcd.setCallsign(name)`
- Вызывает `tm.setPlayerDisplayName(player, name)` — это пишет и в displayName!
- Вызывает `handler.setDogTagName(player, name)` — переименовывает dog tag

### 3. TeamManager — формирование ника над головой
**`com/pigeostudios/pwp/core/TeamManager.java`**
- `updatePlayerDisplayName()` (строка 125): строит `fullName` как:
  - `[Admin] ` если админ
  - `[Donat] ` если донат > 0
  - `[Title] ` если есть титул
  - `RankPrefix + " " + callsign` (или realName если callsign пустой)
- `setPlayerDisplayName()` (строка 428): пишет и в `displayName` и вызывает `syncPlayerNameData()`
- `syncPlayerData()` (строка 309): шлёт и `displayName` и `callsign` в пакете

### 4. База данных (SQLite)
**`com/pigeostudios/pwp/data/CentralDatabase.java`**
- Колонки: `callsign TEXT DEFAULT ''` (строка 61), `display_name TEXT DEFAULT ''` (строка 58)
- Сохраняет оба поля (строки 160, 157)
- Загружает оба поля (строки 299, 296)

### 5. Сетевой пакет синхронизации
**`com/pigeostudios/pwp/network/CombatDataSyncPacket.java`**
- Поля: `displayName` (строка 20), `callsign` (строка 21) — оба шлются
- `handle()` (строка 85): сохраняет `callsign` в `PlayerListEntry`, displayName в `ClientTeamData`

### 6. Клиентские данные
**`com/pigeostudios/pwp/client/PlayerListEntry.java`**
- record: `String callsign` — второе поле (строка 5)

**`com/pigeostudios/pwp/client/ClientTeamData.java`**
- `localPlayerDisplayName` (строка 21) — хранит displayName отдельно
- `setLocalPlayerData()` (строка 147) — сохраняет prefix, suffix, displayName

### 7. Scoreboard
**`com/pigeostudios/pwp/client/gui/scoreboard/data/PlayerScoreboardData.java`**
- `public String nick` (строка 8) — майнкрафтовый ник
- `public String callsign` (строка 9) — кастомный ник

**`com/pigeostudios/pwp/client/gui/scoreboard/data/ScoreboardDataProvider.java`**
- Копирует `ple.callsign()` в `data.callsign` (строка 29)

**`com/pigeostudios/pwp/client/gui/scoreboard/ScoreboardRenderer.java`**
- Отображает `pd.callsign` в колонке Name (строка 245)

### 8. SquadOverlay
**`com/pigeostudios/pwp/client/gui/overlay/SquadOverlay.java`**
- Показывает `rankPrefix + " " + callsign` (строка 81)

### 9. SquadScreen
**`com/pigeostudios/pwp/client/gui/screen/SquadScreen.java`**
- Показывает `ple.callsign()` (строка 148)

### 10. AdminPanel
**`com/pigeostudios/pwp/client/gui/screen/AdminPanel.java`**
- Показывает `ple.callsign()` в списке игроков (строка 307)

### 11. SpawnSelectionScreen
**`com/pigeostudios/pwp/client/gui/screen/SpawnSelectionScreen.java`**
- Показывает `sm.callsign()` для squadmate (строка 70)

### 12. OpenSpawnSelectionScreenPacket
**`com/pigeostudios/pwp/network/OpenSpawnSelectionScreenPacket.java`**
- Record `SquadmateInfo(UUID uuid, String callsign, int teamOrdinal, int cooldownTicks)` (строка 16)

### 13. CombatEventHandler
**`com/pigeostudios/pwp/events/CombatEventHandler.java`**
- Получает callsign из `tm.getOrCreatePlayerData(memberId).getCallsign()` (строка 545)

### 14. PlayerEventHandler
**`com/pigeostudios/pwp/events/PlayerEventHandler.java`**
- `setDogTagName()` — переименовывает dog tag (строка 466)
- `ensureDogTag()` — даёт dog tag с ником (строка 461)
- При входе грузит callsign из БД (строка 127)

### 15. PlayerDataSyncManager
**`com/pigeostudios/pwp/core/PlayerDataSyncManager.java`**
- Экспорт/импорт `data.getCallsign()` / `data.setCallsign()` (строки 63)

### 16. TeamCommand
**`com/pigeostudios/pwp/commands/TeamCommand.java`**
- `/setdisplayname` (строка 347)
- `/team modify setdisplayname` (строка 406)
- `/clearname` (строка 362) — чистит prefix, suffix, displayName

### 17. Lang файлы
**`src/main/resources/assets/pwp/lang/en_us.json`** (строки 167-174)
**`src/main/resources/assets/pwp/lang/ru_ru.json`** (строки 167-174)
- `pwp.chat.welcome.set_callsign` — "Установите свой позывной командой: /callsign <ник>"
- `pwp.chat.callsign.*` — сообщения про callsign

---

## Что нужно сделать

### Логика (серверная часть):
1. **PlayerCombatData**: оставить только `callsign`, удалить `displayName`, `prefix`, `suffix`
2. **CallsignCommand**: 
   - Добавить валидацию regex: `^[a-zA-Zа-яА-Я0-9_\\-§]{2,32}$`
   - Добавить проверку на мат/нецензурное
   - Не дублировать в displayName
3. **TeamManager.updatePlayerDisplayName()**: 
   - Собирать полный формат: `[Admin][Donat][Title] RankPrefix + Callsign`
   - Callsign может содержать `§` цветовые коды
   - Для SPECTATOR: `§7[Spectator] realName`
4. **CentralDatabase**: удалить колонку `display_name`, `prefix`, `suffix`
5. **CombatDataSyncPacket**: удалить `displayName`, `prefix`, `suffix` из пакета
6. **ClientTeamData**: удалить `localPlayerDisplayName`, `localPlayerPrefix`, `localPlayerSuffix`
7. **Команды**: удалить `/setdisplayname`, `/setprefix`, `/setsuffix`, `/clearname`, `/team modify setdisplayname/setprefix/setsuffix`

### Дизайн (клиентская часть — GUI):
8. **ScoreboardRenderer**: Callsign с поддержкой цвета (`§`), усечение по ширине с `...`
9. **SquadOverlay**: Показывать callsign с цветом, формат: `RankPrefix + " " + coloredCallsign`
10. **SquadScreen**: Показывать callsign с цветом
11. **AdminPanel**: Показывать callsign с цветом
12. **SpawnSelectionScreen + SquadmateInfo**: Передавать callsign, рендерить с цветом

### Дополнительно:
13. DogTag: переименовывать в callsign (уже есть, оставить)
14. PlayerEventHandler: при входе проверять и устанавливать callsign
15. Добавить `/rename` или разрешить `§` в /callsign

---

## Формат вывода

Claude должен выдать ВСЕ изменённые файлы целиком (полный код, а не diff). Каждый файл — в отдельном markdown-блоке с указанием полного пути.

Файлы которые нужно изменить:
- `src/main/java/com/pigeostudios/pwp/core/PlayerCombatData.java`
- `src/main/java/com/pigeostudios/pwp/commands/CallsignCommand.java`
- `src/main/java/com/pigeostudios/pwp/core/TeamManager.java`
- `src/main/java/com/pigeostudios/pwp/data/CentralDatabase.java`
- `src/main/java/com/pigeostudios/pwp/network/CombatDataSyncPacket.java`
- `src/main/java/com/pigeostudios/pwp/client/PlayerListEntry.java`
- `src/main/java/com/pigeostudios/pwp/client/ClientTeamData.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/scoreboard/data/PlayerScoreboardData.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/scoreboard/data/ScoreboardDataProvider.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/scoreboard/ScoreboardRenderer.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/overlay/SquadOverlay.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/screen/SquadScreen.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/screen/AdminPanel.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/screen/SpawnSelectionScreen.java`
- `src/main/java/com/pigeostudios/pwp/network/OpenSpawnSelectionScreenPacket.java`
- `src/main/java/com/pigeostudios/pwp/events/CombatEventHandler.java`
- `src/main/java/com/pigeostudios/pwp/events/PlayerEventHandler.java`
- `src/main/java/com/pigeostudios/pwp/core/PlayerDataSyncManager.java`
- `src/main/java/com/pigeostudios/pwp/commands/TeamCommand.java`
- `src/main/resources/assets/pwp/lang/en_us.json`
- `src/main/resources/assets/pwp/lang/ru_ru.json`
