#!/usr/bin/env bash

jar=$(ls neoforge/build/libs/s3-advanced-*.jar 2>/dev/null | sort -V | tail -1)
username=${1:-"Steven Tompkins"}
modpack=${2:-"steves-advanced-storage-env"}

if [ -z "$jar" ]; then
  echo "No jar found in neoforge/build/libs/. Run ./gradlew :neoforge:build first."
  exit 1
fi

modsdir="/mnt/c/Users/${username}/AppData/Roaming/gdlauncher_carbon/data/instances/${modpack}/instance/mods"

# Remove old s3-advanced jars to avoid duplicate mod conflicts
rm -f "$modsdir"/s3-advanced-*.jar

echo "Copying $jar"
cp "$jar" "$modsdir"
