#!/bin/bash
# Auto-restart script for FisioGestao
# Usage: ./restart-service.sh [path/to/app.jar]

APP_JAR="${1:-}"
LOG_FILE="restart.log"
APP_LOG="app.log"
JVM_OPTS="-Xms512m -Xmx1024m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=./heapdumps/ \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+CrashOnOutOfMemoryError"

mkdir -p heapdumps

if [ -z "$APP_JAR" ]; then
    APP_JAR=$(find target -maxdepth 1 -type f -name "*.jar" ! -name "original-*.jar" | head -n 1)
fi

if [ -z "$APP_JAR" ] || [ ! -f "$APP_JAR" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: JAR not found: $APP_JAR" | tee -a "$LOG_FILE"
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting auto-restart monitor for $APP_JAR" >> "$LOG_FILE"

while true; do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting application..." >> "$LOG_FILE"
    java $JVM_OPTS -jar "$APP_JAR" >> "$APP_LOG" 2>&1
    EXIT_CODE=$?
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Application exited with code $EXIT_CODE. Restarting in 10s..." >> "$LOG_FILE"
    sleep 10
done
