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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.util.PatternFilterable;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class CompileModuleInfoJava extends DefaultTask {
	
	@TaskAction
	public void compileModuleInfoJava() {
		Project project = getProject();
		JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
		SourceSetContainer sourceSets = javaPlugin.getSourceSets();

        for (SourceSet sst: sourceSets) {
        	JpmsGradlePlugin.trace("sourceset: %s", sst);

            String name = sst.getName();
            PatternFilterable patternModuleInfo = new org.gradle.api.tasks.util.PatternSet();
            patternModuleInfo.include("**/module-info.java");
            if (!sst.getJava().matching(patternModuleInfo).isEmpty()) {
            	JpmsGradlePlugin.trace("%s has a module-info.java", name);

                // check Java version
                if (!JavaVersion.current().isJava9Compatible()) {
                    project.getLogger().error("At least Java 9 is needed if a module-info.java file is present.");
                    throw new GradleException("Java 9 is needed to compile module-info.java.");
                }

                // if sourceCompatibility is < Java9, exclude module-info.java from the
                // main sourceSet and compile it separately with compatibility set to Java9
                if (javaPlugin.getTargetCompatibility().isJava9Compatible()) {
                	JpmsGradlePlugin.trace("target compatibility is Java 9 or above");
                } else {
                    // define task names
                    String compileModuleInfo = "compileModuleInfo_"+name;

                    // define directories
                    Set<File> sources = sst.getJava().getSrcDirs();
                    File destination = sst.getOutput().getClassesDirs().getSingleFile();
                    FileCollection classes = sst.getCompileClasspath().plus(sst.getOutput());
                    String modulepath = classes.getAsPath()+File.pathSeparator+destination;

                    JpmsGradlePlugin.trace("creating compile task for module-info.java for sourceset %s", name);
                    Map<String,Object> options = new HashMap<>();
                    options.put("type", JavaCompile.class);
                    options.put("dependsOn", sst.getOutput());
                    JavaCompile t_module = (JavaCompile) project.task(options, compileModuleInfo);
                    t_module.doFirst(t -> {
                    	project.getLogger().info("Compiling module-info.java in Java 9 compatibility mode for sourceset {}", name);

                    	JpmsGradlePlugin.trace("module-path: %s", modulepath.replaceAll(File.pathSeparator, "\n"));
                    	JpmsGradlePlugin.trace("class-path: %s", sst.getCompileClasspath().getAsPath().replaceAll(File.pathSeparator, "\n"));
                        
                        String compilerArgs[] = {
                            "--module-path", modulepath,
                            "--add-modules", "ALL-SYSTEM",
                            "-d", destination.toString()
                        };
                        t_module.getOptions().setCompilerArgs(Arrays.asList(compilerArgs));
                    });
                    //inputs.property("moduleName", project.moduleName)

                    t_module.setSourceCompatibility("9");
                    t_module.setTargetCompatibility("9");

                    t_module.setSource(sources);
                    t_module.include("**/module-info.java");
                    t_module.setClasspath(classes);
                    t_module.setDestinationDir(destination);

                    for (Task task: project.getTasksByName("assemble", false)) {
                        JpmsGradlePlugin.trace("%s dependsOn %s", task, t_module);
                    	task.dependsOn(t_module);
                    }

                    JpmsGradlePlugin.trace("removing module-info.java from sourceset %s", name);
                    sst.getJava().exclude("**/module-info.java");
                }
            }
        }
 	}
}
