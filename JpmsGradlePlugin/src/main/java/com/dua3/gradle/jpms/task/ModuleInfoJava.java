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
import java.util.spi.ToolProvider;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class ModuleInfoJava extends DefaultTask {

	@TaskAction
	public void compileModuleInfoJava() {
		Project project = getProject();

        ModuleInfoExtension extension = (ModuleInfoExtension) project.getExtensions().getByName("moduleInfo");

		// get the Java compiler
		ToolProvider javac = ToolProvider.findFirst("javac").
				orElseThrow(() -> new GradleException("could not get ToolProvider instance for javac."));

		// Flag indicating whether module defs have to be removed from javadoc input (use AtomicBoolean because primitive cannot be set in lambda)
		AtomicBoolean needJavadocFix = new AtomicBoolean(false);

		// iterate over all JavaCompile tasks
        project.getTasks()
        	.withType(JavaCompile.class)
        	.forEach(task -> {
                JpmsGradlePlugin.trace("%s", task);

                // bail out if separate compilation is not needed for this task
                if (!isSeparateCompilationOfModuleDefNeeded(task)) {
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
                if (!extension.isMultiRelease()) {
                    needJavadocFix.compareAndSet(false, !moduleDefs.isEmpty());
                }

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
                    String classesDir = t.getOutputs().getFiles().getSingleFile().getPath();
                    String modulepath = classesDir+File.pathSeparator+task.getClasspath().getAsPath();
					JpmsGradlePlugin.trace("module-path: %s", modulepath .replaceAll(File.pathSeparator, "\n"));

					// prepare compiler arguments
                    List<String> compilerArgs = new LinkedList<>();
                    Collections.addAll(compilerArgs, "--release", "9");
                    Collections.addAll(compilerArgs, "--module-path", modulepath);
                    Collections.addAll(compilerArgs, "--add-modules", "ALL-SYSTEM");
                    Collections.addAll(compilerArgs, "-d", classesDir.toString());
                    moduleDefs.getFiles().stream().map(File::toString).forEach(compilerArgs::add);
					JpmsGradlePlugin.trace("compiler arguments: %s", compilerArgs);

					// start compilation
					TaskHelper.runTool(javac, project, compilerArgs);
        		});
        	});

        // fix Javadoc inputs (because javadoc will throw an exception for module definitions in Java 8 compatibility)
        if (needJavadocFix.get()) {
            JpmsGradlePlugin.trace("fixing javadoc");
        	fixJavaDoc();
        }

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

	private void fixJavaDoc() {
		Project project = getProject();

		// iterate over all JavaCompile tasks
        project.getTasks()
        	.withType(Javadoc.class)
        	.forEach(task -> {
                JpmsGradlePlugin.trace("%s", task);

                FileCollection inputs =
                		task.getSource().filter(f -> !f.getName().equals("module-info.java"));

                // before executing JavaCompile task, remove module definitions from task input
        		task.doFirst(t -> {
        			JpmsGradlePlugin.trace("remove module def from javadoc input: %s", t);
                    task.setSource(inputs.getAsFileTree());
        		});
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
