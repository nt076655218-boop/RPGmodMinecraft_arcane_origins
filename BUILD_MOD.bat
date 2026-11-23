@echo off
chcp 65001 >nul
title Сборка мода Arcane Origins (NeoForge 1.21.1)
echo ============================================================
echo   Arcane Origins - Автоматическая сборка .jar (NeoForge 1.21.1)
echo ============================================================
echo.

java -version >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена!
    echo Установи Java 21 (Temurin JDK 21): https://adoptium.net/
    pause
    exit /b 1
)

set "GRADLE_VER=8.10.2"
set "GRADLE_DIR=%~dp0.gradle-dist\gradle-%GRADLE_VER%"
set "GRADLE_BIN=%GRADLE_DIR%\bin\gradle.bat"

if not exist "%GRADLE_BIN%" (
    echo [1/3] Скачиваю Gradle %GRADLE_VER% ^(нужно только 1 раз^)...
    if not exist "%~dp0.gradle-dist" mkdir "%~dp0.gradle-dist"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VER%-bin.zip' -OutFile '%~dp0.gradle-dist\gradle.zip'"
    if errorlevel 1 (
        echo [ОШИБКА] Не удалось скачать Gradle. Проверь интернет.
        pause
        exit /b 1
    )
    echo [2/3] Распаковываю Gradle...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%~dp0.gradle-dist\gradle.zip' -DestinationPath '%~dp0.gradle-dist' -Force"
    del "%~dp0.gradle-dist\gradle.zip" >nul 2>&1
)

echo [3/3] Компилирую мод (при первом запуске займёт 2-5 минут)...
call "%GRADLE_BIN%" build --no-daemon
if errorlevel 1 (
    echo.
    echo [ОШИБКА] Сборка не удалась! Убедись, что установлена именно Java 21 ^(JDK 21^).
    pause
    exit /b 1
)

for %%f in ("build\libs\arcaneorigins-*.jar") do (
    copy /Y "%%f" "%~dp0" >nul
    echo.
    echo ============================================================
    echo   ГОТОВО! Файл мода скопирован прямо в эту папку:
    echo   %%~nxf
    echo   Просто перекинь его в папку mods твоего NeoForge 1.21.1!
    echo ============================================================
)
pause
