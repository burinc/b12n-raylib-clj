#!/bin/bash
# Package the app as a native macOS application (.app / .dmg)
# Requires: JDK 22+ with jpackage and jlink (for coffi native access)

set -e

# Ensure we use Java 22+ (required by coffi)
export JAVA_HOME=$(/usr/libexec/java_home -v 22 2>/dev/null || /usr/libexec/java_home -v 23 2>/dev/null || /usr/libexec/java_home -v 24 2>/dev/null || /usr/libexec/java_home -v 25 2>/dev/null)
echo "Using JAVA_HOME: $JAVA_HOME"

APP_NAME="Asteroids"
APP_VERSION="1.0.0"  # macOS requires version to start with non-zero
MAIN_CLASS="examples.asteroids"
VENDOR="Ertugrul"
JAR_NAME="raylib-clojure-playground-0.1.0-SNAPSHOT-standalone.jar"

# Navigate to project root
cd "$(dirname "$0")/.."

echo "==> Building uberjar..."
lein uberjar

echo "==> Preparing staging directory..."
rm -rf target/bundle target/staging target/runtime
mkdir -p target/staging
cp "target/$JAR_NAME" target/staging/

echo "==> Copying native raylib library..."
cp libs/libraylib.6.0.0.dylib target/staging/

echo "==> Removing quarantine attribute..."
xattr -d com.apple.quarantine target/staging/libraylib.6.0.0.dylib 2>/dev/null || true

echo "==> Creating minimal JRE with jlink..."
# Modules needed for Clojure + coffi (FFI):
# - java.base: core Java + java.lang.foreign (FFI API)
# - java.logging: logging support
# - jdk.unsupported: sun.misc.Unsafe (used by some libs)
jlink \
  --add-modules java.base,java.logging,jdk.unsupported \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=zip-6 \
  --output target/runtime

RUNTIME_SIZE=$(du -sh target/runtime | cut -f1)
echo "==> Custom runtime size: $RUNTIME_SIZE"

echo "==> Creating native macOS bundle..."

# Create the native application using jpackage with custom runtime
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "$VENDOR" \
  --input target/staging \
  --main-jar "$JAR_NAME" \
  --main-class "$MAIN_CLASS" \
  --dest target/bundle \
  --runtime-image target/runtime \
  --java-options "-XstartOnFirstThread" \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --java-options "-Xmx512m" \
  --java-options '-Djava.library.path=$APPDIR' \
  --mac-package-name "$APP_NAME" \
  --mac-app-category "public.app-category.games"

echo "==> Removing quarantine from app bundle..."
xattr -cr "target/bundle/$APP_NAME.app" 2>/dev/null || true

echo "==> Ad-hoc signing app bundle..."
codesign --force --deep --sign - "target/bundle/$APP_NAME.app"

BUNDLE_SIZE=$(du -sh "target/bundle/$APP_NAME.app" | cut -f1)
echo "==> App bundle created at: target/bundle/$APP_NAME.app ($BUNDLE_SIZE)"

# Optionally create DMG installer
read -p "Create DMG installer? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  jpackage \
    --type dmg \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "$VENDOR" \
    --input target/staging \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --dest target/bundle \
    --runtime-image target/runtime \
    --java-options "-XstartOnFirstThread" \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    --java-options "-Xmx512m" \
    --java-options '-Djava.library.path=$APPDIR' \
    --mac-package-name "$APP_NAME" \
    --mac-app-category "public.app-category.games"
  
  echo "==> Removing quarantine from DMG..."
  xattr -cr "target/bundle/$APP_NAME-$APP_VERSION.dmg" 2>/dev/null || true
  
  DMG_SIZE=$(du -sh "target/bundle/$APP_NAME-$APP_VERSION.dmg" | cut -f1)
  echo "==> DMG installer created at: target/bundle/$APP_NAME-$APP_VERSION.dmg ($DMG_SIZE)"
fi

echo "==> Done!"
