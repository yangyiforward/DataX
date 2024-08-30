#!/bin/bash

##########################################################################
###  usage: /bin/bash build-jar.sh                                     ###
##########################################################################

set -e

echo '##########################################################################'
echo 'build-jar.sh start'
echo '##########################################################################'

echo 'check environment variables'
echo '##########################################################################'
if [ "x$JAVA_HOME" = "x" ]; then
    echo "JAVA_HOME is not set"
    exit 1;
fi

echo JAVA_HOME="$JAVA_HOME"
echo '##########################################################################'
echo 'build jar'
echo '##########################################################################'
if [ -f mvnw ]; then
    /bin/bash mvnw clean package -Dmaven.test.skip assembly:assembly
    TARGET_FOLDER=./target
elif [ -f gradlew ]; then
    /bin/bash gradlew clean build -x test
    TARGET_FOLDER=./build/libs
else
    echo "未找到 mvn wrapper 或 gradle wrapper 可执行文件, 无法完成构建!"
    exit 1;
fi

#SERVICE_PACKAGE_NAME=`ls $TARGET_FOLDER/*.jar | grep -v original | grep -v sources`
SERVICE_PACKAGE_NAME=$(ls ${TARGET_FOLDER}/*.tar.gz | grep -v "sources.jar")

if [ "x$SERVICE_PACKAGE_NAME" = "x" ]; then
    echo "build failed"
    exit 1;
fi

echo ${SERVICE_PACKAGE_NAME} > ./${TARGET_FOLDER}/SERVICE_PACKAGE_NAME

echo '##########################################################################'
echo 'build-jar.sh end'
echo '##########################################################################'
