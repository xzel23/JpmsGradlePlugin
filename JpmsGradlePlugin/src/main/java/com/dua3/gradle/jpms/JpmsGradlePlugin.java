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

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

import com.dua3.gradle.jpms.task.ModuleInfoJava;
import com.dua3.gradle.jpms.task.JLink;
import com.dua3.gradle.jpms.task.JLinkExtension;
import com.dua3.gradle.jpms.task.ModuleInfoExtension;

public class JpmsGradlePlugin implements Plugin<Project>{

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
        	System.out.println("["+pluginname+"] "+msg);
        }
    }

    public static JavaVersion getJavaVersion() {
    	return JavaVersion.current();
    }

    /**
     * Applies this plugin to the given Gradle project
     * @param project The Gradle project
     */
    @Override
    public void apply(Project project) {
    	Gradle gradle = project.getGradle();    	
    	String gradleVersion = gradle.getGradleVersion();
    	
        trace("gradle version: %s", gradleVersion);
        
    	if (gradleVersion.compareTo("4.6")<0) {
    		project.getLogger().warn("Unknown Gradle version: {}", gradleVersion);
    		project.getLogger().warn("Plugin needs version 4.6 or above");
    	}
    	
        trace("applying plugin %s", pluginname);

        // create and automatically add moduleInfo task
        project.getLogger().info("Adding moduleInfo task to project");

        trace("creating moduleInfo extension");
        project.getExtensions().create("moduleInfo", ModuleInfoExtension.class);

        Map<String,Object> optionsModuleInfo = new HashMap<>();
        optionsModuleInfo.put("type", ModuleInfoJava.class);
        ModuleInfoJava moduleInfo = (ModuleInfoJava) project.task(optionsModuleInfo, "moduleInfo");

        for (Task task: project.getTasksByName("compileJava", false)) {
            trace("%s dependsOn %s", task, moduleInfo);
        	task.dependsOn(moduleInfo);
        }

        // move dependencies from classpath to modulepath
        project.getTasks()
    	.withType(JavaCompile.class)
    	.stream()
        .forEach(task -> task.doFirst(t -> {
    		JavaVersion version = JavaVersion.toVersion(task.getTargetCompatibility());
    		trace("task %s, target compatibility: %s", task, version);
    		
    		if (version.isJava9Compatible()) {
	            trace("moving entries from classpath to modulepath for task %s", task);
	        	CompileOptions options = task.getOptions();
	        	List<String> compilerArgs = new ArrayList<>(options.getAllCompilerArgs());
	        	compilerArgs.add("--module-path");
	        	compilerArgs.add(task.getClasspath().getAsPath());
	        	options.setCompilerArgs(compilerArgs);
	        	task.setClasspath(project.files());
    		}
        }));
        
        // add 'jlink' task
        project.getLogger().info("Adding jlink task to project");

        trace("creating jlink extension");
        project.getExtensions().create("jlink", JLinkExtension.class);

        trace("creating jlink task");
        Map<String,Object> optionsJLink = new HashMap<>();
        optionsJLink.put("type", JLink.class);
        JLink jlink = (JLink) project.task(optionsJLink, "jlink");
        jlink.dependsOn("build");
    }
}
