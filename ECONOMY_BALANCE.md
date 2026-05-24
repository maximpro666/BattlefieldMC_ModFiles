# ЭКОНОМИКА BATTLEFIELD: MODERN COMBAT (PWP) — ПОФУНКЦИОНАЛЬНАЯ СХЕМА

> Трёхвалютная прогрессия: **BC** (матчевые) → **WC** (постоянные, анлок доступа) → **VC** (командные)
> Три режима сложности: **Easy** (45 мин), **Normal** (1 час), **Hard** (2.5 часа)

---

## 1. ДВИЖОК ЭКОНОМИКИ (CORE)

### 1.1 EconomyState — хранилище валют

| Поле | Тип | Безопасность | Кап |
|---|---|---|---|
| `playerBC` | `ConcurrentHashMap<UUID, Integer>` | ✅ ConcurrentHashMap | 9999 |
| `playerWC` | `ConcurrentHashMap<UUID, Integer>` | ✅ WC по UUID | 300/матч |
| `teamVC` | `ConcurrentHashMap<String, Integer>` | ✅ | 5000/матч |

**Контракты:**
- `addBC(uuid, amount)` — атомарно, не выше капа
- `deductBC(uuid, amount)` — возвращает false если не хватает
- `addVC(team, amount)` — атомарно, не выше капа
- `addWC(uuid, amount)` — атомарно

### 1.2 BattlefieldRuntime — персистентность

- `saveBCBatch()` — пакетное сохранение BC в БД
- `loadBC(uuid)` — загрузка при входе

### 1.3 economy.json

Конфиг (`run/config/battlefield/economy.json`) — мультипликаторы цен, скидки.

### 1.4 Админ-команды

| Команда | Функция |
|---|---|
| `/eco give <player> <amount>` | addBC |
| `/eco set <player> <amount>` | setBC |
| `/eco balance [player]` | просмотр |

### 1.5 Множители сложности

| Параметр | Easy | Normal | Hard |
|---|---|---|---|
| Длительность | 45 мин | 1 час | 2.5 часа |
| Цены техники (BC) | ×0.7 | ×1.0 | ×1.3 |
| Upkeep | ×0.3 | ×1.0 | ×1.2 |
| Цены боеприпасов | ×0.7 | ×1.0 | ×1.3 |
| Пассивный фронтлайн | 1 BC/2.25с | 1 BC/3с | 1 BC/7.5с |
| Пропусков апкипа | 5 | 3 | 2 |
| XP множитель | ×0.7 | ×1.0 | ×1.5 |

---

## 2. ТИКЕТНАЯ СИСТЕМА

### 2.1 Механика

`TicketManager`: два пула (NATO, Russia), синхронизация всем игрокам.

| Действие | Значение | Статус |
|---|---|---|
| Смерть игрока | −1 тикет | ✅ `deductTicket()` |
| Потеря техники | −15 тикетов | 🚫 |
| Захват точки | +10 тикетов | 🚫 |
| Оборона точки | +5 тикетов | 🚫 |
| Сквад-плей | +2 тикета | 🚫 |
| Командир | +5 тикетов | 🚫 |
| Прогрессия матча | +3 тикета | 🚫 |
| Овертайм долив | −2/мин | 🚫 |
| Conquest bleed | Разница CP × 1 / 5с | ✅ |

**Пояснения трёх механик тикетов из tickets.json:**

**Прогрессия матча +3** — пассивный доход тикетов со временем (например, +3 каждые N минут). Не реализовано — в коде нет таймера.

**Овертайм долив −2/мин** — если овертайм активирован (матч зашёл за лимит по времени), команда, у которой меньше флагов, теряет 2 тикета/мин. Не реализовано — овертайм не подключён к тикетам.

**Conquest bleed Разница CP × 1 / 5с** — каждые 5 сек команда с большим числом захваченных точек (Control Points) снимает 1 тикет врагу за каждую точку разницы. Пример: 3:1 по CP = 2 тикета/5с врагу.

### 2.2 Стартовые тикеты по режимам

