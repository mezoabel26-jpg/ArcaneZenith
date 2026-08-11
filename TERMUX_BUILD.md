# Termux Build Útmutató

## A probléma

A NeoForge MDK a `createMinecraftArtifacts` lépésben a Vineflower decompilert
futtatja amely 1-2 GB RAM-ot igényel. Termux-on ez az Android memória limit
miatt kilövi a folyamatot.

## Megoldás 1: Swap fájl (ajánlott, root nélkül)

```bash
# Termux-ban futtasd ezeket EGYSZER a build előtt:
cd ~
fallocate -l 2G swapfile || dd if=/dev/zero of=swapfile bs=1M count=2048
chmod 600 swapfile
mkswap swapfile
swapon swapfile

# Ellenőrzés
free -h

# Most buildeld a modot
cd /storage/emulated/0/ArcaneZenithGame/ArcaneZenith
bash ./gradlew build
```

## Megoldás 2: Termux:Widget + build script

Ha a swap sem elég, próbáld éjszaka buildelni amikor más alkalmazások nem futnak:

```bash
# Zárj be minden más alkalmazást!
# Majd:
bash ./gradlew build --no-daemon
```

## Megoldás 3: PC-n buildelni

A projekt Windows/Mac/Linux-on is buildelhető ugyanezzel a ZIP-pel:

```bash
# Windows:
gradlew.bat build

# Linux/Mac:
chmod +x gradlew
./gradlew build
```

Output: `build/libs/arcanezenith-0.1.0.jar`

## Megjegyzés

Az első build után a decompile cache-elve van a
`~/.gradle/caches/` mappában, és a következő buildek sokkal gyorsabbak
és kevesebb RAM-ot igényelnek.
