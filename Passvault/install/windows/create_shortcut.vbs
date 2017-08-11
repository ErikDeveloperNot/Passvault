Set WshShell = CreateObject("WScript.Shell")

installDir= Wscript.Arguments(0)

strDesktop = WshShell.SpecialFolders("Desktop")
Set oMyShortCut= WshShell.CreateShortcut(strDesktop+"\Passvault.lnk")

oMyShortCut.IconLocation = installDir+"\install\windows\vault.ico"
oMyShortCut.TargetPath = installDir+"\passvault.bat" 
oMyShortCut.WorkingDirectory = installDir
oMyShortCut.Description = "Passvault"
oMyShortCut.Save