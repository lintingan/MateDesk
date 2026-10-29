#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PROJECT="$ROOT/android-project"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"

if [[ ! -d "$SDK/platforms/android-35" ]]; then
  echo "Android SDK Platform 35 belum ditemukan di: $SDK"
  echo "Buka Android Studio → Settings → Languages & Frameworks → Android SDK."
  echo "Pasang Android SDK Platform 35 dan Android SDK Build-Tools 35.0.0."
  read -r -p "Tekan Enter untuk menutup..."
  exit 1
fi
export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"
AS_JAVA="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [[ -x "$AS_JAVA/bin/java" ]]; then
  export JAVA_HOME="$AS_JAVA"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
cd "$PROJECT"
chmod +x ./gradlew
./gradlew --no-daemon :app:assembleDebug
APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK" ]]; then
  echo "
Build selesai. APK installable: $APK"
  open -R "$APK"
else
  echo "Build selesai, tetapi APK tidak ditemukan pada lokasi yang diharapkan." >&2
  exit 1
fi
read -r -p "Tekan Enter untuk menutup..."
