#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ADVANCED_DIR="$(dirname "$SCRIPT_DIR")"
SIMPLE_DIR="$(dirname "$ADVANCED_DIR")/steves-simple-storage"

username=${1:-"Steven Tompkins"}
modpack=${2:-"steves-advanced-storage-env"}
modsdir="/mnt/c/Users/${username}/AppData/Roaming/gdlauncher_carbon/data/instances/${modpack}/instance/mods"

simple_jar=$(ls "$SIMPLE_DIR"/neoforge/s3/build/libs/s3-*.jar 2>/dev/null | sort -V | tail -1)
advanced_jar=$(ls "$ADVANCED_DIR"/neoforge/build/libs/s3-advanced-*.jar 2>/dev/null | sort -V | tail -1)

if [ -z "$simple_jar" ]; then
  echo "No jar found in steves-simple-storage/neoforge/build/libs/. Run scripts/build.sh first."
  exit 1
fi

if [ -z "$advanced_jar" ]; then
  echo "No jar found in neoforge/build/libs/. Run scripts/build.sh first."
  exit 1
fi

# Remove old jars to avoid duplicate mod conflicts
rm -f "$modsdir"/s3-*.jar
rm -f "$modsdir"/s3-advanced-*.jar

echo "Copying $simple_jar"
cp "$simple_jar" "$modsdir"

echo "Copying $advanced_jar"
cp "$advanced_jar" "$modsdir"
