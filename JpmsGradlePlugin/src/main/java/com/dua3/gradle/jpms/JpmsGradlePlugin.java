/*
 *   Copyright 2018 Axel Howind
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.dua3.gradle.jpms;

import com.dua3.gradle.jpms.task.Bundle;
import com.dua3.gradle.jpms.task.JLink;
import com.dua3.gradle.jpms.task.ModuleInfoJava;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.eclipse.GenerateEclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.Classpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpmsGradlePlugin implements Plugin<Project> {

	static final String ENV_DEBUG = "DEBUG_JPMS_GRADLE_PLUGIN";

	@Internal
	static String pluginname = JpmsGradlePlugin.class.getSimpleName();

	@Internal
	static boolean debug = Boolean.getBoolean(ENV_DEBUG);

	public JpmsGradlePlugin() {
		trace("plugin loaded");
	}

	public static void trace(String fmt, Object... args) {
		trace(debug, String.format(fmt, args));
	}

	public static void trace(boolean dbg, Object obj) {
		String msg = String.valueOf(obj);
		if (dbg || debug) {
			System.out.println("[" + pluginname + "] " + msg);
		}
	}

	public static void trace(boolean dbg, String fmt, Object... args) {
		trace(dbg, String.format(fmt, args));
	}

	public static void trace(Object obj) {
		String msg = String.valueOf(obj);
		if (debug) {
			System.out.println("[" + pluginname + "] " + msg);
		}
	}

	public static boolean isGradleVersionAtLeast(Project project, int major, int minor) {
		String[] versionParts = project.getGradle().getGradleVersion().split("\\.");
		int gradleMajor = Integer.parseInt(versionParts[0]);
		int gradleMinor = versionParts.length>1 ? Integer.parseInt(versionParts[1]) : 0;
		trace("Gradle version major=%d, minor=%d", gradleMajor, gradleMinor);
		return gradleMajor>major || (gradleMajor==major && gradleMinor>=minor);
	}

	/**
	 * Applies this plugin to the given Gradle project
	 * 
	 * @param project The Gradle project
	 */
	@Override
	public void apply(Project project) {
		if (!isGradleVersionAtLeast(project, 4,6)) {
			project.getLogger().warn("Plugin needs Gradle version 4.6 or above");
		}

		JavaVersion javaVersion = JavaVersion.current();
		trace("java version: %s", javaVersion);

		if (!javaVersion.isJava9Compatible()) {
			project.getLogger().warn("Plugin needs Java version 9 or above, current version is: {}", javaVersion);
		}

		trace("applying plugin %s", pluginname);

		// create extension
		createJigsawExtension(project);

		// create and automatically add moduleInfo task
		ModuleInfoJava moduleInfo = addModuleInfoTask(project);

		// move dependencies from classpath to modulepath
		moveDependenciesToModulePath(project, moduleInfo);

		// do the same for the eclipse classpath
		moveEclipseDependenciesToModulePath(project);

		// add tasks to project
		addJLinkTask(project);
		addDeployTasks(project);
	}

	private void moveDependenciesToModulePath(Project project, ModuleInfoJava moduleInfo) {
		trace("moveDependenciesToModulePath");

		JigsawExtension jigsaw = (JigsawExtension) project.getExtensions().getByName("jigsaw");

		project.afterEvaluate(p -> {
			// move dependencies to modulle path
			p.getTasks().withType(JavaCompile.class).stream().forEach(task -> {
				boolean isTest = task.getName().contains("Test");

				if (!isTest) {
					trace("%s dependsOn %s", task, moduleInfo);
					task.dependsOn(moduleInfo);

					task.doFirst(t -> {
						JavaVersion version = JavaVersion.toVersion(task.getTargetCompatibility());
						trace("task %s, target compatibility: %s", task, version);

						if (version.isJava9Compatible()) {
							trace("moving entries from classpath to modulepath for task %s", task);
							CompileOptions options = task.getOptions();
							List<String> compilerArgs = new ArrayList<>(options.getAllCompilerArgs());
							compilerArgs.add("--module-path");
							compilerArgs.add(task.getClasspath().getAsPath());
							options.setCompilerArgs(compilerArgs);
							task.setClasspath(p.files());
						}
					});
				} else {
					task.doFirst(t -> {
						JavaVersion version = JavaVersion.toVersion(task.getTargetCompatibility());
						trace("task %s, target compatibility: %s", task, version);

						if (version.isJava9Compatible()) {
							trace("patching module system for test task %s", task);
							CompileOptions options = task.getOptions();
							List<String> compilerArgs = new ArrayList<>(options.getAllCompilerArgs());
							compilerArgs.add("--module-path");
							compilerArgs.add(task.getClasspath().getAsPath());
							if (jigsaw.hasModule()) {
								compilerArgs.add("--patch-module");
								compilerArgs.add(String.format("%s=%s", jigsaw.getModule(), "src"));
								compilerArgs.add("--add-modules");
								compilerArgs.add(jigsaw.getTestLibraryModule());
								compilerArgs.add("--add-reads");
								compilerArgs.add(String.format("%s=%s", jigsaw.getModule(), jigsaw.getTestLibraryModule()));
							}
							options.setCompilerArgs(compilerArgs);
						}
					});
				}
			});
		});
	}

	@SuppressWarnings("unchecked")
	private void moveEclipseDependenciesToModulePath(Project project) {
		trace("moveEclipseDependenciesToModulePath");
		project.afterEvaluate(p -> {
			p.getTasks().withType(GenerateEclipseClasspath.class).forEach(task -> {
				JpmsGradlePlugin.trace("eclipse task found");
				task.doFirst(t -> {
					task.getClasspath().getFile().getWhenMerged().add(arg -> {
						JpmsGradlePlugin.trace("moving classpath entries to module path ('-->' = yes, '   ' = no) ...");
						Classpath cp = (Classpath) arg;
						cp.getEntries().stream().forEach(e -> {
							if (e instanceof AbstractClasspathEntry) {
								AbstractClasspathEntry entry = (AbstractClasspathEntry) e;
	
								String kind = entry.getKind();
	
								final boolean move;
								switch (kind) {
								case "src":
									// I have observed that paths for project dependencies in multi-project builds
									// start with a dash
									move = entry.getPath().startsWith("/");
									break;
								case "con":
								case "lib":
									move = true;
									break;
								default:
									move = false;
									break;
								}
	
								if (move) {
									JpmsGradlePlugin.trace("--> [" + kind + "] " + entry.getPath());
									Map<String, Object> entryAttributes = entry.getEntryAttributes();
									entryAttributes.put("module", true);
								} else {
									JpmsGradlePlugin.trace("    [" + kind + "] " + entry.getPath());
								}
							}
						});
					});
				});
			});
		});
	}

	private void createJigsawExtension(Project project) {
		trace("creating jigsawInfo extension");
		project.getExtensions().create("jigsaw", JigsawExtension.class);
	}

	private ModuleInfoJava addModuleInfoTask(Project project) {
		project.getLogger().info("Adding moduleInfo task to project");
		Map<String, Object> optionsModuleInfo = new HashMap<>();
		optionsModuleInfo.put("type", ModuleInfoJava.class);
		ModuleInfoJava moduleInfo = (ModuleInfoJava) project.task(optionsModuleInfo, "moduleInfo");
		return moduleInfo;
	}

	private void addJLinkTask(Project project) {
		project.getLogger().info("Adding jlink task to project");

		trace("creating jlink task");
		Map<String, Object> optionsJLink = new HashMap<>();
		optionsJLink.put("type", JLink.class);
		JLink jlink = (JLink) project.task(optionsJLink, "jlink");
		jlink.dependsOn("build");
	}

	private void addDeployTasks(Project project) {
		project.getLogger().info("Adding deploy tasks to project");

		trace("creating bndle task");
		Map<String, Object> optionsBundle = new HashMap<>();
		optionsBundle.put("type", Bundle.class);
		Bundle bundle = (Bundle) project.task(optionsBundle, "bundle");
		bundle.dependsOn("jlink");
	}

}
