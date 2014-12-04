#!/bin/bash
set -e
. /var/lib/jenkins/jobs/master.get_branch_repo/workspace/cli/debian/pack-funcs
#generic pack script for all cli subutai scripts
productName=$1

# 1) Check if the version is changed or not. If not changed, dont create a new debian.
checkPackageVersion $productName
# 4) Create the Debian package
generateDebianPackage $productName
# 5) Create the Wrapper Repo Debian Package
generateRepoPackage $productName