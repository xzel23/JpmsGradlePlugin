#!/usr/bin/env bash
(cd JpmsGradlePlugin && ./gradlew --no-daemon clean build publishToMavenLocal) \
&& echo "========== JDK9 ==========" \
&& (cd TestJpmsGradlePlugin \
    && ./gradlew --no-daemon clean build --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true \
    && ./gradlew --no-daemon test --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true \
    && ./gradlew --no-daemon run --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true \
    && ./gradlew --no-daemon eclipse --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true) \
&& echo "========== JDK8 ==========" \
&& (cd TestJpmsGradlePlugin-Jdk8 \
    && ./gradlew --no-daemon clean build --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true \
    && ./gradlew --no-daemon test --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true \
    && ./gradlew --no-daemon run --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true \
    && ./gradlew --no-daemon eclipse --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true) \
