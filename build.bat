@echo off
setlocal enabledelayedexpansion

echo ===============================
echo 1) Gradle build (incremental, tests skipped)
echo ===============================

call gradlew.bat build -x test --build-cache
if %errorlevel% neq 0 (
    echo Build failed.
    pause
    exit /b %errorlevel%
)

echo.
echo ===============================
echo 2) Copy JAR
echo ===============================

set "SOURCE=C:\Users\HP\Desktop\Projects\Hytale\ISkyblock\build\libs\ISkyblock-0.0.3.jar"
set "DEST=C:\Users\HP\Desktop\Projects\Hytale\server\mods\ISkyblock-0.0.3.jar"

if not exist "%SOURCE%" (
    echo ERROR: Source JAR not found: %SOURCE%
    pause
    exit /b 1
)

copy /Y "%SOURCE%" "%DEST%"
if %errorlevel% neq 0 (
    echo ERROR: Copy failed.
    pause
    exit /b %errorlevel%
)

echo JAR copied successfully.

echo.
echo ===============================
echo 3) Launch Server
echo ===============================

cd /d "C:\Users\HP\Desktop\Projects\Hytale\server"

"C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" -jar HytaleServer.jar --assets "C:\Users\HP\AppData\Roaming\Hytale\install\release\package\game\latest\Assets.zip"

pause