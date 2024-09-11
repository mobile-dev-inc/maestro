#!/usr/bin/env bash
set -euo pipefail

if [ "$(basename "$PWD")" != "maestro" ]; then
	echo "This script must be run from the maestro root directory"
	exit 1
fi

DEVICE="${1:-}"
if [ -z "$DEVICE" ]; then
	DEVICE="iPhone 15 Pro"
	echo "No device passed, will default to $DEVICE"
fi

xctestrun_file="$(find ./build/Products -maxdepth 1 -name '*.xctestrun' -print -quit)"
if [ "$(wc -l "$xctestrun_file")" = 1 ]; then
	echo "xctestrun file found: $xctestrun_file"
elif [ "$(wc -l "$xctestrun_file")" = 0 ]; then
	echo "xctestrun file not found in ./build/Products. Did you build the runner?"
	exit 1
elif [ "$(wc -l "$xctestrun_file")" -gt 1 ]; then
	echo "Multiple xctestrun files found in ./build/Products. Only 1 can be present."
	exit 1
fi

xcodebuild test-without-building \
	-xctestrun "$xctestrun_file" \
	-destination "platform=iOS Simulator,name=iPhone 15 Pro" `#-destination "generic/platform=iOS Simulator"` \
	-destination-timeout 1
