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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.eclipse.GenerateEclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.Classpath;

import com.dua3.gradle.jpms.task.JLink;
import com.dua3.gradle.jpms.task.JLinkExtension;
import com.dua3.gradle.jpms.task.ModuleInfoExtension;
import com.dua3.gradle.jpms.task.ModuleInfoJava;

public class JpmsGradlePlugin implements Plugin<Project> {

	static final String ENV_DEBUG = "DEBUG_JPMS_GRADLE_PLUGIN";

	@Internal
	static String pluginname = JpmsGradlePlugin.class.getSimpleName();

	@Internal
	static boolean debug = Boolean.getBoolean(ENV_DEBUG);

	public static void trace(String fmt, Object... args) {
		trace(String.format(fmt, args));
	}

	public static void trace(Object obj) {
		String msg = String.valueOf(obj);
		if (debug) {
			System.out.println("[" + pluginname + "] " + msg);
		}
	}

	private boolean isCompatible(String version) {
		if ("4.6".compareTo(version) <= 0) {
			return true;
		}

		// version is a String (for older gradle versions it's all we have), so check
		// for Gradle >= 4.10 manually
		if (version.startsWith("4.1") && version.length() >= 4 && Character.isDigit(version.charAt(3))) {
			return true;
		}

		return false;
	}

	/**
	 * Applies this plugin to the given Gradle project
	 * 
	 * @param project The Gradle project
	 */
	@Override
	public void apply(Project project) {
		Gradle gradle = project.getGradle();
		String gradleVersion = gradle.getGradleVersion();
		trace("gradle version: %s", gradleVersion);

		if (!isCompatible(gradleVersion)) {
			project.getLogger().warn("Unknown Gradle version: {}", gradleVersion);
			project.getLogger().warn("Plugin needs Gradle version 4.6 or above");
		}

		JavaVersion javaVersion = JavaVersion.current();
		trace("java version: %s", javaVersion);

		if (!javaVersion.isJava9Compatible()) {
			project.getLogger().warn("Plugin needs Java version 9 or above, current version is: {}", javaVersion);
		}

		trace("applying plugin %s", pluginname);

		// create and automatically add moduleInfo task
		ModuleInfoJava moduleInfo = addModuleInfoTask(project);

		// move dependencies from classpath to modulepath
		moveDependenciesToModulePath(project, moduleInfo);

		// do the same for the eclipse classpath
		moveEclipseDependenciesToModulePath(project);

		// add 'jlink' task
		addJLinkTask(project);
	}

	private void moveDependenciesToModulePath(Project project, ModuleInfoJava moduleInfo) {
		trace("moveDependenciesToModulePath");
		project.afterEvaluate(p -> {
			// move dependencies to modulle path
			p.getTasks().withType(JavaCompile.class).stream().forEach(task -> {
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
									// if have observed that paths for project dependencies in multi-project builds
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
		

	private ModuleInfoJava addModuleInfoTask(Project project) {
		project.getLogger().info("Adding moduleInfo task to project");

		trace("creating moduleInfo extension");
		project.getExtensions().create("moduleInfo", ModuleInfoExtension.class);

		Map<String, Object> optionsModuleInfo = new HashMap<>();
		optionsModuleInfo.put("type", ModuleInfoJava.class);
		ModuleInfoJava moduleInfo = (ModuleInfoJava) project.task(optionsModuleInfo, "moduleInfo");
		return moduleInfo;
	}

	private void addJLinkTask(Project project) {
		project.getLogger().info("Adding jlink task to project");

		trace("creating jlink extension");
		project.getExtensions().create("jlink", JLinkExtension.class);

		trace("creating jlink task");
		Map<String, Object> optionsJLink = new HashMap<>();
		optionsJLink.put("type", JLink.class);
		JLink jlink = (JLink) project.task(optionsJLink, "jlink");
		jlink.dependsOn("build");
	}

}
