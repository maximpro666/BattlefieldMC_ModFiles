# ЭКОНОМИКА BATTLEFIELD: MODERN COMBAT (PWP) — ПОЛНАЯ СХЕМА БАЛАНСА

> Трёхвалютная прогрессия: **BC** (матчевые) → **WC** (постоянные, анлок доступа) → **VC** (командные)
> Три режима сложности: **Easy** (45 мин, Аркада), **Normal** (1 час, Стандарт), **Hard** (2.5 часа, Хардкор)
> WC — постоянная валюта, тратится на разблокировку доступа к технике и классам

---

## 1. ТРИ РЕЖИМА СЛОЖНОСТИ

### 1.1 Базовые параметры

| Параметр | Easy | Normal | Hard |
|---|---|---|---|
| Длительность матча | 45 мин | 1 час | 2.5 часа |
| Стартовые тикеты | 600 | 1000 | 2000 |
| Death cost (стоимость смерти) | ×0.7 | ×1.0 | ×1.5 |
| BC за убийство | 8 | 5 | 3 |
| BC за захват точки | 200 | 150 | 80 |
| BC за победу | 20 | 15 | 10 |
| WC за победу | 20 | 60 | 200 |
| WC за поражение | 12 | 15 | 60 |
| First Win of the Day (+25 WC) | ✅ | ✅ | ✅ |
| Weekly (5 побед, +100 WC) | ✅ | ✅ | ✅ |
| Пассивный фронтлайн доход | Вкл (1 BC/3 с) | Вкл (1 BC/3 с) | Вкл (1 BC/3 с) |
| Множитель стоимости техники (BC) | ×0.7 | ×1.0 | ×1.3 |
| Множитель стоимости апкипа | ×0.0 (выкл) | ×1.0 | ×1.5 |
| Множитель цен на боеприпасы | ×0.7 | ×1.0 | ×1.3 |
| Пропусков апкипа до деспавна | — (нет апкипа) | 3 | 2 |
| Weekend bonus (×2 XP + WC) | ✅ | ✅ | ✅ |

### 1.2 Ticket drain

| Действие | Easy | Normal | Hard |
|---|---|---|---|
| Death cost | ×0.7 (14) | ×1.0 (20) | ×1.5 (30) |

### 1.3 Настройка голосования

- `difficultyVotingEnabled: true` — каждая карта появляется в голосовании в 3 вариантах (Easy/Normal/Hard)
- `difficultyVotingEnabled: false` — каждая карта использует `MapConfig.difficulty` (задаётся админом через `/map difficulty <name> <easy\|normal\|hard>`)
- Настройка в `run/config/battlefield/mode.json`

---

## 2. BATTLE COINS (BC) — МАТЧЕВАЯ ВАЛЮТА

### 2.1 Заработок за матч (Normal, 1 час)

> **Цель:** соло-игрок зарабатывает на танк за ~12-15 мин активной игры

| Источник | Расчёт | BC за матч |
|---|---|---|
| Фронтлайн (у точки ~20 мин) | ~400 тиков × 1 BC | 400 |
| Убийства (15-20) | ~17 × 5 BC | 85 |
| Ассисты (8-12) | ~10 × 2 BC | 20 |
| Захваты точек (2-3) | ~2.5 × 150 BC | 375 |
| Kill Streak (каждые 5) | ~3 активации × 1 BC | 3 |
| Победа | 1 × 15 BC | 15 |
| MVP (топ-3) | редко | 0-10 |
| **Итого средний игрок** | | **~898 BC** |
| **Итого топ-игрок** (25 фрагов, 4 захвата, победа) | | **~1250 BC** |

Доход по времени (соло, Normal):

| Время у точки | Фронтлайн | + убийства/захваты | Всего |
|---|---|---|---|
| 5 мин | 100 | 25 | **125 BC** — транспорт |
| 10 мин | 200 | 100 | **300 BC** — БМП |
| 15 мин | 300 | 175 | **475 BC** — танк |
| 20 мин | 400 | 250 | **650 BC** — вертолёт |
| 30 мин | 600 | 350 | **950 BC** — самолёт |
| Полный матч (60 мин) | 1200 | 500 | **1700 BC** — всё что хочешь |

### 2.2 Kill Streak бонус

- Каждые 5 убийств без смерти: +1 BC (×5→+1, ×10→+2, ×15→+3 и т.д.)
- Счётчик сбрасывается при смерти
- Отображается в HUD как `Kill Streak x5`

### 2.3 MVP бонус (в конце матча)

- 1-е место по очкам: +10 BC
- 2-е место: +5 BC
- 3-е место: +3 BC

### 2.4 Бесплатные киты

- **Rifleman** 🟢 (0 BC) и **Combat Medic** 🟢 (0 BC) доступны всегда, даже при 0 BC
- Никакого принудительного BC floor — естественная защита через бесплатные классы
- Если BC кончились — берёшь Rifleman, зарабатываешь, покупаешь класс/технику

### 2.5 Траты BC

> Все цены — для соло-игрока на Normal. С ×0.7 на Easy, ×1.3 на Hard.

| Покупка | Стоимость (Normal) | Время соло | Примечание |
|---|---|---|---|
| FOB (спавн-точка) | 100 BC | 5 мин | Спавн команды |
| Класс/набор | 0-5 BC | 0-15 с | Зависит от класса |
| Транспорт (пикап/уаз) | 50 BC | 2-3 мин | |
| БМП/БТР (T2) | 200 BC | 8-10 мин | |
| IFV/Stryker (T3) | 300 BC | 12-15 мин | |
| Танк (T4) | 450 BC | 15-18 мин | |
| SPAA/ПВО | 350 BC | 12-15 мин | |
| Вертолёт (T5) | 600 BC | 20-25 мин | |
| Транспортный вертолёт | 250 BC | 10-12 мин | |
| Самолёт лёгкий (F-16) | 700 BC | 25-30 мин | |
| Самолёт тяжёлый (A-10) | 1000 BC | 30-40 мин | |
| Ракеты/снаряды | 60-400 BC | | |
| Апкип техники | 5-40 BC / интервал | | |

---

## 3. WAR CREDITS (WC) — ПОСТОЯННАЯ ВАЛЮТА (АНЛОК)

### 3.1 Заработок

| Источник | WC | Период |
|---|---|---|
| Победа Easy | 20 | Каждый матч |
| Поражение Easy | 6 | Каждый матч |
| Победа Normal | 60 | Каждый матч |
| Поражение Normal | 15 | Каждый матч |
| Победа Hard | 200 | Каждый матч |
| Поражение Hard | 60 | Каждый матч |
| Daily Login | +10 | Раз в день (просто зайти) |
| First Win of the Day | +25 | Раз в день |
| Casual Streak (3 матча) | +15 | Раз в день (за 3-ю игру) |
| Weekly Challenge (5 побед) | 100 | Раз в неделю |
| Squad Bonus | ×1.1 к WC/BC | В отряде с игроками |
| **Казуал (1-2 ч/день)** | **~110-150 WC/день** | |
| **Задрот (4+ ч/день)** | **~270-430 WC/день** | |

### 3.2 WC — эффективность по режимам

| Режим | WC/час (50% винрейт) | WC/час (Hard — эталон) |
|---|---|---|
| Easy | (20+6)/2 / 0.75ч = **17.3 WC/ч** | ×1.0 |
| Normal | (60+15)/2 / 1.0ч = **37.5 WC/ч** | ×2.17 |
| Hard | (200+60)/2 / 2.5ч = **52 WC/ч** | ×3.0 |

### 3.3 WC — разблокировка доступа к технике (Tech Tree)

Каждый тир — постоянная разблокировка. Купил единицу техники — получил доступ к ней навсегда.

#### Tier 1 (10-30 WC) — Лёгкая / транспорт
0-1 матч Easy

| Техника | WC | Фракция | ID |
|---|---|---|---|
| Пикап M2 | 10 | NATO | `superbwarfare:technical_m2` |
| Пикап DShK | 10 | Russia | `superbwarfare:technical_dshk` |
| HMMWV (пустой) | 15 | NATO | `superbwarfare:hmmwv` |
| UAZ-469 | 15 | Russia | `superbwarfare:uaz` |

#### Tier 2 (50-100 WC) — IFV / Лёгкая броня
1-2 матча Normal

| Техника | WC | Фракция | ID |
|---|---|---|---|
| BMP-2 | 100 | Russia | `superbwarfare:bmp_2` |
| LAV-25 | 100 | NATO | `superbwarfare:lav25` |
| BTR-82A | 80 | Russia | `superbwarfare:btr_82a` |
| Stryker ICV | 80 | NATO | `superbwarfare:stryker_icv` |

