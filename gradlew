#!/bin/sh

##############################################################################
# Gradle start up script for UN*X
##############################################################################

APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

# Find java
if [ -n "$JAVA_HOME" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
else
    JAVA_EXE="java"
fi

# Auto-download gradle-wrapper.jar if missing
if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
    echo "Downloading gradle-wrapper.jar..."
    JAR_URL="https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$JAR_URL" -o "$GRADLE_WRAPPER_JAR"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$JAR_URL" -O "$GRADLE_WRAPPER_JAR"
    else
        echo "ERROR: curl or wget required to download gradle-wrapper.jar"
        exit 1
    fi
fi

exec "$JAVA_EXE" \
    -Xmx64m -Xms64m \
    -Dorg.gradle.appname="gradlew" \
    -classpath "$GRADLE_WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
