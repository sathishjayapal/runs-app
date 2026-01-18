# Garmin FIT SDK Installation Guide

## Quick Start

Follow these steps to install the Garmin FIT SDK and enable full FIT file parsing with complete data extraction.

---

## Step 1: Download Garmin FIT SDK

1. **Visit the Garmin Developer Portal:**
   ```
   https://developer.garmin.com/fit/download/
   ```

2. **Download the FIT SDK:**
   - Click on "FIT SDK" download button
   - You may need to create a free Garmin developer account
   - Download the latest version (e.g., `FitSDKRelease_21.141.00.zip`)

3. **Save the file** to your Downloads folder or a temporary location

---

## Step 2: Install SDK to Local Maven Repository

### Option A: Using the Provided Script (Recommended)

We've created a script to automate the installation:

```bash
cd /Users/skminfotech/IdeaProjects/runs-app
./setup-garmin-sdk.sh
```

**The script will:**
1. Prompt you to download the SDK
2. Guide you to place it in the correct location
3. Extract the SDK
4. Install `fit.jar` to your local Maven repository
5. Clean up temporary files

### Option B: Manual Installation

If you prefer to install manually:

1. **Extract the downloaded ZIP file:**
   ```bash
   unzip ~/Downloads/FitSDKRelease_21.141.00.zip -d /tmp/garmin-fit-sdk
   ```

2. **Locate the fit.jar file:**
   ```bash
   find /tmp/garmin-fit-sdk -name "fit.jar"
   ```
   
   It's typically located at: `/tmp/garmin-fit-sdk/FitSDKRelease_21.141.00/java/fit.jar`

3. **Install to Maven local repository:**
   ```bash
   mvn install:install-file \
       -Dfile=/tmp/garmin-fit-sdk/FitSDKRelease_21.141.00/java/fit.jar \
       -DgroupId=com.garmin \
       -DartifactId=fit \
       -Dversion=21.141.0 \
       -Dpackaging=jar
   ```

4. **Verify installation:**
   ```bash
   ls -la ~/.m2/repository/com/garmin/fit/21.141.0/
   ```
   
   You should see `fit-21.141.0.jar`

---

## Step 3: Build the Project

Once the SDK is installed, build the project:

```bash
cd /Users/skminfotech/IdeaProjects/runs-app
mvn clean install
```

**Expected output:**
```
[INFO] BUILD SUCCESS
```

If you see dependency errors, verify the SDK was installed correctly in Step 2.

---

## Step 4: Verify FIT Parsing Capability

### Test with a Sample FIT File

1. **Place a FIT file in the import folder:**
   ```bash
   cp ~/path/to/sample.fit /Volumes/MacProHD/HDD_Downloads/16ccd700-0294-4888-b22e-e74e54f6a697_1/DI_CONNECT/DI-Connect-Uploaded-Files/UploadedFiles_0-_Part1/
   ```

2. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Trigger manual import (or wait for scheduled job):**
   ```bash
   curl -X POST http://localhost:8080/api/garmin-import/trigger \
     -u admin@runsapp.com:admin123
   ```

4. **Check the logs for successful parsing:**
   ```
   INFO  - Successfully parsed FIT file: sample.fit - Distance: 3.50 miles, Calories: 450, Duration: 1800 seconds, Max HR: 175
   INFO  - Successfully imported activity from file: sample.fit (ID: 10001)
   ```

---

## What Data is Now Extracted

With the Garmin FIT SDK installed, the parser extracts:

| Data Field | Source | Format | Example |
|------------|--------|--------|---------|
| **Distance** | Session.totalDistance | Miles (converted from meters) | 3.50 |
| **Elapsed Time** | Session.totalElapsedTime | HH:MM:SS | 00:30:00 |
| **Calories** | Session.totalCalories | Integer | 450 |
| **Max Heart Rate** | Session.maxHeartRate | BPM | 175 |
| **Avg Heart Rate** | Session.avgHeartRate | BPM | 155 |
| **Activity Type** | Session.sport | Mapped string | running |
| **Activity Date** | Session.startTime | ISO 8601 | 2024-01-15T06:30:00-06:00 |
| **Activity ID** | Activity.timestamp | Unix timestamp | 1705324200 |

---

## Troubleshooting

### Issue: "Unresolved dependency: com.garmin:fit:jar:21.141.0"

**Cause:** SDK not installed to local Maven repository

**Solution:**
1. Verify the SDK was downloaded
2. Re-run the installation command from Step 2
3. Check `~/.m2/repository/com/garmin/fit/21.141.0/` exists

### Issue: "Cannot find fit.jar in extracted SDK"

**Cause:** SDK structure may have changed

**Solution:**
```bash
# Find the jar file
find /tmp/garmin-fit-sdk -name "*.jar" -type f

# Use the correct path in the install command
```

### Issue: Build succeeds but parsing fails

**Cause:** FIT file may be corrupted or invalid

**Solution:**
1. Check logs for "FIT file integrity check failed"
2. Try with a different FIT file
3. Verify the file is actually a .FIT file (not renamed)

### Issue: "No data extracted from FIT file"

**Cause:** FIT file may not contain session data

**Solution:**
- Check debug logs for "Session data" and "Activity data" messages
- Some FIT files may only contain raw records without summary
- Ensure the FIT file is from a completed activity

---

## SDK Version Information

- **Current Version:** 21.141.0
- **Release Date:** Check Garmin developer portal
- **Compatibility:** Java 8+
- **License:** Garmin FIT SDK License (check SDK documentation)

---

## Updating to a Newer SDK Version

If Garmin releases a new SDK version:

1. Download the new version
2. Install with updated version number:
   ```bash
   mvn install:install-file \
       -Dfile=/path/to/new/fit.jar \
       -DgroupId=com.garmin \
       -DartifactId=fit \
       -Dversion=21.XXX.0 \
       -Dpackaging=jar
   ```
3. Update `pom.xml` dependency version
4. Rebuild the project

---

## Additional Resources

- **Garmin FIT SDK Documentation:** Included in downloaded SDK
- **FIT File Format Specification:** https://developer.garmin.com/fit/protocol/
- **Garmin Developer Forum:** https://forums.garmin.com/developer/

---

## Next Steps After Installation

1. ✅ SDK installed and verified
2. ✅ Project builds successfully
3. ✅ Test with sample FIT files
4. 📋 Configure import folder path (already done in `.env`)
5. 📋 Set up scheduled job (runs every 30 minutes automatically)
6. 📋 Monitor logs for import activity

---

**Installation Complete!** Your runs-app can now extract complete activity data from Garmin FIT files.
