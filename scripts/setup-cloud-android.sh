#!/usr/bin/env bash
set -euo pipefail

# Cloud build tools are kept outside the checkout's tracked source.
task_root=/workspace/mobileAiHand
task_tools="$task_root/.local/android-build"
task_sdk="$task_tools/sdk"
mkdir -p "$task_tools" "$task_sdk/platforms" "$task_sdk/build-tools"

cd "$task_root"

# This cloud image has a JRE but no javac or jlink. Fetch Debian's signed JDK
# package and layer its tools over a private copy of the matching runtime.
if [ ! -x "$task_tools/java/bin/javac" ]; then
  mkdir -p "$task_tools/apt-lists/partial" "$task_tools/apt-archives/partial" "$task_tools/java"
  printf 'deb https://deb.debian.org/debian trixie main\n' > "$task_tools/debian.list"
  apt-get update \
    -o "Dir::Etc::sourcelist=$task_tools/debian.list" \
    -o Dir::Etc::sourceparts=- \
    -o "Dir::State::lists=$task_tools/apt-lists" \
    -o "Dir::Cache::archives=$task_tools/apt-archives"
  (cd "$task_tools" && apt-get download \
    -o "Dir::Etc::sourcelist=$task_tools/debian.list" \
    -o Dir::Etc::sourceparts=- \
    -o "Dir::State::lists=$task_tools/apt-lists" openjdk-21-jdk-headless)
  task_jdk_deb=$(find "$task_tools" -maxdepth 1 -name 'openjdk-21-jdk-headless_*.deb' -print -quit)
  test -n "$task_jdk_deb"
  dpkg-deb -x "$task_jdk_deb" "$task_tools/jdk-package"
  cp -a /usr/lib/jvm/java-21-openjdk-amd64/. "$task_tools/java/"
  for task_tool in javac jlink jmod; do
    cp "$task_tools/jdk-package/usr/lib/jvm/java-21-openjdk-amd64/bin/$task_tool" "$task_tools/java/bin/$task_tool"
  done
fi

download_checked() {
  task_url=$1
  task_file=$2
  task_hash=$3
  task_hash_tool=$4
  if [ ! -f "$task_file" ]; then curl -fsSL --retry 2 "$task_url" -o "$task_file"; fi
  printf '%s  %s\n' "$task_hash" "$task_file" | "$task_hash_tool" -c
}

download_checked \
  https://dl.google.com/android/repository/platform-35_r02.zip \
  "$task_tools/platform-35.zip" 0bb560a90a7a2cbd0dd8348224d518b638fe7949 sha1sum
download_checked \
  https://dl.google.com/android/repository/build-tools_r35_linux.zip \
  "$task_tools/build-tools-35.zip" 2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc sha1sum
download_checked \
  https://dl.google.com/android/repository/platform-tools_r37.0.1-linux.zip \
  "$task_tools/platform-tools.zip" 477254aa5f903c15cf51001717bdf347fb6b53e0 sha1sum
download_checked \
  https://services.gradle.org/distributions/gradle-8.9-bin.zip \
  "$task_tools/gradle-8.9.zip" d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab sha256sum

if [ ! -f "$task_sdk/platforms/android-35/android.jar" ]; then
  unzip -q "$task_tools/platform-35.zip" -d "$task_sdk/platforms"
fi
if [ ! -x "$task_sdk/build-tools/35.0.0/aapt2" ]; then
  unzip -q "$task_tools/build-tools-35.zip" -d "$task_sdk/build-tools"
  mv "$task_sdk/build-tools/android-15" "$task_sdk/build-tools/35.0.0"
fi
if [ ! -x "$task_sdk/platform-tools/adb" ]; then
  unzip -q "$task_tools/platform-tools.zip" -d "$task_sdk"
fi
if [ ! -x "$task_tools/gradle-8.9/bin/gradle" ]; then
  unzip -q "$task_tools/gradle-8.9.zip" -d "$task_tools"
fi

printf 'sdk.dir=%s\n' "$task_sdk" > "$task_root/android-app/local.properties"
export JAVA_HOME="$task_tools/java"
export GRADLE_USER_HOME="$task_tools/gradle-user-home"
export ANDROID_USER_HOME="$task_tools/android-user-home"
mkdir -p "$GRADLE_USER_HOME" "$ANDROID_USER_HOME"
cd "$task_root/android-app"
"$task_tools/gradle-8.9/bin/gradle" \
  -Dorg.gradle.java.home="$JAVA_HOME" \
  -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 \
  -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 \
  :app:assembleDebug :app:testDebugUnitTest --console=plain --no-daemon
