Changes
=======

Version 1.1.1
-------------

 - fix issue with compiling tests when no module name is given

Version 1.1
-----------

- fix ConcurrentModificationException when using newer Gradle/JDK versions

Version 1.0.3
-------------

- add debug output to help identify incompatibilities with Gradle versions

Version 1.0.2
-------------

- fix ClassCastException in Javadoc task when using Gradle 6.4+

Version 1.0.1
-------------

- fix ConcurrentModificationException when using Gradle 6.6+
- update Gradle wrapper for compiling under JDK 15

Version 1.0
-----------

no changes

Version 1.0-BETA7
-----------------

- fix debug output (jigsaw.debug)

Version 1.0-BETA6
-----------------

- update gradle to 6.0.1 (plugin now compilable on JDK 13)
- use single instance of gradle wrapper
- enable debug output when jigsaw.debug is set

Version 1.0-BETA5
-----------------

- fix format exception when jpackager not found

Version 1.0-BETA4
-----------------

- JPACKAGER can be either set via environment variable or property with the latter taking precedence

Version 1.0-BETA3
-----------------

- improved error message if jpackageer tool could not be found when trying to create a bundle.
- some small cleanups in the build script.

Version 1.0-BETA2
-----------------

- display error message when module is needed but was not set.

Version 0.9.0
-------------

- __BREAKING__: the former three extension definitions `moduleInfo`, `jlink`, and `bundle` have been merged into `jigsaw` since some attributes had to be defined redundantly.

Version 0.8.3
-------------

- FIX: Bundle task fails with message "could not delete output folder"
- Added a PowerShell build script to build the plugin on windows.
- Use `--no-daemon` flag when building the plugin to avoid running the test build with an old plugin version.

Version 0.8.2
-------------

- *BREAKING:* `bundle.appClass` has been renamed to `bundle.main`.
- Introduced new bundle type 'installer' which is an alias for the current platform's default installer bundle type. This makes it obsolete to change the installer type in build.gradle when compiling on/for another platform.
- When creating bundles, default values for the attributes in the jlink-extension are determined based on the values given in the bundle-extension. This means it's now optional to define jlink settings in build.gradle.

Version 0.8.1
-------------

- The location of the `jpackager` tool can be specified when calling gradle like this: `gradle bundle -DPATH_TO_JPACKAGER=/path/to/jpackager/jpackager`.

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
