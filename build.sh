#!/usr/bin/env bash
(cd JpmsGradlePlugin && ./gradlew --no-daemon clean build publishToMavenLocal) \
&& (cd TestJpmsGradlePlugin && ./gradlew --no-daemon clean build --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true && ./gradlew --no-daemon run) \
&& (cd TestJpmsGradlePlugin && ./gradlew --no-daemon eclipse --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true)
