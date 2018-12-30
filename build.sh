#!/bin/bash
(cd JpmsGradlePlugin && ./gradlew clean build publishToMavenLocal) \
&& (cd TestJpmsGradlePlugin && ./gradlew clean build --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true && ./gradlew run) \
&& (cd TestJpmsGradlePlugin && ./gradlew eclipse --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true)
