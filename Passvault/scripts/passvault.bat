ECHO off
TITLE Passvault

CD <VAULT_DIR>\.passvault

SET LIB=<VAULT_DIR>\.passvault\lib\*

:: registration server url
SET REG_SERVER=passvault.erikdevelopernot.net:8443

:: local database name
SET DB_NAME=pass_vault

:: storage type, either 'json' or 'file'
SET DB_TYPE=json

:: if dadtabase type is cbl purge deleted accounts on startup, [ture/false], defaults to false
SET PURGE_DELETES=false

:: verbose logging for syncing
::SET SYNC_DEBUG=debug

java -cp %LIB% -D"java.util.logging.config.file=logging.properties" -D"com.passvault.data.file=.\data\data.json" -D"com.passvault.register.server=%REG_SERVER%" -D"com.passvault.store.purge=%PURGE_DELETES%" -D"com.passvault.sync.logging=%SYNC_DEBUG%" com.passvault.tools.PasswordVault %DB_NAME% %DB_TYPE% 2> err.log
