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
package com.dua3.gradle.jpms.task;

import com.dua3.gradle.jpms.JigsawExtension;
import com.dua3.gradle.jpms.JpmsGradlePlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ModuleInfoJava extends DefaultTask {

	@TaskAction
	public void compileModuleInfoJava() {
		Project project = getProject();

		JigsawExtension jigsaw = (JigsawExtension) project.getExtensions().getByName("jigsaw");

		// Flag indicating whether module defs have to be removed from javadoc input (use AtomicBoolean because primitive cannot be set in lambda)
		AtomicBoolean hasModuleInfos = new AtomicBoolean(false);

		// iterate over all JavaCompile tasks
		List<JavaCompile> javaCompileTasks = new ArrayList<>(project.getTasks().withType(JavaCompile.class));
		
		javaCompileTasks
        	.forEach(task -> {
				JpmsGradlePlugin.trace(jigsaw.isDebug(), "%s", task);

				boolean isTest = task.getName().contains("Test");
				JpmsGradlePlugin.trace(jigsaw.isDebug(), "%s is a test task: %s", task, isTest);

				// bail out if separate compilation is not needed for this task
				boolean separateModules = isSeparateCompilationOfModuleDefNeeded(task);
                if (!separateModules || isTest) {
                    JpmsGradlePlugin.trace(jigsaw.isDebug(), "task %s has target compatibility %s or is test task, separate compilation not needed", task, task.getTargetCompatibility());
                    return;
                }

                // separate module definitions from other sources
                FileCollection moduleDefs =
                		task.getSource().filter(f -> f.getName().equals("module-info.java"));
                JpmsGradlePlugin.trace(jigsaw.isDebug(), "module definitions: %s", moduleDefs.getFiles());

                FileCollection sources =
                		task.getSource().filter(f -> !f.getName().equals("module-info.java"));
                JpmsGradlePlugin.trace(jigsaw.isDebug(), "other sources: %s", sources.getFiles());

                // set needJavadocFix to true if needed
                hasModuleInfos.compareAndSet(false, !moduleDefs.isEmpty());

                // before executing JavaCompile task, remove module definitions from task input
        		task.doFirst(t -> {
                    JpmsGradlePlugin.trace(jigsaw.isDebug(), "removing module definitions from task input");
                    task.setSource(sources.getAsFileTree());
        		});

                // at last, compile the module definition with Java 9 compatibility
        		task.doLast(t -> {
        			if (moduleDefs.isEmpty()) {
                        JpmsGradlePlugin.trace(jigsaw.isDebug(), "task has no module def");
        				return;
        			}

                    JpmsGradlePlugin.trace(jigsaw.isDebug(), "compiling module definitions for task");

                    // define directories
                    String classesDir = t.getOutputs().getFiles().getFiles().stream()
							.peek(f -> JpmsGradlePlugin.trace(jigsaw.isDebug(), "inspecting output file: %s", f))
							.map(File::getPath)
							.reduce((s1,s2) -> {
								if (!s2.equals(s1)) {
									JpmsGradlePlugin.trace(jigsaw.isDebug(), "output directory already set to %s. ignoring directory: %s", s1, s2);
								}
								return s1;
							}).orElseThrow(() -> new IllegalStateException("could not determine output directory"));
                    String modulepath = classesDir+File.pathSeparator+task.getClasspath().getAsPath();
					JpmsGradlePlugin.trace(jigsaw.isDebug(), "module-path: %s", modulepath.replaceAll(File.pathSeparator, "\n"));

					// prepare compiler arguments
                    List<String> compilerArgs = new LinkedList<>();
                    Collections.addAll(compilerArgs, "--release", "9");
                    Collections.addAll(compilerArgs, "--module-path", modulepath);
                    Collections.addAll(compilerArgs, "-d", classesDir.toString());
                    moduleDefs.getFiles().stream().map(File::toString).forEach(compilerArgs::add);
					JpmsGradlePlugin.trace(jigsaw.isDebug(), "compiler arguments: %s", compilerArgs);

					// start compilation
					TaskHelper.runTool(TaskHelper.JAVAC, project, compilerArgs);
        		});
        	});

		// fix Javadoc inputs (because javadoc will throw an exception for module definitions in Java 8 compatibility)
	    JpmsGradlePlugin.trace(jigsaw.isDebug(), "fixing javadoc");
    	fixJavadocTasks(hasModuleInfos.get());

		// setup module path for run tasks
		JpmsGradlePlugin.trace(jigsaw.isDebug(), "fixing run tasks");
		fixRunTasks(hasModuleInfos.get());

		// if target is a multi-release jar, move module definitions into the corresponding subfolder
        if (jigsaw.isMultiRelease()) {
            JpmsGradlePlugin.trace(jigsaw.isDebug(), "creating multi-release jar");
            multiReleaseJar();
        }
    }

	private static boolean isSeparateCompilationOfModuleDefNeeded(JavaCompile task) {
		String targetCompatibility = task.getTargetCompatibility();
		boolean needSeparateModuleDef;
		switch (targetCompatibility) {
		case "1.1":
		case "1.2":
		case "1.3":
		case "1.4":
		case "1.5":
		case "1.6":
		case "1.7":
		case "1.8":
		case "1":
		case "2":
		case "3":
		case "4":
		case "5":
		case "6":
		case "7":
		case "8":
			needSeparateModuleDef = true;
			break;
		default:
			needSeparateModuleDef = false;
			break;
		}
		return needSeparateModuleDef;
	}

	private void fixJavadocTasks(boolean removeModuleInfo) {
		Project project = getProject();

		JigsawExtension jigsaw = (JigsawExtension) project.getExtensions().getByName("jigsaw");

		// iterate over all Javadoc tasks
        project.getTasks()
        	.withType(Javadoc.class)
        	.forEach(task -> {
				JpmsGradlePlugin.trace(jigsaw.isDebug(), "%s", task);

				if (removeModuleInfo) {
					// remove module-info.java from input
					FileCollection inputs =
								task.getSource().filter(f -> !f.getName().equals("module-info.java"));

						// before executing JavaCompile task, remove module definitions from task input
						task.doFirst(t -> {
							JpmsGradlePlugin.trace(jigsaw.isDebug(), "remove module def from javadoc input: %s", t);
							task.setSource(inputs.getAsFileTree());
						});
				} else {
					// set the module path for Javadoc
					File moduleDir = new File(task.getOutputs().getFiles().getSingleFile(), task.getClasspath().getAsPath());

					// Support for setting the module path for Javadoc was added in Gradle 6.4.
					// For older Gradle versions, the module path can be set by appending a String option,
					// however that leads to a ClassCastException in Gradle 6.4+, so we need different
					// approaches dependingon the Gradle version.
					if (JpmsGradlePlugin.isGradleVersionAtLeast(project, 6, 4)) {
						List<File> modulePath = new ArrayList<>();
						modulePath.add(moduleDir);
						JpmsGradlePlugin.trace(jigsaw.isDebug(), "module-path: %s", modulePath);
						CoreJavadocOptions options = (CoreJavadocOptions) task.getOptions();
						options.setModulePath(modulePath);
					} else {
						String modulePath = moduleDir.getAbsolutePath();
						CoreJavadocOptions options = (CoreJavadocOptions) task.getOptions();
						JpmsGradlePlugin.trace(jigsaw.isDebug(), "using Gradle pre-6.4 workaround to set module-path");
						JpmsGradlePlugin.trace(jigsaw.isDebug(), "module-path: %s", modulePath);
						options.addStringOption("-module-path", modulePath);						
					}
				}
        	});
	}

	/** Java command line switch for setting module path */
	private static final String RUN_MODULE_PATH = "--module-path";

	private void fixRunTasks(boolean usesModules) {
		if (!usesModules) {
			return;
		}

		Project project = getProject();

		JigsawExtension jigsaw = (JigsawExtension) project.getExtensions().getByName("jigsaw");

		// iterate over all JavaExec tasks
        project.getTasks()
        	.withType(JavaExec.class)
        	.forEach(task -> {
				JpmsGradlePlugin.trace(jigsaw.isDebug(), "%s", task);

				// determine class path entries and prepare module path
				String classesDir = task.getClasspath().getAsPath();
				JpmsGradlePlugin.trace(jigsaw.isDebug(), "classpath: %s", classesDir);

				String modulepath = classesDir+File.pathSeparator+task.getClasspath().getAsPath();
				JpmsGradlePlugin.trace(jigsaw.isDebug(), "modulepath: %s", modulepath);

				// find out if module path is already specified
				List<String> args = task.getJvmArgs();
				int idxFlag = args.indexOf(RUN_MODULE_PATH);

				if (idxFlag<0) {
					// module path is not specified, add it now
					args.add(RUN_MODULE_PATH);
					args.add(modulepath);
					JpmsGradlePlugin.trace(jigsaw.isDebug(), "task '%s': setting module-path to %s", task.getName(), modulepath.replaceAll(File.pathSeparator, "\n"));
					task.setJvmArgs(args);
				} else {
					// module path is already specified, don't change it
					JpmsGradlePlugin.trace(jigsaw.isDebug(), "task '%s': module-path already set", task.getName());
				}
        	});
	}

	private void multiReleaseJar() {
        Project project = getProject();

		JigsawExtension jigsaw = (JigsawExtension) project.getExtensions().getByName("jigsaw");

        // iterate over all Jar tasks
        project.getTasks()
            .withType(Jar.class)
            .forEach(task -> {
                JpmsGradlePlugin.trace(jigsaw.isDebug(), "%s", task);

                JpmsGradlePlugin.trace(jigsaw.isDebug(), "Setting Multi-Release attribute in manifest");
                task.getManifest().getAttributes().put("Multi-Release", "true");

                JpmsGradlePlugin.trace(jigsaw.isDebug(), "Fixing paths of module definitions");
                task.filesMatching("module-info.class", fileCopyDetails -> {
                    fileCopyDetails.setPath("META-INF/versions/9/"+fileCopyDetails.getName());
                });
            });
	}
}