| Режим | Easy | Normal | Hard |
|---|---|---|---|
| Conquest | 600 | 1000 | 2000 |
| Rush | 300 | 500 | 1000 |
| Breakthrough | 500 | 800 | 1500 |
| Frontline | 400 | 700 | 1200 |
| Domination | 200 | 400 | 800 |
| Tank Superiority | 300 | 500 | 1000 |
| CTF | — | — | — |
| Destruction | 400 | 700 | 1400 |
| Air Superiority | 200 | 400 | 800 |
| TDM | 100 | 150 | 300 |

---

## 3. ПРОГРЕССИЯ ИГРОКА

### 3.1 Ранги

| Ранг | Опыт | Название | Доступ к технике | Лидерство |
|---|---|---|---|---|
| 0 | 0 | Recruit | T1 | Нет |
| 1 | 500 | Private | T2 | Нет |
| 2 | 1500 | Corporal | T3 | Нет |
| 3 | 3500 | Sergeant | T4 | Отряд |
| 4 | 6500 | Staff Sergeant | T5 | Отряд |
| 5 | 11000 | Lieutenant | T6 | Ком. взвода |
| 6 | 18000 | Captain | — | Ком. взвода |
| 7 | 28000 | Major | — | Ком. роты |
| 8 | 42000 | Colonel | — | Ком. роты |

### 3.2 XP за действия

| Действие | XP | За матч |
|---|---|---|
| Убийство | 25 | 425 |
| Ассист | 12 | 120 |
| Захват точки | 80 | 200 |
| Оборона точки | 40 | 120 |
| Уничтожение техники | 75 | 75 |
| Хил (10 HP) | 10 | 50 |
| Рес (медик) | 25 | 100 |
| Пополнение (support) | 10 | 40 |
| Победа в матче | 250 | 125 |
| **Средний за матч** | | **~1255** |

### 3.3 Время до рангов

| Переход | Матчей (Normal) | Часов | Расчёт |
|---|---|---|---|
| Recruit → Private | 1 | 1 ч | 500 XP |
| → Corporal | 2 | 2 ч | 1500 XP |
| → Sergeant | 3 | 3 ч | 3500 XP |
| → Staff Sergeant | 6 | 6 ч | 6500 XP |
| → Lieutenant | 9 | 9 ч | 11000 XP |
| → Captain | 15 | 15 ч | 18000 XP |
| → Major | 23 | 23 ч | 28000 XP |
| → Colonel | 34 | 34 ч | 42000 XP |

⚠️ Расхождение D3: оригинальная таблица завышала матчи в ×1.4 (не учтены проигрыши).

### 3.4 WC — постоянная валюта

| Источник | WC | Период |
|---|---|---|
| Победа Easy | 20 | Каждый матч |
| Поражение Easy | 12 | Каждый матч |
| Победа Normal | 60 | Каждый матч |
| Поражение Normal | 15 | Каждый матч |
| Победа Hard | 200 | Каждый матч |
| Поражение Hard | 60 | Каждый матч |
| Daily Login | +10 | Раз в день |
| First Win of the Day | +25 | Раз в день |
| Casual Streak (3 матча) | +15 | Раз в день |
| Weekly Challenge (5 побед) | 100 | Раз в неделю |
| Squad Bonus | ×1.25 | В отряде |

**Эффективность (50% винрейт):**
- Easy: **31.3 WC/ч** (35+12)/2/0.75
- Normal: **37.5 WC/ч** (60+15)/2/1.0
- Hard: **52 WC/ч** (200+60)/2/2.5

### 3.5 Tech Tree — WC анлоки

**Tier 1** (10-30 WC)

| Техника | WC | Фракция |
|---|---|---|
| Пикап M2 | 10 | NATO |
| Пикап DShK | 10 | Russia |
| HMMWV | 15 | NATO |
| UAZ-469 | 15 | Russia |

**Tier 2** (50-100 WC)

| Техника | WC | Фракция |
|---|---|---|
| BMP-2 | 100 | Russia |
| LAV-25 | 100 | NATO |
| BTR-82A | 80 | Russia |
| Stryker ICV | 80 | NATO |

**Tier 3** (150-250 WC)

