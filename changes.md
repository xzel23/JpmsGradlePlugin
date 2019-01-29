Changes
=======

Version 0.8.1
-------------

- The location of the `jüackager` tool can be specified when calling gradle like this: `gradle bundle -DPATH_TO_JPACKAGER=/path/to/jpackager/jpackager`.

Version 0.8.0
-------------

- FEATURE: create application images and installers for your modularized  JavaFX project with the new `bundle` task. Have a look at the FxBrowser project for an example.
- BREAKING: renamed `jlink.module` to `jlink.mainModule` because of incompatibilities
- `jlink.compression`now defaults to `2`

Version 0.7.1
-------------
- BUGFIX: plugin did not modify the module path correctly when it was loaded with the new plugin mechanism.

Version 0.7
-----------
- I finally have multi project JPMS builds workingg with the eclipse plugin. I recommend that you do *not* use the Buildship plugin becuase it always messes up the module path again. Instead use the eclipse plugin together with this plugin and run the tasks `gradle cleanEclipse eclipse` on your project to generate an eclipse project configuration. You can repeat it later and then refresh your project in eclipse after gradle completes.

Version 0.6
-----------
- Don't use java.util.spi.ToolProvider directly. The plugin can now be compiled under JDK 8! This also should avoid Exceptions when building Java 8 projects on Java 8 that do not include module-info.java (aka projects that don't need this plugin). It's my first take on making GitLab AutoDevOps happy. Background: I use this plugin in a project that's hosted on GitLab. Even though everything compiles just fine (I have provided a JDK 11 Dockerfile), when running tests, Gradle is executed under Java 8 and supposedly will spawn test runners to use Java 8 - whatever the reason might be.

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
