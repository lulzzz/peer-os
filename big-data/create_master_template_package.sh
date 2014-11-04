#!/bin/bash
##################################################
# @Author: Furkan Bicak
# @Date: 16.10.2014
# @Parameters:
#   - jenkins_user:
#   - jenkins_ip_address:
# @Returns:
#   - master debian package under /lxc-data/tmpdir directory
# @LastEdit: 17/10/2014-08:54
##################################################

main() {
  # Environment specific variables
  #bridge_name=eth0
  jenkins_user="jenkins"
  jenkins_ip_address="172.16.1.178"  

  # General Variables 
  # These packages should have the latest versions on this machine
  local packages_should_be_installed="subutai-cli,subutai-cli-dev,git,lxc,expect"
  # These packages should be available to be installed on master template
  local packages_should_be_available="ksks-logstash,jmxtrans,subutai-mastertemplate-setup"

  jenkins_machine=$jenkins_user@$jenkins_ip_address


  assert_root_user
  apt-get update
  assert_packages_available $packages_should_be_installed
  assert_packages_available $packages_should_be_available
  install_latest_packages
  destroy_master_template
  create_master_template
  export_master_template
}


assert_root_user() {
  # Make sure only root can run our script
  if [ "$(id -u)" != "0" ]; then
     echo "This script must be run as root" 1>&2
     exit 1
  fi
}


assert_packages_available() {
  if [ -z $1 ]; then
    echo "Please provide the package list to assert_packages_available method!"
    exit 1
  fi
  local packages_should_be_available=$1
  IFS=', ' read -a debian_packages <<< "$packages_should_be_available"
  for debian_package in "${debian_packages[@]}"
  do
    echo "Checking package $debian_package"
    apt-cache show $debian_package > /dev/null 2>&1
    status=$?
    if [ $status == "0" ]; then
      echo "$debian_package exists..."
    else
      echo "Please add package $debian_package to the apt repository first!"
      exit 1
    fi
  done
}


install_latest_packages() {
  echo "installing latest debian packages"
  IFS=', ' read -a debian_packages <<< "$packages_should_be_installed"
  for debian_package in "${debian_packages[@]}"
  do
    echo "Installing latest package of $debian_package"
    apt-get install --assume-yes --force-yes $debian_package
  done
}


destroy_master_template() {
  echo "destroying master template"
/usr/bin/expect<<EOF
spawn subutai master_destroy
expect "Do you wish to destroy master template?"
send -- "Y\r"
expect eof
EOF
  rm -rf /lxc-data/tmpdir/master*
}


create_master_template() {
  echo "creating master template"
  subutai master_create
}


export_master_template() {
  echo "exporting master template's debian package"
  subutai master_export
  echo "Size of master template package is: " `du -hs /lxc-data/tmpdir/master-subutai-template*.deb`
}

main
exit 0
