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

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class ModuleInfoJava extends DefaultTask {

	@TaskAction
	public void compileModuleInfoJava() {
		Project project = getProject();

        ModuleInfoExtension extension = (ModuleInfoExtension) project.getExtensions().getByName("moduleInfo");

		// Flag indicating whether module defs have to be removed from javadoc input (use AtomicBoolean because primitive cannot be set in lambda)
		AtomicBoolean hasModuleInfos = new AtomicBoolean(false);

		// iterate over all JavaCompile tasks
        project.getTasks()
        	.withType(JavaCompile.class)
        	.forEach(task -> {
                JpmsGradlePlugin.trace("%s", task);

				// bail out if separate compilation is not needed for this task
				boolean separateModules = isSeparateCompilationOfModuleDefNeeded(task);
                if (!separateModules) {
                    JpmsGradlePlugin.trace("task %s has target compatibility %s, separate compilation not needed", task, task.getTargetCompatibility());
                    return;
                }

                // separate module definitions from other sources
                FileCollection moduleDefs =
                		task.getSource().filter(f -> f.getName().equals("module-info.java"));
                JpmsGradlePlugin.trace("module definitions: %s", moduleDefs.getFiles());

                FileCollection sources =
                		task.getSource().filter(f -> !f.getName().equals("module-info.java"));
                JpmsGradlePlugin.trace("other sources: %s", sources.getFiles());

                // set needJavadocFix to true if needed
                hasModuleInfos.compareAndSet(false, !moduleDefs.isEmpty());

                // before executing JavaCompile task, remove module definitions from task input
        		task.doFirst(t -> {
                    JpmsGradlePlugin.trace("removing module definitions from task input");
                    task.setSource(sources.getAsFileTree());
        		});

                // at last, compile the module definition with Java 9 compatibility
        		task.doLast(t -> {
        			if (moduleDefs.isEmpty()) {
                        JpmsGradlePlugin.trace("task has no module def");
        				return;
        			}

                    JpmsGradlePlugin.trace("compiling module definitions for task");

                    // define directories
                    String classesDir = t.getOutputs().getFiles().getFiles().stream()
							.peek(f -> JpmsGradlePlugin.trace("inspecting output file: %s", f))
							.map(File::getPath)
							.reduce((s1,s2) -> {
								if (!s2.equals(s1)) {
									JpmsGradlePlugin.trace("output directory already set to %s. ignoring directory: %s", s1, s2);
								}
								return s1;
							}).orElseThrow(() -> new IllegalStateException("could not determine output directory"));
                    String modulepath = classesDir+File.pathSeparator+task.getClasspath().getAsPath();
					JpmsGradlePlugin.trace("module-path: %s", modulepath.replaceAll(File.pathSeparator, "\n"));

					// prepare compiler arguments
                    List<String> compilerArgs = new LinkedList<>();
                    Collections.addAll(compilerArgs, "--release", "9");
                    Collections.addAll(compilerArgs, "--module-path", modulepath);
                    Collections.addAll(compilerArgs, "-d", classesDir.toString());
                    moduleDefs.getFiles().stream().map(File::toString).forEach(compilerArgs::add);
					JpmsGradlePlugin.trace("compiler arguments: %s", compilerArgs);

					// start compilation
					TaskHelper.runTool(TaskHelper.JAVAC, project, compilerArgs);
        		});
        	});

		// fix Javadoc inputs (because javadoc will throw an exception for module definitions in Java 8 compatibility)
	    JpmsGradlePlugin.trace("fixing javadoc");
    	fixJavadocTasks(hasModuleInfos.get());

		// setup module path for run tasks
	    JpmsGradlePlugin.trace("fixing run tasks");
    	fixRunTasks(hasModuleInfos.get());

        // if target is a multi-release jar, move module definitions into the corresponding subfolder
        if (extension.isMultiRelease()) {
            JpmsGradlePlugin.trace("creating multi-release jar");
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

		// iterate over all Javadoc tasks
        project.getTasks()
        	.withType(Javadoc.class)
        	.forEach(task -> {
				JpmsGradlePlugin.trace("%s", task);

				if (removeModuleInfo) {
					// remove module-info.java from input
					FileCollection inputs =
								task.getSource().filter(f -> !f.getName().equals("module-info.java"));

						// before executing JavaCompile task, remove module definitions from task input
						task.doFirst(t -> {
							JpmsGradlePlugin.trace("remove module def from javadoc input: %s", t);
							task.setSource(inputs.getAsFileTree());
						});
				} else {
					// set module path
					// TODO cleanup duplicate code
                    String classesDir = task.getOutputs().getFiles().getSingleFile().getPath();
                    String modulepath = classesDir+File.pathSeparator+task.getClasspath().getAsPath();
					JpmsGradlePlugin.trace("module-path: %s", modulepath.replaceAll(File.pathSeparator, "\n"));

					CoreJavadocOptions options = (CoreJavadocOptions) task.getOptions();
					options.addStringOption("-module-path", modulepath);
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

		// iterate over all JavaExec tasks
        project.getTasks()
        	.withType(JavaExec.class)
        	.forEach(task -> {
				JpmsGradlePlugin.trace("%s", task);

				// determine class path entries and prepare module path
				String classesDir = task.getClasspath().getAsPath();
				JpmsGradlePlugin.trace("classpath: %s", classesDir);

				String modulepath = classesDir+File.pathSeparator+task.getClasspath().getAsPath();
				JpmsGradlePlugin.trace("modulepath: %s", modulepath);

				// find out if module path is already specified
				List<String> args = task.getJvmArgs();
				int idxFlag = args.indexOf(RUN_MODULE_PATH);

				if (idxFlag<0) {
					// module path is not specified, add it now
					args.add(RUN_MODULE_PATH);
					args.add(modulepath);
					JpmsGradlePlugin.trace("task '%s': setting module-path to %s", task.getName(), modulepath.replaceAll(File.pathSeparator, "\n"));
					task.setJvmArgs(args);
				} else {
					// module path is already specified, don't change it
					JpmsGradlePlugin.trace("task '%s': module-path already set", task.getName());
				}
        	});
	}

	private void multiReleaseJar() {
        Project project = getProject();

        // iterate over all Jar tasks
        project.getTasks()
            .withType(Jar.class)
            .forEach(task -> {
                JpmsGradlePlugin.trace("%s", task);

                JpmsGradlePlugin.trace("Setting Multi-Release attribute in manifest");
                task.getManifest().getAttributes().put("Multi-Release", "true");

                JpmsGradlePlugin.trace("Fixing paths of module definitions");
                task.filesMatching("module-info.class", fileCopyDetails -> {
                    fileCopyDetails.setPath("META-INF/versions/9/"+fileCopyDetails.getName());
                });
            });
	}
}
