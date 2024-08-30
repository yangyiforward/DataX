#!/bin/bash

##########################################################################
###  usage: /bin/bash phecda-upload-jar.sh {ARTIFACTORY_REPOSITORY}    ###
##########################################################################

set -e

if [ $# -lt 1 ]; then
   echo "usage: /bin/bash phecda-upload-jar.sh {ARTIFACTORY_REPOSITORY}"
   exit 1;
fi

ARTIFACTORY_REPOSITORY=$1

if [ "x${ARTIFACTORY_DOMAIN}" = "x" ]; then
    ARTIFACTORY_DOMAIN="repo.cicc.com.cn"
fi

if [ "x${PHECDA_DOMAIN}" = "x" ]; then
    PHECDA_DOMAIN="phecda.cicc.group"
fi

echo '##########################################################################'
echo 'phecda-upload-jar.sh start'
echo '##########################################################################'

echo 'build jar'
echo '##########################################################################'

/bin/bash ci/scripts/build-jar.sh

if [ $? != 0 ]; then
    echo "build jar 失败"
    exit 1;
fi

if [ -f mvnw ]; then
    TARGET_FOLDER=./target
else
    TARGET_FOLDER=./build/libs
fi

SERVICE_PACKAGE_NAME=$(cat ./${TARGET_FOLDER}/SERVICE_PACKAGE_NAME)

echo '##########################################################################'

echo 'push jar'
echo '##########################################################################'

if [ "x$GIT_BRANCH" = "x" ]; then
    GIT_BRANCH=`git rev-parse --abbrev-ref HEAD | awk -F '/' '{print $NF}'`
else
    GIT_BRANCH=`echo $GIT_BRANCH | awk -F '/' '{print $NF}'`
fi

CURR_DATE=`date "+%Y%m%d%H%M%S"`
COMMIT_ID=`git log -1 --abbrev=8 --pretty=format:%h`
#JAR_FULL_NAME=${APP_NAME}-${GIT_BRANCH}-${CURR_DATE}-${COMMIT_ID}.jar
JAR_FULL_NAME=${APP_NAME}-${GIT_BRANCH}-${CURR_DATE}-${COMMIT_ID}.tar.gz

cp ${SERVICE_PACKAGE_NAME} ${TARGET_FOLDER}/${JAR_FULL_NAME}

cd ${TARGET_FOLDER}
/usr/install/jfrogv2/jfrog rt u ${JAR_FULL_NAME} ${ARTIFACTORY_REPOSITORY}/${APP_NAME}/uat/ --recursive=false

if [ $? != 0 ]; then
    cd ..
    echo "JAR 上传失败"
    exit 1;
fi
cd ..

echo '##########################################################################'

echo 'phecda callback'
echo '##########################################################################'
curl -X POST -F "imageName=https://${ARTIFACTORY_DOMAIN}/artifactory/${ARTIFACTORY_REPOSITORY}/${APP_NAME}/uat/${JAR_FULL_NAME}" -F "crid=${CR_ID}" -F "compileId=${jobId}" -F "appName=${APP_NAME}" http://${PHECDA_DOMAIN}/package-switch/callBack

echo '##########################################################################'

echo 'build.sh end'
echo '##########################################################################'
