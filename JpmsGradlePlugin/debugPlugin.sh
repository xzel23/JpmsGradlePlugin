#!/usr/bin/env bash

# Debug Script to debug plugin in IntelliJ:
# 1. Start this script. It will pause execution until debugger is connected
# 2. Start remote debuuging session in IntelliJ

DIR=`dirname $0`

(
    cd ${DIR}
    ./gradlew build publishToMavenLocal \
    && ( cd ../TestJpmsGradlePlugin && ./gradlew -PDEBUG_JPMS_GRADLE_PLUGIN=true -Dorg.gradle.debug=true --no-daemon test & ) \
    && sleep 5
)
