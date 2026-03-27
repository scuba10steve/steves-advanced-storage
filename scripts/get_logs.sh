#!/usr/bin/env bash

version=${1:-"0.1.0"}
username=${1:-"Steven Tompkins"}
modpack=${2:-"steves-advanced-storage-env"}

cp -r "/mnt/c/Users/${username}/AppData/Roaming/gdlauncher_carbon/data/instances/${modpack}/logs" "logs"
