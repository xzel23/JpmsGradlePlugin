# JpmsGradlePlugin
This plugin adds some support for the Java Platform Module System (JPMS) to gradle builds.

## Task `compileModuleInfo` for pre-Java 9 builds

If a `module-info.java` is present in a Java 8 build, the file is removed from the source set and instead compiled in Java 9 compatibility mode. This makes it possible to use the same Jar file in Java 8 and moduarized Java 9+ buillds.
