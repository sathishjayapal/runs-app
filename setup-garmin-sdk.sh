#!/bin/bash

# Garmin FIT SDK Installation Script
# This script downloads and installs the Garmin FIT SDK to your local Maven repository

set -e

echo "=========================================="
echo "Garmin FIT SDK Installation Script"
echo "=========================================="
echo ""

# Configuration
FIT_SDK_VERSION="21.141.00"
DOWNLOAD_URL="https://developer.garmin.com/fit/download/"
TEMP_DIR="/tmp/garmin-fit-sdk"
SDK_ZIP="FitSDKRelease_${FIT_SDK_VERSION}.zip"

echo "Step 1: Checking for existing SDK..."
if [ -d "$TEMP_DIR" ]; then
    echo "Cleaning up previous download..."
    rm -rf "$TEMP_DIR"
fi

mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

echo ""
echo "=========================================="
echo "MANUAL DOWNLOAD REQUIRED"
echo "=========================================="
echo ""
echo "The Garmin FIT SDK must be downloaded manually from:"
echo "  $DOWNLOAD_URL"
echo ""
echo "Please follow these steps:"
echo ""
echo "1. Open the URL above in your browser"
echo "2. Download the 'FIT SDK' (Java version)"
echo "3. The file will be named something like: FitSDKRelease_21.141.00.zip"
echo "4. Move the downloaded ZIP file to: $TEMP_DIR"
echo ""
echo "Press ENTER after you've downloaded and moved the file to $TEMP_DIR"
read -r

# Find the ZIP file
echo ""
echo "Step 2: Looking for FIT SDK ZIP file..."
ZIP_FILE=$(find "$TEMP_DIR" -name "FitSDKRelease*.zip" -o -name "fit-sdk*.zip" | head -n 1)

if [ -z "$ZIP_FILE" ]; then
    echo "ERROR: Could not find FIT SDK ZIP file in $TEMP_DIR"
    echo "Please ensure you've downloaded and moved the file."
    exit 1
fi

echo "Found: $ZIP_FILE"

# Extract the ZIP
echo ""
echo "Step 3: Extracting SDK..."
unzip -q "$ZIP_FILE" -d "$TEMP_DIR/extracted"

# Find the fit.jar file
echo ""
echo "Step 4: Locating fit.jar..."
FIT_JAR=$(find "$TEMP_DIR/extracted" -name "fit.jar" | head -n 1)

if [ -z "$FIT_JAR" ]; then
    echo "ERROR: Could not find fit.jar in the extracted SDK"
    echo "Please check the SDK structure"
    exit 1
fi

echo "Found: $FIT_JAR"

# Install to local Maven repository
echo ""
echo "Step 5: Installing to local Maven repository..."
mvn install:install-file \
    -Dfile="$FIT_JAR" \
    -DgroupId=com.garmin \
    -DartifactId=fit \
    -Dversion=21.141.0 \
    -Dpackaging=jar

echo ""
echo "=========================================="
echo "Installation Complete!"
echo "=========================================="
echo ""
echo "The Garmin FIT SDK has been installed to your local Maven repository."
echo "You can now build the runs-app project with full FIT file parsing support."
echo ""
echo "Next steps:"
echo "  1. Build the project: mvn clean install"
echo "  2. Run the application: mvn spring-boot:run"
echo ""
echo "Cleaning up temporary files..."
rm -rf "$TEMP_DIR"

echo "Done!"
