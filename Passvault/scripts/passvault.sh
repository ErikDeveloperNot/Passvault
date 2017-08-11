#!/bin/bash

## set Java executable
#JAVA=

## set Jars directory
LIB=.:./lib/*

## database name
DB=pass_vault

## passvault directory
VAULT_DIR=<VAULT_DIR>/.passvault

## storage type either cbl or file
STORAGE=cbl

## if dadtabase type is cbl purge deleted accounts on startup, [ture/false], defaults to false
PURGE_DELETES=false

## SSL setting
#TRUST_STORE=/opt/ssl/keystores/passvault_store.jks
#TRUST_PASSWORD=passvault

## Sync Debug logging
#SYNC_DEBUG=debug

## registration server
#REG_SERVER=localhost:8443
REG_SERVER=ec2-52-53-254-139.us-west-1.compute.amazonaws.com:8443

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

# check if TRUST_STORE is set, if so add it to the JAVA_OPTS
if [ "$TRUST_STORE" != "" ]
then
  JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=$TRUST_STORE -Djavax.net.ssl.trustStorePassword=$TRUST_PASSWORD"
fi

(
cd $VAULT_DIR &&
exec $JAVA_EX -cp ${LIB_DIR} $JAVA_OPTS -Djava.util.logging.config.file=logging.properties -Dcom.passvault.sync.logging=$SYNC_DEBUG -Dcom.passvault.register.server=$REG_SERVER -Dcom.passvault.store.purge=$PURGE_DELETES com.passvault.tools.PasswordVault $DB $STORAGE 2> err.log
)
