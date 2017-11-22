#!/bin/bash

echo "Configuring shell Script"
INSTALL_DIR=`pwd`
#sed -e "s|<VAULT_DIR>|${INSTALL_DIR}|g" -i passvault.sh
chmod 755 passvault.sh


function mac_setup {
   echo "Setting up Mac Pasvault.app"
   mkdir -p Passvault.app/Contents/MacOS
   mkdir Passvault.app/Contents/Resources
   mkdir .passvault/data
   cp ./install/mac/vault.icns Passvault.app/Contents/Resources
   cp ./install/mac/info.plist Passvault.app/Contents
   cp ./install/mac/Passvault Passvault.app/Contents/MacOS
   sed -i "" "s|<VAULT_DIR>|${INSTALL_DIR}|g" Passvault.app/Contents/MacOS/Passvault
   echo "Passvault.app setup.."
}

function linux_setup {
   # this works for ubuntu, will need to test others
   echo "Setting up Linux"
   sed -e "s|<VAULT_DIR>|${INSTALL_DIR}|g" -i ./install/linux/passvault.desktop
   chmod 755 ./install/linux/passvault.desktop
   cp ./install/linux/passvault.desktop ./
   cp ./install/linux/passvault.desktop ~/Desktop
   echo "Passvault setup.."
}



OS="`uname`"
case $OS in
  'Linux')
    OS='Linux'
    sed -e "s|<VAULT_DIR>|${INSTALL_DIR}|g" -i passvault.sh
    linux_setup
    ;;
  'Darwin') 
    OS='Mac'
    sed -i "" "s|<VAULT_DIR>|${INSTALL_DIR}|g" passvault.sh
    mac_setup
    ;;
  *) 
    # for everything else for now just setup sh script
    	sed -e "s|<VAULT_DIR>|${INSTALL_DIR}|g" -i passvault.sh
    	echo "To start run ${INSTALL_DIR}/passvault.sh"
    ;;
esac


echo "done"



