#!/bin/bash

# Exit on error
set -e

# Function to display usage
usage() {
    echo "Usage: $0 [ios|android]"
    echo "  ios     - Run tests for iOS platform"
    echo "  android - Run tests for Android platform"
    exit 1
}

# Check if platform argument is provided
if [ $# -ne 1 ]; then
    usage
fi

PLATFORM=$(echo "$1" | tr '[:upper:]' '[:lower:]')

# Validate platform argument
if [ "$PLATFORM" != "ios" ] && [ "$PLATFORM" != "android" ]; then
    echo "Error: Invalid platform. Must be 'ios' or 'android'"
    usage
fi

# Set platform-specific variables
if [ "$PLATFORM" = "ios" ]; then
    APP_ID="com.superwall.Advanced"
else
    APP_ID="com.superwall.superapp"
fi

echo "Building Expo app for $PLATFORM..."
cd ui_test_app

# Prebuild native projects if needed
if [ ! -d "android" ] || [ ! -d "ios" ]; then
    echo "Prebuild native projects..."
    npx expo prebuild --platform $PLATFORM
fi

# Create production export
echo "Creating production export..."
npx expo export --platform $PLATFORM --output-dir dist

if [ "$PLATFORM" = "ios" ]; then
    echo "Building iOS app..."
    # Build iOS app - you may need to adjust the scheme name
    cd ios
    # Try to find the correct scheme name
    SCHEME=$(xcodebuild -list | grep -A 1 "Schemes:" | tail -1 | xargs)
    if [ -z "$SCHEME" ]; then
        SCHEME="SuperwallUITests"  # fallback
    fi
    echo "Using scheme: $SCHEME"
    xcodebuild -scheme "$SCHEME" -destination 'platform=iOS Simulator,name=iPhone 15' -configuration Release build
    
    # Install the app to the simulator
    echo "Installing iOS app to simulator..."
    # Find the built .app bundle
    APP_PATH=$(find . -name "*.app" -path "*/Build/Products/*" | head -1)
    if [ -n "$APP_PATH" ]; then
        echo "Found app at: $APP_PATH"
        # Get the simulator device ID for iPhone 15
        DEVICE_ID=$(xcrun simctl list devices | grep "iPhone 15" | grep "(Booted)" | sed 's/.*(\([^)]*\)).*/\1/' | head -1)
        if [ -z "$DEVICE_ID" ]; then
            # Boot the iPhone 15 simulator if not already booted
            echo "Booting iPhone 15 simulator..."
            DEVICE_ID=$(xcrun simctl list devices | grep "iPhone 15" | sed 's/.*(\([^)]*\)).*/\1/' | head -1)
            xcrun simctl boot "$DEVICE_ID"
            sleep 5
        fi
        echo "Installing app to device: $DEVICE_ID"
        xcrun simctl install "$DEVICE_ID" "$APP_PATH"
        echo "iOS app installed successfully"
    else
        echo "Error: Could not find built .app bundle"
        echo "Looking for .app files..."
        find . -name "*.app" -type d
        exit 1
    fi
    cd ..
else
    echo "Building Android APK..."
    # Build Android APK in release mode to avoid dev server dependency
    cd android
    echo "Starting Gradle build..."
    timeout 300 ./gradlew assembleRelease --no-daemon --info
    BUILD_STATUS=$?
    cd ..
    
    if [ $BUILD_STATUS -eq 124 ]; then
        echo "Error: Android build timed out after 5 minutes"
        exit 1
    elif [ $BUILD_STATUS -ne 0 ]; then
        echo "Error: Android build failed with status $BUILD_STATUS"
        exit 1
    fi
    
    echo "Build completed successfully"
    
    # Install the APK on the connected device
    echo "Installing APK on connected device..."
    echo "Current directory: $(pwd)"
    APK_PATH="android/app/build/outputs/apk/release/app-release.apk"
    echo "Looking for APK at: $APK_PATH"
    
    if [ -f "$APK_PATH" ]; then
        echo "APK found! Installing..."
        adb install -r "$APK_PATH"
        INSTALL_STATUS=$?
        if [ $INSTALL_STATUS -eq 0 ]; then
            echo "APK installed successfully"
        else
            echo "Error: APK installation failed with status $INSTALL_STATUS"
            exit 1
        fi
    else
        echo "Error: APK not found at expected location: $APK_PATH"
        echo "Current directory contents:"
        ls -la android/app/build/outputs/apk/
        echo "Looking for APK files..."
        find . -name "*.apk" -type f
        exit 1
    fi
fi
cd ..

echo "========================================="
echo "Running Maestro tests for $PLATFORM..."
echo "========================================="

# Run the tests
maestro test -e "PLATFORM_ID=$APP_ID" ui_test_app/maestro/handler/flow.yaml
maestro test -e "PLATFORM_ID=$APP_ID" ui_test_app/maestro/flow.yaml
maestro test -e "PLATFORM_ID=$APP_ID" ui_test_app/maestro/delegate/flow.yaml
if [ "$PLATFORM" = "ios" ]; then
    maestro test -e "PLATFORM_ID=$APP_ID" ui_test_app/maestro/purchasecontroller/test_pc_purchases.yaml
fi

maestro test -e "PLATFORM_ID=$APP_ID" ui_test_app/maestro/purchasecontroller/no_pc_purchases.yaml

echo "All tests completed successfully!" 