#!/usr/bin/env bash

# get version from positional argument
if [ $# -gt 1 ] ; then
    echo "ERROR"
    echo "to build and test the plugin: $0"
    echo "to release a new version of the plugin: $0 <version>"
    exit 1
fi

if [ $# -eq 1 ] ; then
    VERSION=$1
    TAG="v${VERSION}"
    RELEASE=1

    # check that version matches regex
    REGEX=^[0-9]+\(\\.\[0-9\]+\)*\[a-z\]?\(-\[A-Za-z0-9\]+\)?\$
    if ! [[ "$VERSION" =~ ${REGEX} ]]; then
        echo "ERROR - version '${VERSION}' does not match regex ${REGEX}"
        exit 1
    fi

    # check if that version has already be released
    if [ $(git tag -l "${TAG}") ]; then
        echo "ERROR - version '${VERSION}' has already been tagged with tag '${TAG}'"
        exit 1
    fi
else
    VERSION="SNAPSHOT"
    RELEASE=0
    echo "building a snapshot version. To release a new version of the plugin, use: $0 <version>"
fi

# start the build
echo 
echo "building as version $VERSION"
echo 

VERSION_ARG="-Pversion=${VERSION}"
EXTRA_ARGS="--no-daemon --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true"

(cd JpmsGradlePlugin && ../gradlew ${VERSION_ARG} --no-daemon clean build publishToMavenLocal) \
&& echo "========== JDK9 ==========" \
&& (cd TestJpmsGradlePlugin \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} clean build \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} test \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} run \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} eclipse) \
&& echo "========== JDK8 ==========" \
&& (cd TestJpmsGradlePlugin-Jdk8 \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} clean build \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} test \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} run \
    && ../gradlew ${VERSION_ARG} ${EXTRA_ARGS} eclipse) \
|| exit

if [ $RELEASE -eq 1 ] ; then
    echo
    read -p "Release plugin as version ${VERSION} ('YES' to continue)? " -r
    echo  
    if [[ $REPLY =~ ^YES|yes$ ]] ; then
        (cd JpmsGradlePlugin && ./gradlew ${VERSION_ARG} --no-daemon publishPlugins) \
        && git tag ${TAG} \
        && echo "tagged and released" \
        || exit 1
    else
        echo "not tagged and released"
    fi
fi