| Техника | WC | Фракция |
|---|---|---|
| M2 Bradley | 200 | NATO |
| BMP-3 | 200 | Russia |
| M1128 Stryker MGS | 150 | NATO |
| 2S25 Sprut-SD | 150 | Russia |
| LAV-AD (ПВО) | 200 | NATO |
| Pantsir-S1 (ПВО) | 200 | Russia |
| La Révolution ⭐ | 180 | NATO |

**Tier 4** (300-400 WC)

| Техника | WC | Фракция |
|---|---|---|
| M1A2 Abrams | 350 | NATO |
| T-90MS | 350 | Russia |
| Type 99A | 350 | China |
| Leopard 2A7 | 400 | NATO |
| T-80BVM | 350 | Russia |
| Challenger 2 | 400 | NATO |
| Mk 42 ⭐ | 350 | NATO |
| Mle 1934 ⭐ | 350 | Russia |
| YX-100 ⭐ | 400 | China |

**Tier 5** (500-800 WC)

| Техника | WC | Фракция |
|---|---|---|
| AH-64 Apache | 700 | NATO |
| Ka-52 Alligator | 700 | Russia |
| Mi-28 Havoc | 600 | Russia |
| M6 Linebacker | 500 | NATO |
| Tunguska-M1 | 500 | Russia |
| AH-6 Little Bird | 500 | NATO |
| RAH-66 Comanche | 800 | NATO |
| PLZ-05 | 600 | China |
| TOS-1 Buratino | 600 | Russia |

**Tier 6** (900-1500 WC)

| Техника | WC | Фракция |
|---|---|---|
| F-16 Fighting Falcon | 1200 | NATO |
| Su-27 Flanker | 1200 | Russia |
| MiG-29 Fulcrum | 1000 | Russia |
| F/A-18 Hornet | 1300 | NATO |
| M1A2 SEPv3 ⭐ | 900 | NATO |
| T-14 Armata ⭐ | 900 | Russia |
| A-10 Warthog | 1200 | NATO |
| Su-25 Frogfoot | 1000 | Russia |
| Eurofighter Typhoon | 1400 | NATO |
| Su-57 Felon | 1400 | Russia |
| F-35B Lightning II ⭐ | 1200 | NATO |
| AC-130U ⭐ | 1300 | NATO |
| Merkava Mk.4 ⭐ | 1000 | NATO |
| B-52 Stratofortress ⭐ | 1400 | NATO |
| B-2 Spirit ⭐ | 1400 | NATO |

> ⭐ премиум — анлок техники за WC (не скины, ТТХ как у обычной того же тира). Legend Pack ($115): все анлоки + ×1.5 XP.

---

## 4. BC — МАТЧЕВЫЙ ДОХОД

### 4.1 Пассивный фронтлайн

| Режим | Доход | Условие |
|---|---|---|
| Easy | 1 BC / 2.25с | В зоне точки |
| Normal | 1 BC / 3с | В зоне точки |
| Hard | 1 BC / 7.5с | В зоне точки |

**★ Баланс:** пассив доминирует над активными действиями (5:1).
Фикс: убрать безусловный пассив, доход только при врагах в радиусе 50м.

### 4.2 Активные действия

| Действие | BC | Easy | Normal | Hard |
|---|---|---|---|---|
| Убийство | 15 | 8 | 5→**15** | 3 |
| Ассист | 8 | — | 2→**8** | — |
| Захват точки | 150 | 200 | 150 | 80 |
| Kill Streak (×5) | 15 | — | 1→**15** | — |
| Kill Streak (×10) | 40 | — | 2→**40** | — |
| Kill Streak (×15) | 80 | — | 3→**80** | — |
| MVP 1-е | 75 | — | 10→**75** | — |
| MVP 2-е | 40 | — | 5→**40** | — |
| MVP 3-е | 20 | — | 3→**20** | — |
| Лечение 4 HP | 1 | — | 2 HP→**4 HP** | — |
| Revive | 10 | — | 5→**10** | — |
| Medic Streak | 10 | — | 3→**10** | — |

### 4.3 Доход по времени

**Normal (1 час)**

| У точки | Пассив | Актив | Всего |
|---|---|---|---|
| 5 мин | 100 | 25 | **125 BC** |
| 10 мин | 200 | 100 | **300 BC** |
| 15 мин | 300 | 175 | **475 BC** — танк |
| 20 мин | 400 | 250 | **650 BC** |
| 30 мин | 600 | 350 | **950 BC** |
| Полный матч | 1200 | 500 | **1700 BC** |

