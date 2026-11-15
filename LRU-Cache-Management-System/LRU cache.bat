
@echo off
title LRU Cache Management System

echo Starting LRU Cache...
echo.

echo Project directory:
echo %~dp0

echo.
echo Checking compiled classes...

if not exist "%~dp0out\com\lru\Main.class" (
    echo ERROR: Main.class not found!
    pause
    exit /b
)

echo Main.class found.
echo.

echo Checking Java...
where java
echo.

java -cp "%~dp0out" com.lru.Main 5

echo.
echo Application exited.
pause
