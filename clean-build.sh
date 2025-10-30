#!/bin/bash

echo "🛑 Stopping Gradle daemon..."
./gradlew --stop

echo "🧹 Cleaning KSP caches..."
rm -rf app/build/kspCaches
rm -rf app/build/generated/ksp
rm -rf app/.cxx
rm -rf .gradle/
rm -rf app/build/

echo "🔄 Syncing Gradle..."
./gradlew --refresh-dependencies

echo "🏗️ Building project..."
./gradlew clean build --no-daemon

echo "✅ Done!"