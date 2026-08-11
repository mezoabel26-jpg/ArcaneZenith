# Arcane Zenith — Textúra Csere Útmutató / Texture Replacement Guide

Minden textúra egy konkrét PNG fájl a `src/main/resources/assets/arcanezenith/textures/`
mappában. Egyszerűen **cseréld le a fájlt a sajátodra** — azonos névvel, azonos mérettel —
és a mod automatikusan az új textúrát fogja használni.

---

## 📁 Mappák / Folder Structure

```
src/main/resources/assets/arcanezenith/textures/
├── block/          ← Blokk textúrák (ércek, asztal)
├── item/           ← Item textúrák (varázspálca, kristályok, tárgyak)
└── particle/       ← Particle animációk (spellenkénti effektek)
```

---

## 🧱 BLOKK TEXTÚRÁK / Block Textures

Minden blokk textúra **16×16 pixel**, egyszerű PNG. Cseréld le 1:1-ben.

| Fájl | Blokk | Megjegyzés |
|------|-------|-----------|
| `block/astralit_ore.png` | Astralit Érc | Kék-arany ragyogó kristály, Y>110 hegyekben |
| `block/void_quartz_ore.png` | Void-Quartz Érc | Sötét lila kristály, mélyen Y<-32 |
| `block/ignis_pyrite_ore.png` | Ignis Pyrite Érc | Vörös-narancs, Netherben |
| `block/etherium_crystal_ore.png` | Etherium Kristály Érc | Halvány cián, Y<-48 |
| `block/arcane_infusion_table.png` | Arcane Infusion Table | Az Infusion Asztal textúrája |

**Csere lépései:**
1. Nyisd meg az új képedet bármilyen képszerkesztőben (Photoshop, GIMP, Aseprite)
2. Méretezd **16×16 pixelre**
3. Mentsd el `PNG` formátumban a fenti névvel
4. Helyezd be a `block/` mappába (régi fájl felülírásával)
5. Gradle build vagy `/reload` a játékban

---

## 🗡️ ITEM TEXTÚRÁK / Item Textures

Minden item textúra **16×16 pixel**, PNG. A modell JSON automatikusan hivatkozik rájuk.

| Fájl | Item | Megjegyzés |
|------|------|-----------|
| `item/arcane_wand.png` | Arcane Wand | A főfegyver varázspálca |
| `item/arcane_shard.png` | Arcane Shard | Pontszerző shard (jobb klikk = +10 pont) |
| `item/arcane_codex.png` | Arcane Codex | A grimoire könyv item |
| `item/spell_scroll.png` | Spell Scroll | Tier-4 ultimate unlock tekercs |
| `item/astralit_crystal.png` | Astralit Crystal | Astralit érc dropp |
| `item/void_quartz.png` | Void Quartz | Void-Quartz érc dropp |
| `item/ignis_pyrite.png` | Ignis Pyrite | Ignis Pyrite érc dropp |
| `item/etherium_shard.png` | Etherium Shard | Etherium érc dropp (ritka) |

**Csere lépései:**
1. Készítsd el az új **16×16** PNG képet
2. Másold be a `item/` mappába azonos névvel
3. A modell JSON (`models/item/`) automatikusan megtalálja — nem kell változtatni

---

## ✨ PARTICLE TEXTÚRÁK / Particle Textures

A particle textúrák **spritesheets** — animált képek, ahol minden frame egymás alatt van.
**A méret kritikus!** Ha rossz méretet adsz meg, a particle nem jelenik meg vagy crash lesz.

### Mérettáblázat / Size Reference

