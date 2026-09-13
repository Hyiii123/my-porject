@echo off
chcp 65001 >nul
title Zhiwen-Share Project Console

echo Starting Zhiwen-Share project control script...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-project.ps1" %*

echo.
echo =======================================================
echo Done. Press any key to close this window...
pause >nul