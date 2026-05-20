# BattlefieldMC GUI — Полный рефакторинг + Новый дизайн

## Скрипты сборки и деплоя

Скрипты будут предоставлены отдельно (у тебя нет доступа к файловой системе для их чтения). Спроси у меня когда понадобятся.

---

## 1. Что нужно сделать

### 1.1 Все GUI-экраны

| Экран | Файл | Статус |
|-------|------|--------|
| **SpawnSelectionScreen** | Существует | Редизайн |
| **ClassSelectionScreen** | Существует | Редизайн |
| **KitSelectionScreen** | Существует | Редизайн |
| **KitCustomizationScreen** | Существует | Редизайн |
| **KitLoadoutScreen** | Существует | Редизайн |
| **EscapeMenu (PauseScreen)** | Новый | Создать |
| **MainMenu (TitleScreen)** | Новый | Создать |
| **LoadingScreen** | Новый | Создать |
| **HUD (InGameOverlay)** | Новый | Создать |

### 1.2 Все core-компоненты

| Компонент | Статус |
|-----------|--------|
| **BButton** — кнопка (DEFAULT/PRIMARY/GHOST/DANGER), accent line, scale(0.97) on press | Улучшить |
| **BCard** — анимированная карточка | Улучшить |
| **BScrollPanel** — добавить `ensureVisible()`, `scrollTo()`, `isMouseOver()`, `resetScroll()` | Улучшить |
| **BProgressBar** — pulse, animated, showLabel | Улучшить |
| **BSlider** — без изменений | Оставить |
| **AnimationHelper** — без изменений | Оставить |
| **TopBar** — BATTLEFIELD + screen name + SP/BC/Rank + time + team icon | Создать |
| **StatusBar** — connection status + team + player + kit | Создать |
| **BreadcrumbNav** — путь навигации + quick nav buttons | Создать |
| **SlotRow** — универсальный слот с ‹ › для оружия/брони | Создать |
| **Chip** — компактная кнопка-тег (selected/hover) | Создать |
| **AccentLine** — градиентная линия 2px | Создать |
| **StatusDot** — 6px кружок с glow | Создать |
| **LockBadge** — overlay с 🔒 и причиной | Создать |
| **KitCard** — вынести из KitSelectionScreen | Создать |
| **ClassCard** — вынести из ClassSelectionScreen | Создать |
| **KitPreviewPanel** — превью предмета + слоты оружия | Создать |
| **KitConfigPanel** — скролл с оружием/аттачментами/бронёй | Создать |
| **KitSaveDeployBar** — футер с кнопками | Создать |

---

## 2. Что должно быть на экранах (чего нет в JSX)

### 2.1 MainMenu (TitleScreen)

- Полноэкранный фон: коллекция 5-10 изображений из `/assets/teamsystem/textures/gui/mainmenu/` с **плавным crossfade** (2-3s transition, 8-12s показ)
- Затемнение поверх (BG_OVERLAY или градиент)
- Центр: логотип BATTLEFIELD + подзаголовок + AccentLine + кнопки (Join Server / Singleplayer / Options / Quit)
- Справа внизу: версия сборки, MC 1.20.1, логотипы загруженных модов (16x16/24x24)
- Реализация: mixin `TitleScreen` или `ScreenEvent.Init`, сохранить доступ к Multiplayer/Options/Language/Realms

### 2.2 LoadingScreen

Два режима:

**Forge-загрузка:**
- Полноэкранный фон с crossfade (те же изображения, что в MainMenu)
- Затемнение (0xBB000000)
- Центр: "BATTLEFIELD" + "LOADING — [mod_name]"
- Внизу: **сетка логотипов модов** — каждый логотип загорается по мере загрузки мода (серые → цветные). Если логотипа нет — заглушка.

**Подключение к серверу / получение KitConfig:**
- Фон с crossfade + затемнение
- Центр: "BATTLEFIELD" + "LOADING" + пульсирующий AccentLine + ProgressBar + статус ("Connecting...", "Receiving kit config...")
- Скрывать при получении `KitConfigSyncPacket`

### 2.3 EscapeMenu (PauseScreen)

