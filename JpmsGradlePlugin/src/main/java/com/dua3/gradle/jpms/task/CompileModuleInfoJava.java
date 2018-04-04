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
import java.util.spi.ToolProvider;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class CompileModuleInfoJava extends DefaultTask {
	
	@TaskAction
	public void compileModuleInfoJava() {
		Project project = getProject();

		// get the Java compiler
		ToolProvider javac = ToolProvider.findFirst("javac").
				orElseThrow(() -> new GradleException("could not get ToolProvider instance for javac."));

		// iterate over all JavaCompile tasks
        project.getTasks()
        	.withType(JavaCompile.class)
        	.forEach(task -> {
                JpmsGradlePlugin.trace("%s", task);
                
                // separate module definitions from other sources
                FileCollection moduleDefs = 
                		task.getSource().filter(f -> f.getName().equals("module-info.java"));
                JpmsGradlePlugin.trace("module definitions: %s", moduleDefs.getFiles());
                
                FileCollection sources = 
                		task.getSource().filter(f -> !f.getName().equals("module-info.java"));
                JpmsGradlePlugin.trace("other sources: %s", sources.getFiles());

                // before executing JavaCompile task, remove module definitions from task input
        		task.doFirst(t -> {
                    JpmsGradlePlugin.trace("removing module definitions from task input: %s", moduleDefs.getFiles());
                    JpmsGradlePlugin.trace("before: %s", t.getInputs().getSourceFiles().getFiles());
                    task.setSource(sources.getAsFileTree());
                    JpmsGradlePlugin.trace("after: %s", t.getInputs().getSourceFiles().getFiles());
        		});
        		
                // at last, compile the module definition with Java 9 compatibility
        		task.doLast(t -> {
                    JpmsGradlePlugin.trace("compiling module definitions for task");

                    // define directories
                    String classesDir = t.getOutputs().getFiles().getSingleFile().getPath();
                    String modulepath = classesDir;
                    
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
                    javac.run(System.out, System.err, compilerArgs.toArray(new String[0]));
        		});
        	});
 	}
}