#### Tier 3 (150-250 WC) — APC / Stryker / Light Tank
2-4 матча Normal

| Техника | WC | Фракция | ID |
|---|---|---|---|
| M2 Bradley | 200 | NATO | `superbwarfare:m2_bradley` |
| BMP-3 | 200 | Russia | `superbwarfare:bmp_3` |
| M1128 Stryker MGS | 150 | NATO | `superbwarfare:stryker_mgs` |
| 2S25 Sprut-SD | 150 | Russia | `superbwarfare:2s25_sprut` |
| LAV-AD (ПВО) | 200 | NATO | `superbwarfare:lav_ad` |
| Pantsir-S1 (ПВО) | 200 | Russia | `superbwarfare:pantsir_s1` |
| **La Révolution** ⭐ премиум | 180 | NATO Preorder | `superbwarfare:la_revolution` |

#### Tier 4 (300-400 WC) — Main Battle Tank
5-7 матчей Normal

| Техника | WC | Фракция | ID |
|---|---|---|---|
| M1A2 Abrams | 350 | NATO | `superbwarfare:m1a2_abrams` |
| T-90MS | 350 | Russia | `superbwarfare:t90ms` |
| Type 99A | 350 | China | `superbwarfare:type_99a` |
| Leopard 2A7 | 400 | NATO (Germany) | `superbwarfare:leopard_2a7` |
| T-80BVM | 350 | Russia | `superbwarfare:t80bvm` |
| Challenger 2 | 400 | NATO (UK) | `superbwarfare:challenger_2` |
| **Mk 42** ⭐ премиум | 350 | NATO | `superbwarfare:mk42` |
| **Mle 1934** ⭐ премиум | 350 | Russia | `superbwarfare:mle_1934` |
| **YX-100** ⭐ премиум | 400 | China | `superbwarfare:yx_100` |

#### Tier 5 (500-800 WC) — Attack Heli / SPAA / Artillery
8-13 матчей Normal

| Техника | WC | Фракция | ID |
|---|---|---|---|
| AH-64 Apache | 700 | NATO | `superbwarfare:ah64_apache` |
| Ka-52 Alligator | 700 | Russia | `superbwarfare:ka52` |
| Mi-28 Havoc | 600 | Russia | `superbwarfare:mi28_havoc` |
| M6 Linebacker | 500 | NATO | `superbwarfare:m6_linebacker` |
| Tunguska-M1 | 500 | Russia | `superbwarfare:tunguska_m1` |
| AH-6 Little Bird | 500 | NATO | `superbwarfare:ah6_little_bird` |
| RAH-66 Comanche | 800 | NATO | `superbwarfare:rah66_comanche` |
| PLZ-05 (арта) | 600 | China | `superbwarfare:plz_05` |
| TOS-1 Buratino | 600 | Russia | `superbwarfare:tos_1` |

#### Tier 6 (900-1500 WC) — Jet / Heavy Tank / Elite
15-25 матчей Normal

| Техника | WC | Фракция | ID |
|---|---|---|---|
| F-16 Fighting Falcon | 1200 | NATO | `superbwarfare:f16` |
| Su-27 Flanker | 1200 | Russia | `superbwarfare:su27` |
| MiG-29 Fulcrum | 1000 | Russia | `superbwarfare:mig29` |
| F/A-18 Hornet | 1300 | NATO | `superbwarfare:fa18` |
| M1A2 SEPv3 | 900 | NATO | `superbwarfare:m1a2_sepv3` |
| T-14 Armata | 900 | Russia | `superbwarfare:t14_armata` |
| A-10 Warthog | 1200 | NATO | `superbwarfare:a10` |
| Su-25 Frogfoot | 1000 | Russia | `superbwarfare:su25` |
| Eurofighter Typhoon | 1400 | NATO | `superbwarfare:eurofighter` |
| Su-57 Felon | 1400 | Russia | `superbwarfare:su57` |
| **F-35B Lightning II** ⭐ премиум | 1500 | NATO (USMC) | `superbwarfare:f35b` |
| **AC-130U** ⭐ премиум | 1500 | NATO | `superbwarfare:ac130u` |
| **Merkava Mk.4** ⭐ премиум | 1000 | NATO (Israel) | `superbwarfare:merkava_mk4` |
| **B-52 Stratofortress** ⭐ премиум | 1500 | NATO | `superbwarfare:b52` |
| **B-2 Spirit** ⭐ премиум | 1500 | NATO | `superbwarfare:b2_spirit` |

> ⭐ премиум — доступна только игрокам с Legend Pack ($115) или с WC (удвоенная стоимость для нон-донаторов)

---

## 4. VEHICLE CREDITS (VC) — КОМАНДНАЯ ВАЛЮТА

- Генерируется каждые 60 секунд за захваченные точки
- **Small** CP: 50 VC/мин
- **Medium** CP: 100 VC/мин
- **Major** CP: 200 VC/мин
- Обнуляется каждый матч
- Тратится только на спавн техники

---

## 5. ЦЕНЫ НА ТЕХНИКУ (BC — СОЛО)

> Цель: соло-игрок копит на танк **за 12-15 минут** активной игры.
> Цены указаны для **Normal** (×1.0). Easy ×0.7, Hard ×1.3.
> Дополнительно требуется VC для спавна (командная валюта).

### 5.1 Транспорт / T1 (2-3 мин соло)

| Техника | BC | VC | Примечание |
|---|---|---|---|
| Пикап M2 / DShK | 50 | 20 | |
| HMMWV / UAZ-469 | 40 | 15 | |

### 5.2 БМП / БТР / T2 (8-10 мин соло)

| Техника | BC | VC | Примечание |
|---|---|---|---|
| BMP-2 | 200 | 80 | |
| LAV-25 | 200 | 80 | |
| BTR-82A | 180 | 70 | |
| Stryker ICV | 180 | 70 | |

### 5.3 IFV / Stryker / T3 (12-15 мин соло)

| Техника | BC | VC | Примечание |
|---|---|---|---|
| M2 Bradley | 300 | 120 | |
| BMP-3 | 300 | 120 | |
| M1128 Stryker MGS | 280 | 110 | |
| LAV-AD (ПВО) | 350 | 130 | |
| Pantsir-S1 (ПВО) | 350 | 130 | |

### 5.4 Танки / T4 (15-18 мин соло)

| Техника | BC | VC | Примечание |
|---|---|---|---|
| M1A2 Abrams | 450 | 180 | |
| T-90MS | 450 | 180 | |
| Type 99A | 450 | 180 | |
| Leopard 2A7 | 500 | 200 | |
| T-80BVM | 450 | 180 | |
| Challenger 2 | 500 | 200 | |
| M1A2 SEPv3 | 500 | 200 | |
| T-14 Armata | 500 | 200 | |
| **Премиум** (Mk42, Mle, YX-100) | 450 | 180 | |

### 5.5 Вертолёты / T5 (20-25 мин соло)

| Техника | BC | VC | Примечание |
|---|---|---|---|
| AH-64 Apache | 600 | 250 | |
| Ka-52 Alligator | 600 | 250 | |
| Mi-28 Havoc | 550 | 230 | |
| AH-6 Little Bird | 400 | 180 | |
| RAH-66 Comanche | 650 | 280 | |
| Транспортный вертолёт (UH-60/Mi-8) | 250 | 120 | |

### 5.6 Самолёты / T6 (25-40 мин соло)

| Техника | BC | VC | Примечание |
|---|---|---|---|
| F-16 Fighting Falcon | 700 | 300 | |
| Su-27 Flanker | 700 | 300 | |
| MiG-29 Fulcrum | 650 | 280 | |
| F/A-18 Hornet | 750 | 320 | |
| A-10 Warthog | 1000 | 400 | Тяжёлый |
| Su-25 Frogfoot | 800 | 350 | Тяжёлый |
| Eurofighter Typhoon | 800 | 350 | |
| Su-57 Felon | 900 | 380 | |
| **Премиум** (F-35B, AC-130, B-52) | 1000-1200 | 400-500 | |

### 5.7 Сводка по времени

| Категория | BC (Normal) | Время соло | Реалистичность для 1 игрока |
|---|---|---|---|
| Транспорт | 40-50 | 2-3 мин | ✅ Сразу |
| БМП/БТР | 180-200 | 8-10 мин | ✅ Легко |
| IFV/ПВО | 280-350 | 12-15 мин | ✅ Нормально |
| Танк | 450-500 | 15-18 мин | ✅ Вполне |
| Вертолёт | 400-650 | 20-25 мин | ✅ За полматча |
| Самолёт | 650-1200 | 25-40 мин | ⚡ За матч |

