# ✦ Arcane Zenith — NeoForge 1.21.1 Magic Mod

![Build Status](https://github.com/YOUR_USERNAME/ArcaneZenith/actions/workflows/build.yml/badge.svg)

> *"A csillagok tanúi lesznek bukásodnak."*
> — Archon of the Shattered Sky

## ⚡ GitHub Actions Build (Ajánlott — nem kell RAM, nem kell PC)

**1. Töltsd fel a kódot GitHub-ra:**
```bash
git init
git add .
git commit -m "Arcane Zenith mod source"
git remote add origin https://github.com/YOUR_USERNAME/ArcaneZenith.git
git push -u origin main
```

**2. A build automatikusan elindul** — látod a GitHub Actions fülön.

**3. JAR letöltése:** Actions → legutóbbi build → Artifacts → `arcanezenith-mod`

**A GitHub Actions ingyenes** (public repo esetén korlátlan, private repónál 2000 perc/hó).

---

High-fantasy magic overhaul mod. 24 varázslat 5 tierben, 24 egyedi particle típus,
10 GLSL post-processing shader (köztük egy beállítható Dark Fantasy game shader),
3-fázisú boss encounter, 6 AI-vezérelt entitás, procedurális Arcane Spire struktúra,
Shattered Ley-Line Wastes biom, 4 arcane érc, teljes progression rendszer.

Inspiráció: Harry Potter, Marvel Dr. Strange, Elden Ring, God of War, Naruto,
Fate/Zero, Total War: Warhammer.

---

## Tartalom

### 24 Varázslat

#### Tier 0–4: Alap varázslatok (14)

| Varázslat | Szint | Max Seb. | Shader | Saját Particle |
|---|---|---|---|---|
| Arcane Bolt | 0 | 12 + 22 AoE | — | `arcane_spark` |
| Teleport | 1 | 30 (decoy) | — | `shadow_wisp` |
| God's Spear | 3 | 55 + 40 | — | `golden_light` + `rune_ring` |
| Horde of Blades | 2 | 24 / 14 parry | — | `frost_shard` |
| Thunder War | 2 | 22/villám | LIGHTNING_FLASH | `thunder_spark` |
| Gravitational Collapse | 3 | 60 AoE | GRAVITY_LENS | `gravity_dust` |
| Plasma Annihilator | 3 | 8→28 ramp | HEAT_HAZE | `plasma_beam` + `plasma_spiral` |
| Angel's Help | 3 | — (heal) | HOLY_BLOOM | `holy_star` + `heaven_beam` |
| Aether Wings | 2 | 16+sebesség | — | `holy_star` |
| Mage's Hammer | 4 | 120 crush | — | `heaven_beam` + `lava_geyser` |
| Time Silence | 4 | 3× buffered | TIME_STOP | `rune_ring` |
| Judgment of Heaven | 4 | 100+15/spike | HOLY_BLOOM | `holy_star` + `heaven_beam` |
| Cataclysmic Rift | 4 | 30/geyser | HEAT_HAZE | `lava_geyser` |
| Singularity Collapse | 4 | 100 TRUE | GRAVITY_LENS | `gravity_dust` + `singularity_nova` |

#### Tier 5: Legendary varázslatok (10) — Boss-gyilkos szint

| Varázslat | Inspiráció | Max Seb. | Shader | Saját Particle |
|---|---|---|---|---|
| Eldritch Tempest | Dr. Strange | 360 | ARCANE_OVERDRIVE | `eldritch_whip` |
| Avada Curse | Harry Potter | 200 TRUE | BLOOD_CURSE | `death_flash` |
| Starscourge Meteor | Elden Ring | 280 TRUE | STELLAR_FIRE | `meteor_trail` |
| Excalibur Beam | Fate/Zero | 320 TRUE | HOLY_BLOOM + ARCANE_OVERDRIVE | `excalibur_beam` |
| Vortex Essence | Iron's Spells | 260 | VOID_RIFT | `vortex_ring` |
| Blades of Chaos | God of War | 330 | STELLAR_FIRE | `chaos_blade` |
| Lightning Storm | Total War: Warhammer | 14 400\* | ARCANE_OVERDRIVE | `storm_bolt` |
| Chidori | Naruto | 180 TRUE | LIGHTNING_FLASH | `chidori_spark` |
| Glintstone Phalanx | Elden Ring | 1120 | — | `glint_dagger` |
| Crimson Bands | Marvel Cyttorak | 300 | BLOOD_CURSE | `crimson_chain` |

\*Lightning Storm: 12 villám/s × 30s × 40 dmg — egész hadseregek ellen

---

### 10 GLSL Post-Processing Shader

| Shader | Típus | Használja |
|---|---|---|
| **Dark Fantasy** | 🎮 **Game Shader** — mindig aktív | Minden harc |
| Gravity Lens | Spell | Gravitational Collapse, Singularity, Vortex |
| Time Stop | Spell | Time Silence |
| Heat Haze | Spell | Plasma Annihilator, Cataclysmic Rift |
| Lightning Flash | Spell | Thunder War, Chidori |
| Holy Bloom | Spell | Angel's Help, Judgment, Excalibur Beam |
| Void Rift | Spell | Vortex Essence, VoidWalker |
| Blood Curse | Spell | Avada Curse (zöld), Crimson Bands (vörös) |
| Stellar Fire | Spell | Starscourge Meteor, Blades of Chaos |
| Arcane Overdrive | Spell | Eldritch Tempest, Excalibur, Lightning Storm |

#### Dark Fantasy Game Shader
A mod leglátványosabb funkciója. Egyetlen optimalizált pass:
- **Kontraszt boost** — a világ sötétebb és drámaibb
- **Color grading** — 4 stílus: Sötét (teal+amber) / Hideg / Meleg / Semleges
- **Heavy vignette** — ovális fekete keret
- **Film grain** — filmes szemcsézettség
- **Chromatic aberration** — RGB szétválás a széleken

**Beállítás:** Mods → Arcane Zenith → Config
**5 preset:** Ki / Halvány / Normál / Cinematic / MAX
**Élő előnézet** — csúszka húzásánál azonnal látod a változást

---

### Entitások és Boss

| Entitás | Típus | Signature Mechanic |
|---|---|---|
| Arcane Zealot | 👹 Monster | Blink escape, 40% barrier reflect, CC bolt, mana drain |
| Chrono-Weaver | 👹 Monster | Health rewind (5s, 2×), dimensional rift |
| Void-Walker | 👹 Monster | Pozíció-csere (1s telegraph), Void Shroud 50% HP-n |
| Mana-Leech Drake | 🐉 Monster | Mana drain aura 6 blokk, plasma lélegzet (mana charge) |
| Ethereal Familiar | 🤝 Társ | Item vacuum 10 blokk, mana boost, homing bolt |
| **Archon of the Shattered Sky** | 💀 **BOSS** | 3 fázis, scripted intro, boss bar |

#### Archon Boss Encounter
**Scripted intro:** 30 blokkon belül megközelítve:
1. Azonnali vihar — ég elsötétül
2. Képernyő shake + FOV-punch + void rift shader
3. 80 particle burst + robbanás
4. `"A csillagok tanúi lesznek bukásodnak."` — chat üzenet
5. Tényleges villám csap le a boss felett

**Fázis 1 (100–66% HP):** Lightning Shield (non-magic sebzés visszaveri), 180° sweep beam, Zealot hullámok 30s-enként
**Fázis 2 (65–33% HP):** Gravitáció inverzió + aréna szétesik + meteor eső 3s-enként
**Fázis 3 (32–0% HP):** 5-bolt rapid tűz + 15s Time Silence minden 45s-ben

---

### Progresszió Rendszer

- **Arcane Points** szerzés: varázslatok elsütésével, bossokon
- **Spell unlock:** Arcane Codex → Skill Tree tab → kattintás + AP cost
- **5 Tier:** T0 (ingyenes) → T5 (340–500 AP)
- **Element rendszer:** 7 elem (IGNIS, FULGUR, UMBRA, SYLVA, CRYO, CHRONO, SANCTUM)
- **Arcane Resonance aura:** 100/200/300/400/500+ AP szinteknél egyre látványosabb

#### Spell Combo Rendszer
Ha egymás után 3 különböző elemet sütsz el:
- **+30% sebzés** 10 másodpercig
- **Elem-színű aura** kering körülötted
- **Speed I** buff
- Chat értesítés: `✦ ELEMENTAL COMBO!`

---

### Világ Generálás

#### Arcane Spire Struktúra
Procedurálisan generált deepslate torony — **nincs .nbt template**, tisztán Java-ban épül:
- 22 szint magas, 7×7 deepslate brick falak
- Spirál lépcső belül
- Sarokbástyák az 5., 10., 15. szinten
- Ablakok vasráccsal minden 3. szinten
- **Tető:** Archon boss platform, 12-pontos ametiszt rúna-körrel
- **3 loot láda:** 1. szint, 2. szint, tető (arcane_spire_top loot table)
- **Archon spawner** a tetőn
- Generálódik: síkságokon, réteken, hegyeken (~2000 blokkonként)

#### Shattered Ley-Line Wastes Biom
- Sötét lila köd és ég
- Teal-fekete víz
- Szürke-lila fű és levelek
- **END_ROD ambient particlek** (arcane energiakibocsátás)
- Nincs eső
- Enderman-ek nagyobb számban

#### 4 Arcane Érc
| Érc | Biom | Y Tartomány | Drop |
|---|---|---|---|
| Astralit Ore | Overworld | Y 110–220 | Astralit Crystal |
| Void-Quartz Ore | Overworld | Y −64 to −32 | Void Quartz |
| Ignis Pyrite Ore | Nether | Y 10–110 | Ignis Pyrite |
| Etherium Crystal Ore | Overworld | Y −64 to −48 | Etherium Shard |

---

### Cinematic Rendszerek

- **Cast Animation:** Tier 2+ varázslatoknál spirális töltési fázis (4–16 tick), tier-alapú színnel
- **Ellenség reakció:** 3 szint — stagger (20+ dmg), heavy hit (50+ dmg), devastate (100+ dmg)
- **Scripted boss intro:** Drámai belépési jelenet az Archonnál
- **Kamera shake + FOV punch:** Minden nagy varázslatra
- **Időjárás effektek:** Thunder War, Lightning Storm, Cataclysmic Rift vihart idéz
- **Fény blokkok:** Singularity Collapse, Excalibur Beam valódi fényt bocsát ki
- **Ködfal:** Darkness + Slowness debuff közelben lévő ellenségeknek
- **Arcane Resonance:** Passzív aura 5 vizuális szinten

---

### Particle Rendszer (24 típus)

Minden varázslatnak saját dedikált particle-ja van, valódi pixel-art sprite pack-ből konvertálva (Pixelart Spells + Magic Pack 9 forrásokból). Minecraft vertical spritesheet formátum, NEAREST filter.

---

## Build és Telepítés

> ⚠️ Részletes build útmutató: **`BUILD_INSTRUCTIONS.md`**

### Gyors start

```bash
# 1. Gradle Wrapper generálás (csak egyszer kell)
gradle wrapper --gradle-version 8.8

# 2. Build
./gradlew build

# 3. Kimenet
# → build/libs/arcanezenith-0.1.0.jar
```

**Windows:**
```cmd
gradlew.bat build
```

**IntelliJ IDEA:** File → Open → build.gradle → Gradle panel → build task

### Követelmények
- **Java JDK 21** (pontosan — nem 17, nem 22)
- **NeoForge 21.1.72** a Minecraft 1.21.1-hez
- **4 GB RAM** Gradle buildhez

---

## Játékmenet Útmutató

### Első lépések
1. Crafting table → Arcane Wand (wand item)
2. Jobb klikk → Arcane Codex kinyílik (animáltan)
3. Skill Tree tab → első varázslat feloldása
4. **G** → radial spell menu
5. Jobb klikk → varázslat elsütése (Tier 2+ töltési animációval)
6. **V** → gyors varázslat-csere

### Haladó tippek
- Kombináld az elemeket: IGNIS → FULGUR → UMBRA = **+30% bónusz**
- Az Arcane Resonance aura jelzi a szinted — 500 pt-nál heaven beam is szól
- Az Arcane Spire-ban lévő loot ládák szükségesek a Tier 5 varázslatok gyors feloldásához
- Az Archon előtt oldd fel legalább 3 Tier 5 varázslatot

---

## Konfiguráció

**Fájl:** `.minecraft/config/arcanezenith-client.toml`
**In-game:** Mods → Arcane Zenith → Config

```toml
[dark_fantasy_shader]
    enabled = true
    base_intensity = 0.35      # 0.0=ki, 1.0=teljes
    combat_boost = true        # combat-ban max intenzitás
    contrast = 1.25
    saturation = 0.78
    vignette = 1.1
    film_grain = 0.028
    color_style = "dark"       # dark / cold / warm / neutral
    chromatic_aberration = true

[spell_effects]
    screen_shake = true
    fov_punch = true
    spell_shaders = true
    particle_intensity = 1.0   # 0.5=fele, 2.0=dupla
```

---

## Kódstruktúra

```
src/main/java/com/arcanezenith/
├── ArcaneZenith.java              # Fő mod osztály, regisztrálások
├── capability/                    # ManaData, ModAttachments
├── client/                        # ClientSetup, HUD, Codex screen
│   ├── effect/                    # PostEffectManager, CameraShake
│   └── particle/                  # ModParticles, ArcaneParticleBase
├── combat/                        # TerrainDestruction, ModDamageTypes
├── config/                        # ArcaneZenithConfig
├── entity/                        # 5 mob + Ethereal Familiar
│   └── boss/                      # ArchonEntity (3-phase)
├── event/                         # ManaRegenHandler, EnemyReactionHandler
├── item/                          # WandItem, CodexItem, ScrollItem, stb.
├── network/                       # ModNetworking, packets
├── progression/                   # SpellRegistry, SpellProgress
├── spell/                         # 24 Spell implementáció
│   ├── [BaseSpells]               # Tier 0-4 (14 db)
│   └── [LegendarySpells]          # Tier 5 (10 db)
└── worldgen/                      # ArcaneSpire, ModBiomes, ModStructures
```

---

## Ami Approximált (Nem 100% Design-Spec)

- **Entity modellek** — mind a 6 entitás vanilla megjelenéssel, teljes AI és gameplay működéssel
- **Projektil entitások** — hitscan közelítés valódi repülő entitás helyett
- **Time Silence** — NoAI+Slowness közelítés, repülő lövedékek nem fagynak meg
- **Cast animation** — server-side scheduled, nem valódi client animáció

## Ami Még Nincs Implementálva

- Saját `.ogg` hangfájlok (jelenleg vanilla hangok átirányítva)
- Custom entity model/skin (GeckoLib integráció szükséges)
- Biom generálás finomhangolása (floating rocks, arcane geysers)
- Arcane Sentinel társ (Runic Golem)
- Jigsaw-alapú multi-piece struktúra variánsok

---

## Licensz

All Rights Reserved — saját fejlesztés, NeoForge MDK alapon.
