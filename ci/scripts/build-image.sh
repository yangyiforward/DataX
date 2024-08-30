#!/bin/bash

#######################################################################################
###  usage: /bin/bash build-image.sh {ENVIRONMENT} {ARTIFACTORY_REPOSITORY}         ###
#######################################################################################

set -e

if [ $# -lt 2 ]; then
   echo "usage: /bin/bash build-image.sh {ENVIRONMENT} {ARTIFACTORY_REPOSITORY}"
   exit 1;
fi

echo '##########################################################################'
echo 'build-image.sh start'
echo '##########################################################################'

echo 'check build params'
echo '##########################################################################'

if [ -f mvnw ]; then
    TARGET_FOLDER=./target
else
    TARGET_FOLDER=./build/libs
fi

ENVIRONMENT=$1
ARTIFACTORY_REPOSITORY=$2
IMAGE_BASE=repo.cicc.com.cn/$ARTIFACTORY_REPOSITORY/

if [ "x$GIT_BRANCH" = "x" ]; then
    GIT_BRANCH=`git rev-parse --abbrev-ref HEAD | awk -F '/' '{print $NF}'`
else
    GIT_BRANCH=`echo $GIT_BRANCH | awk -F '/' '{print $NF}'`
fi

CURR_DATE=`date "+%Y%m%d%H%M%S"`
COMMIT_ID=`git log -1 --abbrev=8 --pretty=format:%h`
IMAGE_FULL_NAME=${IMAGE_BASE,,}${APP_NAME,,}/${ENVIRONMENT,,}':'${GIT_BRANCH,,}-${CURR_DATE,,}-${COMMIT_ID,,}
echo ENVIRONMENT=${ENVIRONMENT}
echo GIT_BRANCH=${GIT_BRANCH}
echo COMMIT_ID=${COMMIT_ID}
echo IMAGE_FULL_NAME=${IMAGE_FULL_NAME}
echo '##########################################################################'

echo 'build project'
echo '##########################################################################'
/bin/bash ci/scripts/build-jar.sh

if [ $? != 0 ]; then
    exit 1;
fi

echo '##########################################################################'

echo 'build image'
echo '##########################################################################'

SERVICE_PACKAGE_NAME=$(cat ./${TARGET_FOLDER}/SERVICE_PACKAGE_NAME)


if [ "x${BUILD_PLATFORMS}" = "x" ]; then
    BUILD_PLATFORMS="linux/amd64"
fi

if [ "${BUILD_PLATFORMS}" = "linux/amd64" ]; then
    docker build -f ci/images/Dockerfile -t ${IMAGE_FULL_NAME} --build-arg SERVICE_PACKAGE_NAME=${SERVICE_PACKAGE_NAME} .
elif [ $(echo "${BUILD_PLATFORMS}" | sed "s#[,;]# #g" | awk '{print NF}') -gt 1 ]; then
    for PLATFORM in $(echo "${BUILD_PLATFORMS}" | sed "s#[,;]# #g" | awk '{for( i=1; i<=NF; i++) print $i}'); do
        echo "########## build platform ${PLATFORM} ##########"
        docker buildx build --platform "${PLATFORM}" -f ci/images/Dockerfile -t ${IMAGE_FULL_NAME}-$(echo "${PLATFORM}" | sed "s#/#-#g") --build-arg SERVICE_PACKAGE_NAME=${SERVICE_PACKAGE_NAME} . --load
    done
else
    docker buildx build --platform "$BUILD_PLATFORMS" -f ci/images/Dockerfile -t ${IMAGE_FULL_NAME} --build-arg SERVICE_PACKAGE_NAME=${SERVICE_PACKAGE_NAME} . --load
fi


if [ $? != 0 ]; then
    echo "docker build 失败"
    exit 1;
fi

echo ${IMAGE_FULL_NAME} > ./${TARGET_FOLDER}/IMAGE_FULL_NAME


echo '##########################################################################'

echo 'build-image.sh end'
echo '##########################################################################'