---

## 6. ЦЕНЫ НА БОЕПРИПАСЫ

| Аммуниция | Базовая (Normal) | ID |
|---|---|---|
| **ПТ-ракеты** | | |
| RPG rocket | 60 | `tacz:rpg_rocket` |
| RPG TBG | 100 | `superbwarfare:tbg_rocket` |
| Javelin missile | 300 | `superbwarfare:javelin_missile` |
| Igla / Stinger | 250 | `superbwarfare:medium_anti_air_missile` |
| ATGM (TOW/9M120) | 200 | `superbwarfare:medium_anti_ground_missile` |
| AGM-65 / Kh-39 | 400 | `superbwarfare:large_anti_ground_missile` |
| Hellfire | 250 | `superbwarfare:hellfire_missile` |
| **Танковые снаряды** | | |
| APFSDS | 100 | `superbwarfare:apfsds_shell` |
| HE | 60 | `superbwarfare:he_shell` |
| HEAT | 80 | `superbwarfare:heat_shell` |
| **Корабельные** | | |
| AP/HE/CM/GS | 120 | `superbwarfare:large_shell_ap` / `large_shell_he` / `large_shell_cm` / `large_shell_gs` |
| **Авиационные** | | |
| HYDRA / S-8 rocket | 150 | `superbwarfare:small_rocket` |
| AIM-9 | 180 | `superbwarfare:aim9_missile` |
| AIM-120 | 400 | `superbwarfare:aim120_missile` |
| AGM-114 | 350 | `superbwarfare:agm114_missile` |
| Авиабомба | 300 | `superbwarfare:medium_aerial_bomb` |
| Зажигательный (WP) | 200 | `superbwarfare:large_shell_wp` |
| Картечь | 80 | `superbwarfare:large_shell_gs` |

---

## 7. UPKEEP ТЕХНИКИ (Normal)

| Категория | Интервал | BC/тик | BC/мин | За 1 час |
|---|---|---|---|---|
| Транспорт (пикап, катер, грузовик) | 120 с | 5 | 2.5 | 150 |
| БМП / БТР / ПВО | 90 с | 15 | 10 | 600 |
| Танк | 60 с | 30 | 30 | 1800 |
| Артиллерия (PLZ, Type63, TOS) | 90 с | 20 | 13.3 | 800 |
| Вертолёт | 60 с | 35 | 35 | 2100 |
| Самолёт | 60 с | 40 | 40 | 2400 |
| FOB | — | 0 | 0 | 0 |

**Механика:** каждую секунду проверяется баланс владельца. Если BC < стоимость — пропуск. На Normal 3 пропуска = деспавн. На Hard 2 пропуска = деспавн. На Easy апкип выключен.

---

## 8. ДОНЕЙШЕН — LEGEND PACK

| Параметр | Значение |
|---|---|
| Цена | $115 / 10350₽ |
| Разблокировка | ВСЯ техника (все тиры) |
| Разблокировка | ВСЕ киты (включая премиум) |
| Будущий контент | Автоматически включён |
| Баланс | Доступ, НЕ имба-оружие |
| Других донат-паков | НЕТ |

---

## 9. КЛАССЫ И КИТЫ — ПОЛНЫЕ ТАБЛИЦЫ

### 9.1 Фракция NATO

#### ASSAULT

**Rifleman** 🟢 — ранг 0, 0 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M4A1 / SCAR-L / G36K | `tacz:m4a1` / `tacz:scar_l` / `tacz:g36k` | `tacz:556x45` ×240 |
| Secondary | M1911 / Glock 17 | `tacz:m1911` / `tacz:glock_17` | `tacz:45acp` / `tacz:9mm` ×34 |
| Grenade | M67 | `warbornexplosives:m67` ×2 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Breacher** — ранг 1, 2 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | MP5 / Vector | `tacz:hk_mp5a5` / `tacz:vector45` | `tacz:9mm` / `tacz:45acp` ×180 |
| Secondary | Deagle | `tacz:deagle` | `tacz:50ae` ×21 |
| Special | C4 | `superbwarfare:c4_bomb` ×2 | |
| Grenade | M67 | `warbornexplosives:m67` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Shock Trooper** ⭐ — ранг 4, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | HK416A5 / SCAR-L | `tacz:hk416a5` / `tacz:scar_l` | `tacz:556x45` ×240 |
| Secondary | Glock 17 | `tacz:glock_17` | `tacz:9mm` ×34 |
| Underbarrel | M79 + 40mm ×2 | `superbwarfare:m_79` | `superbwarfare:grenade_40mm` ×2 |
| Grenade | M67 | `warbornexplosives:m67` ×1 | |
| Medical | Bandaid Box + Stimpack | `marbledsfirstaid:bandaid_box` ×1 + `marbledsfirstaid:stimpack` ×1 | |

---

#### MEDIC (лимит: 2 на отряд)

**Combat Medic** 🟢 — ранг 0, 0 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M4A1 | `tacz:m4a1` | `tacz:556x45` ×240 |
| Secondary | M1911 | `tacz:m1911` | `tacz:45acp` ×34 |
| Grenade | M18 Smoke | `warbornexplosives:m18_smoke` ×2 | |
| Medical | Medkit ×3 | `marbledsfirstaid:medkit` ×3 | |

**Field Surgeon** ⭐ — ранг 2, 0 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | MP5 | `tacz:hk_mp5a5` | `tacz:9mm` ×180 |
| Secondary | M1911 | `tacz:m1911` | `tacz:45acp` ×34 |
| Grenade | M18 Smoke | `warbornexplosives:m18_smoke` ×2 | |
| Medical | Medkit ×3 + Morphine ×1 | `marbledsfirstaid:medkit` ×3 + `marbledsfirstaid:morphine` ×1 | |

---

#### ENGINEER (лимит: 4 на команду)

**Combat Engineer** 🟢 — ранг 1, 3 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M4A1 | `tacz:m4a1` | `tacz:556x45` ×240 |
| Secondary | Glock 17 | `tacz:glock_17` | `tacz:9mm` ×34 |
| Special | Repair Tool + Claymore ×2 | `superbwarfare:repair_tool` + `superbwarfare:claymore_mine` ×2 | |
| Grenade | M67 | `warbornexplosives:m67` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Anti-Tank** — ранг 2, 3 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | SCAR-L | `tacz:scar_l` | `tacz:556x45` ×240 |
| Secondary | Glock 17 | `tacz:glock_17` | `tacz:9mm` ×34 |
| Special | RPG + 3 rockets + C4 | `superbwarfare:rpg` + `superbwarfare:rpg_rocket_standard` ×3 + `superbwarfare:c4_bomb` ×1 | |
| Grenade | M67 | `warbornexplosives:m67` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Sapper** ⭐ — ранг 4, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | G36K | `tacz:g36k` | `tacz:556x45` ×240 |
| Secondary | Deagle | `tacz:deagle` | `tacz:50ae` ×21 |
| Special | Repair + C4 ×2 + Claymore ×2 | `superbwarfare:repair_tool` + `superbwarfare:c4_bomb` ×2 + `superbwarfare:claymore_mine` ×2 | |
| Grenade | M67 | `warbornexplosives:m67` ×1 | |
| Medical | Bandaid Box + Stimpack | `marbledsfirstaid:bandaid_box` ×1 + `marbledsfirstaid:stimpack` ×1 | |

---

#### SUPPORT

**Machinegunner** — ранг 2, 3 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M249 | `tacz:m249` | `tacz:556x45` ×480 |
| Secondary | M1911 | `tacz:m1911` | `tacz:45acp` ×34 |
| Medical | Bandages ×2 | `marbledsfirstaid:bandages` ×2 | |

**Heavy Gunner** ⭐ — ранг 4, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M249 | `tacz:m249` | `tacz:556x45` ×480 |
| Secondary | Glock 17 | `tacz:glock_17` | `tacz:9mm` ×34 |
| Special | M18 Smoke | `superbwarfare:m18_smoke_grenade` ×2 | |
| Medical | Bandages + Stimpack | `marbledsfirstaid:bandages` ×2 + `marbledsfirstaid:stimpack` ×1 | |

---

#### SNIPER (лимит: 2 на команду)

