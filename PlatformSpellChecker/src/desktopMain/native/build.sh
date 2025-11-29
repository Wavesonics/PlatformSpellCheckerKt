#!/bin/bash

# Build script for NSSpellChecker JNI wrapper library

# Set up build directories
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../../../.."
BUILD_DIR="$PROJECT_ROOT/build/native/macos"
# Output to resources directory that will be included in JAR
RESOURCES_DIR="$PROJECT_ROOT/src/desktopMain/resources"
OUTPUT_DIR_ARM64="$RESOURCES_DIR/darwin-aarch64"
OUTPUT_DIR_X86_64="$RESOURCES_DIR/darwin-x86-64"

# Create build directories if they don't exist
mkdir -p "$BUILD_DIR"
mkdir -p "$OUTPUT_DIR_ARM64"
mkdir -p "$OUTPUT_DIR_X86_64"

# Compiler and flags
CC="clang"
CFLAGS="-arch x86_64 -arch arm64 -mmacosx-version-min=10.12 -fPIC -O2"
FRAMEWORKS="-framework Foundation -framework AppKit"
LDFLAGS="-dynamiclib -install_name @rpath/libNSSpellCheckerJNI.dylib"

# Source files
SOURCES="$SCRIPT_DIR/NSSpellCheckerJNI.m"
# Build universal binary first
UNIVERSAL_OUTPUT="$BUILD_DIR/libNSSpellCheckerJNI.dylib"

echo "Building NSSpellChecker JNI library..."
echo "Source: $SOURCES"
echo "Universal output: $UNIVERSAL_OUTPUT"

# Compile the library as universal binary
$CC $CFLAGS $LDFLAGS $FRAMEWORKS -o "$UNIVERSAL_OUTPUT" "$SOURCES"

if [ $? -eq 0 ]; then
    echo "Successfully built universal binary: $UNIVERSAL_OUTPUT"

    # Copy universal binary to both architecture-specific directories
    echo "Copying to JNA resource directories..."
    cp "$UNIVERSAL_OUTPUT" "$OUTPUT_DIR_ARM64/libNSSpellCheckerJNI.dylib"
    cp "$UNIVERSAL_OUTPUT" "$OUTPUT_DIR_X86_64/libNSSpellCheckerJNI.dylib"

    echo "Libraries installed to:"
    echo "  - $OUTPUT_DIR_ARM64/libNSSpellCheckerJNI.dylib"
    echo "  - $OUTPUT_DIR_X86_64/libNSSpellCheckerJNI.dylib"

    # Show library info
    echo ""
    echo "Library info:"
    file "$UNIVERSAL_OUTPUT"
    echo ""
    echo "Architectures:"
    lipo -info "$UNIVERSAL_OUTPUT"
    echo ""
    echo "Exported symbols:"
    nm -g "$UNIVERSAL_OUTPUT" | grep "T _" | head -20
else
    echo "Build failed!"
    exit 1
fi