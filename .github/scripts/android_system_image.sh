#!/usr/bin/env bash
# Prints the sdkmanager/avdmanager system-image id the emulator jobs install for an
# android-<version>. From API 37 the x86_64 image ships only as the 16 KB page-size
# variant, so the tag switches from google_apis to google_apis_ps16k at API >= 37.
# Mirrors the tag derivation in maestro-client DeviceSpec.Android, so the AVD name the
# CLI creates in test-start-device matches what the Verify step expects.
#
#   android_system_image.sh android-37.1  -> system-images;android-37.1;google_apis_ps16k;x86_64
#   android_system_image.sh android-32    -> system-images;android-32;google_apis;x86_64
#
# <version> may carry a minor platform version (37.1); the tag is chosen from the
# integer API level.
set -euo pipefail

version=$1
api="${version#android-}"; api="${api%%.*}"
tag=google_apis
[ "$api" -ge 37 ] && tag=google_apis_ps16k
echo "system-images;${version};${tag};x86_64"
