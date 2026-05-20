# BattlefieldMC GUI — Полный рефакторинг + Новый дизайн

## Скрипты для сборки и деплоя

**[ВСТАВЬ buildAndDeploy СЮДА]**

**[ВСТАВЬ ПУТЬ К PrismLauncher И СКРИПТ КОПИРОВАНИЯ СЮДА]**

**[ВСТАВЬ ЛЮБЫЕ ДОПОЛНИТЕЛЬНЫЕ СКРИПТЫ СЮДА]**

---

## 1. Что нужно сделать — полный перечень

### 1.1 Все GUI-экраны мода

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
| **HUD (InGameOverlay)** | Новый (или существующий) | Создать/редизайн |

### 1.2 Все core-компоненты

| Компонент | Файл | Статус |
|-----------|------|--------|
| **BButton** | Существует | Улучшить |
| **BCard** | Существует | Улучшить |
| **BScrollPanel** | Существует | Улучшить |
| **BProgressBar** | Существует | Улучшить |
| **BSlider** | Существует | Улучшить |
| **AnimationHelper** | Существует | Оставить |
| **TopBar** | Новый | Создать |
| **StatusBar** | Новый | Создать |
| **BreadcrumbNav** | Новый | Создать |
| **SlotRow** | Новый | Создать (общий для всех экранов) |
| **Chip** | Новый | Создать |
| **AccentLine** | Новый | Создать |
| **StatusDot** | Новый | Создать |
| **LockBadge** | Новый | Создать |
| **KitCard** | Новый | Вынести из KitSelectionScreen |
| **ClassCard** | Новый | Вынести из ClassSelectionScreen |
| **KitPreviewPanel** | Новый | Вынести из KitCustomizationScreen |
| **KitConfigPanel** | Новый | Вынести из KitCustomizationScreen |
| **KitSaveDeployBar** | Новый | Вынести из KitCustomizationScreen |
| **WeaponSlotWidget** | Новый | Вынести |
| **ArmorSlotWidget** | Новый | Вынести |
| **AttachmentChipList** | Новый | Вынести |

---

## 2. Новый дизайн-файл (вся визуальсистема)

### 2.1 Цветовая палитра (UITheme.java)

```java
// === BACKGROUNDS ===
BG_SCREEN       = 0xCC0A0A0A  // фон экрана
BG_PANEL        = 0xFF141414  // панели, хедер, футер
BG_SURFACE      = 0xFF1C1C1C  // карточки, поверхности-подложки
BG_SLOT         = 0xFF242424  // ячейки слотов
BG_HUD          = 0xAA0F0F0F  // HUD
BG_BLACK        = 0xFF000000  // чистый чёрный
BG_TOOLTIP      = 0xCC1C1C1C  // тултипы
BG_OVERLAY      = 0x66000000  // затемнение поверх экрана

// === TEXT ===
TEXT_PRIMARY    = 0xFFEFEFEF  // основной
TEXT_SECONDARY  = 0xFF909090  // второстепенный
TEXT_MUTED      = 0xFF606060  // затемнённый

// === ACCENT (оранжевый) ===
ACCENT          = 0xFFE07B00  // главный акцент
ACCENT_DIM      = 0x80E07B00  // полупрозрачный
ACCENT_GHOST    = 0x26E07B00  // призрачный

// === BORDERS ===
BORDER          = 0xFF2E2E2E
BORDER_ACCENT   = ACCENT
BORDER_ACTIVE   = ACCENT

// === TEAMS ===
TEAM_NATO       = 0xFF1C5FAD  // синий
TEAM_NATO_BG    = 0xAA0D2A55
TEAM_RUSSIA     = 0xFFAD1C1C  // красный
TEAM_RUSSIA_BG  = 0xAA550D0D

// === STATUS ===
STATUS_OK       = 0xFF50B050  // зелёный
STATUS_WARN     = 0xFFCCA030  // жёлтый
STATUS_DANGER   = 0xFFCC3030  // красный

// === HUD SPECIFIC ===
HUD_HP_BG       = 0xAA0A0A0A
HUD_HP_FULL     = 0xFF50B050
HUD_HP_MID      = 0xFFCCA030
HUD_HP_LOW      = 0xFFCC3030
HUD_AMMO_BG     = 0xAA0A0A0A
HUD_AMMO_TEXT   = TEXT_PRIMARY
HUD_MINIMAP_BG  = 0xCC0A0A0A
HUD_MINIMAP_BORDER = BORDER

// === MENU SPECIFIC ===
MENU_TITLE_BG   = BG_SCREEN
MENU_BUTTON_BG  = BG_SLOT
MENU_BUTTON_HOVER = ACCENT_GHOST

// === LOADING SCREEN ===
LOADING_BG      = BG_BLACK
LOADING_BAR_BG  = BG_SLOT
LOADING_BAR_FILL= ACCENT

// === ESCAPE MENU ===
ESCAPE_OVERLAY  = BG_OVERLAY
ESCAPE_PANEL_BG = BG_PANEL

// === ALPHA-УРОВНИ ===
ALPHA_FULL      = 0xFF
ALPHA_HIGH      = 0xE0
ALPHA_MID       = 0xAA
ALPHA_LOW       = 0x66
ALPHA_GHOST     = 0x26
```

