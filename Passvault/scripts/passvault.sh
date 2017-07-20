#!/bin/bash

## set Java executable
#JAVA=

## set Jars directory
LIB=/Users/user1/Git/Password-Vault/PasswordVault/bin:~/.passvault/libs

## database name
DB=pass_vault

## passvault directory
VAULT_DIR=/opt/tmp/tmp/.passvault

## storage type either cbl or file
STORAGE=cbl

## SSL setting
#TRUST_STORE=/opt/ssl/keystores/passvault_store.jks
#TRUST_PASSWORD=passvault

## Sync Debug logging
#SYNC_DEBUG=debug

## registration server
REG_SERVER=localhost:8443

## extra java options
#JAVA_OPTS="-Djavax.net.debug=all"

################
# end variables
################

if [[ ! -e $VAULT_DIR ]]
then
  mkdir $VAULT_DIR
fi

# check if JAVA was set, if so use it; if not see if it is in path, if not report/exit
if [ "$JAVA" != "" ]
then
  JAVA_EX=$JAVA
else
  if hash java 2> /dev/null
  then
    JAVA_EX=`which java` 
  else
    echo "Java needs to be installed, set JAVA environment variable or intstall Java"
    exit 1
  fi
fi


# check if LIB was set, if not use default from VAULT_DIR
if [ "$LIB" != "" ]
then
  LIB_DIR=$LIB
else
  LIB_DIR=${VAULT_DIR}/libs
fi

(
cd $VAULT_DIR &&
exec $JAVA_EX -cp .:${LIB_DIR}/* $JAVA_OPTS -Djava.util.logging.config.file=logging.properties -Dcom.passvault.sync.logging=$SYNC_DEBUG -Djavax.net.ssl.trustStore=$TRUST_STORE -Djavax.net.ssl.trustStorePassword=$TRUST_PASSWORD -Dcom.passvault.register.server=$REG_SERVER com.passvault.tools.PasswordVault $DB $STORAGE
)
