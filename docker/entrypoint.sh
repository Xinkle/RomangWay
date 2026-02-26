#!/usr/bin/env sh
set -eu

CONFIG_PATH="${SECRET_PROFILE_PATH:-/config/secret_profile.properties}"

if [ -f "$CONFIG_PATH" ]; then
  echo "[romangway] Using config file: $CONFIG_PATH"
else
  echo "[romangway] Config file not found at $CONFIG_PATH. Falling back to environment variables."
fi

exec java -jar /app/app.jar