- Замена через mixin на `PauseScreen`
- Затемнение BG_OVERLAY
- Центральная панель с кнопками: Return to Game, Team Select, Settings, Disconnect, Quit
- Не ломать `onDisconnect()`, `disconnectScreen`, `handleSystemSuspension`

### 2.4 HUD (InGameOverlay)

- Регистрация через `RegisterGuiOverlayEvent` (Forge)
- **Слева сверху**: HP Bar (цвет по проценту), Armor Bar, Ammo Display (Share Tech Mono, `current / max`), Stamina Bar
- **Справа сверху**: Minimap (рамка, чёрный фон, точки игроков)
- **Справа снизу**: Kit Info (текущий кит, класс, слоты)
- **Сверху**: Compass (N/S/W/E + тики)
- Все элементы с `AnimationHelper.withAlpha()` для guiOpacity

---

## 3. Визуальный дизайн

**JSX-файл (`BattlefieldMC_GUI.jsx`) — это точный визуальный референс.** Там есть:
- Все цвета и их использование
- Расположение всех элементов каждого экрана
- Анимации (CSS keyframes)
- Поведение компонентов (BButton, Chip, StatusDot, LockBadge, AccentLine, SlotRow, TopBar, StatusBar, Breadcrumb)
- Стили ховеров, нажатий, переходов

Переносить в Java один-в-один, сохраняя все отступы, размеры, цвета, анимации и логику.

### Что JSX не показывает (но должно быть в Java):

- **UITheme.java** — добавить недостающие цвета из JSX: `BG_TOOLTIP`, `BG_OVERLAY`, `HUD_HP_BG/FULL/MID/LOW`, `HUD_AMMO_BG/TEXT`, `HUD_MINIMAP_BG/BORDER`, `MENU_BUTTON_BG/HOVER`, `LOADING_BAR_BG/FILL`, `ESCAPE_OVERLAY/PANEL_BG`
- **Шрифты** — Rajdhani (основной UI) и Share Tech Mono (цифры/ID) — вшить в ресурспак `/assets/teamsystem/font/`
- **BButton.setVariant(Variant)** — DEFAULT, PRIMARY, GHOST, DANGER (цвета и поведение из JSX `variants` объекта)
- **Z-order**: scanline(0) → BG → панели → карточки → слоты → текст → accent линии → hover → кнопки → тултипы → lock overlay(10) → loading(11)
- **Клиппинг кликов**: все `mouseClicked()` на скроллах должны проверять `isMouseOver()` перед обработкой

---

## 4. Баги, которые нужно починить

### 4.1 Save & Deploy не нажимается (KitCustomizationScreen)

Конфиг-панель в `mouseClicked()` не проверяет, что клик внутри видимых границ скролла. Клик на кнопку может быть перехвачен невидимым элементом (Y-позиция совпадает из-за scrollOffset). Фикс: обернуть обработку конфиг-кликов в `if (my >= scrollTop && my <= scrollBot)`.

### 4.2 Клик по карточке кита вне скролла (KitSelectionScreen)

Клик-детекшн карточек не проверяет `cy >= panelY && cy + CELL_H <= panelY + scrollPanel.getHeight()`. Фикс: добавить проверку видимости.

### 4.3 Сортировка китов

Доступные киты (по SP/BC) сверху, заблокированные снизу — уже пофикшено, не сломать.

### 4.4 Имена предметов

Все отображаемые имена через `ItemResolver.getDisplayName(id)`, не через `formatAttachName()`.

---

## 5. Что НЕ трогать

- **Пакеты**: `network/`, `data/`, `core/`, `events/`
- **Файлы**: `KitConfig.java`, `KitConfigSyncPacket.java` (GZIP), `KitSelectPacket.java`, `KitSavePacket.java`, `PlayerCombatData.java`, `CombatEventHandler.java`, `ClientTeamData.java`, `ItemResolver.java`, `TaczAttachmentResolver.java`, `LockChecker.java`, `LockState.java`, `SpawnScreenHelper.java`
- **Звуки**: `ClientSoundHandler`, `ModSounds`
- **Локализация**: `I18n`
- **Анимации**: `AnimationHelper` — только дополнять, не ломать
- **Сетевые пакеты**: не менять формат