| Fájl | Méret | Frames | Frame méret | Spell |
|------|-------|--------|-------------|-------|
| `particle/arcane_spark.png` | 16×128 | 8 | 16×16 | Arcane Bolt — kék-lila szikra |
| `particle/rune_ring.png` | 32×128 | 4 | 32×32 | Általános rúna gyűrű |
| `particle/void_core.png` | 16×64 | 4 | 16×16 | Void mag — fekete-lila |
| `particle/golden_light.png` | 16×64 | 4 | 16×16 | Arany fény — God's Spear |
| `particle/plasma_beam.png` | 8×128 | 16 | 8×8 | Plasma beam streak |
| `particle/thunder_spark.png` | 16×128 | 8 | 16×16 | ThunderWar — villámlás |
| `particle/holy_star.png` | 16×96 | 6 | 16×16 | Angels Help / Judgment |
| `particle/shadow_wisp.png` | 12×96 | 8 | 12×12 | Teleport — árnyék füst |
| `particle/frost_shard.png` | 8×96 | 6 | 8×16 | Horde of Blades — jégszilánk |
| `particle/lava_geyser.png` | 16×128 | 8 | 16×16 | Cataclysmic Rift — láva |
| `particle/plasma_spiral.png` | 16×128 | 8 | 16×16 | Plasma Annihilator — spirál |
| `particle/gravity_dust.png` | 8×48 | 6 | 8×8 | Gravitational Collapse — por |
| `particle/heaven_beam.png` | 16×192 | 6 | 16×32 | Mages Hammer / Judgment — fényoszlop |
| `particle/singularity_nova.png` | 32×256 | 8 | 32×32 | Singularity Collapse — nova |

### Hogyan csináld a particle spritesheet-et?

**Az animáció szabálya:** frame 0 (első animációs kocka) = a kép TETEJE, frame N = a kép ALJA.
A Minecraft felülről lefelé olvassa a frameket.

**Példa — 8 frame-es, 16×16-os particle:**
```
Teljes kép: 16 pixel széles × 128 pixel magas
Frame 0: sor 0-15     (tetején)
Frame 1: sor 16-31
Frame 2: sor 32-47
...
Frame 7: sor 112-127  (alján)
```

**Aseprite-tal (ajánlott):**
1. Új canvas: `16×16` pixel
2. Rajzolj 8 frame-et az animáció timeline-on
3. `File → Export Sprite Sheet`
4. Layout: `Vertical Strip`
5. Mentés PNG-ként a megfelelő névvel

**GIMP-pel:**
1. Nyiss egy `16×128` méretű képet
2. Rajzolj kézzel minden 16px magas sávot
3. Exportálás PNG-ként

### Particle JSON — nem kell változtatni

A `particles/` mappában lévő JSON fájlok (pl. `thunder_spark.json`) hivatkoznak
a textúrára, de **nem kell szerkeszteni** őket, ha a PNG fájl neve és mérete
ugyanaz marad.

---

## 🔄 Gyors csere összefoglaló / Quick Swap Summary

```
Textúra típusa    Mappa                      Méret
─────────────────────────────────────────────────
Blokk             textures/block/            16×16
Item              textures/item/             16×16
Particle (alap)   textures/particle/         16×(16×frameCount)
Particle (nagy)   textures/particle/         lásd táblázatot
```

**Szabályok:**
- ✅ Azonos fájlnév kötelező
- ✅ PNG formátum kötelező
- ✅ Méretnek egyeznie kell (particle-nél kritikus!)
- ✅ RGBA (átlátszóság) támogatott és ajánlott
- ❌ Nem kell JSON-t szerkeszteni, ha a méret és név stimmel
- ❌ Nem kell újrakompilálni — a textúra csere resource pack szinten működik

---

## 🎨 Tippek / Tips

**Pixel-art stílus megtartása:**
Minecraft stílushoz maradj 16×16 vagy 32×32 pixelnél, ne használj anti-aliasing-et,
és tartsd meg az erős kontrasztokat.

**Átlátszóság (alpha):**
Minden particle textúra RGBA csatornás — az alpha csatorna kontrol
a fade-in/fade-out animációt. Ha az alpha 0, a particle láthatatlan.

**Tesztelés:**
A Gradle build után indítsd el a modot és add ki a `/particle arcanezenith:thunder_spark ~ ~ ~`
parancsot a chat-ben, hogy azonnal lásd az új textúrát.