**Scout Sniper** — ранг 2, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | AWP (bolt) | `tacz:ai_awp` | `tacz:338` ×40 |
| Secondary | MK14 (DMR) | `tacz:mk14` | `tacz:308` ×80 |
| Tertiary | Glock 17 | `tacz:glock_17` | `tacz:9mm` ×34 |
| Grenade | M18 Smoke | `warbornexplosives:m18_smoke` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Heavy Sniper** — ранг 4, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M95 (.50 cal) | `tacz:m95` | `tacz:50bmg` ×30 |
| Secondary | M1911 | `tacz:m1911` | `tacz:45acp` ×34 |
| Grenade | M67 | `warbornexplosives:m67` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Ghost** ⭐ — ранг 5, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M700 (bolt) | `tacz:m700` | `tacz:30_06` ×40 |
| Secondary | MK14 (DMR) | `tacz:mk14` | `tacz:308` ×80 |
| Tertiary | Deagle | `tacz:deagle` | `tacz:50ae` ×21 |
| Special | M18 Smoke | `superbwarfare:m18_smoke_grenade` ×2 | |
| Medical | Bandaid Box + Stimpack | `marbledsfirstaid:bandaid_box` ×1 + `marbledsfirstaid:stimpack` ×1 | |

---

#### HEAVY AT (лимит: 1 на отряд, 2 на команду)

**RPG Gunner** 🟢 — ранг 1, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | SCAR-L | `tacz:scar_l` | `tacz:556x45` ×180 |
| Secondary | M1911 | `tacz:m1911` | `tacz:45acp` ×34 |
| Special | RPG-7 + 3 rockets | `tacz:rpg7` + `tacz:rpg_rocket` ×3 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Javelin Operator** — ранг 3, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | P90 | `tacz:p90` | `tacz:57x28` ×200 |
| Secondary | Glock 17 | `tacz:glock_17` | `tacz:9mm` ×34 |
| Special | Javelin + 2 missiles | `superbwarfare:javelin` + `superbwarfare:javelin_missile` ×2 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

---

#### HEAVY ARMOR

**Juggernaut** — ранг 2, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M249 | `tacz:m249` | `tacz:556x45` ×240 |
| Secondary | AA-12 | `tacz:aa12` | `tacz:12g` ×40 |
| Special | M18 Smoke | `superbwarfare:m18_smoke_grenade` ×1 | |
| Medical | Bandages ×2 | `marbledsfirstaid:bandages` ×2 | |

**Bulwark** ⭐ — ранг 5, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M249 | `tacz:m249` | `tacz:556x45` ×240 |
| Secondary | M870 | `tacz:m870` | `tacz:12g` ×40 |
| Special | C4 + Smoke | `superbwarfare:c4_bomb` ×1 + `superbwarfare:m18_smoke_grenade` ×2 | |
| Medical | Bandages + Stimpack | `marbledsfirstaid:bandages` ×2 + `marbledsfirstaid:stimpack` ×1 | |

---

### 9.2 Фракция Russia

#### ASSAULT

**Shturmovik** 🟢 — ранг 0, 0 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | AK-47 / Type 81 | `tacz:ak47` / `tacz:type_81` | `tacz:762x39` ×240 |
| Secondary | MP-443 / Glock 17 | `superbwarfare:mp_443` / `tacz:glock_17` | `tacz:9mm` ×34 |
| Grenade | F-1 / RGD-5 | `warbornexplosives:f_1` / `warbornexplosives:rgd_5` ×2 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Storm Group** — ранг 1, 2 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | MP5 / P90 | `tacz:hk_mp5a5` / `tacz:p90` | `tacz:9mm` / `tacz:57x28` ×180 |
| Secondary | CZ75 | `tacz:cz75` | `tacz:9mm` ×34 |
| Special | C4 | `superbwarfare:c4_bomb` ×2 | |
| Grenade | F-1 | `warbornexplosives:f_1` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Guardsman** ⭐ — ранг 4, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | QBZ-191 / AK-47 | `tacz:qbz_191` / `tacz:ak47` | `tacz:58x42` / `tacz:762x39` ×240 |
| Secondary | MP-443 | `superbwarfare:mp_443` | `tacz:9mm` ×34 |
| Special | RPG + C4 | `superbwarfare:rpg` + `superbwarfare:c4_bomb` ×1 | |
| Grenade | RGN | `warbornexplosives:rgn` ×1 | |
| Medical | Bandaid Box + Stimpack | `marbledsfirstaid:bandaid_box` ×1 + `marbledsfirstaid:stimpack` ×1 | |

---

#### MEDIC (лимит: 2 на отряд)

**Polevoy Medik** 🟢 — ранг 0, 0 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | AK-47 | `tacz:ak47` | `tacz:762x39` ×240 |
| Secondary | MP-443 | `superbwarfare:mp_443` | `tacz:9mm` ×34 |
| Grenade | RDG-2 Smoke | `warbornexplosives:rdg_2` ×2 | |
| Medical | Medkit ×3 | `marbledsfirstaid:medkit` ×3 | |

**Sanitar** ⭐ — ранг 2, 0 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | MP5 | `tacz:hk_mp5a5` | `tacz:9mm` ×180 |
| Secondary | CZ75 | `tacz:cz75` | `tacz:9mm` ×34 |
| Grenade | RDG-2 Smoke | `warbornexplosives:rdg_2` ×2 | |
| Medical | Medkit ×3 + Morphine ×1 | `marbledsfirstaid:medkit` ×3 + `marbledsfirstaid:morphine` ×1 | |

---

#### ENGINEER (лимит: 4 на команду)

**Combat Saper** 🟢 — ранг 1, 3 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | AK-47 | `tacz:ak47` | `tacz:762x39` ×240 |
| Secondary | MP-443 | `superbwarfare:mp_443` | `tacz:9mm` ×34 |
| Special | Repair Tool + PMN-1 mine ×2 | `superbwarfare:repair_tool` + `warbornexplosives:pmn_1` ×2 | |
| Grenade | RGD-5 | `warbornexplosives:rgd_5` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Anti-Tank Saper** — ранг 2, 3 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | Type 81 | `tacz:type_81` | `tacz:762x39` ×240 |
| Secondary | MP-443 | `superbwarfare:mp_443` | `tacz:9mm` ×34 |
| Special | RPG + 3 rockets + C4 | `superbwarfare:rpg` + `superbwarfare:rpg_rocket_standard` ×3 + `superbwarfare:c4_bomb` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Heavy Saper** ⭐ — ранг 4, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | QBZ-191 | `tacz:qbz_191` | `tacz:58x42` ×240 |
| Secondary | Deagle | `tacz:deagle` | `tacz:50ae` ×21 |
| Special | Repair + C4 ×2 + Claymore ×2 | `superbwarfare:repair_tool` + `superbwarfare:c4_bomb` ×2 + `superbwarfare:claymore_mine` ×2 | |
| Medical | Bandaid Box + Stimpack | `marbledsfirstaid:bandaid_box` ×1 + `marbledsfirstaid:stimpack` ×1 | |

---

#### SUPPORT

**Pulemetchik** — ранг 2, 3 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | RPK | `tacz:rpk` | `tacz:762x39` ×480 |
| Secondary | CZ75 | `tacz:cz75` | `tacz:9mm` ×34 |
| Medical | Bandages ×2 | `marbledsfirstaid:bandages` ×2 | |

**Heavy Gunner** ⭐ — ранг 4, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | RPK | `tacz:rpk` | `tacz:762x39` ×480 |
| Secondary | MP-443 | `superbwarfare:mp_443` | `tacz:9mm` ×34 |
| Special | M18 Smoke | `superbwarfare:m18_smoke_grenade` ×2 | |
| Medical | Bandages + Stimpack | `marbledsfirstaid:bandages` ×2 + `marbledsfirstaid:stimpack` ×1 | |

---

#### SNIPER (лимит: 2 на команду)

**Razvedchik Sniper** — ранг 2, 4 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | Kar98 (bolt) | `tacz:kar98` | `tacz:792x57` ×40 |
| Secondary | SVD (DMR) / SKS | `tacz:sks_tactical` | `tacz:762x39` ×80 |
| Tertiary | CZ75 | `tacz:cz75` | `tacz:9mm` ×34 |
| Grenade | RDG-2 Smoke | `warbornexplosives:rdg_2` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Anti-Material Sniper** — ранг 4, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M107 (.50 cal) | `tacz:m107` | `tacz:50bmg` ×30 |
| Secondary | CZ75 | `tacz:cz75` | `tacz:9mm` ×34 |
| Grenade | F-1 | `warbornexplosives:f_1` ×1 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Spetsnaz Sniper** ⭐ — ранг 5, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | M700 (bolt) | `tacz:m700` | `tacz:30_06` ×40 |
| Secondary | SVD / SKS | `tacz:sks_tactical` | `tacz:762x39` ×80 |
| Tertiary | Deagle | `tacz:deagle` | `tacz:50ae` ×21 |
| Special | RDG-2 Smoke | `warbornexplosives:rdg_2` ×2 | |
| Medical | Bandaid Box + Stimpack | `marbledsfirstaid:bandaid_box` ×1 + `marbledsfirstaid:stimpack` ×1 | |

