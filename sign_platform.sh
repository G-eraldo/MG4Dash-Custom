#!/usr/bin/env bash
# macOS/Linux counterpart of sign_platform.ps1. The AOSP test key stays local in keys/.
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$repo_dir"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"

key="$repo_dir/keys/platform.pk8"
cert="$repo_dir/keys/platform.x509.pem"
if [[ ! -s "$key" || ! -s "$cert" ]]; then
    echo "Missing keys/platform.pk8 or keys/platform.x509.pem; see README.fr.md." >&2
    exit 1
fi

apksigner="$(find "$ANDROID_HOME/build-tools" -maxdepth 2 -type f -name apksigner | sort -r | head -1)"
if [[ -z "$apksigner" ]]; then
    echo "Android build-tools with apksigner are required." >&2
    exit 1
fi

bash "$repo_dir/gradlew" :app:assembleCarRelease --no-daemon
version="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)"
source_apk="$repo_dir/app/build/outputs/apk/car/release/app-car-release-unsigned.apk"
out="$repo_dir/dist/MG4Dash-$version.apk"
if [[ ! -s "$source_apk" ]]; then
    echo "Car release APK not found at $source_apk" >&2
    exit 1
fi
mkdir -p dist
"$apksigner" sign --key "$key" --cert "$cert" \
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled false \
    --out "$out" "$source_apk"
"$apksigner" verify --verbose --print-certs "$out"
shasum -a 256 "$out"
