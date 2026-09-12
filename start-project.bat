@echo off
chcp 65001 >nul
title 智问学伴 - 一键启停控制台

echo 正在启动智问学伴项目控制脚本...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-project.ps1" %*

echo.
echo =======================================================
echo 执行完成。按任意键退出本窗口...
pause >nul
