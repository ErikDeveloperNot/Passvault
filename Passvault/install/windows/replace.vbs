Const ForReading = 1

Const ForWriting = 2

passvaultBat = Wscript.Arguments(0)

installDir= Wscript.Arguments(1)

Set objFSO = CreateObject("Scripting.FileSystemObject")

Set objFile = objFSO.OpenTextFile(passvaultBat, ForReading)

strText = objFile.ReadAll

objFile.Close

strNewText = Replace(strText, "<VAULT_DIR>", installDir)

Set objFile = objFSO.OpenTextFile(passvaultBat, ForWriting)

objFile.WriteLine strNewText

objFile.Close
