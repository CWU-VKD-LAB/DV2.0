@echo off
setlocal
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-dv.ps1" %*
if errorlevel 1 exit /b %errorlevel%