---

#### HEAVY AT (лимит: 1 на отряд, 2 на команду)

**RPG Gunner** 🟢 — ранг 1, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | AK-47 | `tacz:ak47` | `tacz:762x39` ×180 |
| Secondary | CZ75 | `tacz:cz75` | `tacz:9mm` ×34 |
| Special | RPG-7 + 3 rockets | `tacz:rpg7` + `tacz:rpg_rocket` ×3 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

**Igla Operator** — ранг 3, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | P90 | `tacz:p90` | `tacz:57x28` ×200 |
| Secondary | MP-443 | `superbwarfare:mp_443` | `tacz:9mm` ×34 |
| Special | Igla 9K38 + 2 SAM | `superbwarfare:igla_9k38` + `superbwarfare:medium_anti_air_missile` ×2 | |
| Medical | Bandaid Box | `marbledsfirstaid:bandaid_box` ×1 | |

---

#### HEAVY ARMOR

**Juggernaut** — ранг 2, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | RPK | `tacz:rpk` | `tacz:762x39` ×240 |
| Secondary | AA-12 | `tacz:aa12` | `tacz:12g` ×40 |
| Special | RDG-2 Smoke | `warbornexplosives:rdg_2` ×1 | |
| Medical | Bandages ×2 | `marbledsfirstaid:bandages` ×2 | |

**Bulwark** ⭐ — ранг 5, 5 BC

| Слот | Оружие | ID | Патроны |
|---|---|---|---|
| Primary | RPK | `tacz:rpk` | `tacz:762x39` ×240 |
| Secondary | M870 | `tacz:m870` | `tacz:12g` ×40 |
| Special | C4 + Smoke | `superbwarfare:c4_bomb` ×1 + `superbwarfare:m18_smoke_grenade` ×2 | |
| Medical | Bandages + Stimpack | `marbledsfirstaid:bandages` ×2 + `marbledsfirstaid:stimpack` ×1 | |

---

## 10. СВОДКА КЛАССОВ

| Класс | Китов | Премиум | Бесплатный с ранга 0 |
|---|---|---|---|
| Assault (NATO) | 3 | ✅ Shock Trooper | ✅ Rifleman 🟢 |
| Assault (Russia) | 3 | ✅ Guardsman | ✅ Shturmovik 🟢 |
| Medic (NATO) | 2 | ✅ Field Surgeon | ✅ Combat Medic 🟢 |
| Medic (Russia) | 2 | ✅ Sanitar | ✅ Polevoy Medik 🟢 |
| Engineer (NATO) | 3 | ✅ Sapper | ✅ Combat Engineer 🟢 |
| Engineer (Russia) | 3 | ✅ Heavy Saper | ✅ Combat Saper 🟢 |
| Support (NATO) | 2 | ✅ Heavy Gunner | ❌ (ранг 2) |
| Support (Russia) | 2 | ✅ Heavy Gunner | ❌ (ранг 2) |
| Sniper (NATO) | 3 | ✅ Ghost | ❌ (ранг 2) |
| Sniper (Russia) | 3 | ✅ Spetsnaz Sniper | ❌ (ранг 2) |
| Heavy AT (NATO) | 2 | ❌ | ❌ (ранг 1) |
| Heavy AT (Russia) | 2 | ❌ | ❌ (ранг 1) |
| Heavy Armor (NATO) | 2 | ✅ Bulwark | ❌ (ранг 2) |
| Heavy Armor (Russia) | 2 | ✅ Bulwark | ❌ (ранг 2) |

---

## 11. ПОЛНЫЙ СПИСОК ID ПАТРОНОВ

| ID | Калибр | Применение |
|---|---|---|
| `tacz:556x45` | 5.56×45 NATO | M4A1, HK416, SCAR-L, G36K, AUG, QBZ-191, M249 |
| `tacz:762x39` | 7.62×39 Soviet | AK-47, Type 81, SKS, RPK |
| `tacz:58x42` | 5.8×42 Chinese | QBZ-95, QBZ-191 |
| `tacz:308` | 7.62×51 (.308) | MK14, SCAR-H |
| `tacz:338` | .338 Lapua | AWP |
| `tacz:50bmg` | .50 BMG | M107, M95 |
| `tacz:30_06` | .30-06 | M700 |
| `tacz:792x57` | 7.92×57 Mauser | Kar98 |
| `tacz:9mm` | 9×19 Parabellum | MP5, Glock 17, MP-443, CZ75, Uzi |
| `tacz:45acp` | .45 ACP | M1911, Vector |
| `tacz:57x28` | 5.7×28 | P90 |
| `tacz:12g` | 12 Gauge | AA-12, M870 |
| `tacz:50ae` | .50 AE | Deagle |
| `tacz:545x39` | 5.45×39 | AK-74 (запасной) |
| `tacz:762x54` | 7.62×54R | PKM, Dragunov |
| `tacz:46x30` | 4.6×30 | MP7 |
| `tacz:rpg_rocket` | RPG rocket | RPG-7 |
| `tacz:40mm` | 40mm grenade | M79, GP-25 |

---

## 12. Superb Warfare — РАКЕТЫ И СПЕЦСНАРЯДЫ

| ID | Тип |
|---|---|
| `superbwarfare:rpg_rocket_standard` | РПГ (обычная) |
| `superbwarfare:tbg_rocket` | ТБГ (термобарик) |
| `superbwarfare:javelin_missile` | Javelin |
| `superbwarfare:medium_anti_air_missile` | ПЗРК (Igla / Stinger) |
| `superbwarfare:medium_anti_ground_missile` | ПТУР (TOW / 9M120) |
| `superbwarfare:large_anti_ground_missile` | AGM-65 / Kh-39 |
| `superbwarfare:apfsds_shell` | Бронебойный (танк) |
| `superbwarfare:he_shell` | ОФ (танк) |
| `superbwarfare:heat_shell` | Кумулятивный (танк) |
| `superbwarfare:large_shell_ap` | AP (большой калибр, корабль) |
| `superbwarfare:large_shell_he` | HE (большой) |
| `superbwarfare:large_shell_gs` | Картечь (большой) |
| `superbwarfare:large_shell_cm` | КУМ (большой) |
| `superbwarfare:large_shell_wp` | Зажигательный |
| `superbwarfare:small_shell_ap` | AP (малый, БМП) |
| `superbwarfare:small_shell_he` | HE (малый) |
| `superbwarfare:small_shell_gs` | GS (малый) |
| `superbwarfare:small_shell_aa` | Зенитный (малый) |
| `superbwarfare:small_rocket` | HYDRA / S-8 |
| `superbwarfare:hellfire_missile` | Hellfire |
| `superbwarfare:aim9_missile` | AIM-9 Sidewinder |
| `superbwarfare:aim120_missile` | AIM-120 AMRAAM |
| `superbwarfare:agm114_missile` | AGM-114 Hellfire |
| `superbwarfare:hydra_rocket` | HYDRA 70mm |
| `superbwarfare:medium_aerial_bomb` | Mk82 / SC250 |
| `superbwarfare:mortar_shell` | Миномётный снаряд |
| `superbwarfare:grenade_40mm` | 40mm граната (M79) |
| `superbwarfare:c4_bomb` | C4 |
| `superbwarfare:claymore_mine` | Claymore |
| `superbwarfare:blu_43_mine` | BLU-43 |
| `superbwarfare:m18_smoke_grenade` | M18 Smoke |
| `superbwarfare:repair_tool` | Ремонтный инструмент |
| `superbwarfare:taser` | Электрошокер |

---

## 13. HEALING / МЕДИЦИНА

| Предмет | ID | Кому | Действие |
|---|---|---|---|
| Medkit | `marbledsfirstaid:medkit` | Медик | Лечит других, ×3 использования |
| Bandaid Box | `marbledsfirstaid:bandaid_box` | Все | Самолечение, ×4 использования |
| Bandages | `marbledsfirstaid:bandages` | Support, Heavy Armor | Самолечение, ×2 использования |
| Morphine | `marbledsfirstaid:morphine` | Field Surgeon, Sanitar | Воскрешение, 1 использование |
| Stimpack | `marbledsfirstaid:stimpack` | Premium киты | Реген HP + ускорение |

### Награды за лечение

| Действие | XP | BC |
|---|---|---|
| Лечение 2 HP | 2 | 1 |
| Воскрешение (revive) | 25 | 5 |
| Medic Assist (++ heal) | 5 | 0 |
| Medic Streak (5 лечений без смерти) | 30 | 3 |

---

## 14. ПРОГРЕССИЯ (Ranks + XP)

