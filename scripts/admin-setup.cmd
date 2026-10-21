@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0admin-setup.ps1" %*
exit /b %ERRORLEVEL%
