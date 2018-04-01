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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Internal;

import com.dua3.gradle.jpms.task.CompileModuleInfoJava;

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

    /**
     * Applies this plugin to the given Gradle project
     * @param project The Gradle project
     */
    public void apply(Project project) {
        trace("applying plugin %s", pluginname);
        
        Map<String,Object> options = new HashMap<>();
        options.put("type", CompileModuleInfoJava.class);
        CompileModuleInfoJava compileModuleInfo = (CompileModuleInfoJava) project.task(options, "compileModuleInfo");
        //compileModuleInfo.mustRunAfter("compileJava");
        
        for (Task task: project.getTasksByName("compileJava", false)) {
            trace("%s dependsOn %s", task, compileModuleInfo);
        	task.dependsOn(compileModuleInfo);
        }
    }
}