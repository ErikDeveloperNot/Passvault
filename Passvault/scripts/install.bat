ECHO "Starting Installer"

FOR /f %%i in ('cd') do set CWD=%%i
ECHO %CWD%

MKDIR .passvault\data

ECHO "Running replace script"
cscript.exe .\install\windows\replace.vbs "./passvault.bat" %CWD%

ECHO "Running create shortcut script"
cscript.exe .\install\windows\create_shortcut.vbs %CWD%

ECHO "Install done"