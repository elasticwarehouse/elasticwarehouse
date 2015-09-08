@echo off
set ELEVATE_APP=%1
set ELEVATE_PARMS=
for /f "tokens=* delims=" %%P in (%ELEVATE_APP%) do (
    set ELEVATE_APP=%%P
)
echo Set objShell = CreateObject("Shell.Application") > %TEMP%\elevatedapp.vbs
echo Set objWshShell = WScript.CreateObject("WScript.Shell") >>%TEMP%\elevatedapp.vbs
echo Set objWshProcessEnv = objWshShell.Environment("PROCESS") >>%TEMP%\elevatedapp.vbs
echo objShell.ShellExecute "cmd.exe", "/K cd ""%CD%"" && ""%ELEVATE_APP%"" ", "%CD%", "runas" >>%TEMP%\elevatedapp.vbs
cscript %TEMP%\elevatedapp.vbs
DEL %TEMP%\elevatedapp.vbs