### 2.2 Шрифты

- **Основной**: `Rajdhani` (Google Fonts) — для всего UI текста (полужирный, 600-700 weight, letter-spacing 0.06-0.08em)
- **Моноширинный**: `Share Tech Mono` (Google Fonts) — для чисел, ID предметов, технической информации
- **Fallback**: стандартный `Minecraft.getInstance().font` только для рендера предметов / где нужен оригинальный шрифт MC

Шрифты грузятся через `FontLoader` или `Minecraft.getDefaultResourcePack()`. На клиенте должны быть вшиты в ресурспак мода (`/assets/teamsystem/font/`).

### 2.3 Анимации

- **Появление экрана**: fadeSlideUp (opacity 0→1, translateY 14→0, 220ms ease-out)
- **Stagger**: элементы появляются с задержкой 40-60ms каждый (anim-in-1..6)
- **Hover карточек**: translateY(-2px), border-color → ACCENT_DIM, background → BG_SURFACE (120ms)
- **Hover кнопок**: фон, рамка, цвет текста (120ms ease)
- **Нажатие кнопки**: scale(0.97) (instant на mousedown, обратно на mouseup)
- **Scanline-эффект**: анимированная полоса сверху вниз (6s linear infinite) — опционально, для атмосферы
- **Accent pulse**: box-shadow пульсация для выбранных элементов (1.2s)
- **Progress bar**: width transition 300ms ease
- **Scroll**: плавный ensureVisible через tick()

### 2.4 Отступы и размеры

- **Padding экранов**: 16px
- **Padding карточек**: 10-14px
- **Gap сетки**: 8-10px
- **Толщина акцентной линии**: 2px
- **Толщина рамки**: 1px
- **Толщина левого акцента**: 2-3px
- **Высота TopBar**: 44px
- **Высота StatusBar**: 28px
- **Высота Breadcrumb**: 28px
- **Минимальная высота экрана**: 600px
- **Scrollbar ширина**: 4px

### 2.5 Z-порядок

```
0  → scanline overlay
1  → BG_SCREEN / фоны
2  → панели (BG_PANEL)
3  → поверхности, карточки (BG_SURFACE)
4  → слоты (BG_SLOT)
5  → текст, иконки
6  → accent линии, рамки
7  → hover-подсветки
8  → кнопки, виджеты
9  → тултипы, бейджи
10 → lock overlay
11 → loading screen
```

---

## 3. Описание каждого экрана

### 3.1 TopBar (общий для всех экранов, 44px)

```
[ █ BATTLEFIELD • SPAWN SELECTION ] [SP 1,250 | BC 340 | RANK 3] [12:34:56] [N]
```

