Changes
=======

Version 0.5.1
-------------
 - Set module path for 'run' task if using Gradle`s 'application' plugin.

Version 0.5.0
-------------
- The plugin now is compatible with Gradle 5.0 (tested with 5.0-rc1).
- Fixed Javadoc building with JDK 11.
- Fixed Gradle compatibility check.

Version 0.4.0
-------------
 - The plugin is now compiled in Java 8 compatibility mode. To function properly, it still needs to be executed on Java 9 or above. But now it at least outputs a message about the incompatible Java version (in previous versions, UnsupportedClassVersionError was thrown without further diagnostivs when run with JDK 8). As long as no `module-info.java` is present and no `jlink` task is defined in `build.gradle`, it should now even be possible to build a Jar on JDK 8 when the plugin is applied (even if there's no point in doing so).