**Easy (45 мин)**

| У точки | Пассив | Актив | Всего |
|---|---|---|---|
| 5 мин | 133 | 65 | **198 BC** |
| 10 мин | 267 | 200 | **467 BC** — танк |
| 15 мин | 400 | 335 | **735 BC** |
| Полный матч | 1200 | 600 | **~1800 BC** |

**Hard (2.5 часа)**

| У точки | Пассив | Актив | Всего |
|---|---|---|---|
| 5 мин | 40 | 12 | **52 BC** |
| 10 мин | 80 | 30 | **110 BC** |
| 30 мин | 240 | 130 | **370 BC** — танк |
| 1 час | 480 | 270 | **750 BC** |
| Полный матч | 1200 | 420 | **~1620 BC** |

---

## 5. VC — КОМАНДНАЯ ВАЛЮТА

### 5.1 Генерация

- Каждые 60 секунд за захваченные точки
- **Small** CP: 50 VC/мин
- **Medium** CP: 100 VC/мин
- **Major** CP: 120 VC/мин (было 200)
- Обнуляется каждый матч
- Тратится только на спавн техники

### 5.2 Дополнительные источники

- Оборона точки под атакой: +30 VC/мин
- Уничтожение техники врага: +10 команде

### 5.3 Капы

- 400 VC/мин на команду
- 5000 VC/матч на команду (после — 0 VC/мин, только кил/оборона)

---

## 6. ТЕХНИКА

### 6.1 Проверки при спавне

Последовательность (фикс H1, H2, D9):

1. Проверка WC анлока — есть ли у игрока техника в tech tree
2. Проверка ранга — `playerRank >= vehicle.getRequiredTier()`
3. Проверка BC — `deductBC(player, cost)`
4. Проверка VC — `deductVC(team, vcCost)`
5. **Создание entity** на валидной позиции (L7)
6. Если entity не создалось — **рефанд BC + VC**

### 6.2 Цены (Normal)

**Транспорт / T1**

| Техника | BC | VC |
|---|---|---|
| Пикап M2 / DShK | 50 | 20 |
| HMMWV / UAZ-469 | 40 | 15 |

**БМП / БТР / T2**

| Техника | BC | VC |
|---|---|---|
| BMP-2 | 200 | 80 |
| LAV-25 | 200 | 80 |
| BTR-82A | 180 | 70 |
| Stryker ICV | 180 | 70 |

**IFV / Stryker / T3**

| Техника | BC | VC |
|---|---|---|
| M2 Bradley | 300 | 120 |
| BMP-3 | 300 | 120 |
| M1128 Stryker MGS | 280 | 110 |
| LAV-AD (ПВО) | 350 | 130 |
| Pantsir-S1 (ПВО) | 350 | 130 |

**Танки / T4**

| Техника | BC | VC |
|---|---|---|
| M1A2 Abrams | 450 | 180 |
| T-90MS | 450 | 180 |
| Type 99A | 450 | 180 |
| Leopard 2A7 | 500 | 200 |
| T-80BVM | 450 | 180 |
| Challenger 2 | 500 | 200 |
| Премиум (Mk42, Mle, YX-100) | 450 | 180 |

**Вертолёты / T5**

| Техника | BC | VC |
|---|---|---|
| AH-64 Apache | 600 | 250 |
| Ka-52 Alligator | 600 | 250 |
| Mi-28 Havoc | 550 | 230 |
| AH-6 Little Bird | 400 | 180 |
| RAH-66 Comanche | 650 | 280 |
| Транспортный вертолёт | 250 | 120 |

**Элитные танки / T6**

| Техника | BC | VC |
|---|---|---|
| M1A2 SEPv3 ⭐ | 500 | 200 |
| T-14 Armata ⭐ | 500 | 200 |

**Самолёты / T6**