- Слева: логотип BATTLEFIELD (ACCENT, bold, letter-spacing 0.12em) + разделитель • + название текущего экрана
- Справа: SP (оранжевый), BC (голубой #60A5FA), RANK (жёлтый), время (pulse-анимация, Share Tech Mono), командный аватар (круг, цвет команды, буква N/R)

### 3.2 Breadcrumb (под TopBar, 28px)

```
SPAWN › CLASSES › KITS › CUSTOMIZE     [⬡ Quick Loadout] [⌂ Spawn]
```

- Хлебные крошки показывают путь навигации
- Текущий экран — ACCENT
- Справа: кнопки быстрой навигации

### 3.3 StatusBar (внизу, 28px)

```
[●] CONNECTED | Team NATO | Ghost_47 | Kit: RIFLEMAN                    BattlefieldMC v2.0 • MC 1.20.1 Forge
```

- Статус подключения (зелёная точка)
- Команда
- Имя игрока
- Текущий кит
- Версия

### 3.4 SpawnSelectionScreen

**Макет:** две колонки (55% / 45%).

**Левая колонка (список точек спавна):**
- Хедер "SPAWN POINTS"
- Список карточек точек спавна:
  - Цветовая индикация: BASE → синий (TEAM), FOB → оранжевый (ACCENT), SQUADMATE → зелёный (OK), BEACON → фиолетовый
  - Левая граница 3px цвета типа точки
  - StatusDot (зона безопасна/опасна)
  - Название точки + тип
  - Дистанция (Share Tech Mono, справа)
  - Contest/статус (красный если опасно)
  - Выбранная точка: фон ACCENT_GHOST, рамка ACCENT_DIM

**Правая колонка (инфо о точке):**
- Название + цветная точка
- AccentLine
- Grid 2×2: TYPE, TEAM, DISTANCE, STATUS
- Текущий кит (если выбран)
- Кнопки: [✦ Classes] [▶ SPAWN HERE]

### 3.5 ClassSelectionScreen

**Макет:** Grid 3 колонки.

- Хедер: ← Back | "SELECT YOUR CLASS" | "N classes available"
- Карточка класса:
  - Иконка (emoji, 26px)
  - Название (UPPERCASE)
  - "N kits"
  - Левая граница 3px ACCENT
  - Hover: translateY(-2px), фон BG_SURFACE, рамка ACCENT_DIM, снизу accent gradient line
  - Lock: LockBadge (затемнение + 🔒 + причина)

### 3.6 KitSelectionScreen

**Макет:** Две колонки (58% / 42%).

**Хедер:** ← Back | [иконка класса] CLASS NAME — SELECT KIT | "N/M available"

**Левая колонка (сетка китов):**
- Grid 3 колонки, скролл
- Сортировка: доступные сверху, locked снизу
- Карточка кита:
  - Название (12px, bold)
  - Описание (10px, muted, 2 строки)
  - Стоимость / FREE / Rank
  - Если selected: зелёный бейдж "✓ EQUIPPED"
  - Левая граница: OK (зелёный) если equipped, ACCENT если активен, BORDER если нет
  - Lock: LockBadge
- Подсказка внизу: "Right-click to select & customize"

**Правая колонка (детали кита):**
- Название (ACCENT, 15px) + описание
- Список слотов: PRIMARY, SECONDARY, SPECIAL, GRENADE
- Каждый слот: иконка, название предмета
- Кнопки: [Select Kit] [Customize →]

### 3.7 KitCustomizationScreen

**Макет:** Три зоны — Preview (190px слева) | Config (flex-1, scroll) | FooterBar.

**FooterBar:** ← Back | Kit Name customization | [▶ Save & Deploy]

**Preview panel:**
- 3D превью предмета (110px, центрировано, с иконкой и ID под ним)
- Акцентная линия
- Список слотов оружия (каждый: иконка, slot name, weapon name)
- Активный слот подсвечен (ACCENT_GHOST фон + ACCENT_DIM рамка)

**Config panel (scroll):**
- **WEAPONS** — список слотов, каждый: иконка | LABEL | weapon name | ‹ 1/N ›
- **ATTACHMENTS** — если есть у активного оружия: категория → список Chip-ов
- **ARMOR** — Grid 2 колонки: слот | item name | ‹ ›

### 3.8 KitLoadoutScreen (упрощённый)

- Центрированный список 4 слотов с ‹ ›
- Кнопка [▶ Save Loadout]

### 3.9 EscapeMenu (новый)

**Что делает:** Заменяет стандартный `PauseScreen` Minecraft.

**Макет:**
- Затемнение фона (BG_OVERLAY)
- Центральная панель (ESCAPE_PANEL_BG, border, left accent)
- Кнопки:
  - ▶ Return to Game
  - ✦ Team Select
  - ⚙ Settings
  - ⌂ Disconnect
  - ✕ Quit Game
- Внизу: версия мода, сервер

**Важно:** Не ломать сетевые логики `onDisconnect()`, `disconnectScreen`, `handleSystemSuspension`.

### 3.10 MainMenu (новый)

**Что делает:** Заменяет/дополняет стандартный `TitleScreen`. Показывается при запуске игры.

**Фоновые изображения (главная особенность):**
- Полноэкранный фон, занимает 100% экрана (z-index: 0, под всеми элементами)
- Коллекция из 5-10 красивых изображений (скриншоты битв, военная техника, солдаты, пейзажи)
- Изображения **плавно сменяют друг друга** — crossfade переход за 2-3 секунды
- Каждое изображение держится 8-12 секунд перед сменой
- Затемнение поверх изображения (градиент или полупрозрачный слой) чтобы текст читался
- Изображения загружаются из ресурспака мода: `/assets/teamsystem/textures/gui/mainmenu/`

**Макет:**
- Фон: полноэкранное изображение с crossfade + затемнение (BG_OVERLAY или градиент) + scanline эффект
- Центр (по вертикали и горизонтали):
  - Логотип "BATTLEFIELD" (ACCENT, крупный, 36-48px, letter-spacing 0.15em, text-shadow для читаемости на светлом фоне)
  - Подзаголовок: "TEAM SYSTEM v2.0" (TEXT_MUTED, 14px)
  - AccentLine
  - Кнопки (сгруппированы, gap 8px):
    - [▶ Join Server] (primary, самая широкая)
    - [✧ Singleplayer] (default)
    - [⚙ Options] (default)
    - [✕ Quit] (ghost/danger)
- Справа внизу: версия сборки, MC 1.20.1 Forge, список загруженных модов (с логотипами модов — маленькие иконки 16x16)

**Логотипы модов на этапе загрузки:**
- В правом нижнем углу (или левом, или отдельной полосой) показываются иконки/логотипы всех загруженных модов (как в современных сборках)
- Каждый логотип — 16x16 или 24x24, берётся из мода (logo.png из resources)
- Если много модов — показывать в виде сетки или карусели
- Анимация: логотипы появляются по мере загрузки модов (Forge LoadingScreen)

**Важно:** Сохранить доступ к `MultiplayerScreen`, `OptionsScreen`, `LanguageScreen`, `Realms` и т.д. Чинить через mixins или события `ScreenEvent.Init`.

### 3.11 LoadingScreen (новый)

**Что делает:** Показывается при:
- Запуске игры (Forge mod loading)
- Подключении к серверу
- Получении KitConfig (ожидание `KitConfigSyncPacket`)
- Переключении карты/мира

**Фоновые изображения:**
- Полноэкранные красивые изображения (как в MainMenu) — коллекция артов/скриншотов
- Изображения плавно сменяют друг друга с crossfade (2-3s transition, 8-12s показ)
- Во время Forge-загрузки: поверх фона показываются **логотипы загружаемых модов**
- Затемнение поверх изображения для читаемости текста

**Макет (Forge LoadingScreen — загрузка модов):**
- Фон: полноэкранное изображение с crossfade + затемнение (0xBB000000)
- Центр:
  - Логотип "BATTLEFIELD" (ACCENT, 32px)
  - Текст статуса: "LOADING — [mod_name]" (Share Tech Mono, TEXT_MUTED, 12px)
- Внизу экрана (или справа):
  - **Полоса/сетка логотипов модов** — маленькие иконки 16x16 или 24x24
  - По мере загрузки каждого мода его логотип загорается/появляется
  - Уже загруженные моды: цветные/яркие
  - Ожидающие: серые/полупрозрачные (или пустое место)
  - Если логотипа нет у мода — показывать заглушку (шестерёнку или букву)

**Макет (подключение к серверу / получение KitConfig):**
- Фон: полноэкранное изображение с crossfade + затемнение
- Центр:
  - "BATTLEFIELD" (ACCENT, крупно, 28px)
  - "LOADING" (TEXT_MUTED, 14px)
  - AccentLine (анимированная — пульсирует)
  - ProgressBar (бесконечная анимация или реальный %)
  - Текст статуса внизу: "Connecting to server..." / "Receiving kit configuration..."

### 3.12 HUD (InGameOverlay)

**Что делает:** Поверх игрового экрана. Показывается если игрок на сервере мода.

**Элементы (сверху вниз, слева):**
- **HP Bar** — полоска здоровья (с цветом по проценту: >60% зелёный, >30% жёлтый, <30% красный). Подпись: "HP" + число.
- **Armor Bar** — полоска брони (синяя/серая)
- **Ammo Display** — текущий магазин / общий запас (Share Tech Mono). Например: `30 / 120`. Если TACZ-оружие — показывает GunId + magazine.
- **Stamina Bar** — полоска выносливости (жёлтая)

**Справа вверху:**
- **Minimap** — маленькая карта (прямоугольник с рамкой, внутри чёрный фон с точками)

**Справа внизу:**
- **Kit Info** — название текущего кита, класс, слоты оружия

**Дополнительно:**
- **Compass** — горизонтальная полоса вверху с N/S/W/E и тиками
- **Target info** — имя цели при прицеливании (опционально)

**Важно:**
- Не перекрывать ванильный HUD если не нужно (или полностью заменять — по конфигу)
- Регистрировать через `RegisterGuiOverlayEvent` (Forge)
- Все элементы с `AnimationHelper.withAlpha()` для учёта `guiOpacity`

---

## 4. Компоненты (новые + улучшенные)

### 4.1 BButton — улучшить

```java
// Добавить:
void setEnabled(boolean enabled)
void setVisible(boolean visible)  // с анимацией fade
void setSize(int w, int h)
void setIcon(ResourceLocation icon)  // иконка слева от текста
void setBadge(String text)  // бейдж справа вверху
void setVariant(Variant variant)  // DEFAULT, PRIMARY, GHOST, DANGER

enum Variant {
    DEFAULT("transparent", BORDER, TEXT_PRIMARY, BG_SURFACE, ACCENT_DIM, ACCENT),
    PRIMARY(ACCENT, ACCENT, 0xFF000000, "#FF8C0A", "#FF8C0A", 0xFF000000),
    GHOST("transparent", "transparent", TEXT_SECONDARY, ACCENT_GHOST, "transparent", ACCENT),
    DANGER("transparent", BORDER, TEXT_SECONDARY, "#3A1010", DANGER, DANGER);
}

// Поведение:
// - Акцентная полоска слева 2px (кроме GHOST)
// - При нажатии scale(0.97)
// - Hover: изменение bg, border, text цвета за 120ms
// - Звук на hover и click
```

### 4.2 BScrollPanel — улучшить

```java
void ensureVisible(int y, int h)  // плавно проскроллить чтобы элемент был виден
void scrollTo(int targetY)        // плавный скролл за N ticks
boolean isMouseOver(int mx, int my)  // проверка внутри панели
int getVisibleTop()
int getVisibleBottom()
int getContentMaxScroll()
void resetScroll()  // scrollOffset = 0, анимированно
void setSmoothScroll(boolean smooth)  // включить/выключить smooth scroll
```

### 4.3 BProgressBar — улучшить

```java
void setColor(int color)           // динамически менять цвет
void setAnimated(boolean animated) // включить анимацию заполнения
void setShowLabel(boolean show)    // показать проценты
void setHeight(int h)
void pulse()                       // эффект пульсации при изменении
```

### 4.4 TopBar (новый компонент)

```java
class TopBar extends AbstractWidget {
    // Принимает:
    // - screenName: String (текущий экран)
    // - playerData: { sp, bc, rank, team, name }
    // - onQuickNav: Consumer<String> (навигация)
    // onBack: Runnable

    // Рендерит:
    // [█ BATTLEFIELD • SCREEN_NAME] [SP N | BC N | RANK N] [HH:MM:SS] [TeamIcon]
}
```

### 4.5 StatusBar (новый компонент)

```java
class StatusBar extends AbstractWidget {
    // Принимает:
    // - connected: boolean
    // - team: String
    // - playerName: String
    // - selectedKit: String

    // Рендерит:
    // [●] CONNECTED | Team N | PLAYER | Kit: KIT_NAME
}
```

### 4.6 SlotRow (общий для всех экранов)

```java
class SlotRow extends AbstractWidget {
    // Универсальный слот для оружия/брони
    // - icon: String (emoji)
    // - label: String (PRIMARY / SECONDARY / HELMET и т.д.)
    // - itemName: String (display name)
    // - canCycle: boolean
    // - onPrev / onNext: Runnable
    // - isActive: boolean

    // Рендерит: [icon] [LABEL] [item name] [‹ index/total ›]
    // Активный: левая граница ACCENT + фон ACCENT_GHOST
}
```

### 4.7 Chip (новый)

```java
class Chip extends AbstractWidget {
    // - label: String
    // - selected: boolean
    // - onClick: Runnable

    // Рендерит: компактная кнопка-тег
    // Selected: ACCENT_GHOST bg, ACCENT border, ACCENT text
    // Hover: BG_SLOT bg, ACCENT_DIM border
}
```

### 4.8 AccentLine (новый)

```java
// Рендерит градиентную линию:
// Горизонтальная: linear-gradient(to right, ACCENT, transparent)
// Вертикальная: linear-gradient(to bottom, ACCENT, transparent)
// Толщина: 2px
```

### 4.9 StatusDot (новый)

```java
// Круглая точка 6px с glow-эффектом
// Цвет: OK / WARN / DANGER
// Box-shadow: 0 0 4px color
```

### 4.10 LockBadge (новый)

```java
// Затемнение поверх карточки (rgba(10,10,10,0.85))
// 🔒 иконка + текст причины
// z-index: 10
```

---

## 5. Баги, которые нужно починить

(см. оригинальный промпт — те же баги)

---

## 6. Что НЕ трогать

- **Пакеты**: `network/`, `data/`, `core/`, `events/`
- **Файлы**: `KitConfig.java`, `KitConfigSyncPacket.java`, `KitSelectPacket.java`, `KitSavePacket.java`, `PlayerCombatData.java`, `CombatEventHandler.java`, `ClientTeamData.java`, `ItemResolver.java`, `TaczAttachmentResolver.java`, `LockChecker.java`, `LockState.java`, `SpawnScreenHelper.java`
- **Звуки**: `ClientSoundHandler`, `ModSounds`
- **Локализация**: `I18n`
- **Анимации**: `AnimationHelper` — только дополнять, не ломать существующие методы
- **Рендер предметов**: `ItemResolver.resolve()` / `getDisplayName()` — только использовать
- **Сетевые пакеты**: не менять формат отправки/получения

---

## 7. Проверка после рефакторинга

Запустить `buildAndDeploy` и проверить весь флоу:

1. **MainMenu** — открывается, кнопки работают
2. **LoadingScreen** — показывается при переходе на сервер
3. **HUD** — отображается в игре, все элементы на месте
4. **SpawnSelectionScreen** — список точек, выбор, кнопка спавна
5. **Classes → Kits → Customize** — полный цикл навигации
6. **EscapeMenu** — открывается по ESC, кнопки работают
7. **Back** — везде корректно возвращает
8. **Save & Deploy** — сохраняет и выбирает кит
9. **Скролл** — везде работает
10. **Анимации** — fade, hover, stagger, accent pulse
11. **Цвета** — все через UITheme
12. **Шрифты** — Rajdhani и Share Tech Mono загружены

Ничего не ломать. Все существующие функции сохранить.

---

## ⚠️ КРИТИЧЕСКИ ВАЖНО: Код только на Java (Minecraft Forge 1.20.1)

**JSX-файл дизайна (BattlefieldMC_GUI.jsx) приложен ТОЛЬКО как визуальный референс — для понимания структуры, цветов, отступов, анимаций, расположения элементов.**

Писать код НУЖНО НА JAVA, для Minecraft Forge 1.20.1:
- `Screen` / `AbstractWidget` / `Button` — вместо React-компонентов
- `GuiGraphics` вместо JSX
- `Minecraft.getInstance().font` вместо CSS-шрифтов
- `AnimationHelper.lerp()` / `easeOutCubic()` вместо CSS animations
- `addRenderableWidget()` вместо рендера списка
- `ArrayList`, `HashMap` вместо useState/map/filter
- `g.fill()`, `g.drawString()` вместо div/span

**Не копировать JSX-синтаксис в Java-код.** Использовать только как референс по визуалу.

---

## 8. Технические детали

- **Шрифты**: грузить через ресурспак `/assets/teamsystem/font/rajdhani.ttf` и `/assets/teamsystem/font/sharetechmono.ttf`, использовать `Font` / `FontManager` Minecraft'а или простой `Minecraft.getInstance().font` если кастомные шрифты не работают
- **MainMenu**: заменять через mixin или `ScreenEvent.Init` — добавлять кнопки поверх стандартного меню, или заменять полностью через конфиг
- **EscapeMenu**: заменять через mixin на `PauseScreen`, добавлять кастомную панель поверх
- **HUD**: рендерить через `RegisterGuiOverlayEvent` (Forge), верхний приоритет
- **LoadingScreen**: показывать при `ClientPlayerNetworkEvent.LoggingIn` и скрывать при получении `KitConfigSyncPacket`
- **Все Click/Scroll**: клипы по границам скролла обязательны
- **Z-order**: кнопки всегда поверх скролл-контента (через `super.render()` в конце)
