#!/usr/bin/env bash

# Setup script for Rust Android development
# This script:
# 1. Installs necessary Rust targets for Android
# 2. Auto-detects system configuration
# 3. Generates .cargo/config.toml with correct linker paths

set -e

echo "🦀 Setting up Rust for Android development..."

# Function to detect operating system
detect_os() {
    case "$(uname -s)" in
        Darwin*)    echo "darwin-x86_64" ;;
        Linux*)     echo "linux-x86_64" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows-x86_64" ;;
        *)          echo "unknown" ;;
    esac
}

# Function to find Android SDK path
find_android_sdk() {
    if [ -n "$ANDROID_HOME" ]; then
        echo "$ANDROID_HOME"
    elif [ -n "$ANDROID_SDK_ROOT" ]; then
        echo "$ANDROID_SDK_ROOT"
    elif [ -d "$HOME/Android/Sdk" ]; then
        echo "$HOME/Android/Sdk"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        echo "$HOME/Library/Android/sdk"
    else
        echo ""
    fi
}

# Function to find the NDK version (from tools.versions.toml or latest)
find_ndk_version() {
    local sdk_path="$1"
    local ndk_dir="$sdk_path/ndk"
    local version_catalog="../gradle/tools.versions.toml"

    # First, try to read NDK version from tools.versions.toml
    if [ -f "$version_catalog" ]; then
        local ndk_version=$(grep "^ndk" "$version_catalog" | cut -d '"' -f 2)
        if [ -n "$ndk_version" ] && [ -d "$ndk_dir/$ndk_version" ]; then
            echo "$ndk_version"
            return
        elif [ -n "$ndk_version" ]; then
            echo "⚠️  Warning: NDK version $ndk_version specified in tools.versions.toml not found in $ndk_dir" >&2
            echo "   Falling back to latest available version..." >&2
        fi
    fi

    # Fall back to finding the latest version
    if [ -d "$ndk_dir" ]; then
        # Find the latest version (highest version number)
        ls -1 "$ndk_dir" | sort -V | tail -n 1
    else
        echo ""
    fi
}

# Function to extract API level from build.gradle and version catalog
extract_api_level() {
    local build_gradle="build.gradle"
    local version_catalog="../gradle/libs.versions.toml"
    
    # First, check if build.gradle uses version catalog
    if [ -f "$build_gradle" ] && grep -q "libs.versions.minSdk" "$build_gradle"; then
        # Extract from version catalog
        if [ -f "$version_catalog" ]; then
            grep "^minSdk" "$version_catalog" | cut -d '"' -f 2
        else
            echo "21"  # Default fallback
        fi
    elif [ -f "$build_gradle" ]; then
        # Look for direct minSdkVersion in build.gradle
        grep -E "minSdkVersion|minSdk" "$build_gradle" | head -n 1 | sed 's/.*[[:space:]]\([0-9]\+\).*/\1/'
    else
        echo "21"  # Default fallback
    fi
}

# Detect system configuration
OS_PLATFORM=$(detect_os)
echo "📱 Detected OS: $OS_PLATFORM"

if [ "$OS_PLATFORM" = "unknown" ]; then
    echo "❌ Unsupported operating system. Please configure manually."
    exit 1
fi

# Find Android SDK
ANDROID_SDK_PATH=$(find_android_sdk)
if [ -z "$ANDROID_SDK_PATH" ]; then
    echo "❌ Android SDK not found. Please set ANDROID_HOME or ANDROID_SDK_ROOT environment variable."
    echo "   Or install Android SDK to ~/Android/Sdk (Linux) or ~/Library/Android/sdk (macOS)"
    exit 1
fi

echo "📁 Found Android SDK: $ANDROID_SDK_PATH"

# Find NDK version
NDK_VERSION=$(find_ndk_version "$ANDROID_SDK_PATH")
if [ -z "$NDK_VERSION" ]; then
    echo "❌ NDK not found in $ANDROID_SDK_PATH/ndk"
    echo "   Please install NDK through Android Studio SDK Manager"
    exit 1
fi

echo "🔨 Found NDK version: $NDK_VERSION"

# Extract API level
API_LEVEL=$(extract_api_level)
echo "📋 Using API level: $API_LEVEL"

# Set executable suffix for Windows
EXE_SUFFIX=""
if [ "$OS_PLATFORM" = "windows-x86_64" ]; then
    EXE_SUFFIX=".exe"
fi

# Construct NDK path
NDK_PATH="$ANDROID_SDK_PATH/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/$OS_PLATFORM/bin"

# Verify NDK path exists
if [ ! -d "$NDK_PATH" ]; then
    echo "❌ NDK toolchain not found at: $NDK_PATH"
    echo "   Please check your NDK installation"
    exit 1
fi

echo "🛠️  NDK toolchain path: $NDK_PATH"

# Install Rust targets for Android
echo "📦 Installing Rust targets for Android..."
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add i686-linux-android
rustup target add x86_64-linux-android

# Create .cargo directory if it doesn't exist
mkdir -p .cargo

# Generate config.toml from template
echo "⚙️  Generating .cargo/config.toml..."
if [ ! -f ".cargo/config.toml.template" ]; then
    echo "❌ Template file .cargo/config.toml.template not found"
    exit 1
fi

# Use sed to replace placeholders in template
sed -e "s|{{NDK_PATH}}|$NDK_PATH|g" \
    -e "s|{{API_LEVEL}}|$API_LEVEL|g" \
    -e "s|{{EXE_SUFFIX}}|$EXE_SUFFIX|g" \
    -e "s|{{PLATFORM}}|$OS_PLATFORM|g" \
    -e "s|{{NDK_VERSION}}|$NDK_VERSION|g" \
    -e "s|{{ANDROID_SDK_PATH}}|$ANDROID_SDK_PATH|g" \
    .cargo/config.toml.template > .cargo/config.toml

echo "✅ Successfully generated .cargo/config.toml"

# Verify generated linkers exist
echo "🔍 Verifying linkers..."
LINKERS=(
    "aarch64-linux-android${API_LEVEL}-clang${EXE_SUFFIX}"
    "armv7a-linux-androideabi${API_LEVEL}-clang${EXE_SUFFIX}"
    "i686-linux-android${API_LEVEL}-clang${EXE_SUFFIX}"
    "x86_64-linux-android${API_LEVEL}-clang${EXE_SUFFIX}"
)

for linker in "${LINKERS[@]}"; do
    if [ -f "$NDK_PATH/$linker" ]; then
        echo "  ✅ $linker"
    else
        echo "  ❌ $linker (not found)"
    fi
done

echo ""
echo "🎉 Rust Android setup complete!"
echo ""
echo "📋 Configuration summary:"
echo "  - OS Platform: $OS_PLATFORM"
echo "  - Android SDK: $ANDROID_SDK_PATH"
echo "  - NDK Version: $NDK_VERSION"
echo "  - API Level: $API_LEVEL"
echo "  - Config file: .cargo/config.toml"
echo ""
echo "🚀 You can now build for Android using:"
echo "   cargo build --target aarch64-linux-android"
echo "   cargo build --target armv7-linux-androideabi"
echo "   cargo build --target i686-linux-android"
echo "   cargo build --target x86_64-linux-android" 