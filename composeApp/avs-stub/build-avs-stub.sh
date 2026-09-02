#!/usr/bin/env bash
#
# Builds a STUB `avs.framework` (static) for iOS device + simulator from src/avs_stub.m.
#
# Why: natrium wraps Kalium but does not expose calling (KaliumConfigs.enableCalling = false).
# Kalium's calling module nonetheless links Wire's proprietary `avs` framework, which is not
# published to Maven. This stub provides just the symbols the Kotlin/Native cinterop references, so
# an iOS app built on top of natrium links and runs WITHOUT calling. Swap in Wire's real
# avs.xcframework to enable calling.
#
# This lives in the demo as the reference pattern a consuming app replicates (see README.md,
# "iOS: linking AVS"). composeApp's build points `-F` at <this-dir>/<slice> per iOS target; with a
# dynamic app framework the stub's symbols get linked in, so no separate avs.framework is needed at
# runtime.
#
# Re-run this after changing the symbol list. Committed outputs live under <this-dir>/<slice>/.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$DIR/src/avs_stub.m"
DEPLOY="14.0"   # keep <= the Kotlin/Native framework min to avoid "built for newer version" warnings

build_slice() {
  local slice="$1" sdk="$2" triple="$3"
  local sdkpath fw obj
  sdkpath="$(xcrun --sdk "$sdk" --show-sdk-path)"
  fw="$DIR/$slice/avs.framework"
  obj="$(mktemp -t avs_stub).o"
  rm -rf "$fw"; mkdir -p "$fw"

  xcrun clang -c -fobjc-arc -isysroot "$sdkpath" -target "$triple" -o "$obj" "$SRC"
  # Static archive named `avs` -> makes avs.framework a static framework.
  xcrun libtool -static -o "$fw/avs" "$obj"
  rm -f "$obj"

  cat > "$fw/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleIdentifier</key><string>com.wire.avs.stub</string>
  <key>CFBundleName</key><string>avs</string>
  <key>CFBundleExecutable</key><string>avs</string>
  <key>CFBundlePackageType</key><string>FMWK</string>
  <key>MinimumOSVersion</key><string>$DEPLOY</string>
</dict></plist>
PLIST
  echo "built $fw/avs ($triple)"
}

build_slice "ios-arm64"           "iphoneos"        "arm64-apple-ios${DEPLOY}"
build_slice "ios-arm64-simulator" "iphonesimulator" "arm64-apple-ios${DEPLOY}-simulator"
echo "done."