---

## 6. Проверка после рефакторинга

Запустить `buildAndDeploy`, проверить:

1. **MainMenu** — фон с crossfade, кнопки работают, логотипы модов видны
2. **LoadingScreen** — фон, логотипы, прогресс-бар
3. **HUD** — все элементы отображаются
4. **SpawnSelectionScreen** — точки, выбор, кнопка спавна
5. **Classes → Kits → Customize** — полная навигация
6. **EscapeMenu** — по ESC, все кнопки
7. **Save & Deploy** — сохраняет и выбирает кит
8. **Скролл, анимации, цвета** — всё работает
9. **Кнопки** — не перехватываются скроллом
10. **Шрифты** — Rajdhani и Share Tech Mono загружены

---

## ⚠️ КРИТИЧЕСКИ: Java, не JSX

JSX-файл — **только визуальный референс**. Писать на Java (Forge 1.20.1):

- `Screen` / `AbstractWidget` / `Button` — вместо React-компонентов
- `GuiGraphics` вместо JSX
- `Minecraft.getInstance().font` вместо CSS-шрифтов (Rajdhani/Share Tech Mono через ресурспак)
- `AnimationHelper.lerp()` / `easeOutCubic()` вместо CSS animations
- `addRenderableWidget()` вместо рендера списка
- `ArrayList`, `HashMap` вместо useState/map/filter
- `g.fill()`, `g.drawString()` вместо div/span

---

## 7. Архитектура проекта

```
com.yourmod.teamsystem.client.gui
  ├── UITheme.java          — константы цветов
  ├── I18n.java             — локализация
  ├── component/
  │   ├── BButton.java      — кнопка с вариантами
  │   ├── BCard.java        — анимированная карточка
  │   ├── BScrollPanel.java — скролл-панель
  │   ├── BProgressBar.java — прогресс-бар
  │   ├── BSlider.java      — слайдер
  │   ├── AnimationHelper.java — утилиты
  │   ├── TopBar.java       — верхняя панель
  │   ├── StatusBar.java    — нижняя панель
  │   ├── BreadcrumbNav.java — хлебные крошки
  │   ├── SlotRow.java      — слот с стрелками
  │   ├── Chip.java         — кнопка-тег
  │   ├── AccentLine.java   — градиентная линия
  │   ├── StatusDot.java    — статус точка
  │   ├── LockBadge.java    — лоок-оверлей
  │   ├── KitCard.java      — карточка кита
  │   ├── ClassCard.java    — карточка класса
  │   └── KitPreviewPanel.java — превью панель
  └── screen/
      ├── SpawnSelectionScreen.java
      ├── ClassSelectionScreen.java
      ├── KitSelectionScreen.java
      ├── KitCustomizationScreen.java
      ├── KitLoadoutScreen.java
      ├── MainMenuScreen.java    (mixin TitleScreen)
      ├── EscapeMenuScreen.java  (mixin PauseScreen)
      ├── LoadingScreen.java
      └── SpawnScreenHelper.java
```

### Навигация

```
SpawnSelectionScreen (OpenSpawnSelectionScreenPacket)
  → Classes → KitSelectionScreen(classId)
    → Select Kit → KitSelectPacket → SpawnSelectionScreen
    → Customize → KitCustomizationScreen(classId, kitId)
      → Save & Deploy → KitSavePacket + KitSelectPacket → close

MainMenuScreen → Join Server → LoadingScreen → SpawnSelectionScreen
EscapeMenu (ESC) → Return to Game / Team Select / Settings / Disconnect
HUD — постоянно поверх игры, обновляется через тики
```

### Сеть и данные

- `KitConfig.get()` — синглтон, GZIP-сжатый JSON от сервера
- `ClientTeamData.localPlayerSP/BC/Rank` — баланс
- `ItemResolver.getDisplayName(id)` — локализованное имя предмета
- `AnimationHelper.withAlpha(color, alpha)` — учитывает `ClientTeamData.guiOpacity`
