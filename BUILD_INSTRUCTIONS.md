# Build Útmutató — Arcane Zenith

## Probléma: hiányzó gradlew / gradle-wrapper.jar

Ha a `./gradlew build` parancs nem működik, az egyik okból:

### 1. gradlew nincs executable (Linux/Mac)
```bash
chmod +x gradlew
./gradlew build
```

### 2. gradle-wrapper.jar hiányzik
A JAR hálózati korlátok miatt nem volt belerakható a ZIP-be. Megoldások:

**A) Ha van Gradle telepítve:**
```bash
gradle wrapper --gradle-version 8.8
./gradlew build
```

**B) Manuális letöltés:**
```bash
# Töltsd le innen:
# https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar
# Mentsd ide: gradle/wrapper/gradle-wrapper.jar
./gradlew build
```

**C) IntelliJ IDEA (ajánlott Windows-ra):**
1. File → Open → válaszd a `build.gradle` fájlt
2. "Open as Project" kattintás
3. Gradle panel → Tasks → build → `build`
4. Automatikusan letölti a wrapper JAR-t

**D) Windows gradlew.bat:**
```cmd
gradlew.bat build
```

## Követelmények

| Követelmény | Verzió |
|---|---|
| Java JDK | **21 pontosan** (nem 17, nem 22) |
| Gradle | 8.8 (automatikus letöltés) |
| RAM | minimum 4 GB szabad |
| Internet | szükséges az első buildhez |

## Build parancsok

```bash
# Release JAR (ez kell a modhoz)
./gradlew build
# Output: build/libs/arcanezenith-0.1.0.jar

# Dev kliens futtatás
./gradlew runClient

# Clean build
./gradlew clean build
```

## Java verzió ellenőrzés

```bash
java -version
# Kell: openjdk version "21.x.x"
```

Ha rossz Java van:
```bash
# Windows: https://adoptium.net/ → JDK 21
# Mac: brew install openjdk@21
# Linux: sudo apt install openjdk-21-jdk
```

## Telepítés (ha a build sikerült)

1. `build/libs/arcanezenith-0.1.0.jar` → `.minecraft/mods/`
2. NeoForge 21.1.72 legyen telepítve
3. Minecraft 1.21.1 launcher profil NeoForge-ra

## Hibaelhárítás

**"Unsupported class file major version 65"**
→ JDK 21 kell, jelenleg más Java fut. Állítsd be a `JAVA_HOME`-ot.

**"Could not resolve net.neoforged:neoforge"**
→ Internet szükséges, a NeoForge Maven szerver le van töltve első buildnél.

**"Out of memory"**
→ `gradle.properties`-ben növeld: `org.gradle.jvmargs=-Xmx4G`

**Particlék nem jelennek meg**
→ Ellenőrizd hogy a `assets/arcanezenith/particles/*.json` fájlok megvannak.
