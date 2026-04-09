#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ADVANCED_DIR="$(dirname "$SCRIPT_DIR")"
SIMPLE_DIR="$(dirname "$ADVANCED_DIR")/steves-simple-storage"

echo "==> Building steves-simple-storage"
cd "$SIMPLE_DIR"
./gradlew :neoforge:s3:build

echo "==> Building steves-advanced-storage"
cd "$ADVANCED_DIR"
./gradlew :neoforge:build

echo "==> Build complete"