### 14.1 Ранги

| Ранг | Min XP | Название | Доступ к технике (тир) | Лидерство |
|---|---|---|---|---|
| 0 | 0 | Recruit | T1 (транспорт) | Нет |
| 1 | 500 | Private | T2 (IFV) | Нет |
| 2 | 1500 | Corporal | T3 (APC/Stryker) | Нет |
| 3 | 3500 | Sergeant | T4 (MBT) | Отряд |
| 4 | 6500 | Staff Sergeant | T5 (Heli/SPAA) | Отряд |
| 5 | 11000 | Lieutenant | T6 (Jet/Elite) | Ком. взвода |
| 6 | 18000 | Captain | — | Ком. взвода |
| 7 | 28000 | Major | — | Ком. роты |
| 8 | 42000 | Colonel | — | Ком. роты |

### 14.2 XP за действия

| Действие | XP | За матч (средний, Normal) |
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
| **XP за матч (средний)** | | **~1255** |

### 14.3 Время до рангов

| Переход | Матчей (Normal) | Часов |
|---|---|---|
| Recruit → Private | ~1 | 1 ч |
| → Corporal | ~2 | 2 ч |
| → Sergeant | ~4 | 4 ч |
| → Staff Sergeant | ~7 | 7 ч |
| → Lieutenant | ~12 | 12 ч |
| → Captain | ~19 | 19 ч |
| → Major | ~30 | 30 ч |
| → Colonel | ~44 | 44 ч |

---

## 15. FOB

| Параметр | Easy | Normal | Hard |
|---|---|---|---|
| Стоимость установки (BC) | 80 | 100 | 150 |
| Макс. на команду | 3 | 3 | 2 |
| Кулдаун на игрока | 20 с | 30 с | 45 с |
| Upkeep | Нет | Нет | Нет |
| При уничтожении | Деспавн | Деспавн | Деспавн |

---

## 16. МАТЧ-ФЛОУ (Normal, 1 час)

```
0-2 мин      → Фармим BC, транспорт, захваты первых точек
3-5 мин      → Появляются BMP/BTR, накоплены VC
5-10 мин     → Танки в бою, вертолёты
10-20 мин    → Полный спавн, пик интенсивности
20-40 мин    → Апкип ест BC, техника деспавнится у бедных
40-50 мин    → Финальный пуш, давление
50-60 мин    → Овертайм, финал
End          → BC сброс, WC + XP начисление, MVP
```

---

## 17. МЕТОД ДАВЛЕНИЯ (Pressure System)

- **Ground pressure** — много наземной техники → открывается Heavy AT класс временно
- **Air pressure** — много вертолётов/самолётов → открывается Igla/Stinger (ПЗРК) класс
- **Siege pressure** — много арты/дальнего боя → открывается Sniper класс
- Pressure сбрасывается, когда угроза уходит

---

## 18. ПРИНЦИПЫ БАЛАНСА

1. **BC — лимитированный ресурс.** За матч ~722 BC. Танк (1000 BC) — инвестиция на 1-2 матча. Нельзя спавнить танк каждые 2 минуты.
2. **Upkeep — налог на бедность.** Если не воевать за точки — техника деспавнится. Стимулирует активную игру.
3. **WC — за лояльность + анлок.** WC открывает доступ к технике и классам, не тратится на бустеры. Easy — слишком мало WC для прогресса. Hard — самый эффективный фарм.
4. **VC — командная координация.** Чем больше точек — тем больше техники у команды.
5. **Kill Streak — награда за скилл.** Небольшой бонус, не ломает экономику.
6. **MVP — признание.** Символический бонус для топ-игроков.
7. **Оружие НЕ зависит от экономики.** Все пушки доступны через классы. Экономика регулирует ЧАСТОТУ использования элитных классов и техники, а не их силу.
8. **Donation — только Legend Pack.** $115 даёт ВСЁ навсегда. Никакой микротранзакции. Никакого pay-to-win (доступ, а не имба-пушки).

---

## 19. ИГРОВЫЕ РЕЖИМЫ (Game Modes)

Каждый режим использует 3 сложности (Easy/Normal/Hard) и нашу экономику.

| Режим | ID | Суть | Длительность (E/N/H) | Тикеты (E/N/H) | Доход BC |
|---|---|---|---|---|---|
| **Захват** (Conquest) | `conquest` | Классика: захват точек, тикеты капают | 45/60/150 мин | 600/1000/2000 | средний |
| **Штурм** (Rush) | `rush` | Атака vs Оборона: взрыв целей, линейно | 30/45/90 мин | 300/500/1000 | высокий (атакующим) |
| **Прорыв** (Breakthrough) | `breakthrough` | Сектора, фронт движется, нет возврата | 40/50/120 мин | 500/800/1500 | средний |
| **Фронт** (Frontline) | `frontline` | Одна линия фронта, взад-вперёд | 35/45/100 мин | 400/700/1200 | высокий (ближний бой) |
| **Господство** (Domination) | `domination` | 3 точки, быстрый кап, хаос | 20/30/60 мин | 200/400/800 | очень высокий |
| **Танковый бой** (Tank Superiority) | `tank_superiority` | Только наземная техника, захват центра | 25/35/75 мин | 300/500/1000 | высокий (за уничтожение) |
| **Точка захвата** (CTF) | `ctf` | Флаг, база, скорость | 30/40/80 мин | — | средний |
| **Уничтожение** (Destruction) | `destruction` | Разрушить здания противника | 40/55/120 мин | 400/700/1400 | средний |
| **Превосходство** (Air Superiority) | `air_superiority` | Только авиация, сбивай всё | 20/30/60 мин | 200/400/800 | авиация ×1.5 |
| **Командный бой** (TDM) | `tdm` | Просто убивай врагов, без точек | 15/20/40 мин | 100/150/300 | высокий (за убийство ×1.5) |

---

## 20. ТЕАТРЫ ВОЕННЫХ ДЕЙСТВИЙ (Theaters)

Каждый театр — набор из 3-5 карт со своей атмосферой, погодой и доступной техникой.

### Европа (NATO vs Russia)

| Театр | Карты | Погода | Тиры техники | Описание |
|---|---|---|---|---|
| **Холодная Война** | fulda_gap, rhine_valley, berlin_wall | clear/fog | T1-T3 | 1980-е. Танки без тепловизоров, старые прицелы |
| **Балканы** | sarajevo, belgrade, mostar | clear/rain | T1-T4 | Городские бои, Югославия 1990-е |
| **Арктика** | murmansk, norway_fjord, ice_lake | snow/blizzard | T1-T5 | Снег, ограниченная видимость |
| **Восточная Европа** | poland_forest, ukraine_steppe, baltic_coast | clear/rain/fog | T1-T6 | Современность. Основной театр |

### Ближний Восток (NATO vs Insurgents / Russia vs Insurgents)

| Театр | Карты | Погода | Тиры техники | Описание |
|---|---|---|---|---|
| **Пустыня** | desert_storm, sandstorm_city, oasis_run | clear/sandstorm | T1-T5 | Песок, жара, песчаные бури |
| **Горы** | mountain_pass, cave_complex, high_peak | clear/fog | T1-T4 | Снайперский рай |
| **Нефтяные поля** | oil_rig, pipeline_valley, refinery | clear/fire | T1-T5 | Нефтяные вышки, взрывоопасно |

### Азия (China vs NATO / Russia vs China)

| Театр | Карты | Погода | Тиры техники | Описание |
|---|---|---|---|---|
| **Джунгли** | vietnam_rice, laos_trail, cambodia_temple | rain/fog | T1-T3 | Густая листва, засады |
| **Тихий Океан** | island_landing, beach_assault, coral_atoll | clear/storm | T2-T6 | Морской десант, авиация |
| **Корея** | dmz_outpost, seoul_subway, north_korea_valley | clear/snow | T1-T5 | ДМЗ, подземные переходы |

### Африка / Фантастика

| Театр | Карты | Погода | Тиры техники | Описание |
|---|---|---|---|---|
| **Саванна** | savanna_village, river_crossing, acacia_plains | clear/drought | T1-T4 | Открытые пространства |
| **Зона Отчуждения** | stalker_dump, pripyat_hospital, chernobyl_forest | radioactive_fog/clear | T1-T4 | Сталкер-тема, аномалии |
| **Апокалипсис** | wasteland_city, ruined_base, highway | ash/snow | T1-T3 | Постапокалипсис, гражданская техника |

---

## 21. СЦЕНАРИИ (Scenario Definitions)

