#!/usr/bin/env bash
set -euo pipefail

if [ "$(basename "$PWD")" != "maestro" ]; then
	echo "This script must be run from the maestro root directory"
	exit 1
fi

rm -rf ./build/Products

xcodebuild \
	ARCHS="x86_64 arm64" \
	ONLY_ACTIVE_ARCH=NO \
	-project ./maestro-ios-xctest-runner/maestro-driver-ios.xcodeproj \
	-scheme maestro-driver \
	-sdk iphonesimulator \
	-destination "generic/platform=iOS Simulator" \
	-IDEBuildLocationStyle=Custom \
	-IDECustomBuildLocationType=Absolute \
	-IDECustomBuildProductsPath="$PWD/build/Products" \
	build-for-testing

xcodebuild \
	ARCHS="x86_64 arm64" \
	ONLY_ACTIVE_ARCH=NO \
	-project ./maestro-ios-xctest-runner/maestro-driver-ios.xcodeproj \
	-scheme maestro-driver \
	-sdk appletvsimulator \
	-destination "generic/platform=tvOS Simulator" \
	-IDEBuildLocationStyle=Custom \
	-IDECustomBuildLocationType=Absolute \
	-IDECustomBuildProductsPath="$PWD/build/Products" \
	build-for-testing

## Remove intermediates, output and copy runner in maestro-ios-driver
cp -r \
	./build/Products/Debug-iphonesimulator/maestro-driverUITests-Runner.app \
	./maestro-ios-driver/src/main/resources/ios/maestro-driverUITests-Runner.app

cp -r \
	./build/Products/Debug-appletvsimulator/maestro-driverUITests-Runner.app \
	./maestro-ios-driver/src/main/resources/tvos/maestro-driverUITests-Runner.app

cp -r \
	./build/Products/Debug-iphonesimulator/maestro-driver.app \
	./maestro-ios-driver/src/main/resources/ios/maestro-driver.app

cp -r \
	./build/Products/Debug-appletvsimulator/maestro-driver.app \
	./maestro-ios-driver/src/main/resources/tvos/maestro-driver.app

cp \
	./build/Products/*iphonesimulator*.xctestrun \
	./maestro-ios-driver/src/main/resources/ios/maestro-driver-config.xctestrun

cp \
	./build/Products/*appletvsimulator*.xctestrun \
	./maestro-ios-driver/src/main/resources/tvos/maestro-driver-config.xctestrun

(cd ./maestro-ios-driver/src/main/resources/ios && zip -r maestro-driverUITests-Runner.zip ./maestro-driverUITests-Runner.app)
(cd ./maestro-ios-driver/src/main/resources/tvos && zip -r maestro-driverUITests-Runner.zip ./maestro-driverUITests-Runner.app)
(cd ./maestro-ios-driver/src/main/resources/ios && zip -r maestro-driver.zip ./maestro-driver.app)
(cd ./maestro-ios-driver/src/main/resources/tvos && zip -r maestro-driver.zip ./maestro-driver.app)
rm -r ./maestro-ios-driver/src/main/resources/ios/*.app
rm -r ./maestro-ios-driver/src/main/resources/tvos/*.app
