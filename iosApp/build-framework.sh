#!/bin/bash

set -e

# Navigate to the root project directory
cd "$(dirname "$0")/.."

# Determine which framework to build based on the target platform
if [[ "$PLATFORM_NAME" == "iphonesimulator" ]]; then
    if [[ "$(uname -m)" == "arm64" ]]; then
        TARGET="iosSimulatorArm64"
    else
        TARGET="iosX64"
    fi
else
    TARGET="iosArm64"
fi

# Build the framework
./gradlew :exampleApp:linkDebugFramework${TARGET}

# Create the output directory if it doesn't exist
mkdir -p "${BUILT_PRODUCTS_DIR}"

# Copy the framework to the expected location
FRAMEWORK_PATH="exampleApp/build/bin/${TARGET,,}/debugFramework/ExampleApp.framework"
if [ -d "$FRAMEWORK_PATH" ]; then
    echo "Copying framework from: $FRAMEWORK_PATH"
    rm -rf "${BUILT_PRODUCTS_DIR}/ExampleApp.framework"
    cp -R "$FRAMEWORK_PATH" "${BUILT_PRODUCTS_DIR}/"
else
    echo "Error: Framework not found at $FRAMEWORK_PATH"
    exit 1
fi

echo "Framework build completed successfully"
