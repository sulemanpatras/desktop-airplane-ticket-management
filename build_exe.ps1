# ==========================================================
#  SkyBook Desktop App Package Automation Script (Windows)
# ==========================================================

$ErrorActionPreference = 'Stop'

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Starting SkyBook stand-alone EXE build process" -ForegroundColor Cyan
Write-Host "==================================================`n" -ForegroundColor Cyan

# ── 1. Convert SVG Logo to PNG & ICO ──────────────────────────
Write-Host "► Step 1: Converting airplane.svg to PNG and ICO..." -ForegroundColor Yellow
python convert_logo.py
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to convert logo SVG using convert_logo.py!"
}
Write-Host "OK: Icons generated successfully.`n" -ForegroundColor Green

# ── 2. Run Maven Package ──────────────────────────────────────
Write-Host "► Step 2: Building SkyBook jar and gathering dependencies..." -ForegroundColor Yellow
mvn clean package
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed!"
}
Write-Host "OK: Maven build successful.`n" -ForegroundColor Green

# ── 3. Copy Application JAR to libs directory ─────────────────
Write-Host "► Step 3: Preparing libs directory..." -ForegroundColor Yellow
if (-not (Test-Path "target/libs")) {
    New-Item -ItemType Directory -Path "target/libs" | Out-Null
}
Copy-Item "target/SkyBook-1.0-SNAPSHOT.jar" "target/libs/SkyBook.jar" -Force
Write-Host "OK: target/libs/SkyBook.jar prepared.`n" -ForegroundColor Green

# ── 4. Bundle standalone App Image with JPackage ──────────────
Write-Host "► Step 4: Bundling standalone EXE and custom JRE with jpackage..." -ForegroundColor Yellow

$JMODS_PATH = "C:\Program Files\Java\openjfx-26.0.1_windows-x64_bin-jmods\javafx-jmods-26.0.1"
$ICON_PATH = "src/skybook/assets/images/airplane.ico"

# Clean destination folder
if (Test-Path "target/dist") {
    Remove-Item "target/dist" -Recurse -Force | Out-Null
}

jpackage --type app-image `
         --name SkyBook `
         --input target/libs `
         --main-jar SkyBook.jar `
         --main-class skybook.ui.MainApp `
         --module-path $JMODS_PATH `
         --add-modules javafx.controls,javafx.fxml `
         --icon $ICON_PATH `
         --dest target/dist `
         --vendor "SkyBook Airlines" `
         --description "SkyBook Airline Ticket Management System"

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage bundling failed!"
}

Write-Host "`n==================================================" -ForegroundColor Green
Write-Host "  Success! Standalone EXE bundled successfully!" -ForegroundColor Green
Write-Host "  App Location: target\dist\SkyBook\SkyBook.exe" -ForegroundColor Green
Write-Host "==================================================`n" -ForegroundColor Green
