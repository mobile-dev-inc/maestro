#!/usr/bin/env bash
# Unit tests for android_system_image.sh. Run: bash .github/scripts/android_system_image_test.sh
set -uo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
subject="$here/android_system_image.sh"
fails=0

check() {
  name=$1; version=$2; expected=$3
  actual=$(bash "$subject" "$version")
  if [ "$actual" = "$expected" ]; then
    echo "ok   - $name"
  else
    echo "FAIL - $name"
    echo "        version:  $version"
    echo "        expected: $expected"
    echo "        actual:   $actual"
    fails=$((fails + 1))
  fi
}

check "below 37 uses google_apis"        android-32   "system-images;android-32;google_apis;x86_64"
check "36 uses google_apis"              android-36   "system-images;android-36;google_apis;x86_64"
check "37.0 uses ps16k"                  android-37.0 "system-images;android-37.0;google_apis_ps16k;x86_64"
check "37.1 uses ps16k"                  android-37.1 "system-images;android-37.1;google_apis_ps16k;x86_64"
check "bare 37 uses ps16k"               android-37   "system-images;android-37;google_apis_ps16k;x86_64"
check "38 uses ps16k"                    android-38   "system-images;android-38;google_apis_ps16k;x86_64"

echo
if [ "$fails" -eq 0 ]; then
  echo "All tests passed."
else
  echo "$fails test(s) failed."
  exit 1
fi
