#!/usr/bin/env bash

if [ $# -gt 1 ] ; then
    echo "ERROR"
    echo "to build and test the plugin: $0"
    echo "to release a new version of the plugin: $0 -Pversion=<version>"
    exit 1
fi

if [ $# -ge 1 ] ; then
    VERSION=$1
else
    VERSION="SNAPSHOT"
fi

echo 
echo "building as version $VERSION"
echo 

VERSION_ARG="-Pversion=${VERSION}"
EXTRA_ARGS="--no-daemon --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true"

(cd JpmsGradlePlugin && ./gradlew ${VERSION_ARG} --no-daemon clean build publishToMavenLocal) \
&& echo "========== JDK9 ==========" \
&& (cd TestJpmsGradlePlugin \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} clean build \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} test \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} run \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} eclipse) \
&& echo "========== JDK8 ==========" \
&& (cd TestJpmsGradlePlugin-Jdk8 \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} clean build \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} test \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} run \
    && ./gradlew ${VERSION_ARG} ${EXTRA_ARGS} eclipse) \