| Техника | BC | VC |
|---|---|---|
| F-16 Fighting Falcon | 700 | 300 |
| Su-27 Flanker | 700 | 300 |
| MiG-29 Fulcrum | 650 | 280 |
| F/A-18 Hornet | 750 | 320 |
| A-10 Warthog | 1000 | 400 |
| Su-25 Frogfoot | 800 | 350 |
| Eurofighter Typhoon | 800 | 350 |
| Su-57 Felon | 900 | 380 |
| Премиум (F-35B, AC-130, B-52) | 1000-1200 | 400-500 |

### 6.3 Upkeep

| Категория | Интервал | BC/тик | BC/мин | За час (Normal) |
|---|---|---|---|---|
| Транспорт | 120 с | 5 | 2.5 | 150 |
| БМП / БТР / ПВО | 90 с | 15 | 10 | 600 |
| Танк | 60 с | 10 | 10 | 600 |
| Артиллерия | 90 с | 20 | 13.3 | 800 |
| Вертолёт | 60 с | 15 | 15 | 900 |
| Самолёт | 60 с | 20 | 20 | 1200 |

**Механика:**
- Upkeep как **долг при деспавне** — вся стоимость за время жизни вычитается при деспавне (anti-abuse #3)
- Grace period: первые 3 мин после спавна без апкипа
- Normal: 3 пропуска = деспавн, Hard: 2, Easy: 5

### 6.4 Динамические цены

Цены снижаются по фазе матча:

| Фаза | % времени | Скидка |
|---|---|---|
| 0-25% | Ранняя | 0% |
| 25-50% | Середина | −10% |
| 50-75% | Поздняя | −20% |
| 75-100% | Финал | −30% |
| Овертайм | 100%+ | −30% кап |

Upkeep НЕ снижается. Пересчёт при открытии меню спавна.

---

## 7. БОЕПРИПАСЫ

### 7.1 Цены (Normal)

| Тип | Базовая | ★ Фикс |
|---|---|---|
| RPG rocket | 60 | 60 |
| RPG TBG | 100 | 100 |
| Javelin missile | 300 | **150** |
| Igla / Stinger | 250 | **120** |
| ATGM (TOW/9M120) | 200 | 200 |
| AGM-65 / Kh-39 | 400 | **200** |
| Hellfire | 250 | **150** |
| APFSDS | 100 | **120** |
| HE | 60 | 60 |
| HEAT | 80 | 80 |
| HYDRA / S-8 | 150 | 150 |
| AIM-9 | 180 | 180 |
| AIM-120 | 400 | 400 |
| AGM-114 | 350 | 350 |
| Авиабомба | 300 | 300 |

### 7.2 Ресапплай

- H3: BC за ракету списан до создания предмета (фикс: создать → списать)
- M4: Бесплатный ресапплай аммо (фикс: ввести стоимость)

### 7.3 ID предметов

**Патроны:** `tacz:556x45`, `tacz:762x39`, `tacz:58x42`, `tacz:308`, `tacz:338`, `tacz:50bmg`, `tacz:30_06`, `tacz:792x57`, `tacz:9mm`, `tacz:45acp`, `tacz:57x28`, `tacz:12g`, `tacz:50ae`, `tacz:545x39`, `tacz:762x54`, `tacz:46x30`, `tacz:rpg_rocket`, `tacz:40mm`

**Снаряды SW:** `rpg_rocket_standard`, `tbg_rocket`, `javelin_missile`, `medium_anti_air_missile`, `medium_anti_ground_missile`, `large_anti_ground_missile`, `apfsds_shell`, `he_shell`, `heat_shell`, `large_shell_ap/he/gs/cm/wp`, `small_shell_ap/he/gs/aa`, `small_rocket`, `hellfire_missile`, `aim9_missile`, `aim120_missile`, `agm114_missile`, `hydra_rocket`, `medium_aerial_bomb`, `mortar_shell`, `grenade_40mm`, `c4_bomb`, `claymore_mine`, `blu_43_mine`, `m18_smoke_grenade`, `repair_tool`, `taser`

---

## 8. КЛАССЫ

### 8.1 HP по классам

| Тип | Базовые | HP | Премиум | HP |
|---|---|---|---|---|
| Assault | Rifleman / Shturmovik | 20 | Shock Trooper / Guardsman | 24 |
| Medic | Combat Medic / Polevoy Medik | 20 | Field Surgeon / Sanitar | 20 |
| Engineer | Combat Engineer / Combat Saper | 22 | Sapper / Heavy Saper | 24 |
| Support | Machinegunner / Pulemetchik | 24 | Heavy Gunner | 26 |
| Sniper | Scout / Razvedchik | 20 | Ghost / Spetsnaz | 22 |
| Heavy AT | RPG Gunner | 22 | Javelin / Igla Operator | 22 |
| Heavy Armor | Juggernaut | 28 | Bulwark | 30 |

Минимум 20 HP, максимум 30 HP.

### 8.2 NATO

**ASSAULT**

**Rifleman** 🟢 — ранг 0, 0 BC, 20 HP
Primary: M4A1 / SCAR-L / G36K (5.56×45 ×240) | Secondary: M1911 (.45 ACP ×28) / Glock 17 (9mm ×60) | Grenade: M67 ×2 | Medical: Bandaid Box

**Breacher** — ранг 1, 2 BC, 20 HP
Primary: MP5 / Vector (9mm / .45 ×180) | Secondary: Deagle (.50 AE ×21) | Special: C4 ×2 | Grenade: M67 | Medical: Bandaid Box

**Shock Trooper** ⭐ — ранг 4, 4 BC, 24 HP
Primary: HK416A5 / SCAR-L (5.56×45 ×240) | Secondary: Glock 17 (9mm ×60) | Underbarrel: M79 + 40mm ×2 | Grenade: M67 | Medical: Bandaid Box + Stimpack

**MEDIC** (лимит: 2 на отряд)

**Combat Medic** 🟢 — ранг 0, 0 BC, 20 HP
Primary: M4A1 (5.56×45 ×240) | Secondary: M1911 (.45 ACP ×28) | Grenade: M18 Smoke ×2 | Medical: Medkit ×3

**Field Surgeon** ⭐ — ранг 2, 0 BC, 20 HP
Primary: MP5 (9mm ×180) | Secondary: M1911 (.45 ACP ×28) | Grenade: M18 Smoke ×2 | Medical: Medkit ×3 + Morphine

**ENGINEER** (лимит: 4 на команду)

**Combat Engineer** 🟢 — ранг 1, 3 BC, 22 HP
Primary: M4A1 | Secondary: Glock 17 (9mm ×60) | Special: Repair Tool + Claymore ×2 | Grenade: M67 | Medical: Bandaid Box

**Anti-Tank** — ранг 2, 3 BC, 22 HP
Primary: SCAR-L | Secondary: Glock 17 (9mm ×60) | Special: RPG + 3 rockets + C4 | Grenade: M67 | Medical: Bandaid Box

**Sapper** ⭐ — ранг 4, 4 BC, 24 HP
Primary: G36K | Secondary: Deagle (.50 AE ×21) | Special: Repair + C4×2 + Claymore×2 | Grenade: M67 | Medical: Bandaid Box + Stimpack

**SUPPORT**

**Machinegunner** — ранг 2, 3 BC, 24 HP
Primary: M249 (5.56×45 ×480) | Secondary: M1911 (.45 ACP ×28) | Medical: Bandages ×2

**Heavy Gunner** ⭐ — ранг 4, 4 BC, 26 HP
Primary: M249 | Secondary: Glock 17 (9mm ×60) | Special: Smoke ×2 | Medical: Bandages + Stimpack

**SNIPER** (лимит: 2 на команду)

**Scout Sniper** — ранг 2, 4 BC, 20 HP
Primary: AWP (.338 ×40) | Secondary: MK14 (.308 ×80) | Tertiary: Glock 17 (9mm ×60) | Medical: Bandaid Box

**Heavy Sniper** — ранг 4, 5 BC, 20 HP
Primary: M95 (.50 ×30) | Secondary: M1911 (.45 ACP ×28) | Grenade: M67 | Medical: Bandaid Box

**Ghost** ⭐ — ранг 5, 5 BC, 22 HP
Primary: M700 (.30-06 ×40) | Secondary: MK14 (.308 ×80) | Tertiary: Deagle (.50 AE ×21) | Medical: Bandaid Box + Stimpack

**HEAVY AT** (лимит: 1 на отряд, 2 на команду)

**RPG Gunner** 🟢 — ранг 1, 5 BC, 22 HP
Primary: SCAR-L (5.56×45 ×180) | Secondary: M1911 (.45 ACP ×28) | Special: RPG-7 + 3 rockets | Medical: Bandaid Box

**Javelin Operator** — ранг 3, 5 BC, 22 HP
Primary: P90 (5.7×28 ×200) | Secondary: Glock 17 (9mm ×60) | Special: Javelin + 2 missiles | Medical: Bandaid Box

**HEAVY ARMOR**

**Juggernaut** — ранг 2, 5 BC, 28 HP
Primary: M249 (5.56×45 ×240) | Secondary: AA-12 (12g ×40) | Medical: Bandages ×2

**Bulwark** ⭐ — ранг 5, 5 BC, 30 HP
Primary: M249 | Secondary: M870 (12g ×40) | Special: C4 + Smoke | Medical: Bandages + Stimpack

### 8.3 Russia

**ASSAULT**

**Shturmovik** 🟢 — ранг 0, 0 BC, 20 HP
Primary: AK-47 / Type 81 (7.62×39 ×240) | Secondary: MP-443 (9mm ×60) / Glock 17 (9mm ×60) | Grenade: F-1 / RGD-5 ×2 | Medical: Bandaid Box

**Storm Group** — ранг 1, 2 BC, 20 HP
Primary: MP5 / P90 (9mm / 5.7×28 ×180) | Secondary: CZ75 (9mm ×60) | Special: C4 ×2 | Grenade: F-1 | Medical: Bandaid Box

**Guardsman** ⭐ — ранг 4, 4 BC, 24 HP
Primary: QBZ-191 / AK-47 | Secondary: MP-443 (9mm ×60) | Special: RPG + C4 | Grenade: RGN | Medical: Bandaid Box + Stimpack

**MEDIC** (лимит: 2 на отряд)

**Polevoy Medik** 🟢 — ранг 0, 0 BC, 20 HP
Primary: AK-47 | Secondary: MP-443 (9mm ×60) | Grenade: RDG-2 Smoke ×2 | Medical: Medkit ×3

**Sanitar** ⭐ — ранг 2, 0 BC, 20 HP
Primary: MP5 | Secondary: CZ75 (9mm ×60) | Grenade: RDG-2 Smoke ×2 | Medical: Medkit ×3 + Morphine

**ENGINEER** (лимит: 4 на команду)

**Combat Saper** 🟢 — ранг 1, 3 BC, 22 HP
Primary: AK-47 | Secondary: MP-443 (9mm ×60) | Special: Repair + PMN-1 ×2 | Grenade: RGD-5 | Medical: Bandaid Box

**Anti-Tank Saper** — ранг 2, 3 BC, 22 HP
Primary: Type 81 | Secondary: MP-443 (9mm ×60) | Special: RPG + 3 rockets + C4 | Medical: Bandaid Box

**Heavy Saper** ⭐ — ранг 4, 4 BC, 24 HP
Primary: QBZ-191 | Secondary: Deagle (.50 AE ×21) | Special: Repair + C4×2 + Claymore×2 | Grenade: M67 | Medical: Bandaid Box + Stimpack

**SUPPORT**

**Pulemetchik** — ранг 2, 3 BC, 24 HP
Primary: RPK (7.62×39 ×480) | Secondary: CZ75 (9mm ×60) | Medical: Bandages ×2

**Heavy Gunner** ⭐ — ранг 4, 4 BC, 26 HP
Primary: RPK | Secondary: MP-443 (9mm ×60) | Special: Smoke ×2 | Medical: Bandages + Stimpack

**SNIPER** (лимит: 2 на команду)

**Razvedchik Sniper** — ранг 2, 4 BC, 20 HP
Primary: Kar98 (7.92×57 ×40) | Secondary: SKS (7.62×39 ×80) | Tertiary: CZ75 (9mm ×60) | Medical: Bandaid Box

**Anti-Material Sniper** — ранг 4, 5 BC, 20 HP
Primary: M107 (.50 ×30) | Secondary: CZ75 (9mm ×60) | Grenade: F-1 | Medical: Bandaid Box

**Spetsnaz Sniper** ⭐ — ранг 5, 5 BC, 22 HP
Primary: M700 (.30-06 ×40) | Secondary: SKS | Tertiary: Deagle (.50 AE ×21) | Medical: Bandaid Box + Stimpack

**HEAVY AT** (лимит: 1 на отряд, 2 на команду)

**RPG Gunner** 🟢 — ранг 1, 5 BC, 22 HP
Primary: AK-47 | Secondary: CZ75 (9mm ×60) | Special: RPG-7 + 3 rockets | Medical: Bandaid Box

**Igla Operator** — ранг 3, 5 BC, 22 HP
Primary: P90 | Secondary: MP-443 (9mm ×60) | Special: Igla 9K38 + 2 SAM | Medical: Bandaid Box

**HEAVY ARMOR**

**Juggernaut** — ранг 2, 5 BC, 28 HP
Primary: RPK (7.62×39 ×240) | Secondary: AA-12 (12g ×40) | Medical: Bandages ×2

**Bulwark** ⭐ — ранг 5, 5 BC, 30 HP
Primary: RPK | Secondary: M870 (12g ×40) | Special: C4 + Smoke | Medical: Bandages + Stimpack

### 8.4 Сводка классов

| Класс | Китов | Премиум | Бесплатный (ранг 0) |
|---|---|---|---|
| Assault | 3 | ✅ Shock Trooper / Guardsman | ✅ Rifleman / Shturmovik |
| Medic | 2 | ✅ Field Surgeon / Sanitar | ✅ Combat Medic / Polevoy Medik |
| Engineer | 3 | ✅ Sapper / Heavy Saper | ✅ Combat Engineer / Combat Saper |
| Support | 2 | ✅ Heavy Gunner | ❌ (ранг 2) |
| Sniper | 3 | ✅ Ghost / Spetsnaz Sniper | ❌ (ранг 2) |
| Heavy AT | 2 | ❌ | ❌ (ранг 1) |
| Heavy Armor | 2 | ✅ Bulwark | ❌ (ранг 2) |

### 8.5 Squad bonus

- Бонус работает если в одном отряде (без дистанции)
- ×1.25 к BC/WC (было ×1.1)
- Squad spawn: спавн на соклане

### 8.6 Давление (Pressure System) 🚫

| Давление | Триггер | Эффект |
|---|---|---|
| Ground | >50% команды врага в технике | Heavy AT: ×1.5 BC за уничтожение |
| Air | >3 авиации врага | ПЗРК ракеты ×0.5 |
| Siege | >3 снайперов/арты врага | +25% урона по FOB/арте |

Классы уже открыты (ранг 1-2), давление только баффает экономику.

---

## 9. FOB

### 9.1 Параметры

| Параметр | Easy | Normal | Hard |
|---|---|---|---|
| Стоимость | 80 BC | **300 BC** | 150 BC |
| Макс на команду | 2 | 2 | 2 |
| Кулдаун (командный) | 60 с | 90 с | 120 с |
| Установка | 10с | 10с | 10с |
| Upkeep | 5 BC/мин | 5 BC/мин | 5 BC/мин |
| При уничтожении | Деспавн | Деспавн | Деспавн |

---

## 10. МЕДИЦИНА

### 10.1 Предметы

| Предмет | ID | Применение |
|---|---|---|
| Medkit | `marbledsfirstaid:medkit` | Медик, лечит других, ×3 |
| Bandaid Box | `marbledsfirstaid:bandaid_box` | Все, самолечение, ×4 |
| Bandages | `marbledsfirstaid:bandages` | Support/Heavy Armor, ×2 |
| Morphine | `marbledsfirstaid:morphine` | Воскрешение |
| Stimpack | `marbledsfirstaid:stimpack` | Премиум, реген + ускорение |

### 10.2 Награды

| Действие | XP | BC |
|---|---|---|
| Лечение 4 HP | 2 | 1 |
| Воскрешение | 25 | **10** |
| Medic Assist | 5 | 0 |
| Medic Streak (5 лечений) | 30 | **10** |

### 10.3 Анти-абьюз

- Рейт-лимит: 5 хилов/мин на игрока (anti-abuse #4)
- Селф-хил = 0 BC (anti-abuse #5)

---