### 21.1 OP: Нордланд (Arctic Front)
*NATO пытается остановить продвижение России в Арктике*

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `nordland_vanguard` | Conquest | Normal | norway_fjord | NATO vs Russia |
| `nordland_icebreaker` | Rush | Hard | ice_lake | NATO (атака) vs Russia (оборона) |
| `nordland_blizzard` | Breakthrough | Normal | murmansk | Russia (атака) vs NATO (оборона) |
| `nordland_freezer` | Domination | Easy | norway_fjord | NATO vs Russia |

### 21.2 OP: Грозный (Caucasus Front)
*Битва за Кавказ, нефтяные поля и горные перевалы*

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `caucasus_peak` | Conquest | Hard | mountain_pass | Russia vs Insurgents |
| `caucasus_oil` | Frontline | Normal | oil_rig | Russia vs Insurgents |
| `caucasus_ambush` | Rush | Easy | pipeline_valley | Insurgents vs Russia |

### 21.3 OP: Песчаная Буря (Desert Storm)
*NATO вторгается в пустынный регион*

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `sandstorm_assault` | Breakthrough | Hard | sandstorm_city | NATO vs Insurgents |
| `sandstorm_convoys` | Conquest | Normal | desert_storm | NATO vs Insurgents |
| `sandstorm_oasis` | Domination | Easy | oasis_run | NATO vs Insurgents |
| `sandstorm_reaper` | Tank Superiority | Hard | desert_storm | NATO vs Insurgents |

### 21.4 OP: Тигр (Pacific Storm)
*Китай vs NATO за острова в Тихом Океане*

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `pacific_beachhead` | Rush | Hard | beach_assault | NATO (атака) vs China (оборона) |
| `pacific_islands` | Conquest | Normal | island_landing | NATO vs China |
| `pacific_coral` | Air Superiority | Hard | coral_atoll | NATO vs China |

### 21.5 OP: Стальной Кулак (Eastern Europe)
*Полномасштабный конфликт NATO vs Russia*

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `steel_fist_offensive` | Breakthrough | Hard | ukraine_steppe | NATO (атака) vs Russia (оборона) |
| `steel_fist_forest` | Conquest | Normal | poland_forest | NATO vs Russia |
| `steel_fist_coast` | Frontline | Easy | baltic_coast | NATO vs Russia |
| `steel_fist_armor` | Tank Superiority | Normal | ukraine_steppe | NATO vs Russia |
| `steel_fist_heaven` | Air Superiority | Hard | poland_forest | NATO vs Russia |

### 21.6 OP: Красный Рассвет (Cold War)
*Альтернативная история — вторжение СССР в Западную Германию*

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `red_dawn_gap` | Conquest | Normal | fulda_gap | NATO vs USSR |
| `red_dawn_rhine` | Rush | Hard | rhine_valley | USSR (атака) vs NATO (оборона) |
| `red_dawn_wall` | Domination | Easy | berlin_wall | NATO vs USSR |

### 21.7 OP: Зона (S.T.A.L.K.E.R. Crossover)

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `zone_dump` | Conquest | Normal | stalker_dump | NATO vs Russia (+ аномалии) |
| `zone_hospital` | Rush | Hard | pripyat_hospital | NATO (атака) vs Russia (оборона) |
| `zone_forest` | Domination | Easy | chernobyl_forest | NATO vs Russia |

### 21.8 OP: Чёрный Лёд (Arctic Hardcore)

| Сценарий | Режим | Сложность | Карта | Фракции |
|---|---|---|---|---|
| `black_ice_outpost` | Conquest | Hard | murmansk | NATO vs Russia |
| `black_ice_blizzard` | Team Deathmatch | Hard | ice_lake | NATO vs Russia |
| `black_ice_fjord` | Breakthrough | Hard | norway_fjord | Russia vs NATO |

### 21.9 Исторические сценарии

| Сценарий | Режим | Сложность | Описание |
|---|---|---|---|
| `history_d_day` | Rush | Hard | Омаха Бич. США vs Германия |
| `history_operation_barbarossa` | Conquest | Hard | СССР vs Германия. Восточный фронт |
| `history_desert_storm_91` | Breakthrough | Normal | Ирак vs Коалиция. Пустыня |
| `history_falklands` | Domination | Normal | Великобритания vs Аргентина |
| `history_1968_prague` | Rush | Easy | СССР в Чехословакии |

---

## 22. ИВЕНТЫ (Match Events)

### 22.1 Случайные (RNG, происходят во время матча)

| Ивент | ID | Шанс | Эффект | Длительность |
|---|---|---|---|---|
| **Авианалёт** | `event_air_strike` | 10% | Случайная зона — бомбардировка | 1 волна |
| **Сброс припасов** | `event_supply_drop` | 15% | Ящик с BC/боеприпасами на карте | 30 с до падения |
| **Пылевая буря** | `event_sandstorm` | 5% (пустыня) | Видимость до 30 м | 90 с |
| **Метель** | `event_blizzard` | 5% (арктика) | Видимость падает, звук глушится | 90 с |
| **Радиация** | `event_radiation` | 5% (зона) | Зоны становятся радиоактивными | 60 с |
| **Туман** | `event_fog` | 10% | Видимость 50 м | 120 с |
| **Разведка** | `event_recon` | 10% | Враги подсвечены на радаре | 30 с |
| **EMP** | `event_emp` | 3% | Техника обездвижена | 15 с |
| **Кризис БК** | `event_ammo_crisis` | 5% | Цены на боеприпасы ×2 | 60 с |
| **Топливный кризис** | `event_fuel_crisis` | 5% | Апкип ×2 | 60 с |
| **Баунти** | `event_bounty` | 8% | BC за убийство ×2 | 90 с |
| **Двойной XP** | `event_double_xp` | 5% | XP за всё ×2 | 120 с |
| **Секретное оружие** | `event_mystery_weapon` | 2% | Случайный игрок получает мощное оружие | 1 жизнь |
| **Артобстрел** | `event_artillery` | 8% | Очередь арты по точке | 30 с |
| **Рой дронов** | `event_drone_swarm` | 3% | Дроны атакуют зону | 45 с |
| **Контратака** | `event_counterattack` | 10% | AI-отряд атакует точку | пока жив |

### 22.2 Эскалационные (по фазам)

| Фаза | Ивент | Эффект |
|---|---|---|
| SKIRMISH → CONFLICT | `event_reinforcements` | +50 тикетов каждой команде |
| CONFLICT → BATTLE | `event_heavy_armor` | Стоимость танков -20% |
| BATTLE → TOTAL_WAR | `event_total_war` | Вся техника -30%, апкип +50% |

### 22.3 Плановые (по времени матча)

| Время | Ивент | Описание |
|---|---|---|
| 5 мин | `event_early_airdrop` | Первый сброс припасов |
| 25% матча | `event_supply_1` | Сброс припасов |
| 50% матча | `event_double_xp_short` | ×2 XP на 60 с |
| 75% матча | `event_desperate_measures` | Цены на технику -20% |
| Овертайм | `event_last_stand` | Бесконечные тикеты обороняющимся на 30 с |

### 22.4 Командирские (голосование командира, тратят BC команды)

| Ивент | ID | Стоимость BC | Эффект |
|---|---|---|---|
| **Артобстрел** | `commander_artillery` | 200 | Арта по выбранной зоне |
| **Разведка** | `commander_recon` | 100 | Подсвет врагов на 20 с |
| **Сброс снабжения** | `commander_supply` | 150 | Всем в отряде +50 BC |
| **EMP** | `commander_emp` | 300 | Техника врага стоит 10 с |
| **Орбитальный удар** | `commander_orbital` | 500 | Мощный удар по зоне (только Total War) |

---

## 23. СООТВЕТСТВИЕ ТЕХНИКИ ТЕАТРАМ

| Театр | Доступные тиры | Запрещённая техника |
|---|---|---|
| Холодная Война | T1-T3 | T4-T6 (нет современных танков/самолётов) |
| Балканы | T1-T4 | T6 (нет 5-го поколения авиации) |
| Восточная Европа | T1-T6 | — |
| Пустыня | T1-T5 | T6 (5-е поколение редко) |
| Джунгли | T1-T3 | T4-T6 (нет тяжёлой техники) |
| Арктика | T1-T5 | T6 (авиация ограничена погодой) |
| Африка | T1-T4 | T5-T6 (повстанцы, дорогая техника) |
| Зона | T1-T4 | T6 (аномалии сбивают джеты) |
| Танковый бой | T4 (танки) + T3 (БМП) | T1, T2, T5, T6 |

---

## 24. ПРИМЕР СЦЕНАРИЯ (JSON)

