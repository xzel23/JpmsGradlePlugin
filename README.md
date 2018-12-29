# JpmsGradlePlugin
This plugin adds some support for the Java Platform Module System (JPMS) to gradle builds.

## Applying the plugin

```
    plugins {
        id "com.dua3.gradle.jpms" version "0.6"
    }
```

## Task `moduleInfo` to create modularised Jars that are compatible with Java 8

If a `module-info.java` is present in a Java 8 build, the file is removed from the source set and instead compiled in Java 9 compatibility mode. This makes it possible to use the same Jar file in Java 8 and moduarized Java 9+ buillds.

This task is added automatically when applying the plugin.

In theory, a `module-info.java` file should be ignored when a Jar is used in a pre-Java 9 build. However some tools (**SpotBugs** < 3.1.3 being an example) will fail if a module-info is present in a library. As a workaround, you can create a multi-release jar like this:

```
    moduleInfo {
        multiRelease = true
    }
```

I have also heard but not confirmed myself that the **android** toolchain chokes if a module-info is present. I'd be glad to hear if setting `multiRelease = true` solves this.

**Eclipse** (tested in Oxygen.3a) still has problems when a multi-release jar is used.

## Task `jlink` to create standalone apps

To create standalone applications as described in Steve Perkin's [blog](https://steveperkins.com/using-java-9-modularization-to-ship-zero-dependency-native-apps/), you can use the `jlink` task like this:

```
    jlink {
        module = 'cli'
        main = 'cli.Main'
        application = 'cli'
        compress = 2
    }
```
## Fixing Gradle´s 'javadoc' task

When creating JDK-8 compatible jars, `module-info.java` is removed from javadoc input to prevent errors.

## Removing `module-info.java` from eclipse project if Gradle`s 'eclipse' plugin is used

Eclipse as of version 2018-09 still has problems with JDK 11 and modularized builds. As a workaround, `module-info.java` will be ignored when creating an eclipse project. You can use eclipse for development, but you should however use gradle from the command line to create artefacts for distribution.

## Configuring the module path for the 'run' task

If you use the 'application' gradle plugin and the project contains `module-info.java`, the option `--module-path` will be automatically set for the gradle 'run' task.

## Example project

Have a look at the [fxbrowser](https://github.com/xzel23/fxbrowser) project for an example on how to build a non-trivial standalone application using the plugin.
