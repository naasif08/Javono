@echo off
REM Javono CLI launcher for Windows

REM Resolve the directory of this script
SET SCRIPT_DIR=%~dp0

REM Run Javono CLI jar with all arguments passed to the batch file
java -cp "%SCRIPT_DIR%javono.jar" javono.cli.JavonoCli %*
