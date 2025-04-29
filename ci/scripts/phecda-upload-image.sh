#!/bin/bash

##########################################################################
###  usage: /bin/bash phecda-upload-image.sh {ARTIFACTORY_REPOSITORY}  ###
##########################################################################

set -e

if [ $# -lt 1 ]; then
   echo "usage: /bin/bash phecda-upload-image.sh {ARTIFACTORY_REPOSITORY}"
   exit 1;
fi

ARTIFACTORY_REPOSITORY=$1

echo '##########################################################################'
echo 'phecda-upload-image.sh start'
echo '##########################################################################'

echo 'build image'
echo '##########################################################################'

/bin/bash ci/scripts/build-image.sh dev ${ARTIFACTORY_REPOSITORY}

if [ $? != 0 ]; then
    echo "build image 失败"
    exit 1;
fi

if [ -f mvnw ]; then
    TARGET_FOLDER=./target
else
    TARGET_FOLDER=./build/libs
fi


IMAGE_FULL_NAME=$(cat ./${TARGET_FOLDER}/IMAGE_FULL_NAME)

echo '##########################################################################'

echo 'push image'
echo '##########################################################################'

if [ "x${BUILD_PLATFORMS}" = "x" ]; then
    BUILD_PLATFORMS="linux/amd64"
fi

if [ $(echo "${BUILD_PLATFORMS}" | sed "s#[,;]# #g" | awk '{print NF}') -eq 1 ]; then
    /usr/install/jfrogv2/jfrog rt dp ${IMAGE_FULL_NAME} ${ARTIFACTORY_REPOSITORY}
    docker rmi ${IMAGE_FULL_NAME}
else
    MANIFESTS=""
    for PLATFORM in $(echo "${BUILD_PLATFORMS}" | sed "s#[,;]# #g" | awk '{for(i=1; i<=NF; i++) print $i}'); do
        echo "########## push platform ${PLATFORM} ##########"
        CURRENT_IMAGE=${IMAGE_FULL_NAME}-$(echo "${PLATFORM}" | sed "s#/#-#g")
        /usr/install/jfrogv2/jfrog rt dp ${CURRENT_IMAGE} ${ARTIFACTORY_REPOSITORY}
        docker rmi ${CURRENT_IMAGE}
        MANIFESTS="${MANIFESTS} ${CURRENT_IMAGE}"
    done

    echo "########## create docker manifest ##########"
    docker manifest create ${IMAGE_FULL_NAME} ${MANIFESTS}
    echo "########## push docker manifest ##########"
    docker manifest push ${IMAGE_FULL_NAME}
    docker manifest rm ${IMAGE_FULL_NAME}
fi


echo '##########################################################################'

echo 'phecda callback'
echo '##########################################################################'
curl -X POST -F "imageName=${IMAGE_FULL_NAME}" -F "crid=${CR_ID}" -F "compileId=${COMPILE_ID}" -F "appName=${APP_NAME}" http://phecda.cicc.group/package-switch/callBack

echo '##########################################################################'

echo 'build.sh end'
echo '##########################################################################'
