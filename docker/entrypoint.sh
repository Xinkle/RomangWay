#!/usr/bin/env sh
set -eu

CONFIG_PATH="${SECRET_PROFILE_PATH:-/config/secret_profile.properties}"
LOG_DIR="${LOG_DIR:-/app/logs}"

if [ -f "$CONFIG_PATH" ]; then
  echo "[romangway] Using config file: $CONFIG_PATH"
else
  echo "[romangway] Config file not found at $CONFIG_PATH. Falling back to environment variables."
fi

mkdir -p "$LOG_DIR"
export LOG_DIR
echo "[romangway] Log directory: $LOG_DIR"

if [ -z "${JAVA_TOOL_OPTIONS:-}" ] && [ -n "${ROMANGWAY_JAVA_MEMORY_OPTS:-}" ]; then
  export JAVA_TOOL_OPTIONS="$ROMANGWAY_JAVA_MEMORY_OPTS"
  echo "[romangway] Applying ROMANGWAY_JAVA_MEMORY_OPTS as JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
elif [ -z "${JAVA_TOOL_OPTIONS:-}" ]; then
  echo "[romangway] JAVA_TOOL_OPTIONS not set. No JVM memory limit is applied by default."
else
  echo "[romangway] Using JAVA_TOOL_OPTIONS from environment: $JAVA_TOOL_OPTIONS"
fi

exec java -jar /app/app.jar
