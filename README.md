# JpmsGradlePlugin
This plugin adds some support for the Java Platform Module System (JPMS) to gradle builds.

## Applying the plugin

```
buildscript {
  repositories {
    maven { url  "https://dl.bintray.com/dua3/public" }
  }  
  dependencies {
    classpath group: 'com.dua3.gradle.jpms', name: 'JpmsGradlePlugin', version: '0.2.1'
  }
}

apply plugin: 'com.dua3.gradle.jpms'
```

## Task `moduleInfo` for pre-Java 9 builds

If a `module-info.java` is present in a Java 8 build, the file is removed from the source set and instead compiled in Java 9 compatibility mode. This makes it possible to use the same Jar file in Java 8 and moduarized Java 9+ buillds.

By default, a multi-release jar is created, so that the resulting jar should be compatible with i.e. Android.

This task is added automatically when applying the plugin.

## Task `jlink` to create standalone apps

To create standalone applications as described in Steve Perkin's [blog](https://steveperkins.com/using-java-9-modularization-to-ship-zero-dependency-native-apps/), you can use the `jlink` task like this:

    buildscript {
        repositories {
            maven { url  "https://dl.bintray.com/dua3/public" }
        }  
        dependencies {
            classpath group: 'com.dua3.gradle.jpms', name: 'JpmsGradlePlugin', version: '0.2.1'
        }
    }
    
    jlink {
        module = 'cli'
        main = 'cli.Main'
        application = 'cli'
        compress = 2
    }