```json
{
  "id": "nordland_vanguard",
  "world": "overworld",
  "map": "norway_fjord",
  "mode": "conquest",
  "difficulty": "normal",
  "theater": "arctic",
  "tickets": 1000,
  "maxPlayers": 64,
  "factions": ["nato", "russia"],
  "objectives": [],
  "spawns": [],
  "vehicles": [],
  "weatherPreset": "clear",
  "weatherCycle": ["clear", "snow", "blizzard"],
  "commanderEnabled": true,
  "weight": 10,
  "minPlayers": 0,
  "maxPlayersRotation": 128,
  "cooldownRounds": 0,
  "disabled": false,
  "availableTiers": [1, 2, 3, 4, 5],
  "restrictedVehicles": [],
  "bannedKits": [],
  "eventPool": ["air_strike", "supply_drop", "blizzard", "recon", "bounty"],
  "lore": {
    "operation": "Nordland Vanguard",
    "year": 2025,
    "location": "Norwegian Fjords",
    "story": "NATO forces push north to secure strategic fjord access routes against Russian advance."
  }
}
```

---

## 25. WEATHER2 — ИНТЕГРАЦИЯ В СЦЕНАРИИ

### 25.1 Возможности Weather2

| Система | Тип | Эффект на геймплей |
|---|---|---|
| **Дождь** | Локализованный, биом-зависимый | Видимость ↓, звук ↓ |
| **Гроза** | Облака + молнии | Молнии попадают в технику, пожары |
| **Град** | В составе гроз | Урон игрокам без укрытия (0.5 хп/тик) |
| **Торнадо** (F0-F6) | Смерч | Поднимает технику/игроков, разрушает строения |
| **Ураган** (C0-C5) | Циклон | Огромная зона ветра, постоянный урон |
| **Песчаная буря** | Пустыни | Видимость 10-30м, слепит, наслаивает песок |
| **Снежная буря** | Холодные биомы | Видимость ↓, наслаивает снег |
| **Ветер** | Постоянный | Влияет на полёт пуль, частицы, листву |
| **Облака** | Визуальные 2 слоя | Атмосфера, покрытие 0-100% |
| **Туман** | Через частицы | Видимость снижена |

### 25.2 Команды Weather2 (для ивентов)

| Команда | Описание |
|---|---|
| `/weather2 summon tornado_f1` | Торнадо F1 |
| `/weather2 summon tornado_f5` | Торнадо F5 (апокалипсис) |
| `/weather2 summon sandstorm` | Песчаная буря (если рядом пустыня) |
| `/weather2 kill_all_storms` | Убрать всю погоду |
| `/weather2 wind_speed <0.0-1.5>` | Установить скорость ветра |
| `/weather2 wind_angle <0-359>` | Установить направление ветра |
| `/weather2 wind_event <clear/low/high>` | Событие ветра |

### 25.3 Интеграция с театрами

| Театр | Базовая погода | Штормы | Опасная погода | Ветер |
|---|---|---|---|---|
| **Холодная Война** (Европа) | clear/fog | Грозы (редко) | — | слабый |
| **Балканы** | clear/rain | Грозы (часто) | — | средний |
| **Арктика** | snow/blizzard | Снежные бури | Метель (слепит) | сильный |
| **Восточная Европа** | clear/rain/fog | Грозы | — | средний |
| **Пустыня** | clear/sandstorm | — | Песчаная буря | сильный |
| **Горы** | clear/fog | Грозы | — | сильный |
| **Нефтяные поля** | clear/fire | — | Пожары от молний | средний |
| **Джунгли** | rain/fog | Грозы (очень часто) | — | слабый |
| **Тихий Океан** | clear/storm | Ураганы C0-C4 | Ураган (сносит всё) | очень сильный |
| **Корея** | clear/snow | — | — | средний |
| **Зона** | radioactive_fog | Грозы | Аномалии-погода | переменный |
| **Апокалипсис** | ash/snow | Пылевые бури | Торнадо | сильный |

### 25.4 Weather2 как матчевые ивенты

| Ивент в сценарии | Команда Weather2 | Когда срабатывает |
|---|---|---|
| `event_sandstorm` | `/weather2 summon sandstorm` | Пустынные карты, RNG 5% |
| `event_blizzard` | `/weather2 wind_event high` + снег | Арктические карты, RNG 5% |
| `event_fog` | `/weather2 wind_event low` | Любая карта, RNG 10% |
| `event_tornado` | `/weather2 summon tornado_f1` | Зона/Апокалипсис, RNG 2% |
| `event_hurricane` | `/weather2 summon tornado_f5` | Тихий Океан, RNG 1% (эпик) |
| `event_lightning_storm` | Гроза (естественная) | Джунгли/Балканы, 10% |
| `event_wind_shift` | `/weather2 wind_angle` + speed | Любая карта, меняет направление |

### 25.5 Конфигурация Weather2 для PVP

**Файл: `Weather2/Misc.toml`** — изменить:
```toml
Aesthetic_Only_Mode = false      # Включаем полноценную погоду
overcastMode = false              # Weather2 управляет погодой, не ванилла
lockServerWeatherMode = -1        # Не блокировать ванильную погоду
```

**Файл: `Weather2/Storm.toml`** — агрессивность:
```toml
Player_Storm_Deadly_TimeBetweenInTicks = 72000   # ~1 час между опасными штормами
Server_Storm_Deadly_OddsTo1 = 30                 # 1 к 30 на тик
Storm_NoTornadosOrCyclones = true                 # Торнадо ТОЛЬКО через ивенты/команды
Storm_LightningStartsFires = false                # Молнии не поджигают (PVP баланс)
```

**Файл: `Weather2/Tornado.toml`** — контроль торнадо:
```toml
Storm_Tornado_grabPlayer = false            # Торнадо НЕ поднимает игроков (PVP баланс)
Storm_Tornado_grabBlocks = false             # Торнадо НЕ ломает блоки
Storm_Tornado_grabPlayersOnly = false
Storm_Tornado_aimAtPlayerOnSpawn = false
```

**Файл: `Weather2/Sand.toml`** — песчаные бури:
```toml
Storm_NoSandstorms = false                   # Песчаные бури разрешены
Sandstorm_Sand_Buildup_LoopAmountBase = 200  # Умеренное наслоение песка
Sandstorm_Sand_Block_Max_Height = 1          # Макс 1 слой песка
```

**Файл: `Weather2/Snow.toml`** — снежные бури:
```toml
Storm_NoSnowstorms = false                   # Снежные бури разрешены
Snowstorm_Snow_Block_Max_Height = 2          # Макс 2 слоя снега
```

### 25.6 Поля сценария для Weather2

В `scenario.json` добавляем:

```json
{
  "id": "nordland_vanguard",
  "weatherPreset": "snow",
  "weatherCycle": ["snow", "blizzard", "clear"],
  "weather2": {
    "baseWindSpeed": 0.3,
    "baseWindAngle": 270,
    "allowSandstorms": false,
    "allowSnowstorms": true,
    "allowTornadoes": false,
    "allowHurricanes": false,
    "stormFrequency": "low",
    "lightningStrikes": true,
    "windAffectsProjectiles": true
  },
  "eventPool": ["air_strike", "supply_drop", "blizzard", "recon", "bounty"]
}
```

### 25.7 Что менять в коде

| Файл | Изменение |
|---|---|
| `atmosphere.json` | `weatherIntegrationEnabled: false` → `true` |
| `Weather2/Misc.toml` | `Aesthetic_Only_Mode` → `false` |
| `MapConfig.java` | + `weather2Settings` (windSpeed, windAngle, allowTornadoes и т.д.) |
| `GameManager.java` | При старте матча: применить `weather2Settings` из `MapConfig` |
| `CombatEventHandler.java` | Ивенты погоды как события матча (через `/weather2 summon`)|

---

## 26. БАЛАНС РАБОТЯГИ vs ЗАДРОТЫ

| Метрика | Работяга (Easy, 1-2 ч/день) | Задрот (Hard, 4+ ч/день) | Соотношение |
|---|---|---|---|
| WC/день (с Daily + First Win) | ~75-110 | ~250-310 | 1:3 |
| Танк (350 WC) | 3-5 дней | 1.5 дня | ✅ |
| Джет (1200 WC) | 11-16 дней | 4-5 дней | ✅ |
| ВСЁ открыть без доната | ~2-3 месяца | ~1 месяц | ✅ |
| Legend Pack | $115 = моментально | $115 = моментально | ✅ Равны |
| Бесплатные киты 🟢 | ✅ Rifleman всегда | ✅ Всегда есть класс | ✅ |
| Squad Bonus ×1.1 | ✅ С друзьями быстрее | ✅ С отрядом эффективнее | ✅ |
| Easy поражение 12 WC | ✅ Не больно проиграть | — | ✅ |
